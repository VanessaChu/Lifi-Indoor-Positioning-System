
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

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.drawable.ColorDrawable;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication.SourceType;
import ch.nexuscomputing.android.osciprimeics.draw.BufferPreview;
import ch.nexuscomputing.android.osciprimeics.draw.Channel;
import ch.nexuscomputing.android.osciprimeics.draw.DrawMeasureOverlay;
import ch.nexuscomputing.android.osciprimeics.draw.DrawOffsetHandle;
import ch.nexuscomputing.android.osciprimeics.draw.DrawTriggerHandle;
import ch.nexuscomputing.android.osciprimeics.draw.Grid;
import ch.nexuscomputing.android.osciprimeics.draw.HandleDrawable;
import ch.nexuscomputing.android.osciprimeics.draw.InfoText;
import ch.nexuscomputing.android.osciprimeics.draw.InfoTextScreenShot;
import ch.nexuscomputing.android.osciprimeics.draw.OverlayDebug;
import ch.nexuscomputing.android.osciprimeics.draw.OverlayMeasure;
import ch.nexuscomputing.android.osciprimeics.draw.OverlayOffset;
import ch.nexuscomputing.android.osciprimeics.draw.WindowHandle;
import ch.nexuscomputing.android.osciprimeics.source.OsciUsbSource;

public class OsciSurfaceView extends SurfaceView {
	private final OsciPrimeApplication mApplication;

	protected boolean mBlockPaint;
	protected final Handler mHandler;
	private int W;
	private int H;
	public static final int INITIAL_SPACE = 100;
	public static final int DRAWING_SIDE = INITIAL_SPACE * 16;
	public static final int RANGE_INDICATION_OPACITY = 50;//out of 255
	private boolean mAA = true;
	private boolean mZoomOffsetFlag = true;
	private int mAACount;

	private boolean mHandleMove = false;
	private boolean mOffsetMove;

	private static float sOffsetX = -25;
	private static float sOffsetY = -10;
	private static float sZoom = 0.3f;
	float mZoomFactor = 1.0f;

	private static Bitmap sNexusAd;
	private static Typeface sTypeFace = null;

	//private static final AtomicBoolean sTouchLocked = new AtomicBoolean(false);
	private final ArrayList<HandleDrawable> mHandles = new ArrayList<HandleDrawable>();

	protected static final long TOUCH_LOCK_DELAY = 150;

	private static float sDefaultZoom = 0.3f;

	@SuppressLint("NewApi")
	public OsciSurfaceView(Context context, Handler handler) {
		super(context);

		setFocusable(true);
		setFocusableInTouchMode(true);
		float x = (float) getResources().getDisplayMetrics().widthPixels;
		float m = 202.02e-6f;
		float q = 0.028383838f;
		sDefaultZoom = m * x + q;
		sZoom = sDefaultZoom;

		mApplication = (OsciPrimeApplication) context.getApplicationContext();
		if (sTypeFace == null) {
			sTypeFace = Typeface.createFromAsset(context.getAssets(),
					"Chewy.ttf");// this was responsible for a memory leak in
			// Android 2.3
		}

		if (sNexusAd == null) {
			try {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 1;
				sNexusAd = Bitmap
						.createScaledBitmap(
								BitmapFactory.decodeResource(
										context.getResources(), R.drawable.ad,
										options),
								(int) OsciPrimeApplication.WIDTH,
								(int) (OsciPrimeApplication.WIDTH * 0.744166667f),
								true);
			} catch (OutOfMemoryError e) {
				try {
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = 2;
					sNexusAd = Bitmap.createScaledBitmap(BitmapFactory
							.decodeResource(context.getResources(),
									R.drawable.ad, options),
							(int) OsciPrimeApplication.WIDTH,
							(int) (OsciPrimeApplication.WIDTH * 0.744166667f),
							true);
				} catch (OutOfMemoryError e1) {
					try {
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inSampleSize = 4;
						try{
							sNexusAd = Bitmap
							.createScaledBitmap(
									BitmapFactory.decodeResource(
											context.getResources(),
											R.drawable.ad, options),
									(int) OsciPrimeApplication.WIDTH,
									(int) (OsciPrimeApplication.WIDTH * 0.744166667f),
									true);
						}catch(OutOfMemoryError err){
							sNexusAd = null;
						}
					} catch (Exception ex) {
						sNexusAd = null;// give up
					}

				}
			}

		}
		setWillNotDraw(false);
		mHandler = handler;
		final ScaleGestureDetector sgd = new ScaleGestureDetector(context,
				new ScaleGestureDetector.OnScaleGestureListener() {

					@Override
					public void onScaleEnd(ScaleGestureDetector detector) {
						mBlockPaint = true;
						mHandler.postDelayed(new Runnable() {
							@Override
							public void run() {
								mBlockPaint = false;
							}
						}, 400);
					}

					@Override
					public boolean onScaleBegin(ScaleGestureDetector detector) {
						return true;
					}

					@Override
					public boolean onScale(ScaleGestureDetector detector) {
						mZoomFactor = sZoom * detector.getScaleFactor();
						setZoom(mZoomFactor);
						return true;
					}
				});

		final GestureDetector gd = new GestureDetector(context,
				new SimpleOnGestureListener() {
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						offset(e1, e2, distanceX, distanceY);
						return true;
					}

					@Override
					public boolean onSingleTapUp(MotionEvent e) {
						if (!mBlockPaint)
							tapOn(e.getX(), e.getY());
						return true;
					}

					@Override
					public boolean onDoubleTap(MotionEvent e) {
						sOffsetX = -25;
						sOffsetY = -10;
						sZoom = sDefaultZoom;
						return true;
					}

				});

		this.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(mApplication.pFullScreenMode){
					mHandler.removeCallbacks(mHideSystemBarRunnable);
				}
				sgd.onTouchEvent(event);
				gd.onTouchEvent(event);
				mZoomOffsetFlag = true;
				mAACount = 0;
				if (event.getAction() == MotionEvent.ACTION_UP) {
					stopScrolling();
				}

				if (event.getAction() == MotionEvent.ACTION_SCROLL) {
					L.d("scroll action");
					float distance = event
							.getAxisValue(MotionEvent.AXIS_VSCROLL);
					mZoomFactor = sZoom * distance;
					setZoom(mZoomFactor);
				}
				postInvalidate();
				return true;
			}
		});

		if (Build.VERSION.SDK_INT >= 12) {
			L.d("attaching generic motion listener");
			this.setOnGenericMotionListener(new OnGenericMotionListener() {
				@Override
				public boolean onGenericMotion(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_SCROLL) {
						float distance = event
								.getAxisValue(MotionEvent.AXIS_VSCROLL);
						if (distance < 0) {
							distance = 0.5f;
						} else if (distance >= 1) {
							distance = 1.5f;
						}
						L.d("distance: " + distance);
						mZoomFactor = sZoom * distance;
						setZoom(mZoomFactor);
						invalidate();
						return true;
					}
					return false;
				}

			});
		}
	}

	public void onConfigLoaded() {
		initHandles();
	}

//	private final Runnable mUnlockTouchEvents = new Runnable() {
//		@Override
//		public void run() {
//			sTouchLocked.set(false);
//		}
//	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		L.d("Keycode " + keyCode);
		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			simpleOffset(0, -20);
			invalidate();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			simpleOffset(0, 20);
			invalidate();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			simpleOffset(-20, 0);
			invalidate();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			simpleOffset(20, 0);
			invalidate();
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		stopScrolling();
		return false;
	}

	private void resample() {
		mHandler.sendEmptyMessage(OsciPrimeICSActivity.RESAMPLE_FROM_SURFACE_VIEW);
	}
	
	private void resampleWithIndex(int triggerIndex) {
		mHandler.sendMessage(Message.obtain(null, OsciPrimeICSActivity.RESAMPLE_FROM_SURFACE_VIEW_WITH_INDEX, triggerIndex, 0));
	}

	protected void stopScrolling() {
		mOffsetMove = false;
		mHandleMove = false;
		for (HandleDrawable handle : mHandles) {
			handle.isFocused = false;
		}
		if (Build.VERSION.SDK_INT >= 11){
			if(mApplication.pFullScreenMode){
				mHandler.postDelayed(mHideSystemBarRunnable,HIDE_SYSTEM_BAR_DELAY);
			}
		}
	}

	protected void tapOn(float x, float y) {
		if (Build.VERSION.SDK_INT >= 11){
			if(mApplication.pFullScreenMode){
				mHandler.post(mHideSystemBarRunnable);
			}
		}
	}
	
	private static final int HIDE_SYSTEM_BAR_DELAY = 2000;

	private static final int RANGE_IOPACITY = 0;
	private final Runnable mHideSystemBarRunnable = new Runnable() {
		@SuppressLint("NewApi")
		@Override
		public void run() {
			setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LOW_PROFILE);
		}
	};

	protected void simpleOffset(float x, float y) {
		mOffsetMove = true;
		sOffsetX += -x / sZoom;
		sOffsetY += -y / sZoom;

		sOffsetX = Math.min(sOffsetX, 3 * DRAWING_SIDE);
		sOffsetY = Math.min(sOffsetY, 3 * DRAWING_SIDE);

		sOffsetX = Math.max(sOffsetX, -3 * DRAWING_SIDE);
		sOffsetY = Math.max(sOffsetY, -3 * DRAWING_SIDE);
	}

	protected void offset(MotionEvent e1, MotionEvent e2, float x, float y) {
		float xT = e2.getX() - W / 2;
		float yT = e2.getY() - H / 2;
		xT /= sZoom;
		yT /= sZoom;
		xT -= sOffsetX;
		yT -= sOffsetY;

		RectF touch = new RectF(xT - 20, yT - 20, xT + 20, yT + 20);

		if (!mOffsetMove || mHandleMove) {
			boolean firstFocused = true;
			for (HandleDrawable od : mHandles) {
				if (od.isFocused) {
					firstFocused = false;
				}
			}

			for (HandleDrawable od : mHandles) {
				if (!od.isFocusable) {
					continue;
				}
				
				if((mApplication.pActiveOverlay & od.layerFlags) == 0)
					continue;
				
				if(!mApplication.pShowBufferPreview && od.mType == HandleDrawable.TYPE_HANDLE_WINDOW)
					continue;
				
				if (od.isFocused || firstFocused) {
					float grow = 1;
					grow = sZoom > 1 ? 1 : sZoom;

					RectF collisionBox = new RectF(od.box);
					collisionBox.offset(od.x, od.y);
					
					if(od.mType != HandleDrawable.TYPE_HANDLE_WINDOW){//troublemaker
						collisionBox.inset(
								(1f-1/grow)*collisionBox.width()/2f,
								(1f-1/grow)*collisionBox.height()/2f
						);
					}else{
						collisionBox.inset(
								(1f-1/grow)*collisionBox.width()/2f,
								0
						);
					}

					if (collisionBox.intersect(touch) || od.isFocused) {
						float currentY, currentX;
						currentY = e2.getY() - H / 2;
						currentY /= sZoom;
						currentY -= sOffsetY;

						currentX = e2.getX() - W / 2;
						currentX /= sZoom;
						currentX -= sOffsetX;
						od.set(currentX, currentY);
						mHandleMove = true;
						od.isFocused = true;
						od.update();
						return; // only one foccusable
					}
				}
			}
		}
		mOffsetMove = true;
		sOffsetX += -x / sZoom;
		sOffsetY += -y / sZoom;

		sOffsetX = Math.min(sOffsetX, 3 * DRAWING_SIDE);
		sOffsetY = Math.min(sOffsetY, 3 * DRAWING_SIDE);

		sOffsetX = Math.max(sOffsetX, -3 * DRAWING_SIDE);
		sOffsetY = Math.max(sOffsetY, -3 * DRAWING_SIDE);
	}

	protected void setZoom(float zoomFactor) {
		sZoom = Math.max(zoomFactor, 0.15f);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		W = getWidth();
		H = getHeight();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		drawFrame(canvas);
	}

	float[] mCh1Copy = new float[] {};
	float[] mCh2Copy = new float[] {};
	int[] mCh1Preview = new int[] {};
	int[] mCh2Preview = new int[] {};
	

	private void drawFrame(Canvas canvas) {
		long t = System.currentTimeMillis();

		OsciPrimeApplication.sDataLock.lock();
		if (mApplication.getCh2().length != mCh1Copy.length) {
			mCh1Copy = new float[mApplication.getCh1().length];
		}
		
		if(mCh1Preview.length != mApplication.getPreviewCh1().length){
			mCh1Preview = new int[mApplication.getPreviewCh1().length];
			mCh2Preview = new int[mApplication.getPreviewCh2().length];//let's not really pretend we are introducing a feature to have different preview lengths, shall we...
		}

		if (mApplication.getCh2().length != mCh2Copy.length) {
			mCh2Copy = new float[mApplication.getCh2().length];
		}

		System.arraycopy(mApplication.getCh1(), 0, mCh1Copy, 0, mCh1Copy.length);
		System.arraycopy(mApplication.getCh2(), 0, mCh2Copy, 0, mCh2Copy.length);
		System.arraycopy(mApplication.getPreviewCh1(), 0, mCh1Preview, 0, mCh1Preview.length);
		System.arraycopy(mApplication.getPreviewCh2(), 0, mCh2Preview, 0, mCh2Preview.length);

		OsciPrimeApplication.sDataLock.unlock();

		Stats.path(System.currentTimeMillis() - t);

		OsciPrimeApplication.dZoom = sZoom;
		OsciPrimeApplication.dOffx = sOffsetX;
		OsciPrimeApplication.dOffy = sOffsetY;

		canvas.save();
		Paint paint = new Paint();
		canvas.translate(W / 2, H / 2);
		canvas.scale(sZoom, sZoom); 
		canvas.translate(sOffsetX, sOffsetY);

		paint.setStyle(Style.STROKE);
		paint.setAntiAlias(true);

		canvas.drawColor(mApplication.pColorBackground);
		paint.setStrokeWidth(0);
		paint.setColor(mApplication.pColorGrid);

		Grid.draw(canvas, mApplication.mGrid, paint);
		Paint paint2 = new Paint();
		paint2.setColor(mApplication.pColorGrid);
		paint2.setStrokeWidth(Math.min(4.0f / sZoom, 8.0f));
		paint2.setStyle(Style.STROKE);
		canvas.drawPath(mApplication.mOrigin, paint2);

		paint2 = new Paint();
		paint2.setColor(mApplication.pColorMeasure);
		paint2.setTypeface(sTypeFace);
		paint2.setAntiAlias(true);
		paint2.setTextSize(70f);

		String text = "Audio Input";
		if (mApplication.pActiveSource == SourceType.USB) {
			text = "Usb Input";
		} else if (mApplication.pActiveSource == SourceType.NETWORK) {
			text = "Network Input";
		}

		if (mApplication.pNetworkSinkSate == OsciPrimeApplication.NETWORK_SINK_DISCONNECTED)
			text += " / Network Sink enabled, waiting for client, IP: "
					+ mApplication.pServerIp;
		else if (mApplication.pNetworkSinkSate == OsciPrimeApplication.NETWORK_SINK_CONNECTED)
			text += " / Network Sink enabled, client connected";

		canvas.drawText(text, -OsciPrimeApplication.WIDTH / 2.f,
				-OsciPrimeApplication.HEIGHT / 2f - 70f, paint2);

		if (sNexusAd != null) {
			canvas.translate(-OsciPrimeApplication.WIDTH / 2,
					OsciPrimeApplication.OFFSET_DRAWING_TWO);
			paint.setColor(mApplication.pColorGrid);
			canvas.drawBitmap(sNexusAd, 0, 0, paint);
			canvas.translate(OsciPrimeApplication.WIDTH / 2,
					-OsciPrimeApplication.OFFSET_DRAWING_TWO);
		}

		paint.setAntiAlias(true);
		paint.setStrokeMiter(6);
		paint.setStrokeWidth(0);

		// CH1
		if (mApplication.pShowCh1) {
			paint.setStyle(Style.STROKE);
			canvas.translate(0, mApplication.pOffsetCh1);
			paint.setColor(mApplication.pColorCh1);
			float attVal = mApplication.getActiveCalibration().getCh1AttenuationValues()[mApplication.pAttenuationSettingCh1];
			if(mApplication.pActiveSource == SourceType.USB){
				attVal *= OsciUsbSource.POST_SCALE;
			}
			float offVal = mApplication.getActiveCalibration().getCh1Offsets()[mApplication.pAttenuationSettingCh1];
			if (mApplication.pDrawClipCh1) {
				paint.setStyle(Style.FILL);
				paint.setAlpha(RANGE_INDICATION_OPACITY);
				float cutpos = 0;
				float cutneg = 0;
				cutpos = attVal
						* ((1 << (mApplication.pResolutionInBits - 1)) - 1);
				cutpos -= (offVal*attVal);
				cutneg = -attVal
						* (1 << (mApplication.pResolutionInBits - 1));
				cutneg -= (offVal*attVal);
				
				float off = mApplication.pOffsetCh1;
				if (invY(0, off) > cutneg && invY(H, off) < cutpos) {
					canvas.drawRect(-OsciPrimeApplication.WIDTH / 2,
							invY(H, off), OsciPrimeApplication.WIDTH / 2,
							invY(0, off), paint);
				} else if (invY(0, off) > cutneg) {
					canvas.drawRect(-OsciPrimeApplication.WIDTH / 2, cutpos,
							OsciPrimeApplication.WIDTH / 2, invY(0, off), paint);
				} else if (invY(H, off) < cutpos) {
					canvas.drawRect(-OsciPrimeApplication.WIDTH / 2,
							invY(H, off), OsciPrimeApplication.WIDTH / 2,
							cutneg, paint);
				} else {
					canvas.drawRect(-OsciPrimeApplication.WIDTH / 2, cutpos,
							OsciPrimeApplication.WIDTH / 2, cutneg, paint);
				}

			}

			paint.setStyle(Style.STROKE);
			paint.setAlpha(255);
			// canvas.drawPath(mCh1, paint);
			Channel.draw(canvas, mCh1Copy, attVal, paint);

			DrawOffsetHandle.ch1(canvas, mApplication);
			if (mApplication.pTriggerChannel == OsciPrimeApplication.CH1) {
				canvas.translate(0, mApplication.pTriggerLevelCh1);
				DrawTriggerHandle.ch1(canvas, mApplication);
				canvas.translate(0, -mApplication.pTriggerLevelCh1);
			}
			canvas.translate(0, -mApplication.pOffsetCh1);
		}

		// CH2
		if (mApplication.pShowCh2) {
			paint.setStyle(Style.STROKE);
			canvas.translate(0, mApplication.pOffsetCh2);
			paint.setColor(mApplication.pColorCh2);

			float attVal = mApplication.getActiveCalibration().getCh2AttenuationValues()[mApplication.pAttenuationSettingCh2];
			float offVal = mApplication.getActiveCalibration().getCh2Offsets()[mApplication.pAttenuationSettingCh2];

			if(mApplication.pActiveSource == SourceType.USB){
				attVal *= OsciUsbSource.POST_SCALE;
			}
			
			if (mApplication.pDrawClipCh2) {
				paint.setStyle(Style.FILL);
				paint.setAlpha(RANGE_INDICATION_OPACITY);
				float cutpos = 0;
				float cutneg = 0;
				cutpos = 
					attVal * ((1 << (mApplication.pResolutionInBits - 1)) - 1);
				cutpos -= (offVal*attVal);
				cutneg = -attVal
						* (1 << (mApplication.pResolutionInBits - 1));
				cutneg -= (offVal*attVal);

				float off = mApplication.pOffsetCh2;
				if (invY(0, off) > cutneg && invY(H, off) < cutpos) {
					canvas.drawRect(-OsciPrimeApplication.WIDTH / 2,
							invY(H, off), OsciPrimeApplication.WIDTH / 2,
							invY(0, off), paint);
				} else if (invY(0, off) > cutneg) {
					canvas.drawRect(-OsciPrimeApplication.WIDTH / 2, cutpos,
							OsciPrimeApplication.WIDTH / 2, invY(0, off), paint);
				} else if (invY(H, off) < cutpos) {
					canvas.drawRect(-OsciPrimeApplication.WIDTH / 2,
							invY(H, off), OsciPrimeApplication.WIDTH / 2,
							cutneg, paint);
				} else {
					canvas.drawRect(-OsciPrimeApplication.WIDTH / 2, cutpos,
							OsciPrimeApplication.WIDTH / 2, cutneg, paint);
				}
			}

			paint.setStyle(Style.STROKE);
			paint.setAlpha(255);

			// canvas.drawPath(mCh2, paint);
			Channel.draw(canvas, mCh2Copy, attVal, paint);
			DrawOffsetHandle.ch2(canvas, mApplication);
			if (mApplication.pTriggerChannel == OsciPrimeApplication.CH2) {
				canvas.translate(0, mApplication.pTriggerLevelCh2);
				DrawTriggerHandle.ch2(canvas, mApplication);
				canvas.translate(0, -mApplication.pTriggerLevelCh2);
			}
			canvas.translate(0, -mApplication.pOffsetCh2);

		}

		for (HandleDrawable handle : mHandles) {
			handle.draw(canvas);
		}

		// measurement
		if (mApplication.pActiveOverlay == OsciPrimeApplication.OVERLAY_MEASURE) {
			DrawMeasureOverlay.draw(canvas, mApplication);

		}
		
		if(mApplication.pShowBufferPreview){
			canvas.translate(-OsciPrimeApplication.WIDTH/2, OsciPrimeApplication.HEIGHT/2);
			if(mApplication.pShowCh1){
				BufferPreview.drawPreview(canvas, mCh1Preview, mApplication.pColorCh1, (int)OsciPrimeApplication.WIDTH, (int)500, mApplication); 
				canvas.translate(0, 500);
			}
			if(mApplication.pShowCh2)
				BufferPreview.drawPreview(canvas, mCh2Preview, mApplication.pColorCh2, (int)OsciPrimeApplication.WIDTH, (int)500, mApplication); 
			
			if(mApplication.pShowCh1){
				canvas.translate(-OsciPrimeApplication.WIDTH/2, -OsciPrimeApplication.HEIGHT/2-500);
			}else{
				canvas.translate(-OsciPrimeApplication.WIDTH/2, -OsciPrimeApplication.HEIGHT/2);
			}
		}
		// //CH1 copy
		// paint.setStyle(Style.STROKE);
		// canvas.translate(0, OsciPrimeApplication.OFFSET_DRAWING_TWO);
		// canvas.translate(0,mApplication.pOffsetCh1);
		// paint.setColor(mApplication.pColorCh1);
		// canvas.drawPath(mApplication.mPathCh1, paint);
		//
		// //CH2 copy
		// paint.setStyle(Style.STROKE);
		// canvas.translate(0,-mApplication.pOffsetCh1+mApplication.pOffsetCh2);
		// paint.setColor(mApplication.pColorCh2);
		// canvas.drawPath(mApplication.mPathCh2, paint);
		// canvas.translate(0,-mApplication.pOffsetCh2);
		// canvas.translate(0, -OsciPrimeApplication.OFFSET_DRAWING_TWO);

		long d = System.currentTimeMillis() - t;
		OsciPrimeApplication.dDrawingTime = d;
		// L.d("Drawing the frame took [ms] " + d);
		

		if (mZoomOffsetFlag) {
			if (d > 15) {
				if (mAACount > 5) {
					mAA = false;
					mZoomOffsetFlag = false;
				} else {
					mAACount++;
				}
			} else {
				mAA = true;
				mAACount = 0;
			}
		}
		// END DRAW WORLD
		canvas.restore();
		// DRAW UI STUFF

		if (mApplication.pActiveOverlay == OsciPrimeApplication.OVERLAY_DEBUG) {
			InfoText.draw(canvas, mApplication, this, new OverlayDebug());
		}
		if (mApplication.pActiveOverlay == OsciPrimeApplication.OVERLAY_MEASURE) {
			InfoText.draw(canvas, mApplication, this, new OverlayMeasure());
		}
		if (mApplication.pActiveOverlay == OsciPrimeApplication.OVERLAY_OFFSET) {
			InfoText.draw(canvas, mApplication, this, new OverlayOffset());
		}

		Stats.draw(System.currentTimeMillis() - t);
	}

	Bitmap mBitmap = null;

	public Bitmap getBitmap(int width) {// h/w = H/W = > h = w*H/W
		int w = width;
		float ratio = (float) OsciPrimeApplication.HEIGHT
				/ (float) OsciPrimeApplication.WIDTH;
		int h = (int) (w * ratio) + 1;
		if (mApplication.pIncludeGrid
				|| mApplication.pActiveOverlay == OsciPrimeApplication.OVERLAY_MEASURE) {
			int numLines = mApplication.pActiveOverlay == OsciPrimeApplication.OVERLAY_MEASURE ? 5
					: 4;
			final int TEXTHEIGHT = (int) (20 * (numLines) * 1.6);
			h = (int) (w * ratio + TEXTHEIGHT) + 1;// include textinfo too
		}
		if (mBitmap != null)
			mBitmap.recycle();
		mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(mBitmap);
		canvas.save();
		canvas.drawRGB(255, 255, 255);
		Paint paint = new Paint();
		paint.setColor(Color.BLUE);
		float factor = OsciPrimeApplication.WIDTH / (float) w;
		canvas.scale(1f / factor, 1f / factor);
		canvas.translate(OsciPrimeApplication.WIDTH / 2,
				OsciPrimeApplication.HEIGHT / 2);

		// copypaste draw world
		paint.setStyle(Style.STROKE);

		paint.setAntiAlias(true);

		canvas.drawColor(mApplication.pColorBackground);
		paint.setStrokeWidth(0);
		paint.setColor(mApplication.pColorGrid);
		if (mApplication.pIncludeGrid) {
			Grid.draw(canvas, mApplication.mGrid, paint);
			// canvas.drawPath(mApplication.mGridPath, paint);
		}
		if (!mAA)
			paint.setAntiAlias(false);

		paint.setStrokeWidth(0);
		if (mApplication.pIncludeCh1) {
			// CH1
			paint.setStyle(Style.STROKE);
			canvas.translate(0, mApplication.pOffsetCh1);
			paint.setColor(mApplication.pColorCh1);

			float attVal = mApplication.getActiveCalibration().getCh1AttenuationValues()[mApplication.pAttenuationSettingCh1];
			Channel.draw(canvas, mCh1Copy, attVal, paint);

			if (mApplication.pIncludeTrigger) {
				if (mApplication.pActiveOverlay == OsciPrimeApplication.OVERLAY_OFFSET
						|| mApplication.pActiveOverlay == OsciPrimeApplication.OVERLAY_DEBUG) {
					// handle
					DrawOffsetHandle.ch1(canvas, mApplication);
					if (mApplication.pTriggerChannel == OsciPrimeApplication.CH1) {
						canvas.translate(0, mApplication.pTriggerLevelCh1);
						DrawTriggerHandle.ch1(canvas, mApplication);
						canvas.translate(0, -mApplication.pTriggerLevelCh1);
					}
				}
			}
			canvas.translate(0, -mApplication.pOffsetCh1);
		}

		if (mApplication.pIncludeCh2) {
			// CH2
			paint.setStyle(Style.STROKE);
			canvas.translate(0, mApplication.pOffsetCh2);
			paint.setColor(mApplication.pColorCh2);

			float attVal = mApplication.getActiveCalibration().getCh1AttenuationValues()[mApplication.pAttenuationSettingCh1];
			Channel.draw(canvas, mCh2Copy, attVal, paint);

			if (mApplication.pIncludeTrigger) {
				if (mApplication.pActiveOverlay == OsciPrimeApplication.OVERLAY_OFFSET
						|| mApplication.pActiveOverlay == OsciPrimeApplication.OVERLAY_DEBUG) {
					// handle
					DrawOffsetHandle.ch2(canvas, mApplication);
					if (mApplication.pTriggerChannel == OsciPrimeApplication.CH2) {
						canvas.translate(0, mApplication.pTriggerLevelCh2);
						DrawTriggerHandle.ch2(canvas, mApplication);
						canvas.translate(0, -mApplication.pTriggerLevelCh2);
					}
				}
			}

			canvas.translate(0, -mApplication.pOffsetCh2);
		}

		// measurement
		if (mApplication.pActiveOverlay == OsciPrimeApplication.OVERLAY_MEASURE) {
			DrawMeasureOverlay.draw(canvas, mApplication);
		}
		// END DRAW WORLD
		canvas.restore();
		if (mApplication.pIncludeGrid) {
			InfoTextScreenShot.draw(canvas, mApplication, width,
					(int) (w * ratio) + 1, new OverlayOffset());
		}
		if (mApplication.pActiveOverlay == OsciPrimeApplication.OVERLAY_MEASURE) {
			InfoTextScreenShot.draw(canvas, mApplication, width,
					(int) (w * ratio) + 1, new OverlayMeasure());
		}
		return mBitmap;
	}

	public static final int OFFSET_HANDLE_WIDTH = 150;
	public static final int OFFSET_HANDLE_HEIGHT = 100;

	private void initHandles() {
		mHandles.clear();
		
		Path handle = new Path();
		handle.setFillType(Path.FillType.EVEN_ODD);

		// arrow up
		handle.moveTo(0, OFFSET_HANDLE_HEIGHT);
		handle.lineTo(OFFSET_HANDLE_WIDTH, OFFSET_HANDLE_HEIGHT);
		handle.lineTo(OFFSET_HANDLE_WIDTH / 2, 0);
		handle.lineTo(0, OFFSET_HANDLE_HEIGHT);
		handle.close();

		mHandles.add(new HandleDrawable(handle, new HandleDrawable.ICallback() {
			@Override
			public void onUpdated(HandleDrawable drawable) {
				mApplication.pMeasureHandleHor2 = drawable.y;
			}
		}, 0, mApplication.pMeasureHandleHor2, 0, 0, OFFSET_HANDLE_WIDTH,
				OFFSET_HANDLE_HEIGHT, mApplication,
				OsciPrimeApplication.OVERLAY_MEASURE, false, false,
				HandleDrawable.COLOR_MEAS, HandleDrawable.TYPE_GENERIC));

		handle.reset();
		handle.moveTo(0, 0);
		handle.lineTo(OFFSET_HANDLE_WIDTH / 2, OFFSET_HANDLE_HEIGHT);
		handle.lineTo(OFFSET_HANDLE_WIDTH, 0);
		handle.lineTo(0, 0);
		handle.close();

		// arrow down
		mHandles.add(new HandleDrawable(handle, new HandleDrawable.ICallback() {
			@Override
			public void onUpdated(HandleDrawable drawable) {
				mApplication.pMeasureHandleHor1 = drawable.y;
			}
		}, 0, mApplication.pMeasureHandleHor1, 0, -OFFSET_HANDLE_HEIGHT,
				OFFSET_HANDLE_WIDTH, OFFSET_HANDLE_HEIGHT, mApplication,
				OsciPrimeApplication.OVERLAY_MEASURE, false, false,
				HandleDrawable.COLOR_MEAS, HandleDrawable.TYPE_GENERIC));

		handle.reset();
		handle.moveTo(0, 0);
		handle.lineTo(0, OFFSET_HANDLE_WIDTH);
		handle.lineTo(OFFSET_HANDLE_HEIGHT, OFFSET_HANDLE_WIDTH / 2);
		handle.lineTo(0, 0);
		handle.close();

		// arrow right
		mHandles.add(new HandleDrawable(handle, new HandleDrawable.ICallback() {
			@Override
			public void onUpdated(HandleDrawable drawable) {
				mApplication.pMeasureHandleVert1 = drawable.x;
			}
		}, mApplication.pMeasureHandleVert1, 0, -OFFSET_HANDLE_HEIGHT, 0,
				OFFSET_HANDLE_HEIGHT, OFFSET_HANDLE_WIDTH, mApplication,
				OsciPrimeApplication.OVERLAY_MEASURE, false, false,
				HandleDrawable.COLOR_MEAS, HandleDrawable.TYPE_GENERIC));

		handle.reset();
		handle.moveTo(OFFSET_HANDLE_HEIGHT, 0);
		handle.lineTo(0, OFFSET_HANDLE_WIDTH / 2);
		handle.lineTo(OFFSET_HANDLE_HEIGHT, OFFSET_HANDLE_WIDTH);
		handle.lineTo(OFFSET_HANDLE_HEIGHT, 0);
		handle.close();

		// arrow left
		mHandles.add(new HandleDrawable(handle, new HandleDrawable.ICallback() {
			@Override
			public void onUpdated(HandleDrawable drawable) {
				mApplication.pMeasureHandleVert2 = drawable.x;
			}
		}, mApplication.pMeasureHandleVert2, 0, 0, 0, OFFSET_HANDLE_HEIGHT,
				OFFSET_HANDLE_WIDTH, mApplication,
				OsciPrimeApplication.OVERLAY_MEASURE, false, false,
				HandleDrawable.COLOR_MEAS, HandleDrawable.TYPE_GENERIC));

		handle.reset();
		handle.moveTo(0, 0);
		handle.lineTo(OFFSET_HANDLE_WIDTH, 0);
		handle.lineTo(OFFSET_HANDLE_WIDTH, OFFSET_HANDLE_HEIGHT / 2);
		handle.lineTo(0, OFFSET_HANDLE_HEIGHT / 2);
		handle.close();

		final HandleDrawable triggerCh1 = new HandleDrawable(
				handle,
				new HandleDrawable.ICallback() {
					@Override
					public void onUpdated(HandleDrawable drawable) {
						mApplication.pTriggerLevelCh1 = -mApplication.pOffsetCh1
								+ drawable.y;
						resample();
					}
				}, OsciPrimeApplication.WIDTH / 2,
				mApplication.pTriggerLevelCh1 + mApplication.pOffsetCh1, 0,
				-OFFSET_HANDLE_HEIGHT / 4, OFFSET_HANDLE_WIDTH,
				OFFSET_HANDLE_HEIGHT / 2, mApplication,
				OsciPrimeApplication.OVERLAY_MEASURE
						| OsciPrimeApplication.OVERLAY_OFFSET, true, false,
				HandleDrawable.COLOR_CH1,
				HandleDrawable.TYPE_HANDLE_TRIGGER_CH1);

		final HandleDrawable triggerCh2 = new HandleDrawable(
				handle,
				new HandleDrawable.ICallback() {
					@Override
					public void onUpdated(HandleDrawable drawable) {
						mApplication.pTriggerLevelCh2 = -mApplication.pOffsetCh2
								+ drawable.y;
						resample();
					}
				}, OsciPrimeApplication.WIDTH / 2,
				mApplication.pTriggerLevelCh2 + mApplication.pOffsetCh2, 0,
				-OFFSET_HANDLE_HEIGHT / 4, OFFSET_HANDLE_WIDTH,
				OFFSET_HANDLE_HEIGHT / 2, mApplication,
				OsciPrimeApplication.OVERLAY_MEASURE
						| OsciPrimeApplication.OVERLAY_OFFSET, true, false,
				HandleDrawable.COLOR_CH2,
				HandleDrawable.TYPE_HANDLE_TRIGGER_CH2);

		// ch1 handle
		mHandles.add(new HandleDrawable(handle, new HandleDrawable.ICallback() {
			@Override
			public void onUpdated(HandleDrawable drawable) {
				mApplication.pOffsetCh1 = drawable.y;
				triggerCh1.set(0, mApplication.pTriggerLevelCh1
						+ mApplication.pOffsetCh1);
			}
		}, -OsciPrimeApplication.WIDTH / 2, mApplication.pOffsetCh1,
				-OFFSET_HANDLE_WIDTH, -OFFSET_HANDLE_HEIGHT / 4,
				OFFSET_HANDLE_WIDTH, OFFSET_HANDLE_HEIGHT / 2, mApplication,
				OsciPrimeApplication.OVERLAY_MEASURE
						| OsciPrimeApplication.OVERLAY_OFFSET, true, false,
				HandleDrawable.COLOR_CH1, HandleDrawable.TYPE_HANDLE_CH1));

		// ch2 handle
		mHandles.add(new HandleDrawable(handle, new HandleDrawable.ICallback() {
			@Override
			public void onUpdated(HandleDrawable drawable) {
				mApplication.pOffsetCh2 = drawable.y;
				triggerCh2.set(0, mApplication.pTriggerLevelCh2
						+ mApplication.pOffsetCh2);
			}
		}, -OsciPrimeApplication.WIDTH / 2, mApplication.pOffsetCh2,
				-OFFSET_HANDLE_WIDTH, -OFFSET_HANDLE_HEIGHT / 4,
				OFFSET_HANDLE_WIDTH, OFFSET_HANDLE_HEIGHT / 2, mApplication,
				OsciPrimeApplication.OVERLAY_MEASURE
						| OsciPrimeApplication.OVERLAY_OFFSET, true, false,
				HandleDrawable.COLOR_CH2, HandleDrawable.TYPE_HANDLE_CH2));

		// trigger ch1 handle
		mHandles.add(triggerCh1);
		mHandles.add(triggerCh2);
		
		handle.reset();
		handle.moveTo(0, 0);
		handle.addRect(0, 0, 100, 500, Direction.CW);
		final HandleDrawable window = new WindowHandle(handle, new HandleDrawable.ICallback() {
			@Override
			public void onUpdated(HandleDrawable drawable) {
				int trigger = (int) ((drawable.x+OsciPrimeApplication.WIDTH/2f)/OsciPrimeApplication.WIDTH*mApplication.pCapturedFrameSize);
				trigger = Math.min(trigger, mApplication.pCapturedFrameSize-1);
				trigger = Math.max(trigger, 0);
				resampleWithIndex(trigger);
				//L.d("trigger index "+trigger+" framesize: "+mApplication.pCapturedFrameSize);
			}
		}, OsciPrimeApplication.WIDTH/2, OsciPrimeApplication.HEIGHT/2,0, 0, 100, 500, mApplication, OsciPrimeApplication.OVERLAY_OFFSET | OsciPrimeApplication.OVERLAY_MEASURE, false, true, HandleDrawable.COLOR_MEAS, HandleDrawable.TYPE_HANDLE_WINDOW);
		mHandles.add(window);
		postInvalidate();
	}

//	private float invX(float xT) {
//		return (xT - .5f * W) / sZoom - sOffsetX;
//	}

	private float invY(float yT, float channelOffset) {
		return (yT - .5f * H) / sZoom - sOffsetY - channelOffset;
	}

}