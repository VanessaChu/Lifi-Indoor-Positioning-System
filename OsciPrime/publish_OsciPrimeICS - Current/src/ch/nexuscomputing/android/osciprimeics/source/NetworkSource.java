
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

package ch.nexuscomputing.android.osciprimeics.source;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import ch.nexuscomputing.android.osciprimeics.IServiceInterface;
import ch.nexuscomputing.android.osciprimeics.L;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;
import ch.nexuscomputing.android.osciprimeics.network.Header;
import ch.nexuscomputing.android.osciprimeics.network.NetworkEngine;
import ch.nexuscomputing.android.osciprimeics.network.NetworkEngine.IEngineCallback;
import ch.nexuscomputing.android.osciprimeics.network.Parameters.HostParameters;
import ch.nexuscomputing.android.osciprimeics.network.Parameters.RemoteParameters;
import ch.nexuscomputing.android.osciprimeics.network.Parameters.SharedParameters;

public class NetworkSource implements Source {

	private final IServiceInterface mSvc;
	private final OsciPrimeApplication mApplication;
	private final String mIp;

	public NetworkSource(OsciPrimeApplication app, IServiceInterface svc) {
		mApplication = app;
		mSvc = svc;
		mIp = mApplication.pIpAddress;
	}
	
	public void start(){
		new Thread(mNewtorkLoop).start();
	}
	
	@Override
	public float getVoltageDivsion(int channel, int setting) {
		if (channel == OsciPrimeApplication.CH1)
			return mApplication.pVoltageDivCh1;
		else
			return mApplication.pVoltageDivCh2;
	}

	@Override
	public float getDefaultAttenuation(int channel, int setting) {
		if (channel == OsciPrimeApplication.CH1)
			return 1f;
		else
			return 1f;
	}

	@Override
	public void attenuationChanged(OsciPrimeApplication app) {
		sendClientParams(app);
		sendSharedParams(app);
	}

	private void sendClientParams(OsciPrimeApplication app) {
		ByteArrayOutputStream back = new ByteArrayOutputStream();
		DataOutputStream stream = new DataOutputStream(back);
		try {
			RemoteParameters.toStream(stream, mApplication);
			stream.flush();
			byte[] bytes = back.toByteArray();
			mEngine.send(Header.CMD_CLIENT_PARAMS, bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendSharedParams(OsciPrimeApplication app) {
		ByteArrayOutputStream back = new ByteArrayOutputStream();
		DataOutputStream stream = new DataOutputStream(back);
		try {
			SharedParameters.toStream(stream, mApplication);
			stream.flush();
			byte[] bytes = back.toByteArray();
			mEngine.send(Header.CMD_SHARED_PARAMS, bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void resample(boolean retrigger) {
		mEngine.send(Header.CMD_RESAMPLE, new byte[] {});
	}

	private final IEngineCallback mEngineCallback = new IEngineCallback() {
		final SharedParameters mSharedParams = new SharedParameters();
		final HostParameters mHostParams = new HostParameters();

		@Override
		public void onTerminated() {
			if (!mSvc.stopNow())
				mSvc.onError();
		}

		@Override
		public void onReceive(Header head, DataInputStream dis) {
			// we expect ony parameters from the other side
			switch (head.command) {
			case Header.CMD_SHARED_PARAMS:
				try {
					SharedParameters.fromStream(dis, mSharedParams);
					mApplication.pAttenuationSettingCh1 = mSharedParams.pAttenuationSettingCh1;
					mApplication.pAttenuationSettingCh2 = mSharedParams.pAttenuationSettingCh2;
				} catch (IOException e) {
					L.e("error parsing");
					e.printStackTrace();
				}
				break;
			case Header.CMD_HOST_PARAMS:
				try {
					HostParameters.fromStream(dis, mHostParams);
					mApplication.pMinFrameSize = mHostParams.pMinFrameSize;
					mApplication.pSamplingFrequency = mHostParams.pSamplingFrequency;
					mApplication.pMaxInterleave = mHostParams.pMaxInterleave;
					
					if(mApplication.getActiveCalibration() == null || mApplication.getActiveCalibration().getCh1Offsets() == null){
						L.d("Calibration is null, may be during network error");
					}else{
						mApplication.getActiveCalibration().getCh1Offsets()[mApplication.pAttenuationSettingCh1] = mHostParams.pCalibrationOffsetCh1;
						mApplication.getActiveCalibration().getCh2Offsets()[mApplication.pAttenuationSettingCh2] = mHostParams.pCalibrationOffsetCh2;
					}
					mApplication.pResolutionInBits = mHostParams.pResolutionInBits;
					mApplication.pVoltageDivCh1 = mHostParams.pVoltageDivCh1;
					mApplication.pVoltageDivCh2 = mHostParams.pVoltageDivCh2;
					
					for(int i = 0; i < OsciPrimeApplication.NUM_ATTENUATION_SETTINGS; i++){
						mApplication.getActiveCalibration().getCh1AttenuationValues()[i] = mHostParams.pAttenuationValuesCh1[i];
						mApplication.getActiveCalibration().getCh2AttenuationValues()[i] = mHostParams.pAttenuationValuesCh2[i];
					}
				} catch (IOException e) {
					L.e("error parsing");
					e.printStackTrace();
				}
				// answer with client params
				sendClientParams(mApplication);
				break;
			case Header.CMD_SAMPLES:
				int channelLen = head.bodyLength / (4 * 2);// 2 channels, 4 bytes for int
				try {
					int[] ch1 = new int[channelLen];
					int[] ch2 = new int[channelLen];
					for (int i = 0; i < channelLen; i++) {
						ch1[i] = dis.readInt();
						ch2[i] = dis.readInt();
					}
					int found = dis.readInt();
					//TODO make preview available
					mSvc.onSourceSamples(ch1, ch2,null,null, ch1.length, found, 0, 0);
				} catch (IOException e) {
					e.printStackTrace();
				}
				sendClientParams(mApplication);
				sendSharedParams(mApplication);
				break;
			default:
				L.e("unknown command in onReceive");
			}
		}

		@Override
		public void onConnected() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onDisconnected() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public OsciPrimeApplication getApplication() {
			return mApplication;
		}
	};

	private final NetworkEngine mEngine = new NetworkEngine(mEngineCallback,
			"datasource");

	private final Runnable mNewtorkLoop = new Runnable() {
		@Override
		public void run() {
			try {
				Thread.currentThread().setName("Network Source Thread");
				mEngine.spawnClient(mIp);// will actually do nothing if already
											// started
				mEngine.send(Header.CMD_START, new byte[] {});
				L.d("sent CMD_START");
				while (true) {
					if (mSvc.stopNow()) {// make sure it is continuously called
						// don't stop the engine...
						// since we might want to resample
						L.d("sending stop to client");
						mEngine.send(Header.CMD_STOP, new byte[] {});
						return;
					}
					Thread.sleep(200);//remain responsive
				}
			} catch (IOException e) {
				L.e("error while connecting or transmitting "+e);
				mSvc.onError();
				e.printStackTrace();
			} catch (InterruptedException e) {
				mSvc.onError();
				e.printStackTrace();
			}
		}
	};

	@Override
	public int getResolutionInBits() {
		return mApplication.pResolutionInBits;
	}

	@Override
	public void forceStop() {
		
	}

	@Override
	public void resample(boolean retrigger, int index) {
		
	}
}
