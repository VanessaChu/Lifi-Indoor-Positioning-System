
    /**
    OsciPrime an Open Source Android Oscilloscope
    Copyright (C) 2012  Manuel Di Cerbo, Nexus-Computing GmbH Switzerland
    Copyright (C) 2012  Andreas Rudolf, Nexus-Computing GmbH Switzerland

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    */

package ch.nexuscomputing.android.osciprimeics;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication.SourceType;
import ch.nexuscomputing.android.osciprimeics.draw.BufferPreview;
import ch.nexuscomputing.android.osciprimeics.sink.NetworkSink;
import ch.nexuscomputing.android.osciprimeics.source.Logger;
import ch.nexuscomputing.android.osciprimeics.source.NetworkSource;
import ch.nexuscomputing.android.osciprimeics.source.OsciAudioSource;
import ch.nexuscomputing.android.osciprimeics.source.OsciUsbSource;
import ch.nexuscomputing.android.osciprimeics.source.Source;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class OsciPrimeService extends Service implements IServiceSinkInterface {
	private OsciPrimeApplication mApplication;

	private static final int NOTIFICATION_ID = 1337;
	protected static final int RUNNING = 0;
	protected static final int IDLE = 1;
	protected static final int TERMINATING = 2;

	protected static final int REGISTER = 1;
	protected static final int START = 2;
	protected static final int STOP = 3;
	protected static final int RESAMPLE = 4;
	protected static final int ATTENUATION_CHANGED = 5;
	protected static final int CALIBRATE = 6;
	protected static final int START_SINK = 7;
	protected static final int STOP_SINK = 8;

	private static Notification sNotification = new Notification();
	private int mStartId;
	private int mState = IDLE;

	private AtomicBoolean mCalibrateNextTime = new AtomicBoolean(false); 

	private Messenger mActivityMessenger;
	private final Messenger mServiceMessenger = new Messenger(new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case REGISTER:
				mActivityMessenger = msg.replyTo;
				status();
				break;
			case START:
				if (mState != RUNNING) {
					mState = RUNNING;
				} else {
					L.e("start called while already running, abort...");
					return;
				}
				L.d("allocating some memory");
				//this is for a later update
				//Logger.allocate();
				status();
				start();
				attenuationChanged();
				break;
			case STOP:
				if (mState == TERMINATING)
					return;
				mState = TERMINATING;
				stop();
				break;
			case RESAMPLE:
				if(msg.arg1 == -1)
					resample();
				else{
					resample(msg.arg1);//trigger index in msg.arg1
				}
				break;
			case ATTENUATION_CHANGED:
				attenuationChanged();
				if (mCurrentSource != null) {
					mCurrentSource.attenuationChanged(mApplication);
				}
				resample();
				break;
			case CALIBRATE:
				mCalibrateNextTime.set(true);
				if (mNetworkSink != null)
					mNetworkSink.calibrate();
				break;
			case START_SINK:
				L.d("starting nw sink");
				mApplication.pNetworkSinkSate = OsciPrimeApplication.NETWORK_SINK_DISCONNECTED;
				if (mNetworkSink == null) {
					mNetworkSink = new NetworkSink(mApplication,
							OsciPrimeService.this);
				}
				break;
			case STOP_SINK:
				L.d("stopping nw sink");
				if(mNetworkSink != null && mApplication.pNetworkSinkSate != OsciPrimeApplication.NETWORK_SINK_DISABLED){
					mNetworkSink.stop();
					mNetworkSink = null;
					mApplication.pNetworkSinkSate = OsciPrimeApplication.NETWORK_SINK_DISABLED;
				}

				break;
			default:
				break;
			}
		};
	});

	private void status() {
		try {
			mActivityMessenger.send(Message.obtain(null,
					OsciPrimeICSActivity.STATUS, mState, -1));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private NetworkSink mNetworkSink;

	public void onCreate() {
		mApplication = (OsciPrimeApplication) getApplicationContext();
	};

	private float[] CH1_CALIBRATION = new float[4];
	private float[] CH2_CALIBRATION = new float[4];
	private int mLastSettingWhileRunningCh1 = 0;
	private int mLastSettingWhileRunningCh2 = 0;

	/**
	 * IMPORTANT!!! The attenuation is only saved to preferences if the source
	 * is running. Maybe also deny the user access to the calibration bars while
	 * the scope is idle..
	 * 
	 * So this method deals with changing the attenuation (setting the voltage
	 * divisions accordingly as well ass the attenuation values). Now if the USB
	 * scope is idle, hardware attenuation will not take place. This is why a
	 * correction factor for the attenuation has to be accounted for, depending
	 * on the attenuation on measurement and the currently active attenuation
	 * setting.
	 * 
	 * the factor is : f = l * dl/dc
	 * 
	 * l = attenuation of setting of measurement, dl = voltage division of
	 * measured setting dc = voltage division of current setting
	 * 
	 */
	@Override
	public void attenuationChanged() {
		if (mCurrentSource != null) {
			mApplication.pVoltageDivCh1 = mCurrentSource.getVoltageDivsion(
					OsciPrimeApplication.CH1,
					mApplication.pAttenuationSettingCh1);
			mApplication.pVoltageDivCh2 = mCurrentSource.getVoltageDivsion(
					OsciPrimeApplication.CH1,
					mApplication.pAttenuationSettingCh2);

			/** load attenuation values **/
			if (mState == RUNNING) {
				mLastSettingWhileRunningCh1 = mApplication.pAttenuationSettingCh1;
				mLastSettingWhileRunningCh2 = mApplication.pAttenuationSettingCh2;
				mApplication.pRunningAttenuationCh1 = mApplication.pAttenuationSettingCh1;
				mApplication.pRunningAttenuationCh2 = mApplication.pAttenuationSettingCh2;
			} else {
				int ch = OsciPrimeApplication.CH1;
				float l = mApplication.getActiveCalibration().getCh1AttenuationValues()[mLastSettingWhileRunningCh1];
				float dl = mCurrentSource.getVoltageDivsion(ch,
						mLastSettingWhileRunningCh1);
				float dc = mCurrentSource.getVoltageDivsion(ch,
						mApplication.pAttenuationSettingCh1);
				L.d(String.format("lcal %f, ldiv %f, cdiv %f", l, dl, dc));
				float fact = l * dl / dc;
				mApplication.getActiveCalibration().getCh1AttenuationValues()[mApplication.pAttenuationSettingCh1] = fact;

				ch = OsciPrimeApplication.CH2;
				l = mApplication.getActiveCalibration().getCh2AttenuationValues()[mLastSettingWhileRunningCh2];
				dl = mCurrentSource.getVoltageDivsion(ch,
						mLastSettingWhileRunningCh2);
				dc = mCurrentSource.getVoltageDivsion(ch,
						mApplication.pAttenuationSettingCh2);
				fact = l * dl / dc;
				mApplication.getActiveCalibration().getCh2AttenuationValues()[mApplication.pAttenuationSettingCh2] = fact;
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mServiceMessenger.getBinder();
	}

	protected void stop() {
		// by setting mState to IDLE the source will get that it should
		// terminate and inform the activity on
		// the next chance
		//recycle preview cache

		if(mState != IDLE)
			BufferPreview.recycle();
		mCurrentSource.forceStop();
		if(mCurrentSource != null){
			mApplication.saveCurrentCalibration();
		}
	}

	private Source mCurrentSource;

	protected void start() {
		if (mCurrentSource != null) {//don't save if no source was active, will override stuff we don't want to
			mApplication.saveCurrentCalibration();
		}
		if (mApplication.pActiveSource == SourceType.AUDIO) {
			mCurrentSource = new OsciAudioSource(this, mApplication);
			notifyService();
		} else if (mApplication.pActiveSource == SourceType.USB) {
			try {
				notifyService();
				mCurrentSource = new OsciUsbSource(this, mApplication);
			} catch (VerifyError e) {
				Toast.makeText(this, "unable to initialize USB source",
						Toast.LENGTH_LONG).show();
				mCurrentSource = null;
				terminateAndClean();
			}
		} else if (mApplication.pActiveSource == SourceType.NETWORK) {
			if (sNetworkSource == null) {
				sNetworkSource = new NetworkSource(mApplication, this);
			}
			mCurrentSource = sNetworkSource;
			sNetworkSource.start();
		}

		if (mCurrentSource != null) {
			mApplication.sourceChanged(mCurrentSource);
			mApplication.pResolutionInBits = mCurrentSource
					.getResolutionInBits();
		}

	}

	private static NetworkSource sNetworkSource;

	private void notifyService() {
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Intent ni = new Intent(this, OsciPrimeICSActivity.class);
		ni.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pi = PendingIntent.getActivity(this, 0, ni,
				PendingIntent.FLAG_UPDATE_CURRENT);
		// Bitmap tmp = BitmapFactory.decodeResource(getResources(),
		// R.drawable.notification);
		// sNotification.contentIntent = pi;
		// sNotification.flags = Notification.FLAG_ONGOING_EVENT;
		// sNotification.icon = R.drawable.notification;
		// sNotification.setLatestEventInfo(this, "OsciPrime",
		// "Collecting Data",
		// pi);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				this);

		builder.setContentIntent(pi).setSmallIcon(R.drawable.notification)
				.setTicker("Collecting Data").setOngoing(true)
				.setWhen(System.currentTimeMillis()).setAutoCancel(false)
				.setContentTitle("OsciPrime").setContentText("Collecting Data");
		sNotification = builder.getNotification();
		nm.notify(NOTIFICATION_ID, sNotification);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		if (mState == RUNNING || mState == TERMINATING) {
			return true;
		}
		if (mApplication.pKillService)
			stopSelf(mStartId);
		return true;
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		this.mStartId = startId;
		startForeground(NOTIFICATION_ID, sNotification);
		return START_STICKY;// run even after unbind if necessary
	}

	private Thread mExportThread;

	private void resample() {
		if (mCurrentSource != null) {
			// boolean retrigger =
			// mApplication.pTriggerAdjusted.compareAndSet(true, false);
			mCurrentSource.resample(true);
		}
	}
	
	private void resample(int index) {
		if (mCurrentSource != null) {
			mCurrentSource.resample(true, index);
		}
	}

	/** CALLED BY THE SOURCE **/
	int dbgcnt = 0;

	private boolean mWasSingleShot = false;

	@Override
	public void onSourceSamples(int[] ch1, int[] ch2, int[] previewCh1, int[] previewCh2, int len, int found,
			float offch1, float offch2) {
		if (mCalibrateNextTime.get()) {
			L.d("calibrating");
			mApplication.getActiveCalibration().getCh1Offsets()[mApplication.pAttenuationSettingCh1] = offch1;
			mApplication.getActiveCalibration().getCh2Offsets()[mApplication.pAttenuationSettingCh2] = offch2;
		}
		mCalibrateNextTime.set(false);
		mApplication.copyData(ch1, ch2, previewCh1, previewCh2, found);
		if (mNetworkSink != null) {
			mNetworkSink.onSamples(ch1, ch2, found);
		}
		if (mApplication.pMode == OsciPrimeApplication.MODE_SINGLESHOT
				&& found >= 0) {
			if (mState == RUNNING)// resample?
				mState = TERMINATING;// like when STOP message is received by
										// the
			// messenger
			stop();
		}
		if (mActivityMessenger != null) {
			try {
				mActivityMessenger.send(Message.obtain(null,
						OsciPrimeICSActivity.SAMPLES));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		Stats.stat(mApplication);
	}

	@Override
	public void onError() {
		terminateAndClean();
	}

	@Override
	public OsciPrimeApplication getContext() {
		return this.mApplication;
	}

	@Override
	public boolean stopNow() {
		// called from the source when it is ready to quit ... so here we
		// quit...
		if (mState == TERMINATING || mState == IDLE) {
			L.d("stopNow called, source should be stoping ...");
			terminateAndClean();
			return true;
		} else
			return false;
	}

	@Override
	public boolean isRunning() {
		return mState == RUNNING;
	}

	/** switch state to idle **/
	private void terminateAndClean() {
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		sNotification = new Notification();
		nm.notify(NOTIFICATION_ID, sNotification);
		mState = IDLE;
		status();
	}

	public static native int nativeTrigger(int[] arr, int len, int tr,
			boolean edge, int singleshot);

	public static native int nativeTriggerBuffer(ByteBuffer arr, int len,
			int tr, boolean edge);

	public static native void nativeInterleave(int[] arr, int buflen,
			int destlen, int trigger, int nthVal, int[] intBuffer, int[] bufferPreview, int previewLen);

	public static native void nativeInterleaveBuffer(ByteBuffer arr,
			int buflen, int[] dest, int destlen, int trigger, int nthVal);

	static {
		System.loadLibrary("trigger");
	}

	@Override
	public void startSampling() {
		try {
			mServiceMessenger.send(Message.obtain(null, START));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stopSampling() {
		try {
			mServiceMessenger.send(Message.obtain(null, STOP));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void calibrateNextTime() {
		mCalibrateNextTime.set(true);
	}

	public void doResample() {
		resample();
	}

	@Override
	public void sinkTerminated() {
		mApplication.pNetworkSinkSate = OsciPrimeApplication.NETWORK_SINK_DISABLED;
	}

	@Override
	public void clientConnected() {
		mApplication.pNetworkSinkSate = OsciPrimeApplication.NETWORK_SINK_CONNECTED;
		status();
	}

	@Override
	public void clientDisconnected() {
		mApplication.pNetworkSinkSate = OsciPrimeApplication.NETWORK_SINK_DISCONNECTED;
		status();
	}

}
