
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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import ch.nexuscomputing.android.osciprimeics.IServiceInterface;
import ch.nexuscomputing.android.osciprimeics.L;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeService;

public class OsciAudioSource implements Source{
	
	private final IServiceInterface mSvc; 
	private final OsciPrimeApplication mApplication;
	public OsciAudioSource(IServiceInterface svc, OsciPrimeApplication app) {
		mSvc = svc;
		mApplication = app;
		new Thread(mAudioLoop).start();
	}
	
	private static int[] sLastCh1, sLastCh2;
	private static int sLastLength, sLastTrigger; 
	private static int[] sPreviewCh1, sPreviewCh2;
	
	private static final float MIN_ATT = 1;
	private static final float MAX_ATT = 5;
	@Override
	public float getDefaultAttenuation(int channel, int setting) {
		float max_x = DIV.length;
		float m = (MAX_ATT-MIN_ATT)/max_x;
		return setting*m+MIN_ATT; 
	} 
	
	public static final float[] DIV = {.16f, .14f, .1f, .075f, .05f};
	@Override
	public float getVoltageDivsion(int channel, int setting) { 
		return DIV[setting];
	}
	
	 
	int[] projection = new int[0];
	private Runnable mAudioLoop = new Runnable() {
		public void run() {
			//final int BLOCK_SIZE = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
			mApplication.setup(
					.5f, 
					.5f,
					4,
					1,
					1,
					1, 
					44100,
					2048, 
					2048, 
					8
			);			
			
			final int BLOCK_SIZE = 8192;
			L.d("Block size: "+BLOCK_SIZE);
			short[] audioShortBuffer = new short[OsciPrimeApplication.MAX_FRAME_SIZE];
			AudioRecord rec = new AudioRecord(
					AudioSource.MIC,
					44100,
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					32*BLOCK_SIZE
			);
			if(rec.getState() == AudioRecord.STATE_UNINITIALIZED){
				L.e("cannot initialzie audio");
				mSvc.onError();
				return;
			}
			rec.startRecording();
			for(;;){//this is the loop
				int len = 0;
				OsciPrimeApplication.dFrameSize = mApplication.pFrameSize;
				if((len = rec.read(audioShortBuffer, 0, mApplication.pFrameSize)) == AudioRecord.ERROR_BAD_VALUE){
					L.e("read: bad value given");
					rec.stop();
					rec.release();
					mSvc.onError();
					return;
				}
				OsciPrimeApplication.dSamplesRead = len;
				if(mSvc.stopNow()){
					L.d("stopping source");
					rec.stop();
					rec.release(); 
					mSvc.onError();//TODO is this necessary?
					return;
				}
				long t = System.currentTimeMillis();
				
				if(projection.length != len){
					projection = new int[len];
				}
				
				for(int i  = 0; i < len; i++){
					projection[i] = (int) (audioShortBuffer[i]/64.0);
				}
				mApplication.pCapturedFrameSize = mApplication.pFrameSize;
				OsciPrimeApplication.dProcessingTook = (System.currentTimeMillis()-t);
				//this is for a later update
				//Logger.write(IntBuffer.wrap(projection));
				onSamples(projection, projection);
			}
		}
	}; 
	@Override
	public void attenuationChanged(OsciPrimeApplication app) {
//		app.pAttenuationCh1 = (app.pAttenuationSettingCh1+1)*.125f;
//		app.pAttenuationCh2 = (app.pAttenuationSettingCh2+1)*.125f;
//		app.pVoltageDivCh1 = 6/(app.pAttenuationSettingCh1+1);
//		app.pVoltageDivCh2 = 6/(app.pAttenuationSettingCh2+1);
	}
	
	
	int[] rch1 = new int[0];
	int[] rch2 = new int[0];
	
	@Override
	public void resample(boolean retrigger) {
		int tr = 0;
		long t = System.currentTimeMillis();
		
		int mode = mApplication.pMode;
		int singleshot = mode == OsciPrimeApplication.MODE_SINGLESHOT ? 1 : 0;
		if (retrigger) {
			if (mApplication.pTriggerChannel == OsciPrimeApplication.CH1){
				tr = OsciPrimeService.nativeTrigger(
						sLastCh1,
						sLastLength,
						(int) (mApplication.pTriggerLevelCh1 / mApplication.getActiveCalibration().getCh1AttenuationValues()[mApplication.pAttenuationSettingCh1]),
						(mApplication.pEdgeCh1 == OsciPrimeApplication.RISING ? true
								: false),singleshot);
			}else{
				tr = OsciPrimeService.nativeTrigger(
						sLastCh2,
						sLastLength,
						(int) (mApplication.pTriggerLevelCh2 / mApplication.getActiveCalibration().getCh2AttenuationValues()[mApplication.pAttenuationSettingCh2]),
						(mApplication.pEdgeCh2 == OsciPrimeApplication.RISING ? true
								: false), singleshot);
			}
			sLastTrigger = tr;
			
		} else {
			tr = sLastTrigger;
		}

		OsciPrimeApplication.dTriggering = System.currentTimeMillis() - t;
		t = System.currentTimeMillis();
		int pov = mApplication.pPointsOnView;// hrhr
		int ilv = mApplication.pInterleave;
		if(rch1.length != pov){
			rch1 = new int[pov];
			rch2 = new int[pov];
		}
		OsciPrimeService.nativeInterleave(sLastCh1, sLastLength, pov, tr, ilv, rch1, sPreviewCh1, sPreviewCh1.length/2);
		OsciPrimeService.nativeInterleave(sLastCh2, sLastLength, pov, tr, ilv, rch2, sPreviewCh2, sPreviewCh2.length/2);
		if (mApplication.pInterleave * mApplication.pPointsOnView >= sLastLength) {
			L.d("Opps");
		}
		OsciPrimeApplication.dInterleave = System.currentTimeMillis() - t;
		//TODO preview available?
		mSvc.onSourceSamples(rch1, rch2, sPreviewCh1, sPreviewCh2, rch1.length, tr, 0, 0);
	}
	
	
	public void onSamples(int[] ch1, int[] ch2) {
		int tr = 0;
		long t = System.currentTimeMillis(); 
		sLastLength = ch1.length;
		
		if(sLastCh1 == null){
			sLastCh1 = new int[sLastLength];
			sLastCh2 = new int[sLastLength];
		}
		
		if(sLastCh1.length < sLastLength){
			sLastCh1 = new int[sLastLength];
			sLastCh2 = new int[sLastLength];
		}
		
		if(sPreviewCh1 == null){
			sPreviewCh1 = new int[2*mApplication.pPreviewLen];
			sPreviewCh2 = new int[2*mApplication.pPreviewLen];
		}
		
		if(sPreviewCh1.length != 2*mApplication.pPreviewLen){
			sPreviewCh1 = new int[2*mApplication.pPreviewLen];
			sPreviewCh2 = new int[2*mApplication.pPreviewLen];
		}
		
		
		System.arraycopy(ch1, 0, sLastCh1, 0, sLastLength);
		System.arraycopy(ch2, 0, sLastCh2, 0, sLastLength);
		
		int mode = mApplication.pMode;
		int singleshot = mode == OsciPrimeApplication.MODE_SINGLESHOT ? 1 : 0;
		
		if(mApplication.getActiveCalibration() == null){//Bug report on Play store ... weird ... yet completely possible
			mSvc.onError();
			return;
		}
		if (mApplication.pTriggerChannel == OsciPrimeApplication.CH1)
			tr = OsciPrimeService.nativeTrigger( 
					ch1,
					ch1.length,
					(int) (mApplication.pTriggerLevelCh1 / mApplication.getActiveCalibration().getCh1AttenuationValues()[mApplication.pAttenuationSettingCh1]),
					(mApplication.pEdgeCh1 == OsciPrimeApplication.RISING ? true
							: false), singleshot);
		else
			tr = OsciPrimeService.nativeTrigger(
					ch2,
					ch2.length,
					(int) (mApplication.pTriggerLevelCh2 / mApplication.getActiveCalibration().getCh2AttenuationValues()[mApplication.pAttenuationSettingCh2]),
					(mApplication.pEdgeCh2 == OsciPrimeApplication.RISING ? true
							: false), singleshot);
		OsciPrimeApplication.dTriggering = System.currentTimeMillis() - t;
		sLastTrigger = tr;
		int pov = mApplication.pPointsOnView;// hrhr
		int ilv = mApplication.pInterleave;
		if(rch1.length != pov){
			rch1 = new int[pov];
			rch2 = new int[pov];
		}
		
		OsciPrimeService.nativeInterleave(ch1, ch1.length, pov, tr, ilv, rch1, sPreviewCh1,sPreviewCh1.length/2);
		OsciPrimeService.nativeInterleave(ch2, ch2.length, pov, tr, ilv, rch2, sPreviewCh2, sPreviewCh2.length/2);
		mSvc.onSourceSamples(rch1, rch2, sPreviewCh1, sPreviewCh2,rch1.length, tr, 0, 0);
	}


	@Override
	public int getResolutionInBits() {
		return 10;
	}


	@Override
	public void forceStop() {
		// TODO Auto-generated method stub
		
	}

	
	public static int[] getLastCh1(){
		return sLastCh1;
	}
	
	public static int[] getLastCh2(){
		return sLastCh2;
	}
	
	public static int getLastLen(){
		return sLastLength;
	}


	@Override
	public void resample(boolean retrigger, int index) {
		sLastTrigger = index;
		resample(false);
	}

}
