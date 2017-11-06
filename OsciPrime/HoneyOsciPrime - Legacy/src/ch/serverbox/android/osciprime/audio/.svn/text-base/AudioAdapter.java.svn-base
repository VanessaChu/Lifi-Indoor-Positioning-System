/**
  * This file is part of OsciPrime
  *
  * Copyright (C) 2011 - Manuel Di Cerbo, Andreas Rudolf
  * 
  * Nexus-Computing GmbH, Switzerland 2011
  *
  * OsciPrime is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * OsciPrime is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with OsciPrime; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, 
  * Boston, MA  02110-1301  USA
  */
package ch.serverbox.android.osciprime.audio;

import java.util.Arrays;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class AudioAdapter {

	private Looper mLooper = null;
	private Handler mHandler = null;
	private boolean mStop = false;
	private boolean mStopped = false;
	private final ch.serverbox.android.osciprime.sources.AudioSource mAudioSource;

	private final Object mLock = new Object();

	public AudioAdapter(ch.serverbox.android.osciprime.sources.AudioSource audioSource){
		mAudioSource = audioSource;
		mAudioSamplingThread.start();
		synchronized (mLock) {			
			while(mHandler == null){
				try {
					mLock.wait();
				} catch (InterruptedException e) {
					e("can't wait for lock, interrupted");
					e.printStackTrace();
				}
			}
		}
	}

	public void startSampling(){
		mHandler.post(mAudioLoop);
	}
	
	public void stopSampling(){
		mStop = true;
		synchronized (mLock) {
			while(!mStopped){
				try {
					mLock.wait();
				} catch (InterruptedException e) {
					e("can't wait for lock");
					e.printStackTrace();
				}
			}
		}
		mStopped = false;//clean up for next time
		mStop = false;
		l("stopped");
	}

	public void quit(){
		l("quitting ...");
		if(mLooper ==  null)
			return;
		mLooper.quit();
		try {
			mAudioSamplingThread.join();
		} catch (InterruptedException e) {
			e("could not join AudioSamplingthread, interrupted");
			e.printStackTrace();
		}
		l("threads joined ...");
	}

	private Thread mAudioSamplingThread = new Thread(new Runnable() {
		public void run() {
			Looper.prepare();
			synchronized (mLock) {
				mHandler = new Handler();
				mLooper = Looper.myLooper();
				mLock.notifyAll();
			}
			Looper.loop();
		}
	});

	public static final int NEW_BYTE_SAMPLES = 99;
	public static final int NEW_SHORT_SAMPLES = 100;
	
	private Runnable mAudioLoop = new Runnable() {
		public void run() {
			//final int BLOCK_SIZE = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
			final int BLOCK_SIZE = mAudioSource.cBlocksize();
			l("Block size: "+BLOCK_SIZE);
			//ByteBuffer audioBuffer = ByteBuffer.allocateDirect(BLOCK_SIZE);
			short[] audioShortBuffer = new short[BLOCK_SIZE];
			AudioRecord rec = new AudioRecord(
					AudioSource.MIC,
					44100,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					16*BLOCK_SIZE
			);
			if(rec.getState() == AudioRecord.STATE_UNINITIALIZED){
				e("cannot initialzie audio");
				mAudioSource.unavailable();
				return;
			}
			rec.startRecording();
			int[] projection = new int[mAudioSource.cBlocksize()];
			for(;;){//this is the loop
				synchronized(mLock){
					if(mStop){//break condition
						mStopped = true;
						rec.stop();
						mLock.notifyAll();
						return;
					}						
				}
				if(rec.read(audioShortBuffer, 0, BLOCK_SIZE) == AudioRecord.ERROR_BAD_VALUE){
					e("read: bad value given");
					rec.stop();
					return;
				}
				
				for(int i  = 0; i < audioShortBuffer.length; i++){
					projection[i] = audioShortBuffer[i];
				}
				
				mAudioSource.onNewSamples(projection, new int[BLOCK_SIZE]);                        
				                                             
			}
		}
	};
	
	private void e(String msg){
		Log.e("AudioAdapter", ">==< "+msg+" >==<");
	}
	private void l(String msg){
		Log.d("AudioAdapter", ">==< "+msg+" >==<");
	}
}
