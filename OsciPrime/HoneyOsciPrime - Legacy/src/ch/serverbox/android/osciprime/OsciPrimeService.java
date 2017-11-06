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

import ch.serverbox.android.osciprime.sources.AudioSource;
import ch.serverbox.android.osciprime.sources.SinusGenerator;
import ch.serverbox.android.osciprime.sources.SourceBase;
import ch.serverbox.android.osciprime.sources.SourceConfiguration;
import ch.serverbox.android.osciprime.sources.TriggerProcessor;
import ch.serverbox.android.osciprime.sources.UsbContinuousSource;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class OsciPrimeService extends Service {

	public static final byte BM_AMP_CH1 = 0x07;
	public static final byte BM_AMP_CH2 = 0x38;
	
	private Messenger mMessenger = null;
	private Messenger mActivityMessenger = null;

	private Handler mServiceHandler = null;
	private WorkerThread mWorkerThread = null;
	private SourceBase mCurrentSource = null;
	private OsciPreferences mPreferences = null;
	private int mStartId = -1;
	private boolean mRunning = false;

	public static final String THREAD_SERVICE = "Service Thread";

	@Override
	public void onCreate() { 
		super.onCreate();
		int src = sourceFromPreferences();
		initPreferences(src);//first thing to do...
		mWorkerThread = new WorkerThread("Service Worker");
		mServiceHandler = new ServiceHandler(mWorkerThread.getLooper());
		mMessenger = new Messenger(mServiceHandler);
		switchSource(src);
	}

	@Override
	public void onDestroy() {
		mWorkerThread.quit();
		//mGeneratorAdapter.quit();
		mCurrentSource.quit();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		l("onBind");
		return mMessenger.getBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mActivityMessenger = null;
		if(!mRunning){
			l("not running, stopping");
			stopSelf(mStartId);
		}
		return true;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		l("onRebind");
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		l("onStartCommand");
		mStartId = startId;
		return START_STICKY;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		l("onStart");
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		l("onConfigurationChanged");
	}
	private class ServiceHandler extends Handler{
		public ServiceHandler(Looper l){
			super(l);
		}
		@Override
		public void handleMessage(Message msg) {
			Message m = null;
			switch(msg.what){
			case OPC.AS_REGISTER_ACTIVITY:
				mActivityMessenger = msg.replyTo;
				l("register from "+Thread.currentThread().getName());
				try {
					msg.replyTo.send(Message.obtain(null, OPC.SA_LINKVIEW));
				} catch (RemoteException e2) {
				}
				pushPreferences();
				break;
			case OPC.BI_ECHO:
				l("echo from "+Thread.currentThread().getName());
				if(mActivityMessenger != null){
					m = Message.obtain(null, OPC.BI_ECHO);
					try {
						l("sending message ...");
						mActivityMessenger.send(m);
						l("... returned.");
					} catch (RemoteException e1) {
						e("Error sending Message to Activity");
						e1.printStackTrace();
					}   
				}else{
					e("Activity not registered in Service");
				} 
				break;
			case OPC.AS_START_SAMPLING: 
				if(mCurrentSource != null && mRunning != true){
					mRunning = true;
					mCurrentSource.loop();
				}
				break;
			case OPC.AS_STOP_SAMPLING:
				if(mCurrentSource != null && mRunning != false){
					mCurrentSource.stop();
				}
				mRunning = false;
				break;
			case OPC.AS_QUERY_STATE:
				m = Message.obtain(null, OPC.SA_ANSWER_STATE);
				if(mRunning)
					m.arg1 = OPC.STATE_RUNNING;
				else
					m.arg1 = OPC.STATE_IDLE;

				m.obj = (SourceConfiguration)mCurrentSource;

				if(mActivityMessenger != null){
					try {
						mActivityMessenger.send(m);
					} catch (RemoteException e) {
						e("error sending message SA_ANSWER_STATE to activity");
						e.printStackTrace();
					}
				}else{
					e("Activity Messenger is null");
				}
				break;
			case OPC.AS_SET_TRIGGER_LEVEL:
				//arg2 is the channel, 1 for ch1, 2 for ch2				
				if(msg.arg2 == TriggerProcessor.CHANNEL_1){
					mCurrentSource.setTriggerCh1(msg.arg1);
					mPreferences.setTriggerCh1(msg.arg1);
					//pushPreferences();
					savePreferences(mCurrentSource.cSourceId());
				}else{
					mCurrentSource.setTriggerCh2(msg.arg1);
					mPreferences.setTriggerCh2(msg.arg1);
					//pushPreferences();
					savePreferences(mCurrentSource.cSourceId());
				}
				break;
			case OPC.AS_SET_TRIGGER_CHANNEL:
				if(msg.arg1 == TriggerProcessor.CHANNEL_1){
					mCurrentSource.setChannel(TriggerProcessor.CHANNEL_1);
					mPreferences.setChannel(msg.arg1);
					//pushPreferences();
					savePreferences(mCurrentSource.cSourceId());
				}else{
					mCurrentSource.setChannel(TriggerProcessor.CHANNEL_2);
					mPreferences.setChannel(msg.arg1);
					//pushPreferences();
					savePreferences(mCurrentSource.cSourceId());
				}
				break;
			case OPC.AS_SET_INTERLEAVE:
				mCurrentSource.setInterleave(msg.arg1);
				mPreferences.setInterleave(msg.arg1);
				mPreferences.setInterleaveIndex(msg.arg2);
				savePreferences(mCurrentSource.cSourceId());
				//pushPreferences();
				break;
			case OPC.AS_SET_POLARITY:
				int i = mPreferences.getChannel();
				if(i==TriggerProcessor.CHANNEL_1){
					mCurrentSource.setPolarityCh1(msg.arg1);
					mPreferences.setPolarityCh1(msg.arg1);
					savePreferences(mCurrentSource.cSourceId());
					//pushPreferences();
				} else {
					mCurrentSource.setPolarityCh2(msg.arg1);
					mPreferences.setPolarityCh2(msg.arg1);
					savePreferences(mCurrentSource.cSourceId());
					//pushPreferences();
				}
				break;				
			case OPC.AS_SET_GAIN:
				//i = mPreferences.getChannel();
				i = msg.arg2;
				if(i==TriggerProcessor.CHANNEL_1){
					if(msg.arg1 < mCurrentSource.cGainTrippletsCh1().length){
						if(mCurrentSource.cGainTrippletsCh1()[msg.arg1] != null){
							byte mod = mCurrentSource.cGainTrippletsCh1()[msg.arg1].cfg;//F3
							byte ch2Cmd = mCurrentSource.cGainTrippletsCh2()[mPreferences.getGainCh2Index()].cfg;
							byte v = (byte)(ch2Cmd & ~BM_AMP_CH1);
							v |= mod & BM_AMP_CH1;
							mCurrentSource.sendCommand(v);
							mPreferences.setGainCh1Index(msg.arg1);
							savePreferences(mCurrentSource.cSourceId());
							//pushPreferences();
						}
					}else{
						mPreferences.setGainCh1Index(0);
						savePreferences(mCurrentSource.cSourceId());
						//pushPreferences();
					}

				} else {
					if(msg.arg1 < mCurrentSource.cGainTrippletsCh2().length){
						if(mCurrentSource.cGainTrippletsCh2()[msg.arg1] != null){
							byte mod = mCurrentSource.cGainTrippletsCh2()[msg.arg1].cfg;//F3
							byte ch1Cmd = mCurrentSource.cGainTrippletsCh1()[mPreferences.getGainCh1Index()].cfg;
							byte v = (byte)(ch1Cmd & ~BM_AMP_CH2);
							v |= mod & BM_AMP_CH2;
							mCurrentSource.sendCommand(v);
							mPreferences.setGainCh2Index(msg.arg1);
							savePreferences(mCurrentSource.cSourceId());
							//pushPreferences();
						}
					}else{
						mPreferences.setGainCh2Index(0);
						savePreferences(mCurrentSource.cSourceId());
						//pushPreferences();
					}
				} 
				break;
			case OPC.OS_SET_CALIBRATION_OFFSET:
				float ch1 = msg.getData().getFloat("ch1", 1);
				float ch2 = msg.getData().getFloat("ch2", 1);
				mPreferences.setCalibrationOffsetCh1(ch1);
				mPreferences.setCalibrationOffsetCh2(ch2);
				savePreferences(mCurrentSource.cSourceId());
				//pushPreferences();
				break;
			case OPC.AS_SET_SOURCE:
				switchSource(msg.arg1);
				break;
			case OPC.AS_SET_CHANNELVISIBLE:
				if(msg.arg2 == 1){
					mPreferences.setChannel1Visible((msg.arg1 == 0 ? false : true ));
					savePreferences(mCurrentSource.cSourceId());
				}else{
					mPreferences.setChannel2Visible((msg.arg1 == 0 ? false : true ));
					savePreferences(mCurrentSource.cSourceId());
				}
				break;
			case OPC.OS_SOURCE_NOT_AVAILABLE:
				mRunning = false;
				switchSource(OPC.SOURCE_GENERATOR);
				break;
			case OPC.AS_QUERY_PREFERENCES:
				pushPreferences();
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	}

	/**
	 * Crucial function to hand over a source
	 * Informs the VertexHolder and OsciTransformer
	 * about new Configuration and Preferences
	 * @param newSource
	 */
	private void switchSource(int newSource){
		l("switching source");
		if(mCurrentSource != null){
			if(mRunning){
				mCurrentSource.stop();
				mCurrentSource.quit();
				mRunning = false;
			}else{
				mCurrentSource.quit();
			}
		}
		sourceToPreference(newSource);
		if(mCurrentSource != null)
			savePreferences(mCurrentSource.cSourceId());
		initPreferences(newSource);
		switch(newSource){
		case OPC.SOURCE_AUDIO:
			mCurrentSource = new AudioSource(mServiceHandler,mPreferences);
			break;
		case OPC.SOURCE_GENERATOR:
			mCurrentSource = new SinusGenerator(mServiceHandler,mPreferences);
			break;
		case OPC.SOURCE_USB:
			mCurrentSource = new UsbContinuousSource(mServiceHandler,mPreferences);
			break;
		default:
			l("EXTREMEERROR!!!!!!!!!!!!!!!!!!!!!");
			return;
		}
		VertexHolder holder = VertexHolder.getVertexholder(mCurrentSource);
		holder.linkServiceHandler(mServiceHandler);
		holder.updateConfig(mCurrentSource, mPreferences);
		OsciTransformer.updateConfiguration(mCurrentSource, mPreferences);
		mServiceHandler.sendEmptyMessage(OPC.AS_QUERY_STATE);
	//	pushPreferences();
	}

	private void initPreferences(int source){
		SharedPreferences sp = getSharedPreferences("src"+source,MODE_PRIVATE);
		mPreferences = new OsciPreferences();
		mPreferences.setChannel(sp.getInt("channel", TriggerProcessor.CHANNEL_1));
		mPreferences.setInterleave(sp.getInt("interleave", 1));
		mPreferences.setPolarityCh1(sp.getInt("polarityCh1", TriggerProcessor.POLARITY_POSITIVE));
		mPreferences.setPolarityCh2(sp.getInt("polarityCh1", TriggerProcessor.POLARITY_POSITIVE));
		mPreferences.setTriggerCh1(sp.getInt("triggerCh1", 0));
		mPreferences.setTriggerCh2(sp.getInt("triggerCh2", 0));
		mPreferences.setGainCh1Index(sp.getInt("gainCh1Index", 0));
		mPreferences.setGainCh2Index(sp.getInt("gainCh2Index", 0));
		mPreferences.setInterleaveIndex(sp.getInt("interleaveIndex", 0));
		mPreferences.setCalibrationOffsetCh1(sp.getFloat("calibrationOffsetCh1", 0));
		mPreferences.setCalibrationOffsetCh2(sp.getFloat("calibrationOffsetCh2", 0));
		mPreferences.setChannel1Visible(sp.getBoolean("channel1Visible", true));
		mPreferences.setChannel2Visible(sp.getBoolean("channel2Visible", true));
	}

	private void savePreferences(int source){
		SharedPreferences sp = getSharedPreferences("src"+source,MODE_PRIVATE);
		Editor e = sp.edit();
		e.putInt("channel", mPreferences.getChannel());
		e.putInt("interleave", mPreferences.getInterleave());
		e.putInt("polarityCh1", mPreferences.getPolarityCh1());
		e.putInt("polarityCh1", mPreferences.getPolarityCh2());
		e.putInt("triggerCh1", mPreferences.getTriggerCh1());
		e.putInt("triggerCh2", mPreferences.getTriggerCh2());
		e.putInt("gainCh1Index", mPreferences.getGainCh1Index());
		e.putInt("gainCh2Index", mPreferences.getGainCh2Index());
		e.putInt("interleaveIndex", mPreferences.getInterleaveIndex());
		e.putFloat("calibrationOffsetCh1", mPreferences.getCalibrationOffsetCh1());
		e.putFloat("calibrationOffsetCh2", mPreferences.getCalibrationOffsetCh2());
		e.putBoolean("channel1Visible", mPreferences.isChannel1Visible());
		e.putBoolean("channel2Visible", mPreferences.isChannel2Visible());
		e.commit();

	}

	/**
	 * Returns the last saved source
	 * @return
	 */
	private int sourceFromPreferences(){
		SharedPreferences sp = getSharedPreferences("default",MODE_PRIVATE);
		return sp.getInt("source", OPC.SOURCE_GENERATOR);
	}

	private void sourceToPreference(int src){
		SharedPreferences sp = getSharedPreferences("default",MODE_PRIVATE);
		sp.edit().putInt("source", src).commit();
	}


	private void pushPreferences(){
		l("push preferences called");
		if(mActivityMessenger != null){
			try {
				mActivityMessenger.send(Message.obtain(null, OPC.SA_PREFERENCES_CHANGED, mPreferences));
			} catch (RemoteException e) {
				e.printStackTrace();
			}			
		}
		VertexHolder.getVertexholder(mCurrentSource).updateConfig(mCurrentSource, mPreferences);
		OsciTransformer.updateConfiguration(mCurrentSource, mPreferences);
	}

	private void e(String msg){
		Log.e("Service", ">==< "+msg+" >==<");
	}
	private void l(String msg){
		Log.d("Service", ">==< "+msg+" >==<");
	}
}
