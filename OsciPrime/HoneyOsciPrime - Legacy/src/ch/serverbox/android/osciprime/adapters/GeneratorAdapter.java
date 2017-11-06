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
package ch.serverbox.android.osciprime.adapters;


import java.util.Arrays;

import ch.serverbox.android.osciprime.sources.SinusGenerator;
import ch.serverbox.android.osciprime.sources.TriggerProcessor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class GeneratorAdapter {

	private Looper mLooper = null;
	private Handler mHandler = null;
	private final SinusGenerator mGenerator; 
	private boolean mStop = false;
	private boolean mStopped = false;

	private final Object mLock = new Object();

	public GeneratorAdapter(SinusGenerator sinusGenerator){
		mGenerator = sinusGenerator;
		mAudioSamplingThread.start();
		mAudioSamplingThread.setName("GeneratorThread");
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
		mHandler.post(mGeneratorLoop);
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

	private Runnable mGeneratorLoop = new Runnable() {
		public void run() {
			final int BLOCK_SIZE = mGenerator.cBlocksize();
			int[] dataBufferCh1 = new int[BLOCK_SIZE/2];
			int[] dataBufferCh2 = new int[BLOCK_SIZE/2];

			for(;;){//this is the loop
				synchronized(mLock){
					if(mStop){//break condition
						mStopped = true;
						mLock.notifyAll();
						return;
					}						
				}
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					return;
				}
								
				float phase = System.currentTimeMillis()/100f; 
				//float phase = (float) (Math.random()*Math.PI*2.0f); 
				for(int i=0;i<BLOCK_SIZE/2;i++){
					dataBufferCh1[i] = (int) (12000*Math.sin(16*2*Math.PI*i/BLOCK_SIZE+phase));
					dataBufferCh2[i] = (i%(BLOCK_SIZE/16) > BLOCK_SIZE/32)?18000:0;
				}
				long t = System.currentTimeMillis(); 
				mGenerator.onNewSamples(dataBufferCh1, dataBufferCh2); 
				//Log.d("FEED-TIME", System.currentTimeMillis()-t+""); 
			}
		}
	};

	private void e(String msg){
		Log.e("AudioAdapter", ">==< "+msg+" >==<");
	}
	private void l(String msg){
		//Log.d("AudioAdapter", ">==< "+msg+" >==<");
	}
}
