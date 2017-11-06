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
  */package ch.serverbox.android.osciprime.sources;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import ch.serverbox.android.osciprime.OPC;
import ch.serverbox.android.osciprime.OsciPrimeService;
import ch.serverbox.android.osciprime.VertexHolder;
import ch.serverbox.android.osciprime.adapters.GeneratorAdapter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TriggerProcessor {

	public static final int MSG_NEW_SAMPLES = 1;

	public static final int POLARITY_POSITIVE = 0;
	public static final int POLARITY_NEGATIVE = 1;
	
	public static final int CHANNEL_1 = 0;
	public static final int CHANNEL_2 = 1;

	private final Handler mSink;

	private int mTriggerCh1 = 0;
	private int mTriggerCh2 = 0;
	
	
	private int mInterleave;
	private int mPolarityCh1;
	private int mPolarityCh2;

	private int mChannel;
	private VertexHolder mVertexHolder;
	
	private boolean mQueueFull = false;

	private int[] mPreviousBuffer = new int[OPC.NUM_POINTS_PER_PLOT];
 
	private final SourceConfiguration mConfiguration;

	public TriggerProcessor(SourceConfiguration cfg, Handler sink){
		//TODO implement
		mConfiguration = cfg;
		mVertexHolder = VertexHolder.getVertexholder(mConfiguration);
		mSink = sink;
		mInterleave = cfg.cTimeDivisionPairs()[0].interleave;
	} 

	public void onFeed(ByteBuffer ch1, ByteBuffer ch2) {
		long t = System.currentTimeMillis();
		int tr = 0;
		if(mChannel == CHANNEL_1)
			tr = nativeTriggerBuffer(ch1, ch1.asIntBuffer().limit(), mTriggerCh1, (mPolarityCh1 == POLARITY_POSITIVE ? false : true));
		else
			tr = nativeTriggerBuffer(ch2, ch2.asIntBuffer().limit(), mTriggerCh2, (mPolarityCh2 == POLARITY_POSITIVE ? false : true));
		  
		int[] rch1 = nativeInterleaveBuffer(ch1, ch1.asIntBuffer().limit(), OPC.NUM_POINTS_PER_PLOT, tr, mInterleave);
		int[] rch2 = nativeInterleaveBuffer(ch2, ch2.asIntBuffer().limit(), OPC.NUM_POINTS_PER_PLOT, tr, mInterleave);
		//l("trigger lvl ch1: "+mTriggerCh1 + " limit: "+ch1.asIntBuffer().limit()+" index: "+tr);
		mVertexHolder.put(rch1, rch2);        
	}      
	    
	public void onFeed(int[] ch1, int[] ch2){   
		long t = System.currentTimeMillis();  
		int tr = 0; 
		if(mChannel == CHANNEL_1)
			tr = nativeTrigger(ch1, ch1.length, mTriggerCh1, (mPolarityCh1 == POLARITY_POSITIVE ? false : true));
		else
			tr = nativeTrigger(ch2, ch2.length, mTriggerCh2, (mPolarityCh2 == POLARITY_POSITIVE ? false : true));
		
		int[] rch1 = nativeInterleave(ch1, ch1.length, OPC.NUM_POINTS_PER_PLOT, tr, mInterleave);
		int[] rch2 = nativeInterleave(ch2, ch2.length, OPC.NUM_POINTS_PER_PLOT, tr, mInterleave);
			 
		mVertexHolder.put(rch1, rch2);
	} 

	private synchronized int trigger(int[] samples){
		int length = samples.length;
		int ref = length/2;//this is the reference, "best" expected trigger
		int lastSample = 0;
		int n;
		int i=0,j=0;  

		long bestCost = length*length;
		long curCost = 0; 
		int triggerIndex = 0;

		if(mPolarityCh1 == POLARITY_POSITIVE){
			for(i = 0; i < length; i++){
				n = samples[i];
				if(n >= mTriggerCh1 && lastSample < mTriggerCh1){
					curCost = Math.abs(i-ref);
					if(curCost < bestCost){
						bestCost = curCost;
						triggerIndex = i;
					}
				}
				lastSample = n;
			}
		}else{
			for(i = 0; i < length; i++){
				n = samples[i];
				if(n <= mTriggerCh1 && lastSample > mTriggerCh1){
					curCost = Math.abs(i-ref);
					if(curCost < bestCost){
						bestCost = curCost;
						triggerIndex = i;
					}
				}
				lastSample = n;
			}
		}
		return triggerIndex;
	}

	private int[] interleave(int trigger, int[] samples){
		int i = 0, j = 0;
		int destlen = OPC.NUM_POINTS_PER_PLOT;
		int nthVal = mInterleave;
		int buflen = samples.length;
		int[] dest = new int[destlen];

		int start = trigger-destlen/2*nthVal;
		int stop = trigger+destlen/2*nthVal;
		if(start < 0){
			start = 0;//no trigger found
		}
		if(stop > buflen-1){
			start = 0;//trigger too far away
		}

		j = start;
		for(i = 0; i < destlen; i++){
			dest[i] = samples[j];
			j += nthVal;
			if(j > buflen-1){//overflow .... what to do?
				return new int[OPC.NUM_POINTS_PER_PLOT];//TODO Throw an Exception?
			}
		}
		return dest;
	}

	public synchronized void setTriggerCh1(int t){
		mTriggerCh1 = t;
	}
	
	public synchronized void setTriggerCh2(int t){
		mTriggerCh2 = t;
	}
	  
	public void setChannel(int ch){
		mChannel = ch; 
	}  

	
	public void setPolarityCh1(int polarity){
		mPolarityCh1 = polarity;
	}
	
	public void setPolarityCh2(int polarity) {
		mPolarityCh2 = polarity;
	}


	public void setInterleave(int n){
		mInterleave = n;
	}
	
	//#define RISING_EDGE 0
	private native int nativeTrigger(int[] arr, int len, int tr, boolean edge);
	private native int nativeTriggerBuffer(ByteBuffer arr, int len, int tr, boolean edge);
	private native int[] nativeInterleave(int[] arr, int buflen, int destlen, int trigger, int nthVal);
	private native int[] nativeInterleaveBuffer(ByteBuffer arr, int buflen, int destlen, int trigger, int nthVal);
	static{
		System.loadLibrary("trigger");
	}
	
	
    private void l(Object s){
    	Log.d(getClass().getSimpleName(), ">==< "+s.toString()+" >==<");
    }




}
