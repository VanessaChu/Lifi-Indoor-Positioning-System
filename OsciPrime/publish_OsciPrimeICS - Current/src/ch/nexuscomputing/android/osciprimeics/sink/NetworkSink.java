
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

package ch.nexuscomputing.android.osciprimeics.sink;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ch.nexuscomputing.android.osciprimeics.IServiceSinkInterface;
import ch.nexuscomputing.android.osciprimeics.L;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;
import ch.nexuscomputing.android.osciprimeics.network.Header;
import ch.nexuscomputing.android.osciprimeics.network.NetworkEngine;
import ch.nexuscomputing.android.osciprimeics.network.NetworkEngine.IEngineCallback;
import ch.nexuscomputing.android.osciprimeics.network.Parameters.HostParameters;
import ch.nexuscomputing.android.osciprimeics.network.Parameters.RemoteParameters;
import ch.nexuscomputing.android.osciprimeics.network.Parameters.SharedParameters;

public class NetworkSink {

	private final OsciPrimeApplication mApplication;
	private final IServiceSinkInterface mSvc;

	public NetworkSink(OsciPrimeApplication app, IServiceSinkInterface svc) {
		mApplication = app;
		mSvc = svc;
		new Thread(mMainLoop).start();
	}
	
	public void onSamples(int[] ch1, int[]ch2, int found){
		//send config first for thise samples
		ByteArrayOutputStream back = new ByteArrayOutputStream();
		DataOutputStream stream = new DataOutputStream(back);
		try {
			HostParameters.toStream(stream,mApplication);
			stream.flush();
			byte[] bytes = back.toByteArray();
			mEngine.send(Header.CMD_HOST_PARAMS, bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		back = new ByteArrayOutputStream();
		stream = new DataOutputStream(back);
		try {
			SharedParameters.toStream(stream,mApplication);
			stream.flush();
			byte[] bytes = back.toByteArray();
			mEngine.send(Header.CMD_SHARED_PARAMS, bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		back = new ByteArrayOutputStream();
		stream = new DataOutputStream(back);
		try {
			for(int i = 0; i < ch1.length; i++){
				stream.writeInt(ch1[i]);
				stream.writeInt(ch2[i]);
			}
			stream.writeInt(found);
			stream.flush();
			byte[] bytes = back.toByteArray();
			mEngine.send(Header.CMD_SAMPLES, bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private final IEngineCallback mCallback = new IEngineCallback() {
		
		final SharedParameters mSharedParams = new SharedParameters();
		final RemoteParameters mRemoteParams = new RemoteParameters();
		
		@Override
		public void onTerminated() {
			mSvc.sinkTerminated();
			if(!mSvc.stopNow())
				mSvc.onError();
		}

		@Override
		public void onReceive(Header head, DataInputStream dis) {
			switch(head.command){
			case Header.CMD_CLIENT_PARAMS:
				try {
					RemoteParameters.fromStream(dis, mRemoteParams);
					mApplication.pInterleave = mRemoteParams.pInterleave;
					mApplication.pFrameSize = mRemoteParams.pFrameSize;
					mApplication.pPointsOnView = mRemoteParams.pPointsOnView;
					mApplication.pAttenuationSettingCh1 = mRemoteParams.pAttenuationSettingCh1;
					mApplication.pAttenuationSettingCh2 = mRemoteParams.pAttenuationSettingCh2;
					mApplication.pEdgeCh1 = mRemoteParams.pEdgeCh1;
					mApplication.pEdgeCh2 = mRemoteParams.pEdgeCh2;
					mApplication.pTriggerLevelCh1 = mRemoteParams.pTriggerLevelCh1;
					mApplication.pTriggerLevelCh2 = mRemoteParams.pTriggerLevelCh2;
					mApplication.pTriggerChannel = mRemoteParams.pTriggerChannel;
					mApplication.pMode = mRemoteParams.pMode;
				} catch (IOException e) {
					L.e("error parsing");
					e.printStackTrace();
				}
				break;
			case Header.CMD_RESAMPLE:
				mSvc.doResample();
				break;
			case Header.CMD_SHARED_PARAMS:
				try {
					SharedParameters.fromStream(dis, mSharedParams);
					boolean newSetting = false;
					if(mApplication.pAttenuationSettingCh1 != mSharedParams.pAttenuationSettingCh1)
						newSetting = true;
					if(mApplication.pAttenuationSettingCh2 != mSharedParams.pAttenuationSettingCh2)
						newSetting = true;
					
					mApplication.pAttenuationSettingCh1 = mSharedParams.pAttenuationSettingCh1;
					mApplication.pAttenuationSettingCh2 = mSharedParams.pAttenuationSettingCh2;
					if(newSetting)
						mSvc.attenuationChanged();
				} catch (IOException e) {
					L.e("error parsing");
					e.printStackTrace();
				}
				break;
			case Header.CMD_START:
				L.d("CMD_START RECEIVED");
				mSvc.startSampling();
				break;
			case Header.CMD_STOP:
				L.d("CMD_STOP RECEIVED");
				mSvc.stopSampling();
				break;
			default:
				L.e("unknown command");
				break;
			}
		}

		@Override
		public void onConnected() {
			mSvc.clientConnected();
		}
		

		@Override
		public void onDisconnected() {
			mSvc.clientDisconnected();
		}

		@Override
		public OsciPrimeApplication getApplication() {
			return mApplication;
		}
	};
	
	public void calibrate(){
		
	}
	
	public void stop(){
		try {
			mEngine.terminate();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final NetworkEngine mEngine = new NetworkEngine(mCallback,
			"data sink");
	
	private final Runnable mMainLoop = new Runnable() {
		@Override
		public void run() {
			try {
				mEngine.spawnServer();
			} catch (IOException e) {
				mSvc.onError();
				e.printStackTrace();
			} catch (InterruptedException e) {
				mSvc.onError();
				e.printStackTrace();
			}
		}
	};
}
