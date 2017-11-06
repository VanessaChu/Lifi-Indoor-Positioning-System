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
package ch.serverbox.android.osciprime.hw;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import ch.serverbox.android.osciprime.sources.UsbContinuousSource;

import android.util.Log;

/**
 * @deprecated
 * @author mdc
 *
 */
public class UsbContinuousInterface{
    private native void startSampling();
    private native void stopSampling();
    private native void sendCommand(char cmd);
    
    private final UsbContinuousSource mContinuousSource;
    

    
    public void callbackContinous(ByteBuffer ch1, ByteBuffer ch2){
    	//Log.d("callback","callback");
    	mContinuousSource.callback(ch1, ch2);
    	return;
    }    
    
    public void callbackContinous(int[] ch1, int[] ch2){
    	//mContinuousSource.callback(ch1, ch2);
    }  
     
    public UsbContinuousInterface(UsbContinuousSource usbContinuousSource){
    	mContinuousSource = usbContinuousSource;
    	l("constructor");
    }
    
    public void start(){ 
    	l("startSampling");
    	FxLoader.loadHex("/fifo.hex");
    	try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {}
    	startSampling();
    }
    
    
    public void stop(){
    	l("stopSampling");
    	stopSampling();
    }
    
    public void command(char cmd){ 
    	sendCommand(cmd);
    }
     
    
    static { 
    	System.loadLibrary("usb");
        System.loadLibrary("oscicontinuous");
    }
    
    private void l(Object s){
    	Log.d(getClass().getSimpleName(), ">==<" +s.toString()+" >==<");
    }
}
