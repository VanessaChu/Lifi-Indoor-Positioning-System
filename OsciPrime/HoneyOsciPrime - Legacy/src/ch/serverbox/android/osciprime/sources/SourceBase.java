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
package ch.serverbox.android.osciprime.sources;

import java.nio.ByteBuffer;

import ch.serverbox.android.osciprime.OPC;
import ch.serverbox.android.osciprime.OsciPreferences;
import ch.serverbox.android.osciprime.OsciTransformer;
import android.os.Handler;

public abstract class SourceBase implements SourceConfiguration{

	private final Handler mSink;
	private final TriggerProcessor mProcessor;
	protected final OsciPreferences mPreferences;
	
	public SourceBase(){
		mSink = null;
		mProcessor = null;
		mPreferences = null;
	}
	
	public SourceBase(Handler sink, OsciPreferences preference){
		mProcessor = new TriggerProcessor(this, sink);
		mPreferences = preference;
		mSink = sink;
		mProcessor.setInterleave(preference.getInterleave());
		mProcessor.setTriggerCh1(preference.getTriggerCh1());
		mProcessor.setTriggerCh2(preference.getTriggerCh2());
		mProcessor.setPolarityCh1(preference.getPolarityCh1());
		mProcessor.setPolarityCh2(preference.getPolarityCh2());
		OsciTransformer.create(this, preference);//each time a source is created 
	}
	
	public abstract void loop();
	public abstract void stop();
	public abstract void quit();
	public abstract void sendCommand(byte cmd);
	
	public void unavailable(){
		mSink.sendEmptyMessage(OPC.OS_SOURCE_NOT_AVAILABLE);
	}
	
	public void onNewSamples(int[] ch1, int[] ch2){
		mProcessor.onFeed(ch1, ch2);
	}	
	
	public void onNewSamples(ByteBuffer ch1, ByteBuffer ch2){
		mProcessor.onFeed(ch1, ch2);
	}	
	public synchronized void setTriggerCh1(int t){
		mProcessor.setTriggerCh1(t);
	}
	
	public synchronized void setTriggerCh2(int t){
		mProcessor.setTriggerCh2(t);
	}
	  
	public void setChannel(int ch){
		mProcessor.setChannel(ch);
	}  

	public void setPolarityCh1(int polarity){
		mProcessor.setPolarityCh1(polarity);
	}

	public void setInterleave(int n){
		mProcessor.setInterleave(n);
	}

	public void setPolarityCh2(int polarity) {
		mProcessor.setPolarityCh2(polarity);
	}
	
}
