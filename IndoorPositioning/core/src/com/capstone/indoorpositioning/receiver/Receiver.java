package com.capstone.indoorpositioning.receiver;


import com.capstone.indoorpositioning.entities.User;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * LiFi Emitter and Receiver
 * The purpose of this demos is to demonstrate data communication using a pair of blue LED (one led as emitter one led as receiver).
 * Communication can go at up to 600bs (can depend on led quality)
 * Receiver hardware :
 * |----1Mohm-----|
 * A0 ------|--- +led- ----|-------GND
 * A byte is sent as follow :
 * Start(0) 8bit data Stop(1), LSB first : 0 b0 b1 b2 b3 b4 b5 b6 b7 1
 *
 * Each bit is coded in manchester with
 * time is from left to right
 * 0 -> 10
 * 1 -> 01
 * A data frame is formatted as follow :
 * 0xAA : sent a number of time to help the receiver compute a signal average for the thresholding of analog values
 * 0xD5 : synchronization byte to indicate start of a frame, breaks the regularity of the 0x55 pattern to be easily
 * N times Effective data excluding command symbols, max length 32 bytes
 * 0x03 : ETX end of frame
 */

class frame {
    boolean is_busy      = false;
    short[]  frame_buffer = new short[38];
    int     frame_index  = -1;
    int     frame_size   = -1;
}

class word {
    long manchester_word;
    long detected_word;   //Notice: we do not have unsigned int in Java. The mask should be altered
    int  time_from_last_sync;
}

public class Receiver implements Runnable
{
    //public static final int  WORD_LENGTH  = 10;
    private static final int  SYNC_SYMBOL  = 0xD5;
    private static final int  ETX          = 0x03;
    private static final int  SAMPLE_SYMB  = 4;
    private static final int  START_SYMBOL = 0x02;
    //private static final int  START_SYMBOL = 0x01;
    private static final int  STOP_SYMBOL  = 0x01;
    //private static final int  STOP_SYMBOL  = 0x02;
    private static final int  MASK         = 0x003C0003;
    private static final int  MASK_CHECK   = ((STOP_SYMBOL << 20) | (START_SYMBOL << 18) | STOP_SYMBOL);
    private static final long SYNC_MANCHES = 0x00006665;
    //private static final long SYNC_MANCHES = 0x0000999A;
    private static final int  EDGE_THRESH  = 4100;                   //8650;        //Modification is needed.

    private static frame state_check = new frame();
    private static word word_check = new word();
    private int[] currentFrame = new int[38];

    private Queue<Short> QueueSamples = new LinkedList<Short>();

    private User user;

    public Receiver(User user){
        this.user = user;
    }

    public synchronized int getCurrentFrame(int index){
        return currentFrame[index];
    }

    public void setNextSample(short sample){
        synchronized (this){
            this.QueueSamples.add(sample);
        }
    }

    private int is_a_word(word word_check, frame state_check){
        //System.out.println(Long.toBinaryString(word_check.manchester_word));
        if(word_check.time_from_last_sync >= 20  || !state_check.is_busy){                                         // we received enough bits to test the sync

            if((word_check.manchester_word & MASK) == MASK_CHECK){                          // testing first position
                //System.out.println("manchester_check result = " + Long.toBinaryString(word_check.manchester_word & MASK) + "     Manchester Word = " + Long.toBinaryString(word_check.manchester_word));
                word_check.detected_word = (word_check.manchester_word >> 2) & 0x0000FFFF;                  // we need to get rid of the stop symbol
                if(!state_check.is_busy){
                    //System.out.println(Long.toBinaryString(word_check.detected_word));
                    if((word_check.detected_word) == SYNC_MANCHES) {
                        //System.out.println("synchronized!!!!!!!!");
                        return 2 ;                        // the sync byte is detected
                    }
                }
                return 1 ;
                // byte with correct framing
            }
        }
        return 0 ;
    }

    private int insert_edge(word word_check, frame state_check, int edge, int edge_period){
        int check_word = 0 ;
        int is_a_word_value = 0;
        int sync_word_detect = 0 ;
        //if( ( (word_check.manchester_word & 0x01) == 1 && edge != 1 ) || ( (word_check.manchester_word & 0x01) == 0 && edge != -1 ) ){ //make sure we don't have same edge ...
        if( (word_check.manchester_word & 0x01) != edge ){ //make sure we don't have same edge ...
            if(edge_period > (SAMPLE_SYMB+1)){
                int last_bit = (int) word_check.manchester_word & 0x0001;
                word_check.manchester_word  = (word_check.manchester_word  << 1) | last_bit ; // signal was steady for longer than a single symbol,
                word_check.time_from_last_sync += 1 ;
                is_a_word_value = is_a_word(word_check, state_check);
                if(is_a_word_value > 0){ //found start stop framing
                    check_word = 1 ;
                    word_check.time_from_last_sync =  0 ;
                    if(is_a_word_value > 1) {
                        sync_word_detect = 1 ; //we detected framing and sync word in manchester format
                        //System.out.println("synchronized");
                    }
                }else{
                    check_word = -1 ;
                }
            }
            //storing edge value in word
            if(edge < 0){
                word_check.manchester_word  = ( word_check.manchester_word  << 1)  ; // signal goes down
            }else{
                word_check.manchester_word  = ( word_check.manchester_word  << 1) | 0x01 ; // signal goes up
            }
            word_check.time_from_last_sync += 1;

            is_a_word_value = is_a_word(word_check, state_check);

            if(sync_word_detect == 0 && is_a_word_value > 0){ //if sync word was detected at previous position, don't take word detection into account
                check_word = 1;
                word_check.time_from_last_sync =  0;
            }
        }else{
            check_word = -1;
        }

        return check_word;
    }

    private static short oldValue = 0;
    private static int steady_count = 0;
    private static int old_edge;

    private void sample_signal_edge(){
        int edge =0;

        // ADD METHOD TO RETURN AUDIO INPUT VALUE!!!!!!!!!!!
        short sensorValue;

        while(QueueSamples.peek() == null){

        }

        synchronized (this){
            //sensorValue =  nextSample;// read result of previously triggered conversion
            sensorValue = QueueSamples.poll();
        }
        //System.out.println("sensorValue: " + sensorValue);

        /*
        if ((sensorValue - oldValue) > EDGE_THRESH)
            edge = 1;
        else if ((oldValue - sensorValue) > EDGE_THRESH)
            edge = -1;
        else
            edge = 0;
        */
        /*
        if(sensorValue >= 0){
            edge = 1;
        }
        else if(sensorValue < 0){
            edge = -1;
        }
        else{
            edge = 0;
        }
        */

        if(sensorValue >= 0){
            if ((sensorValue - oldValue) > EDGE_THRESH)
                edge = 1;
        }
        else if(sensorValue < 0){
            if ((oldValue - sensorValue) > EDGE_THRESH)
                edge = -1;
        }
        else{
            edge = 0;
        }

        oldValue = sensorValue;

        if (edge == 0 || edge == old_edge || steady_count < 2){
            if (steady_count < (4 * SAMPLE_SYMB)){
                steady_count++;
            }
        }
        else{
            if(insert_edge(word_check, state_check, edge, steady_count) == 1) {
                //System.out.println("New word!!!!!!! Flag = " + state_check.is_busy);
                decode(word_check, state_check);
            }

            if (word_check.time_from_last_sync > (8 * SAMPLE_SYMB)){ // limit dist_last_sync to avoid overflow problems
                word_check.time_from_last_sync = 32;
            }
            steady_count = 0;
        }
        old_edge = edge;
    }

    private int add_byte_to_frame(frame state_update, short data){
        //System.out.println("Data = " + Short.toString(data));

        if(data == SYNC_SYMBOL){
            state_update.frame_index = 0 ;
            state_update.frame_size = 0 ;
            state_update.is_busy = true ;
            return 0;
        }
        if(state_update.is_busy){ // we are synced
            state_update.frame_buffer[state_update.frame_index] = data ;
            state_update.frame_index ++ ;
            if(data == ETX){
//                for(int i = 0; i <= state_update.frame_index; i++) {
                for(int i = 0; i < 8; i++){
                    currentFrame[i] = state_update.frame_buffer[i] - 48;
                    user.setLocationData(state_update.frame_buffer[i] - 48, i);
                }
                user.setUpdateLocation(true);

                state_update.frame_size = state_update.frame_index ;
                state_update.frame_index = -1 ;
                state_update.is_busy = false;
                return 1;
            }else if(state_update.frame_index >= 38){ //frame is larger than max size of frame ...
                state_update.frame_index = -1 ;
                state_update.frame_size = -1 ;
                state_update.is_busy = false;
                return -1;
            }
            return 0;
        }
        return -1;
    }

    private void decode(word word_check, frame state_check) {
        int i;
        short received_data;

        received_data = 0;
        for (i = 0; i < 16; i = i + 2) { //decoding Manchester
            received_data = (short)(received_data << 1);
            if (((word_check.detected_word >> i) & 0x03) == 0x01) {
                received_data |= 0x01;
            } else {
                received_data &= ~0x01;
            }
        }
        //System.out.println("Detected data = " + word_check.detected_word + "         Received data = " + received_data);
        //System.out.println("Received data = " + received_data);

        received_data = (short)(received_data & 0xFF);
        if ((received_data < 0x0030 || received_data > 0x0039) && received_data != SYNC_SYMBOL && received_data != ETX) {
            received_data = '\0';
            state_check.is_busy = false;
        }

        //System.out.println("Received data take 2 = " + received_data);

        if ((add_byte_to_frame(state_check, received_data)) > 0){
            state_check.frame_buffer[state_check.frame_size - 1] = '\0';
            //System.out.println(state_check.frame_buffer[0]);
        }
    }

    @Override
    public void run() {
        try{
            //System.out.println("Hi from receiver run");
            //long start = System.nanoTime()/1000;
            //System.out.println(start);
            sample_signal_edge();


        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}