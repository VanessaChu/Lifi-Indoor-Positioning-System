package com.example.vanessachu.lifi;

/**
 * Created by vanessachu on 1/30/17.
 */
import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;


/**
 * Created by vanessachu on 1/30/17.
 */
public class RecordInput implements Runnable{
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder;
    private final Receiver mReceiver;

    public RecordInput(Receiver theReceiver){
        mReceiver = theReceiver;
    }

    /*
    ** Start recording the audio files
    */
    @Override
    public void run() {
        //System.out.println("RecordInput hello");

        short sData[] = new short [AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING)];

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING));

        recorder.startRecording();

        while(true){
            if(recorder.read(sData, 0, AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                    RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING)) == AudioRecord.ERROR_BAD_VALUE){
                recorder.stop();
                return;
            }

            for(int i = 0; i < sData.length; i++){
                mReceiver.setNextSample(sData[i]);
                //System.out.println("RecordInput newData = " + sData[i] );
            }
        }
    }
}
