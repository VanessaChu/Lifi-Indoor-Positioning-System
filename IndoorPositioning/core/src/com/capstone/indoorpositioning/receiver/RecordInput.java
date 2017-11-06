package com.capstone.indoorpositioning.receiver;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.AudioRecorder;

import javax.sound.sampled.AudioFormat;


/**
 * Created by JackH_000 on 2017-03-30.
 */

public class RecordInput implements Runnable {
//    private static final int RECORDER_SAMPLERATE = 8000;
//    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
//    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
//    private AudioRecord recorder;
    private static boolean isRecording = false;
    private AudioRecorder recorder;
    private final Receiver mReceiver;

    public RecordInput(Receiver theReceiver){
        mReceiver = theReceiver;

    }

    /*
    ** Start recording the audio files
    */

    @Override
    public void run() {
        short sData[] = new short [200];

        recorder = Gdx.audio.newAudioRecorder(8000, true);

        Gdx.app.log("AudioRecorder", "Recorder initialized");

        isRecording = true;

        while (isRecording){
            recorder.read(sData, 0, sData.length);

            for(int i = 0; i < sData.length; i++){
                mReceiver.setNextSample(sData[i]);
//                Gdx.app.log("AudioData", "Audio Data: " + sData[i]);
            }
        }

        recorder.dispose();
    }

    public void dispose(){
        isRecording = false;
    }
}
