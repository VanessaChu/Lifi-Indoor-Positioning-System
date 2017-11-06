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
package ch.serverbox.android.osciprime;

import android.os.Looper;
import android.util.Log;

public class WorkerThread implements Runnable{
	private Looper mLooper = null;
	private Object mLock = new Object();

	public WorkerThread(String name){
		Thread mWorkerThead = new Thread(null, this, name);
		mWorkerThead.start();
		synchronized (mLock) {
			while(mLooper == null){
				try {
					mLock.wait();
				} catch (InterruptedException e) {
					e("Interrupted while waiting for lock");
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void run() {
		Looper.prepare();
		synchronized(mLock){
			mLooper = Looper.myLooper();
			mLock.notifyAll();
		}
		Looper.loop();
	}

	public void quit(){
		mLooper.quit();
	}

	public Looper getLooper(){
		return this.mLooper;
	}

	private void e(String msg){
		Log.e("Worker", ">==< "+msg+" >==<");
	}
}
