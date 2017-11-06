
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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.util.SparseArray;
import android.widget.Toast;
import ch.nexuscomputing.android.osciprimeics.calibration.Calibration;
import ch.nexuscomputing.android.osciprimeics.calibration.HttpsTask.IResponseCallback;
import ch.nexuscomputing.android.osciprimeics.news.HttpsTask;
import ch.nexuscomputing.android.osciprimeics.news.News;
import ch.nexuscomputing.android.osciprimeics.source.OsciAudioSource;
import ch.nexuscomputing.android.osciprimeics.source.OsciUsbSource;
import ch.nexuscomputing.android.osciprimeics.source.Source;

/**
 * TODO resample inside UsbSource
 * 
 */
public class OsciPrimeApplication extends Application {

	//public static final Object[] dataLock = new Object[] {};
	public static final ReentrantLock sDataLock = new ReentrantLock();
	
	public static int GRID_DIV = 100;
	public static int GRID_COLS = 26;
	private static int GRID_ROWS = 20;
	public static float WIDTH = GRID_COLS * GRID_DIV;
	public static float HEIGHT = GRID_ROWS * GRID_DIV;

	public static final int OVERLAY_OFFSET = 1;
	public static final int OVERLAY_MEASURE = 2;
	public static final int OVERLAY_DEBUG = 4;

	public static final int MODE_CONTINUOUS = 0;
	public static final int MODE_SINGLESHOT = 1;

	public static float OFFSET_DRAWING_TWO = 1.5f * HEIGHT;

	public static float TRIGGER_HANDLE_PADDING = 10;

	public static final int OFFSET_HANDLE_WIDTH = 200;
	public static final int OFFSET_HANDLE_HEIGHT = 70;

//	public static final int SOURCE_AUDIO = 0;
//	public static final int SOURCE_USB = 1;
//	public static final int SOURCE_NETWORK = 2;
	
	public enum SourceType{
		AUDIO,
		USB,
		NETWORK
	}
	
	public static final int NUM_ATTENUATION_SETTINGS = 5;// 5 settings, (include 0)

	public static final int CH1 = 0, CH2 = 1;
	public static final int FALLING = 0, RISING = 1;
	public int pTriggerChannel = CH1;
	public float pTriggerLevelCh1 = -HEIGHT / 8;
	public float pTriggerLevelCh2 = 0;
	public int pEdgeCh1 = RISING;
	public int pEdgeCh2 = RISING;
//	public float pAttenuationCh1 = .5f;
//	public float pAttenuationCh2 = .5f;
//	public int pCalibrationOffsetCh1;
//	public int pCalibrationOffsetCh2;
	
	public int pAttenuationSettingCh1 = 0;
	public int pAttenuationSettingCh2 = 0;
	
//	public final float[] pAttenuationValuesCh1 = new float[NUM_ATTENUATION_SETTINGS];
//	public final float[] pAttenuationValuesCh2 = new float[NUM_ATTENUATION_SETTINGS];
	
	private final ArrayList<Calibration> mCalibrations = new ArrayList<Calibration>();

	public float pRunningAttenuationCh1 = -1;
	public float pRunningAttenuationCh2 = -1;
	
	public float pVoltageDivCh1 = .16f;
	public float pVoltageDivCh2 = .16f;
	public int pInterleave = 1;
	public int pPreviewLen = 512;
	public int pPointsOnView = 256;
	public float pOffsetCh1 = -HEIGHT / 4;
	public float pOffsetCh2 = HEIGHT / 4;
	public int pColorCh1 = Color.parseColor("#4400CC");
	public int pColorCh2 = Color.parseColor("#CC0044");
	public int pColorGrid = Color.parseColor("#CCCCCC");
	public int pColorMeasure = Color.parseColor("#003399");
	public int pColorBackground = Color.WHITE;
	public int pInterfaceColor = Color.parseColor("#003399");
	// screenshot
	public int pExportWidth = 400;
	public boolean pIncludeGrid = true;
	public boolean pIncludeTrigger = true;
	public boolean pIncludeCh1 = true;
	public boolean pIncludeCh2 = true;
	public SourceType pActiveSource = SourceType.AUDIO;
	public boolean pDrawTriggerLabel = true;
	public boolean pKillService = false;
	public boolean pShowCh1 = true;
	public boolean pShowCh2 = true;
	public boolean pFullScreenMode = false;
	public boolean pStopSamplingOnClose = false;
	public boolean pShowBufferPreview = true;
	
	public boolean pProbeCopensation = true;
	public boolean pHardwareAccelerated = false;
	
	public boolean pDrawClipCh1 = false;
	public boolean pDrawClipCh2 = false;
	public int pResolutionInBits = 10;
	
	public final static int NETWORK_SINK_CONNECTED = 0;
	public final static int NETWORK_SINK_DISCONNECTED = 1;
	public final static int NETWORK_SINK_DISABLED = 2;

	public int pNetworkSinkSate = NETWORK_SINK_DISABLED;
	public String pServerIp = "127.0.0.1";
	
	public String pIpAddress = "localhost";

	public AtomicBoolean pTriggerAdjusted = new AtomicBoolean(false);

	public static final int MAX_FRAME_SIZE = 1 << 20;
	public static final int PROGRESS_MAX = 1000;

//	public static final String PREFERENCES_CALIBRATION = "calibration";
//	public static final String PREFERENCES_CH1_CAL_PREFIX = "cal_ch1_";
//	public static final String PREFERENCES_CH2_CAL_PREFIX = "cal_ch2_";
//	public static final String PREFERENCES_SRC_AUDIO_PREFIX = "audio_";
//	public static final String PREFERENCES_SRC_USB_PREFIX = "usb_";

	public static final float CAL_FACTOR_MAX = 5.0f;
	public static final float CAL_FACTOR_MIN = 0.25f;
	
	// set by the source
	public int pFrameSize = 2048;
	public int pCapturedFrameSize = -1;
	public int pMinFrameSize = 2048;// generally 30[ms]/frame => x/44100 =
									// 0.030, x =.03*44100 for audio, per one
									// channel
									// usb = > 6'000'000 => x = 0.03*6M =
									// 180'000
	public int pSamplingFrequency = 44100;
	public int pMaxInterleave = 8;

	public int pActiveOverlay = OVERLAY_OFFSET;
	public int pMode = MODE_CONTINUOUS;
	public float pMeasureHandleVert1 = -HEIGHT / 4;
	public float pMeasureHandleVert2 = HEIGHT / 4;
	public float pMeasureHandleHor1 = -WIDTH / 5;
	public float pMeasureHandleHor2 = WIDTH / 5;

	public final Path mPathCh1 = new Path();
	public final Path mPathCh2 = new Path();
	public final Path mGridPath = new Path();
	public final Path mOrigin = new Path();
	public final RectF mHandleCh1 = new RectF(-WIDTH / 2 - OFFSET_HANDLE_WIDTH,
			-OFFSET_HANDLE_HEIGHT / 2, -WIDTH / 2, OFFSET_HANDLE_HEIGHT / 2);
	public final RectF mHandleCh2 = new RectF(-WIDTH / 2 - OFFSET_HANDLE_WIDTH,
			-OFFSET_HANDLE_HEIGHT / 2, -WIDTH / 2, OFFSET_HANDLE_HEIGHT / 2);
	public final RectF mHandleTriggerCH1 = new RectF(WIDTH / 2
			+ TRIGGER_HANDLE_PADDING, -OFFSET_HANDLE_HEIGHT / 4, WIDTH / 2
			+ OFFSET_HANDLE_WIDTH + TRIGGER_HANDLE_PADDING,
			OFFSET_HANDLE_HEIGHT / 4);
	 
  
	public final RectF mHandleTriggerCH2 = new RectF(WIDTH / 2
			+ TRIGGER_HANDLE_PADDING, -OFFSET_HANDLE_HEIGHT / 4, WIDTH / 2
			+ OFFSET_HANDLE_WIDTH + TRIGGER_HANDLE_PADDING,
			OFFSET_HANDLE_HEIGHT / 4); 
	
	public final Path mPathMeasureVert1 = new Path();
	public final Path mPathMeasureVert2 = new Path();
	public final Path mPathMeasureHor1 = new Path();
	public final Path mPathMeasureHor2 = new Path();

	public int pBarWidth = 0;// used to offset the info text

	public News pNews = null;
	public boolean pUnreadNews = false;

	//yeah this did not help at all...
	public static native void copy2Data(Path pCh1, Path pCh2, int[] ch1,
			int[] ch2, int len, int points, float width, float attCh1, float attCh2);
	public static native void copy2Points(
			int[] src1, int[] src2,
			float[] dst1 , float[] dst2,
			int len, int points, float width,
			float attCh1, float attCh2);

	static {
		System.loadLibrary("copy");
	}

	public OsciPrimeApplication() {
		initGrid();
		initMeasurementHandles();
		copyData(new int[pPointsOnView], new int[pPointsOnView], new int[2*pPreviewLen], new int[2*pPreviewLen], -1);
	}

	public float[] mGrid = new float[4*(GRID_COLS+1)+4*(GRID_ROWS+1)];
	private void initGrid() {
//		for (int i = 0; i <= GRID_COLS; i++) {
//			mGridPath.moveTo(i * GRID_DIV - WIDTH / 2, HEIGHT / 2);
//			mGridPath.lineTo(i * GRID_DIV - WIDTH / 2, -HEIGHT / 2);
//		}
//
//		for (int i = 0; i <= GRID_ROWS; i++) {
//			mGridPath.moveTo(-WIDTH / 2, i * GRID_DIV - HEIGHT / 2);
//			mGridPath.lineTo(WIDTH / 2, i * GRID_DIV - HEIGHT / 2);
//		} 
		mOrigin.moveTo(0, -HEIGHT / 2);
		mOrigin.lineTo(0, HEIGHT / 2);
		for(int i = 0; i <= GRID_COLS; i++){
			mGrid[4*i] = i * GRID_DIV - WIDTH / 2;
			mGrid[4*i+1] = HEIGHT / 2;
			mGrid[4*i+2] = i * GRID_DIV - WIDTH / 2;
			mGrid[4*i+3] = -HEIGHT / 2;
		}
		
		for(int i = 0; i <= GRID_ROWS; i++){
			mGrid[4*(GRID_COLS+1)+4*i] = -WIDTH / 2;
			mGrid[4*(GRID_COLS+1)+4*i+1] = i * GRID_DIV - HEIGHT / 2;
			mGrid[4*(GRID_COLS+1)+4*i+2] = WIDTH / 2;
			mGrid[4*(GRID_COLS+1)+4*i+3] = i * GRID_DIV - HEIGHT / 2;
		}
	}

	private void initMeasurementHandles() {
		mPathMeasureHor2.setFillType(Path.FillType.EVEN_ODD);
		mPathMeasureHor2.moveTo(-OFFSET_HANDLE_WIDTH / 4, 0);
		mPathMeasureHor2.lineTo(OFFSET_HANDLE_WIDTH / 4, 0);
		mPathMeasureHor2.lineTo(0, -OFFSET_HANDLE_WIDTH / 4);
		mPathMeasureHor2.lineTo(-OFFSET_HANDLE_WIDTH / 4, 0);
		mPathMeasureHor2.close();

		mPathMeasureHor1.setFillType(Path.FillType.EVEN_ODD);
		mPathMeasureHor1.moveTo(-OFFSET_HANDLE_WIDTH / 4, 0);
		mPathMeasureHor1.lineTo(OFFSET_HANDLE_WIDTH / 4, 0);
		mPathMeasureHor1.lineTo(0, OFFSET_HANDLE_WIDTH / 4);
		mPathMeasureHor1.lineTo(-OFFSET_HANDLE_WIDTH / 4, 0);
		mPathMeasureHor1.close();

		mPathMeasureVert1.setFillType(Path.FillType.EVEN_ODD);
		mPathMeasureVert1.moveTo(0, -OFFSET_HANDLE_WIDTH / 4);
		mPathMeasureVert1.lineTo(0, OFFSET_HANDLE_WIDTH / 4);
		mPathMeasureVert1.lineTo(OFFSET_HANDLE_WIDTH / 4, 0);
		mPathMeasureVert1.lineTo(0, -OFFSET_HANDLE_WIDTH / 4);
		mPathMeasureVert1.close();

		mPathMeasureVert2.setFillType(Path.FillType.EVEN_ODD);
		mPathMeasureVert2.moveTo(0, OFFSET_HANDLE_WIDTH / 4);
		mPathMeasureVert2.lineTo(0, -OFFSET_HANDLE_WIDTH / 4);
		mPathMeasureVert2.lineTo(-OFFSET_HANDLE_WIDTH / 4, 0);
		mPathMeasureVert2.lineTo(0, OFFSET_HANDLE_WIDTH / 4);
		mPathMeasureVert2.close();
	}
	
	float[] mCh1Copy = new float[]{};
	float[] mCh2Copy = new float[]{};
	int[] mPreviewCh1 = new int[]{};
	int[] mPreviewCh2 = new int[]{};
	int mLastTrigger = -1;
	
	public float[] getCh1(){return mCh1Copy;}
	public float[] getCh2(){return mCh2Copy;}
	public int[] getPreviewCh1(){return mPreviewCh1;}
	public int[] getPreviewCh2(){return mPreviewCh2;}
	public int getLastTrigger(){return mLastTrigger;}
	
	public void copyData(int[] ch1, int[] ch2, int[] previewCh1, int[] previewCh2, int trigger){
		mLastTrigger = trigger;
		//if not initialized properly!
		if(mCalibrations.size() == 0){
			return;
		}
		long t = System.currentTimeMillis(); 
		if(!sDataLock.tryLock())
			return;
		int len = ch1.length;
		int pov = pPointsOnView;//lol
		float att1 = getActiveCalibration().getCh1AttenuationValues()[pAttenuationSettingCh1]; 
		float att2 = getActiveCalibration().getCh2AttenuationValues()[pAttenuationSettingCh2];
		if(pActiveSource == SourceType.USB){
			att1 *= OsciUsbSource.POST_SCALE;
			att2 *= OsciUsbSource.POST_SCALE;
		}
		
		if(mCh1Copy.length != 4*(pov-1)){
			mCh1Copy = new float[4*(pov-1)];
			mCh2Copy = new float[4*(pov-1)];  
		}
		if(mPreviewCh1.length != 2*pPreviewLen){
			mPreviewCh1 = new int[2*pPreviewLen];
			mPreviewCh2 = new int[2*pPreviewLen];
		}
		if(len == pPointsOnView){
			copy2Points(ch1, ch2, mCh1Copy, mCh2Copy, len-1, pov, WIDTH, att1, att2);
		}else{
			L.d("len != pPointsOnView!!");
		}
		
		if(previewCh1 != null){
			if(mPreviewCh1.length == previewCh1.length){
				System.arraycopy(previewCh1, 0, mPreviewCh1, 0, previewCh1.length);
				System.arraycopy(previewCh2, 0, mPreviewCh2, 0, previewCh2.length);
			}else{
				L.e("preview lengths do not match");
			}
		}

		sDataLock.unlock();
		Stats.copy(System.currentTimeMillis()-t);
	}
	public static float dZoom = 0;
	public static float dOffx = 0;
	public static float dOffy = 0;
	public static long dDrawingTime = 0;
	public static long dSamplesRead = 0;
	public static long dProcessingTook = 0;
	public static long dTriggering = 0;
	public static long dInterleave = 0;
	public static long dLength = 0;
	public static long dPathing = 0;
	public static long dTransferring = 0;
	public static long dFrameSize = 0;
	public static float[] dUsbBenchmark = new float[] { 0, 0, 0 };

	private OsciSurfaceView mSurfaceView;



	public void onStart(OsciPrimeICSActivity osciPrimeICSActivity,
			OsciSurfaceView surfaceView) {
		SharedPreferences sp = osciPrimeICSActivity.getSharedPreferences(
				"default", MODE_PRIVATE);
		mSurfaceView = surfaceView;
		pTriggerChannel = sp.getInt("pTriggerChannel", CH1);
		pEdgeCh1 = sp.getInt("pEdgeCh1", RISING);
		pEdgeCh2 = sp.getInt("pEdgeCh2", RISING);
		pInterleave = sp.getInt("pInterleave", 1);
		pPreviewLen = sp.getInt("pPreviewLen", pPreviewLen);
		pSamplingFrequency = sp.getInt("pSamplingFrequency", 44100);
		pPointsOnView = sp.getInt("pPointsOnView", 256);
		pColorCh1 = sp.getInt("pColorCh1", pColorCh1);
		pColorCh2 = sp.getInt("pColorCh2", pColorCh2);
		pColorGrid = sp.getInt("pColorGrid", pColorGrid);
		pColorMeasure = sp.getInt("pColorMeasure", pColorMeasure);
		pColorBackground = sp.getInt("pColorBackground", pColorBackground);
		pInterfaceColor = sp.getInt("pInterfaceColor",
				Color.parseColor("#003399"));
		pAttenuationSettingCh1 = sp.getInt("pAttenuationSettingCh1", 0);
		pAttenuationSettingCh2 = sp.getInt("pAttenuationSettingCh2", 0);

		// screenshot
		pIncludeCh1 = sp.getBoolean("pIncludeCh1", true);
		pIncludeCh2 = sp.getBoolean("pIncludeCh2", true);
		pIncludeGrid = sp.getBoolean("pIncludeGrid", true);
		pIncludeTrigger = sp.getBoolean("pIncludeTrigger", true);
		pExportWidth = sp.getInt("pExportWidth", 800);

		pOffsetCh1 = sp.getFloat("pOffsetCh1", 0);
		pOffsetCh2 = sp.getFloat("pOffsetCh2", HEIGHT / 4);
		pTriggerLevelCh1 = sp.getFloat("pTriggerLevelCh1", pTriggerLevelCh1);
		pTriggerLevelCh2 = sp.getFloat("pTriggerLevelCh2", pTriggerLevelCh2);
		try{
			pActiveSource = SourceType.values()[sp.getInt("pActiveSource", SourceType.AUDIO.ordinal())];
		}catch (Exception e) {
			pActiveSource = SourceType.USB;
		}
		
		L.d("loading calibration");
		loadSourceCalibrations(sp);

		pDrawTriggerLabel = sp.getBoolean("pDrawTriggerLabel", true);
		pUnreadNews = sp.getBoolean("pUnreadNews", false);
		pKillService = sp.getBoolean("pKillService", false);
		pShowCh1 = sp.getBoolean("pShowCh1", true);
		pShowCh2 = sp.getBoolean("pShowCh2", false);
		
		pShowBufferPreview = sp.getBoolean("pShowBufferPreview", pShowBufferPreview);
		pDrawClipCh1 = sp.getBoolean("pDrawClipCh1", false);
		pDrawClipCh2 = sp.getBoolean("pDrawClipCh2", false);
		
		pFullScreenMode = sp.getBoolean("pFullScreenMode", pFullScreenMode);
		pStopSamplingOnClose = sp.getBoolean("pStopSamplingOnClose", pStopSamplingOnClose);


		pProbeCopensation = sp.getBoolean("pProbeCopensation", false);
		pHardwareAccelerated = sp.getBoolean("pHardwareAccelerated", false);
		
		pIpAddress = sp.getString("pIpAddress", "localhost");
		fetchNews(osciPrimeICSActivity);

		surfaceView.onConfigLoaded();
		osciPrimeICSActivity.updateInterfaceColor();
	}

	public void onStop(OsciPrimeICSActivity osciPrimeICSActivity) {
		SharedPreferences sp = osciPrimeICSActivity.getSharedPreferences(
				"default", MODE_PRIVATE);

		Editor e = sp.edit();

		e.putInt("pTriggerChannel", pTriggerChannel);
		e.putInt("pEdgeCh1", pEdgeCh1);
		e.putInt("pEdgeCh2", pEdgeCh2);
		e.putInt("pInterleave", pInterleave);
		e.putInt("pPreviewLen", pPreviewLen);
		e.putInt("pSamplingFrequency", pSamplingFrequency);
		e.putInt("pPointsOnView", pPointsOnView);
		e.putInt("pColorCh1", pColorCh1);
		e.putInt("pColorCh2", pColorCh2);
		e.putInt("pColorGrid", pColorGrid);
		e.putInt("pColorMeasure", pColorMeasure);
		e.putInt("pColorBackground", pColorBackground);
		e.putInt("pInterfaceColor", pInterfaceColor);
		e.putInt("pAttenuationSettingCh1", pAttenuationSettingCh1);
		e.putInt("pAttenuationSettingCh2", pAttenuationSettingCh2);

		e.putBoolean("pIncludeCh1", pIncludeCh1);
		e.putBoolean("pIncludeCh2", pIncludeCh2);
		e.putBoolean("pIncludeTrigger", pIncludeTrigger);
		e.putBoolean("pIncludeGrid", pIncludeGrid);
		e.putInt("pExportWidth", pExportWidth);

		e.putFloat("pOffsetCh1", pOffsetCh1);
		e.putFloat("pOffsetCh2", pOffsetCh2);
		e.putFloat("pTriggerLevelCh1", pTriggerLevelCh1);
		e.putFloat("pTriggerLevelCh2", pTriggerLevelCh2);

		e.putInt("pActiveSource", pActiveSource.ordinal());

		e.putBoolean("pDrawTriggerLabel", pDrawTriggerLabel);
		e.putBoolean("pUnreadNews", pUnreadNews);
		e.putBoolean("pKillService", pKillService);
		e.putBoolean("pShowCh1", pShowCh1);
		e.putBoolean("pShowCh2", pShowCh2);
		
		e.putBoolean("pShowBufferPreview", pShowBufferPreview);
		e.putBoolean("pDrawClipCh1", pDrawClipCh1);
		e.putBoolean("pDrawClipCh2", pDrawClipCh2);
		
		e.putBoolean("pFullScreenMode", pFullScreenMode);
		e.putBoolean("pStopSamplingOnClose", pStopSamplingOnClose);
		
		e.putBoolean("pProbeCopensation", pProbeCopensation);
		e.putBoolean("pHardwareAccelerated", pHardwareAccelerated);

		e.putString("pIpAddress", pIpAddress);
		e.commit();
	}

	public void setup(float attenuationCh1, float attenuationCh2,
			int attenuationSettingMax, float voltageDivCh1,
			float voltageDivCh2, int interleave, int samplingFrequency,
			int frameSize, int minFrameSize, int maxInterleave) {
		// ok this is shit, at least do a range check
		// so stuipd ....
		pSamplingFrequency = samplingFrequency;
		pMinFrameSize = minFrameSize;
		pMaxInterleave = maxInterleave;
//		pNumAttenuationSettingsCh1 = attenuationSettingMax;
//		pNumAttenuationSettingsCh2 = attenuationSettingMax;

		pAttenuationSettingCh1 = Math.min(pAttenuationSettingCh1,
				NUM_ATTENUATION_SETTINGS);
		pAttenuationSettingCh2 = Math.min(pAttenuationSettingCh2,
				NUM_ATTENUATION_SETTINGS);
		// pPointsOnView = 256; leave
		// pAttenuationCh1 = attenuationCh1;
		// pAttenuationCh2 = attenuationCh2;
		// pVoltageDivCh1 = voltageDivCh1;
		// pVoltageDivCh2 = voltageDivCh2;
		// 4096 max points on view
		pInterleave = Math.min(pMaxInterleave, pInterleave);
		pFrameSize = Math.min(4096 * maxInterleave, pPointsOnView * pInterleave
				* 2);
		pFrameSize = Math.max(pMinFrameSize, pFrameSize);

	}

	public void changeAttenuationCalibration(int progress, int channel) {
		// y = m*x + q => y = (y2-y1)/(x2-x1)*x + q
		if (channel == CH1)
			getActiveCalibration().getCh1AttenuationValues()[pAttenuationSettingCh1] = ((1.0f * progress) / PROGRESS_MAX)
					* (CAL_FACTOR_MAX - CAL_FACTOR_MIN) + CAL_FACTOR_MIN;
		else
			getActiveCalibration().getCh2AttenuationValues()[pAttenuationSettingCh2] = ((1.0f * progress) / PROGRESS_MAX)
					* (CAL_FACTOR_MAX - CAL_FACTOR_MIN) + CAL_FACTOR_MIN;
		
		L.d("put progress into array ch1: %f, ch2 %f",getActiveCalibration().getCh1AttenuationValues()[pAttenuationSettingCh1],getActiveCalibration().getCh2AttenuationValues()[pAttenuationSettingCh2]);
	}

	public int progressFromSetting(int channel) {
		float att = channel == CH1 ? 
				getActiveCalibration().getCh1AttenuationValues()[pAttenuationSettingCh1] :
				getActiveCalibration().getCh2AttenuationValues()[pAttenuationSettingCh2];	
				
		L.d("progresbar: loading attenuation %f",att);

		return (int) ((att - CAL_FACTOR_MIN)
				/ (CAL_FACTOR_MAX - CAL_FACTOR_MIN) * PROGRESS_MAX);
	}

	
	public void sourceChanged(Source src){
		if(pActiveSource == SourceType.NETWORK){
			for(int i = 0; i < getActiveCalibration().getCh1AttenuationValues().length; i++){
				getActiveCalibration().getCh1AttenuationValues()[i] = 1;
				getActiveCalibration().getCh2AttenuationValues()[i] = 1;
			}
		}
		SharedPreferences sp = getSharedPreferences("default", Context.MODE_PRIVATE);
		loadSourceCalibrations(sp);
		/** set the values **/
		pVoltageDivCh1 = src.getVoltageDivsion(
				OsciPrimeApplication.CH1,
				pAttenuationSettingCh1);
		pVoltageDivCh2 = src.getVoltageDivsion(
				OsciPrimeApplication.CH2,
				pAttenuationSettingCh2);
	}

	private static final int NEWS_UPDATE_INTERVAL_HRS = 0;

	private void fetchNews(final OsciPrimeICSActivity activity) {
		SharedPreferences sp = getSharedPreferences("default", MODE_PRIVATE);
		long lastNews = sp.getLong("lastNews", -1);
		try {
			ObjectInputStream ois = new ObjectInputStream(
					getApplicationContext().openFileInput("news"));
			pNews = (News) ois.readObject();
			ois.close();
		} catch (Exception e) {
			L.e("could not read cached news");
		}

		if (lastNews != -1) {
			if (System.currentTimeMillis() - lastNews <= NEWS_UPDATE_INTERVAL_HRS * 60 * 60 * 1000)
				return;// news less than 6hrs old
		}
		HttpsTask task = new HttpsTask("http://www.osciprime.com/news.php",
				new HttpsTask.IResponseCallback() {
					@Override
					public void onStartException(String msg) {
					}

					@Override
					public void onResponse(News news) {
						long issue = -1;
						if (pNews != null) {
							issue = pNews.getIssue();// cached news
						}

						if (news.getIssue() > issue) {
							pUnreadNews = true;
							activity.onNews();
						}
						pNews = news;
						SharedPreferences sp = getSharedPreferences("default",
								MODE_PRIVATE);
						sp.edit()
								.putLong("lastNews", System.currentTimeMillis())
								.commit();
						try {
							ObjectOutputStream oos = new ObjectOutputStream(
									getApplicationContext().openFileOutput(
											"news", MODE_PRIVATE));
							oos.writeObject(news);
							oos.close();
						} catch (Exception e) {
							L.e("could not write news to cache");
						}
					}

					@Override
					public void onKillException(String msg) {
					}

					@Override
					public void onFinish() {
					}

					@Override
					public void onError(String msg) {
					}
				});
		task.start();
	}
	
	public void fetchCalibration(String serial, final Handler handler){
		ch.nexuscomputing.android.osciprimeics.calibration.HttpsTask  task = new ch.nexuscomputing.android.osciprimeics.calibration.HttpsTask("http://www.osciprime.com/cal/"+serial+".txt", new IResponseCallback() {
			
			@Override
			public void onStartException(String msg) {
				
			}
			
			@Override
			public void onResponse(Calibration calibration) {
				int toReplace = -1;
				for(int i = 0; i < mCalibrations.size(); i++){
					if(mCalibrations.get(i).getType() == SourceType.USB){
						toReplace = i;
					}
				}
				mCalibrations.set(toReplace, calibration);
				SharedPreferences sp = getSharedPreferences("default",Context.MODE_PRIVATE);
				saveSourceCalibrations(sp);
				handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(OsciPrimeApplication.this, "Fetched Calibration from Server!", Toast.LENGTH_SHORT).show();
					}
				});
			}
			
			@Override
			public void onKillException(String msg) {
				
			}
			
			@Override
			public void onFinish() {
				
			}
			
			@Override
			public void onError(String msg) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(OsciPrimeApplication.this, "Unable to fetch calibration from server", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
		task.start();
	}
	
	private static final String PREFIX_CH1 = "_ch1_";
	private static final String PREFIX_CH2 = "_ch2_";
	private static final String PREFIX_OFFSET = "_offset_";
	private static final String PREFIX_ATTENUATION = "_attenuation_";
	
	private static final int FIELD_OFFSET = 0;
	private static final int FIELD_ATTENUATION = 1;
	
	public void saveCurrentCalibration(){
		SharedPreferences sp = getSharedPreferences("default",Context.MODE_PRIVATE);
		saveSourceCalibrations(sp);
	}
	
	private void saveSourceCalibrations(SharedPreferences prefs){
		Editor e = prefs.edit();
		String key = "";
		for(Calibration c : mCalibrations){
			key = preferenceKey(c.getType(), CH1, FIELD_OFFSET);
			floatArray2Editor(e, c.getCh1Offsets(), key);
			
			key = preferenceKey(c.getType(), CH2, FIELD_OFFSET);
			floatArray2Editor(e, c.getCh2Offsets(), key);
			
			key = preferenceKey(c.getType(), CH1, FIELD_ATTENUATION);
			floatArray2Editor(e, c.getCh1AttenuationValues(), key);
			
			key = preferenceKey(c.getType(), CH2, FIELD_ATTENUATION);
			floatArray2Editor(e, c.getCh2AttenuationValues(), key);
		}
		e.commit();
	}
	
	private void loadSourceCalibrations(SharedPreferences prefs){
		SourceType[] types = new SourceType[]{SourceType.AUDIO, SourceType.USB, SourceType.NETWORK};
		String key = "";
		mCalibrations.clear(); 
		for(SourceType t : types){
			Calibration cal = new Calibration(t);
			key = preferenceKey(t, CH1, FIELD_OFFSET);
			cal.setCh1Offsets(floatArrayFromSharedPreferences(prefs,key));
			key = preferenceKey(t, CH2, FIELD_OFFSET);
			cal.setCh2Offsets(floatArrayFromSharedPreferences(prefs,key));
			key = preferenceKey(t, CH1, FIELD_ATTENUATION);
			cal.setCh1AttenuationValues(floatArrayFromSharedPreferences(prefs,key));
			key = preferenceKey(t, CH2, FIELD_ATTENUATION);
			cal.setCh2AttenuationValues(floatArrayFromSharedPreferences(prefs,key));
			
			mCalibrations.add(cal);
		}
	}
	
	private String preferenceKey(SourceType type, int channel, int field){
		return type.toString()
		+(channel == CH1 ? PREFIX_CH1 : PREFIX_CH2)
		+(field == FIELD_OFFSET ? PREFIX_OFFSET : PREFIX_ATTENUATION);
	}	
	
	//thx google for not puting this intho the support package -.-
	private void floatArray2Editor(Editor e, float[] arr, String key){
		if(arr != null){
			for(int i = 0; i < arr.length; i++){
				e.putFloat(key+i,arr[i]);
			}
		}else{
			L.e("array is null!");
		}
		
	}
	
	private float[] floatArrayFromSharedPreferences(SharedPreferences prefs, String key){
		float[] values = new float[NUM_ATTENUATION_SETTINGS];
		for(int i = 0; i < NUM_ATTENUATION_SETTINGS; i++){//dirty
			values[i] = prefs.getFloat(key+i, 1.0f);
		}
		return values;
	}
	
	public Calibration getActiveCalibration(){
		for(Calibration c : mCalibrations){
			if(pActiveSource == c.getType()){
				return c;
			}
		}
		L.e("getActiveCalibration: did not find active calibration");
		return null;
	}

	/*
	 * D/>==< OsciPrime ICS >==<(28567): zoom: 0.2927742 D/>==< OsciPrime ICS
	 * >==<(28567): ofx: -2241.3457 D/>==< OsciPrime ICS >==<(28567): ofy:
	 * 479.95285 D/>==< OsciPrime ICS >==<(28567): drawing time in [ms] 4 D/>==<
	 * OsciPrime ICS >==<(28567): read # samples: 2048 D/>==< OsciPrime ICS
	 * >==<(28567): took [ms] 1 D/>==< OsciPrime ICS >==<(28567): Triggering
	 * took [ms] 0 D/>==< OsciPrime ICS >==<(28567): Interleaving took [ms] 0
	 * D/>==< OsciPrime ICS >==<(28567): length 256 D/>==< OsciPrime ICS
	 * >==<(28567): Pathing took [ms] 0 D/>==< OsciPrime ICS >==<(28567):
	 * Transferring data took [ms] 0
	 */
}
