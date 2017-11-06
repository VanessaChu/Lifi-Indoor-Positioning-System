/*
LiFi Emitter and Receiver
The purpose of this demos is to demonstrate data communication using a pair of blue LED (one led as emitter one led as receiver). 
Communication can go at up to 600bs (can depend on led quality) 
Hardware is the following :
I/O 13 ------------- led -------------- GND
Using a blue led should not require resistor, one may be needed for red or green
A byte is sent as follow :
Start(0) 8bit data Stop(1)
 
Each bit is coded in manchester with 
0 -> 10
1 -> 01
A data frame is formatted as follow :
0xAA : sent a number of time to help the received compute a signal average for the thresholding of analog values
0xD5 : synchronization byte to break preamble
N times Effective data excluding command symbols, with N < 32
0x03 : ETX end of frame
*/

#include <TimerOne.h>
#include <util/atomic.h>
//Start of what should be an include ...

// change to alter communication speed, 
// will lower values will result in faster communication
// the receiver must be tuned to the same value
#define SYMBOL_PERIOD 500 

#define WORD_LENGTH 10
#define SYNC_SYMBOL 0xD5
#define ETX 0x03

//Fast manipulation of LED IO. 
#define OUT_LED() DDRD |= ((1 << 2))
#define SET_LED() PORTD |= ((1 << 2))
#define CLR_LED() PORTD &= ~((1 << 2))

unsigned char frame_buffer [38] ; //buffer for frame
char frame_index = -1; // index in frame
char frame_size = -1  ; // size of the frame to be sent

//state variables of the manchester encoder
unsigned char bit_counter = 0 ;
unsigned short data_word = 0 ;  //8bit data + start + stop
unsigned char half_bit = 0 ;
unsigned long int manchester_data ;

/*
 * This function converts the normal data into manchester coding and add start/stop symbol in the code.
 *
 */
void to_manchester(unsigned char data, unsigned long int * data_manchester){
  unsigned int i ;
 (*data_manchester) = 0x02 ; // STOP symbol
 (*data_manchester) = (*data_manchester) << 2 ;
  for(i = 0 ; i < 8; i ++){
    if(data & 0x80) (*data_manchester) |=  0x02  ; // data LSB first
    else (*data_manchester) |= 0x01 ;
    (*data_manchester) = (*data_manchester) << 2 ;
    data = data << 1 ; // to next bit
  }
  (*data_manchester) |= 0x01 ; //START symbol
}

/* 
 *  This is the emitter interrupt of our code.
 */
void emit_half_bit(){
     if(manchester_data & 0x01){
       SET_LED();
     }else{
       CLR_LED();
     }
     bit_counter -- ;
     manchester_data = (manchester_data >> 1);
     if(bit_counter == 0){   
        //is there still bytes to send in the frame ?
        manchester_data = 0xAAAAAAAA ; // keep sending ones if nothing to send
        if(frame_index >= 0 ){
          if(frame_index < frame_size){
            to_manchester(frame_buffer[frame_index], &manchester_data);
            frame_index ++ ;
          }else{
            frame_index = -1 ;
            frame_size = -1 ;
          }
        }
        bit_counter = WORD_LENGTH * 2 ;
      }
}

void init_frame(unsigned char * frame){
  memset(frame, 0xAA, 3);
  frame[3] = SYNC_SYMBOL ;
  frame_index = -1 ;
  frame_size = -1 ;
}

int create_frame(char * data, int data_size, unsigned char * frame){
  memcpy(&(frame[4]), data, data_size);
  frame[4+data_size] = ETX;
  return 1 ;
}


int write(char * data, int data_size){
  if(frame_index >=  0) return -1 ;
  if(data_size > 32) return -1 ;
  create_frame(data, data_size,frame_buffer);
  ATOMIC_BLOCK(ATOMIC_RESTORESTATE){
    frame_index = 0 ;
    frame_size = data_size + 5;
  }
  return 0 ;
}

int transmitter_available(){
  if(frame_index >=  0) return 0 ;
  return 1 ; 
}

void init_emitter(){
  manchester_data = 0xFFFFFFFF ;
  bit_counter = WORD_LENGTH * 2 ;
}

// the setup routine runs once when you press reset:
void setup() {
  // initialize serial communication at 115200 bits per second:
  Serial.begin(115200);
  OUT_LED();
  init_frame(frame_buffer);
  init_emitter();
  Timer1.initialize(SYMBOL_PERIOD); //1200 bauds
  Timer1.attachInterrupt(emit_half_bit); 
}


/*
 * This defines the message we are sending. A recursive number char is added at the end of the message, showing the update of the message.
 *  
 */
char * msg = "01234567890" ;
char com_buffer [32] ;
void loop() {
    static int i = 0 ;
    memcpy(com_buffer, msg, 11);
    com_buffer[11] = i + '0' ;
    if(write(com_buffer, 12) < 0){
      delay(10);
    }else{
      i ++ ; 
      if(i > 9) i = 0 ;
    }
}
