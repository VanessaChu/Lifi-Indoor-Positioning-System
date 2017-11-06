
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import ch.nexuscomputing.android.osciprimeics.BuildConfig;
import ch.nexuscomputing.android.osciprimeics.IServiceInterface;
import ch.nexuscomputing.android.osciprimeics.L;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;
import ch.nexuscomputing.android.osciprimeics.Stats;
import ch.nexuscomputing.android.usb.IUsbConnectionHandler;
import ch.nexuscomputing.android.usb.UsbController;

public class OsciUsbSource implements Source{
	private final IServiceInterface mSvc;
	private final OsciPrimeApplication mApplication;
	public static final float POST_SCALE = 2;
	
	public static final byte BM_AMP_CH1 = 0x07;
	public static final byte BM_AMP_CH2 = 0x38;
	public static final String ACTION_USB_PERMISSION = "ch.serverbox.android.USB";

	private final static int BENCHMARK_COUNT = 10;
	public static final int NUM_BUFFERS = 4;
	public static final int NUM_SUBBUFFERS = 25;
	public static final int MAX_BUFFERSIZE = 16384;//Framework restriction (silly? kind of...)
	/*
	 * Dear friends at Google ... WTF! Why do you only mention this restriction on the ADK
	 * Documentation page and not on the USB-Host Doc page? I mean really, WTF! 
	 * Srsly... please learn from libusb
	 */
	 
	private UsbDevice mDevice;
	private UsbDeviceConnection mConnection; 
	private final UsbManager mUsbManager;
	private UsbEndpoint mCmdEp;
	private Thread mMainThread = null; 
	private UsbController mUsbCont;
	
	public static AtomicBoolean sConfig3Mhz = new AtomicBoolean(true);
	
	private static int[] sInterLeavedCh1; 
	private static int[] sInterLeavedCh2;
	private static int[] sPreviewCh1;
	private static int[] sPreviewCh2;
	
	private static final ByteBuffer[] sBigBuffer  = new ByteBuffer[NUM_BUFFERS];
	private static ByteBuffer sBufferCopy;
	private static final ByteBuffer[][] sByteBuffers = new ByteBuffer[NUM_BUFFERS][NUM_SUBBUFFERS];
	private static final UsbRequest sReq[][] = new UsbRequest[NUM_BUFFERS][NUM_SUBBUFFERS];
	
	private final byte[] CH1_GAINS = new byte[]{(byte)0x3F, (byte)0x12, (byte)0x09, (byte)0x00, (byte)0x1B};
	private final byte[] CH2_GAINS = new byte[]{(byte)0x3F, (byte)0x12, (byte)0x09, (byte)0x00, (byte)0x1B};
	public static final float[] CH1_DEFAULTS = new float[]{2.575f, 2.26f, 3.415f, 3.1525f, 3.9325f};
	public static final float[] CH2_DEFAULTS = new float[]{2.575f, 2.26f, 3.415f, 3.1525f, 3.9325f};
	private final float[] CH1_CALIBRATION = new float[]{0.95625f, 0.96875f, 1.3235f, 1.45f, 1.73f};
	private final float[] CH2_CALIBRATION = new float[]{0.95625f, 0.96875f, 1.35f, 1.45f, 1.73f};
	private final float[] VOLTAGE_DIVISIONS = new float[]{10/POST_SCALE, 5/POST_SCALE, 2.5f/POST_SCALE, 2.0f/POST_SCALE, 1.0f/POST_SCALE};
	
	
	
	private int mOldTrigger;
	
	public static ByteBuffer getBufferCopy(){
		return sBufferCopy;
	}
	
	public OsciUsbSource(IServiceInterface svc, OsciPrimeApplication app) {
		mSvc = svc; 
		mApplication = app;
		mUsbManager = (UsbManager) app.getSystemService(Context.USB_SERVICE);
		mUsbCont = new UsbController(app, mSvc,new IUsbConnectionHandler() {
			@Override 
			public void onDeviceNotFound() {
				mSvc.onError();
				return;
			} 
			
			@Override
			public void onDeviceInitialized(UsbDevice dev) {
				if(mSvc.stopNow()){
					return;
				}
				if(mMainThread == null){
					mDevice = dev; 
					mMainThread = new Thread(mLoop);
					mMainThread.setName("USB Sampling Thread");
					mMainThread.start();
				}else{ 
					mSvc.onError();    
				}
			}
		}); 
	}
	
	
	@Override
	public float getDefaultAttenuation(int channel, int setting) {
		if(channel == OsciPrimeApplication.CH1){
			if(setting < CH1_CALIBRATION.length)
				return CH1_CALIBRATION[setting];
		}else{
			if(setting < CH2_CALIBRATION.length)
				return CH2_CALIBRATION[setting];
		}
		L.e("Attenuation Setting out of bound");
		return 1.0f;
	}
	
	@Override
	public float getVoltageDivsion(int channel, int setting) {
		if(setting < VOLTAGE_DIVISIONS.length)
			return VOLTAGE_DIVISIONS[setting];
		
		L.e("Voltage division Setting out of bound");
		return -1.0f;
	}
	
	@Override
	public void attenuationChanged(OsciPrimeApplication app) {
		if(mConnection != null && mSvc.isRunning()){
			byte mod = CH1_GAINS[app.pAttenuationSettingCh1];
			byte ch2Cmd = CH2_GAINS[app.pAttenuationSettingCh2];
			byte v = (byte)(ch2Cmd & ~BM_AMP_CH1);
			v |= mod & BM_AMP_CH1;
			mConnection.bulkTransfer(mCmdEp, new byte[]{v}, 1, 1000);
			//also issue the command for probe compenstation here
			byte compensation = app.pProbeCopensation ? (byte)0x48 :  (byte)0x40;
			mConnection.bulkTransfer(mCmdEp, new byte[]{compensation}, 1, 1000);
			if(sConfig3Mhz.get()){
				mConnection.bulkTransfer(mCmdEp, new byte[]{(byte)0xC3}, 1, 1000);
			}
		}
	} 
	
	
	
	private Runnable mLoop = new Runnable() { 
		@Override
		public void run() {
			//TEST
			if (mDevice == null){
				mSvc.onError();
				return; 
			}
			mApplication.setup(//TODO refactor names
					CH1_CALIBRATION[mApplication.pAttenuationSettingCh1], 
					CH2_CALIBRATION[mApplication.pAttenuationSettingCh2],
					4,
					1,
					1,
					1, 
					(sConfig3Mhz.get() ? 3000000 : 6000000),
					MAX_BUFFERSIZE*NUM_SUBBUFFERS/2, 
					MAX_BUFFERSIZE*NUM_SUBBUFFERS/2,
					32
			);
			//TODO figure this out setupCalibration();
			initBuffers();
			mConnection = mUsbManager.openDevice(mDevice);
			L.d("Interface Count: "+mDevice.getInterfaceCount());
			L.d("Using "+String.format("%04X:%04X", mDevice.getVendorId(), mDevice.getProductId()));
			if(!mConnection.claimInterface(mDevice.getInterface(0), true)){
				mSvc.onError();
				return; 
			}
			UsbInterface usbIf = mDevice.getInterface(0);				
			for(int i = 0; i < usbIf.getEndpointCount(); i++){
				L.d("EP: "+String.format("0x%02X", usbIf.getEndpoint(i).getAddress()));
				if(usbIf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK){
					L.d("Bulk Endpoint");
				}else{
					L.d("Not Bulk");
				}
			}


			mCmdEp = usbIf.getEndpoint(0);
			
			//initialize attenuation
			attenuationChanged(mApplication);
			
			L.d("Successfully submitted command bulk transfer");

			UsbEndpoint dataEp = usbIf.getEndpoint(2); 

			L.d("initializing request ...");
			for(int i = 0; i < NUM_BUFFERS; i++){
				for(int j = 0; j < NUM_SUBBUFFERS; j++){
					sReq[i][j] = new UsbRequest();
					sReq[i][j].initialize(mConnection, dataEp);
					if(sReq[i][j].queue(sByteBuffers[i][j], MAX_BUFFERSIZE) == false){
						L.e("could not queue transfer");
					}
					sReq[i][j].setClientData(new String("Buffer "+i+" Subbuffer "+j));
				} 
			}
			L.d("... done");
			mainLoop();
		}
		 
		private void mainLoop() {
			if(BuildConfig.DEBUG){
				benchmarkUsb();
			}

			
			
			long t = 0;
			for(;;){				
				for(int i = 0; i < NUM_BUFFERS; i++){
				
					for(int j = 0; j < NUM_SUBBUFFERS; j++){
						UsbRequest r = mConnection.requestWait();
						if(r == null){
							mSvc.onError();
							return;
						}
						r.queue(sByteBuffers[i][j], MAX_BUFFERSIZE);
					}
					t = System.currentTimeMillis();
					//START 
					if(mSvc.stopNow()){ 
						L.d("Returning");
						return;
					}			 
					 
					sBigBuffer[i].position(0); 
				  
					int pov = mApplication.pPointsOnView;
					int previewLen = mApplication.pPreviewLen;
					int interleave = mApplication.pInterleave;
					float off1 = mApplication.getActiveCalibration().getCh1Offsets()[mApplication.pAttenuationSettingCh1];
					float off2 = mApplication.getActiveCalibration().getCh2Offsets()[mApplication.pAttenuationSettingCh2];
					float att1 = mApplication.getActiveCalibration().getCh1AttenuationValues()[mApplication.pAttenuationSettingCh1];
					float att2 = mApplication.getActiveCalibration().getCh2AttenuationValues()[mApplication.pAttenuationSettingCh2];
					att1 *= POST_SCALE;
					att2 *= POST_SCALE;
					int lvlCh1 = (int) mApplication.pTriggerLevelCh1;
					int lvlCh2 = (int) mApplication.pTriggerLevelCh2;  
					int ch = mApplication.pTriggerChannel;
					int edgeCh1 = mApplication.pEdgeCh1;
					int edgeCh2 = mApplication.pEdgeCh2;
					float[] resvalues = new float[2];
					int mode = mApplication.pMode; 
					
					assertInterleaveBuffers(pov, previewLen);
					
					//t = System.currentTimeMillis();
					int found = process(
							sBigBuffer[i],
							sBufferCopy,  
							NUM_SUBBUFFERS*MAX_BUFFERSIZE, 
							sInterLeavedCh1,
							sInterLeavedCh2,
							sPreviewCh1,
							sPreviewCh2,
							pov, previewLen, interleave,//-1, we don't wanna do the preview loop
							off1, off2, att1, att2, 
							lvlCh1, lvlCh2, 
							ch, edgeCh1, edgeCh2,
							mode, -1,
							resvalues 
					); 
					
					if(mOldTrigger < 0 && found >= 0){
						L.d("found trigger");
					}
					mOldTrigger = found;
					mApplication.pCapturedFrameSize = mApplication.pFrameSize;
					mSvc.onSourceSamples( 
						sInterLeavedCh1,
						sInterLeavedCh2,
						sPreviewCh1,
						sPreviewCh2,
						NUM_SUBBUFFERS*MAX_BUFFERSIZE, 
						found, resvalues[0], resvalues[1] 
					);
								 
					int took = (int) (System.currentTimeMillis()-t); 
					Stats.proc(took);
				}
				//System.gc(); 
				//L.d("Time for "+NUM_BUFFERS+" buffers: "+b+" [ms] should not be more than 136[ms]");//+(int)((1000*NUM_BUFFERS*NUM_SUBBUFFERS*MAX_BUFFERSIZE)/(12000000f)));
			}
		}

		private void initBuffers() {
			if(sBigBuffer[0] == null){//initialize only once
				sBufferCopy = ByteBuffer.allocateDirect(MAX_BUFFERSIZE*NUM_SUBBUFFERS);
				sBufferCopy.order(ByteOrder.nativeOrder());
				for(int i = 0; i < NUM_BUFFERS; i++){
					sBigBuffer[i] = ByteBuffer.allocateDirect(MAX_BUFFERSIZE*NUM_SUBBUFFERS);
					sBigBuffer[i].order(ByteOrder.nativeOrder());
					
					sBigBuffer[i].position(0);
					sBigBuffer[i].put(new byte[MAX_BUFFERSIZE*NUM_SUBBUFFERS]);//zero
					for(int j = 0; j < NUM_SUBBUFFERS; j++){
						sBigBuffer[i].position(j*MAX_BUFFERSIZE);
						sByteBuffers[i][j] = sBigBuffer[i].slice();
					}
				}
			}
		}
	};
	
	@Override
	public void resample(boolean retrigger) {
		if(sBufferCopy == null){
			return;//means the scope was not running
		}
		sBufferCopy.position(0);
	
		int pov = mApplication.pPointsOnView;
		int interleave = mApplication.pInterleave;
		float off1 = mApplication.getActiveCalibration().getCh1Offsets()[mApplication.pAttenuationSettingCh1];
		float off2 = mApplication.getActiveCalibration().getCh2Offsets()[mApplication.pAttenuationSettingCh2];
		float att1 = mApplication.getActiveCalibration().getCh1AttenuationValues()[mApplication.pAttenuationSettingCh1];
		float att2 = mApplication.getActiveCalibration().getCh2AttenuationValues()[mApplication.pAttenuationSettingCh2];
		att1 *= POST_SCALE;
		att2 *= POST_SCALE;
		int lvlCh1 = (int) mApplication.pTriggerLevelCh1;
		int lvlCh2 = (int) mApplication.pTriggerLevelCh2;  
		int ch = mApplication.pTriggerChannel;
		int edgeCh1 = mApplication.pEdgeCh1;
		int edgeCh2 = mApplication.pEdgeCh2;
		float[] resvalues = new float[2];
		int mode = mApplication.pMode; 
		
		int previewLen = mApplication.pPreviewLen;
		assertInterleaveBuffers(pov,previewLen); 
		
		int oldTrigger = retrigger ? -1 : mOldTrigger; 
		int found = process(
				sBufferCopy,
				sBufferCopy,
				NUM_SUBBUFFERS*MAX_BUFFERSIZE,
				sInterLeavedCh1,
				sInterLeavedCh2,
				sPreviewCh1,
				sPreviewCh2,
				pov, previewLen, interleave,
				off1, off2, att1, att2, 
				lvlCh1, lvlCh2, 
				ch, edgeCh1, edgeCh2,
				mode, oldTrigger,
				resvalues 
		); 
		mSvc.onSourceSamples( 
			sInterLeavedCh1,
			sInterLeavedCh2,
			sPreviewCh1,
			sPreviewCh2,
			NUM_SUBBUFFERS*MAX_BUFFERSIZE, 
			found, resvalues[0], resvalues[1] 
		);
	}
	
	private void assertInterleaveBuffers(int pov, int numpreview) {
		if(sInterLeavedCh1 == null){
			sInterLeavedCh1 = new int[pov];
			sInterLeavedCh2 = new int[pov];
		}
		
		if(sInterLeavedCh1.length != pov){
			sInterLeavedCh1 = new int[pov];
			sInterLeavedCh2 = new int[pov];
		}
		
		if(sPreviewCh1 == null){//we allocate double the size, one for min, one for max
			sPreviewCh1 = new int[2*numpreview];
			sPreviewCh2 = new int[2*numpreview];
		}
		
		if(sPreviewCh1.length != 2*numpreview){
			sPreviewCh1 = new int[2*numpreview];
			sPreviewCh2 = new int[2*numpreview];
		}
	}
	
	public static native int process(
		ByteBuffer buf, 
		ByteBuffer copy,
		int buflen,
		int[] interleavedCh1,
		int[] interleavedCh2,
		int[] previewCh1,
		int[] previewCh2,
		int interleavelen,
		int previewLen,
		int interleave,
		float calibrationOffsetCh1,
		float calibrationOffsetCh2,
		float attenuationCh1,
		float attenuationCh2,
		int triggerLevelCh1,
		int triggerLevelCh2,
		int triggerChannel,
		int triggerEdgeCh1,
		int triggerEdgeCh2,
		int mode,
		int oldTrigger,
		float[] returnValues
	);
	

	static {
		System.loadLibrary("processor");
	}

	
	private void benchmarkUsb(){
		//benchmark
		long[] times = new long[BENCHMARK_COUNT];
		ArrayList<UsbRequest> requestsToRequeue = new ArrayList<UsbRequest>(NUM_SUBBUFFERS);
		for(int k = 0; k < BENCHMARK_COUNT; k++){
			long init = System.currentTimeMillis();
			for(int i = 0; i < NUM_BUFFERS; i++){
				for(int j = 0; j < NUM_SUBBUFFERS; j++){
					UsbRequest r = mConnection.requestWait();
					if(r == null){
						mSvc.onError();
						return;
					}
					requestsToRequeue.add(r);
				}
				//process here and then requeue everything later
				
				for(int j = 0; j < NUM_SUBBUFFERS; j++){//queue big buffer
					requestsToRequeue.get(j).queue(sByteBuffers[i][j], MAX_BUFFERSIZE);
				}
				requestsToRequeue.clear();
			}
			
			times[k] = System.currentTimeMillis()-init;
		}
		
		long benchMin = 5000;
		long benchMax = 0;
		float benchAvg = 0;
		
		for(int i = 0; i < BENCHMARK_COUNT; i++){
			if(times[i] < benchMin)
				benchMin = times[i];
			
			if(times[i] > benchMax)
				benchMax = times[i];
			
			benchAvg += times[i]/(float)BENCHMARK_COUNT;
		}
		
		OsciPrimeApplication.dUsbBenchmark = new float[]{
				benchAvg,
				benchMax,
				benchMin
		};
	}


	@Override
	public int getResolutionInBits() {
		return 8;
	}


	@Override
	public void forceStop() {
		// TODO Auto-generated method stub
	}

	@Override
	public void resample(boolean retrigger, int index) {
		mOldTrigger = index;
		resample(false);
	}
}

//if(sInterLeavedCh1Buffer == null){
//sInterLeavedCh1Buffer = ByteBuffer.allocateDirect(4*pov);
//sInterLeavedCh2Buffer = ByteBuffer.allocateDirect(4*pov);
//sInterLeavedCh1Buffer.order(ByteOrder.nativeOrder());
//sInterLeavedCh2Buffer.order(ByteOrder.nativeOrder());
//L.d("crating buffers");
//}
//
//if(sInterLeavedCh1Buffer.limit() != pov*4){
//sInterLeavedCh1Buffer = ByteBuffer.allocateDirect(4*pov);
//sInterLeavedCh2Buffer = ByteBuffer.allocateDirect(4*pov);
//sInterLeavedCh1Buffer.order(ByteOrder.nativeOrder());
//sInterLeavedCh2Buffer.order(ByteOrder.nativeOrder());
//L.d("resizing buffers");
//}
//
//sInterLeavedCh1Buffer.position(0);
//sInterLeavedCh2Buffer.position(0);
//sCopyCh1.position(0);
//sCopyCh2.position(0);
