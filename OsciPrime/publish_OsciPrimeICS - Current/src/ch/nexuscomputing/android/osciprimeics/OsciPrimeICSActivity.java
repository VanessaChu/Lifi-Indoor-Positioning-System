
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Html;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication.SourceType;
import ch.nexuscomputing.android.osciprimeics.colorpicker.ColorPicker;
import ch.nexuscomputing.android.osciprimeics.colorpicker.ColorPicker.ColorListener;
import ch.nexuscomputing.android.osciprimeics.news.News;
import ch.nexuscomputing.android.usb.IUsbConnectionHandler;

@SuppressLint("NewApi")
public class OsciPrimeICSActivity extends Activity {
	private OsciPrimeApplication mApplication;

	protected static final int STATUS = 0;
	protected static final int SAMPLES = 1;
	protected static final int RESAMPLE_FROM_SURFACE_VIEW = 2;
	protected static final int RESAMPLE_FROM_SURFACE_VIEW_WITH_INDEX = 3;
	private int mServiceState;
	/** Called when the activity is first created. */
	public static OsciSurfaceView sSurfaceView;
	public static OsciPrimeOverlayHelpView sOsciPrimeOverlayHelpView;

	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case STATUS:
				L.d("ON_STATUS "
						+ (msg.arg1 == OsciPrimeService.IDLE ? "idle"
								: "running"));
				mServiceState = msg.arg1;
				updateState(msg.arg1);
				break;
			case SAMPLES:
				sSurfaceView.postInvalidate();
				break;
			case RESAMPLE_FROM_SURFACE_VIEW:
				resample();
				break;
			case RESAMPLE_FROM_SURFACE_VIEW_WITH_INDEX:
				resampleWithTrigger(msg.arg1);
				break;
			default:
				break;
			}
		};
	};

	private final Messenger mActivityMessenger = new Messenger(mHandler);

	private Messenger mServiceMessenger;

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mServiceMessenger = new Messenger(service);
			Message hello = Message.obtain(mHandler);
			hello.replyTo = mActivityMessenger;
			hello.what = OsciPrimeService.REGISTER;
			try {
				mServiceMessenger.send(hello);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	protected void onStart() {
		super.onStart();
		mApplication.onStart(this, sSurfaceView);
		startService(new Intent(OsciPrimeService.class.getName()));
		bindService(new Intent(OsciPrimeService.class.getName()),
				mServiceConnection, Service.BIND_AUTO_CREATE);
		((ImageButton) findViewById(R.id.btRunStop)).setEnabled(false);
	};

	@Override
	protected void onStop() {
		if(mApplication.pStopSamplingOnClose){
			try {
				if (mServiceMessenger != null
						&& mServiceState == OsciPrimeService.RUNNING) {
					mServiceMessenger.send(mHandler
							.obtainMessage(OsciPrimeService.STOP));
					mServiceMessenger.send(mHandler
							.obtainMessage(OsciPrimeService.STOP_SINK));
					((ImageButton)findViewById(R.id.btRunStop))
							.setImageResource(R.drawable.runstop_a);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		unbindService(mServiceConnection);
		mApplication.onStop(this);
		super.onStop();
	}

	protected void updateState(int arg1) {
		((ImageButton) findViewById(R.id.btRunStop)).setEnabled(true);
		// so when this message arrives it is possible that the singleshot was
		// reset
		if (mApplication.pMode == OsciPrimeApplication.MODE_SINGLESHOT) {
			((ImageButton) findViewById(R.id.btSingleShot))
					.setImageResource(R.drawable.singleshot_a);
		} else {
			((ImageButton) findViewById(R.id.btSingleShot))
					.setImageResource(R.drawable.singleshot);
		}

		if (mServiceMessenger != null && mServiceState == OsciPrimeService.IDLE) {
			((ImageButton) findViewById(R.id.btRunStop))
					.setImageResource(R.drawable.runstop);
		} else {
			((ImageButton) findViewById(R.id.btRunStop))
					.setImageResource(R.drawable.runstop_a);
		}
	}
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mApplication = (OsciPrimeApplication) getApplicationContext();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		sSurfaceView = new OsciSurfaceView(this, mHandler);
		if (Build.VERSION.SDK_INT >= 12) {
			if(mApplication.pFullScreenMode){
				sSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			}
		}
		sOsciPrimeOverlayHelpView = new OsciPrimeOverlayHelpView(this,
				mApplication);
		sOsciPrimeOverlayHelpView.setVisibility(View.GONE);
		setContentView(R.layout.main);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
			fixLowerBarHeight();			
		}
		// HW ACCELERATION
		if (mApplication.pHardwareAccelerated) {
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		}

		((ImageButton) findViewById(R.id.btL))
				.setOnClickListener(new View.OnClickListener() {
					@Override 
					public void onClick(View v) {
						showPanL(v);
					}
				});
		((ImageButton) findViewById(R.id.btLB))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						hidePanL();
					}
				});
		((ImageButton) findViewById(R.id.btR))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showPanR(v);
					}
				});

		((ImageButton) findViewById(R.id.btU))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showPanD(v);
					}
				});

		((ImageButton) findViewById(R.id.btRB))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						hidePanR();
					}
				});

		((ImageButton) findViewById(R.id.btUB))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						hidePanD();
					}
				});

		((ImageButton) findViewById(R.id.btRunStop))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						try {
							if (mServiceMessenger != null
									&& mServiceState == OsciPrimeService.IDLE) {
								mServiceMessenger.send(mHandler
										.obtainMessage(OsciPrimeService.START));
								((ImageButton) v)
										.setImageResource(R.drawable.runstop_a);
							} else {
								mServiceMessenger.send(mHandler
										.obtainMessage(OsciPrimeService.STOP));
								((ImageButton) v)
										.setImageResource(R.drawable.runstop);
							}
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				});
		((ImageButton) findViewById(R.id.btMeasureOverlay))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mApplication.pActiveOverlay = OsciPrimeApplication.OVERLAY_MEASURE;

						((ImageButton) findViewById(R.id.btMeasureOverlay))
								.setImageResource(R.drawable.measure_a);
						((ImageButton) findViewById(R.id.btDebugOverlay))
								.setImageResource(R.drawable.debug);
						((ImageButton) findViewById(R.id.btOffsetOverlay))
								.setImageResource(R.drawable.offset);

						sSurfaceView.postInvalidate();
					}
				});
		((ImageButton) findViewById(R.id.btOffsetOverlay))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mApplication.pActiveOverlay = OsciPrimeApplication.OVERLAY_OFFSET;

						((ImageButton) findViewById(R.id.btMeasureOverlay))
								.setImageResource(R.drawable.measure);
						((ImageButton) findViewById(R.id.btDebugOverlay))
								.setImageResource(R.drawable.debug);
						((ImageButton) findViewById(R.id.btOffsetOverlay))
								.setImageResource(R.drawable.offset_a);

						sSurfaceView.postInvalidate();
					}
				});
		((ImageButton) findViewById(R.id.btDebugOverlay))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mApplication.pActiveOverlay = OsciPrimeApplication.OVERLAY_DEBUG;

						((ImageButton) findViewById(R.id.btMeasureOverlay))
								.setImageResource(R.drawable.measure);
						((ImageButton) findViewById(R.id.btDebugOverlay))
								.setImageResource(R.drawable.debug_a);
						((ImageButton) findViewById(R.id.btOffsetOverlay))
								.setImageResource(R.drawable.offset);

						sSurfaceView.postInvalidate();
					}
				});
		((ImageButton) findViewById(R.id.btSingleShot))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mApplication.pMode == OsciPrimeApplication.MODE_CONTINUOUS) {
							mApplication.pMode = OsciPrimeApplication.MODE_SINGLESHOT;
							((ImageButton) v)
									.setImageResource(R.drawable.singleshot_a);
						} else {
							((ImageButton) v)
									.setImageResource(R.drawable.singleshot);
							mApplication.pMode = OsciPrimeApplication.MODE_CONTINUOUS;
						}
						resample();
						sSurfaceView.postInvalidate();
					}
				});

		((ImageButton) findViewById(R.id.config))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						View child = showOptions(R.layout.general_settings);
						((CheckBox) child
								.findViewById(R.id.chkShowTriggerLabel))
								.setChecked(mApplication.pDrawTriggerLabel);
						((CheckBox) child.findViewById(R.id.chkKillService))
								.setChecked(mApplication.pKillService);
						((CheckBox) child.findViewById(R.id.chkShowCh1))
								.setChecked(mApplication.pShowCh1);
						((CheckBox) child.findViewById(R.id.chkShowCh2))
								.setChecked(mApplication.pShowCh2);
						((CheckBox) child.findViewById(R.id.chkShowClipingCh1))
								.setChecked(mApplication.pDrawClipCh1);
						((CheckBox) child.findViewById(R.id.chkShowClipingCh2))
								.setChecked(mApplication.pDrawClipCh2);
						((CheckBox) child.findViewById(R.id.chkShowBufferPreview))
							.setChecked(mApplication.pShowBufferPreview);
						((CheckBox) child.findViewById(R.id.chkHardware))
								.setChecked(mApplication.pHardwareAccelerated);
						((CheckBox) child.findViewById(R.id.chkFullscreen))
						.setChecked(mApplication.pFullScreenMode);
						((CheckBox) child.findViewById(R.id.chkStopSamplingOnClose))
						.setChecked(mApplication.pStopSamplingOnClose);

						switch (mApplication.pPointsOnView) {
						case 256:
							((RadioButton) child.findViewById(R.id.p256))
									.setChecked(true);
							break;
						case 512:
							((RadioButton) child.findViewById(R.id.p512))
									.setChecked(true);
							break;
						case 1024:
							((RadioButton) child.findViewById(R.id.p1024))
									.setChecked(true);
							break;
						case 2048:
							((RadioButton) child.findViewById(R.id.p2048))
									.setChecked(true);
							break;
						case 4096:
							((RadioButton) child.findViewById(R.id.p4096))
									.setChecked(true);
							break;
						default:
							((RadioButton) child.findViewById(R.id.p256))
									.setChecked(true);
							break;
						}
					}
				});

		((ImageButton) findViewById(R.id.btTriggerSettings))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						View child = showOptions(R.layout.trigger_settings);
						if (mApplication.pTriggerChannel == OsciPrimeApplication.CH1) {
							((RadioButton) child.findViewById(R.id.ch1))
									.setChecked(true);
							if (mApplication.pEdgeCh1 == OsciPrimeApplication.RISING) {
								((RadioButton) child.findViewById(R.id.rising))
										.setChecked(true);
							} else {
								((RadioButton) child.findViewById(R.id.falling))
										.setChecked(true);
							}
						} else {
							((RadioButton) child.findViewById(R.id.ch2))
									.setChecked(true);
							if (mApplication.pEdgeCh2 == OsciPrimeApplication.RISING) {
								((RadioButton) child.findViewById(R.id.rising))
										.setChecked(true);
							} else {
								((RadioButton) child.findViewById(R.id.falling))
										.setChecked(true);
							}
						}
					}
				});

		((ImageButton) findViewById(R.id.btSource))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						View child = showOptions(R.layout.source_settings);
						if (mApplication.pActiveSource == SourceType.AUDIO) {
							((RadioButton) child.findViewById(R.id.srcAudio))
									.setChecked(true);
						} else if (mApplication.pActiveSource == SourceType.USB) {
							((RadioButton) child.findViewById(R.id.srcUsb))
									.setChecked(true);
						} else if (mApplication.pActiveSource == SourceType.NETWORK) {
							((RadioButton) child.findViewById(R.id.srcNetwork))
									.setChecked(true);
						}
						((CheckBox) child.findViewById(R.id.chkNetworkSink))
								.setChecked(!(mApplication.pNetworkSinkSate == OsciPrimeApplication.NETWORK_SINK_DISABLED));
						((CheckBox) child
								.findViewById(R.id.chkProbeCompensation))
								.setChecked(mApplication.pProbeCopensation);
					}
				});

		((ImageButton) findViewById(R.id.btScreenshot))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						View child = showOptions(R.layout.screenshot_settings);
						if (mApplication.pIncludeCh1)
							((CheckBox) child.findViewById(R.id.include_ch1))
									.setChecked(true);
						if (mApplication.pIncludeCh2)
							((CheckBox) child.findViewById(R.id.include_ch2))
									.setChecked(true);
						if (mApplication.pIncludeTrigger)
							((CheckBox) child
									.findViewById(R.id.include_trigger))
									.setChecked(true);
						if (mApplication.pIncludeGrid)
							((CheckBox) child.findViewById(R.id.include_grid))
									.setChecked(true);
						if (mApplication.pExportWidth == 800)
							((RadioButton) child.findViewById(R.id.chkWidth800))
									.setChecked(true);
						if (mApplication.pExportWidth == 1200)
							((RadioButton) child
									.findViewById(R.id.chkWidth1200))
									.setChecked(true);
						if (mApplication.pExportWidth == 1600)
							((RadioButton) child
									.findViewById(R.id.chkWidth1600))
									.setChecked(true);

					}
				});

		((ImageButton) findViewById(R.id.btNews))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						View child = showOptions(R.layout.news);
						TextView title = (TextView) child
								.findViewById(R.id.title);
						TextView text = (TextView) child
								.findViewById(R.id.text);
						TextView link = (TextView) child
								.findViewById(R.id.link);

						News news = mApplication.pNews;
						if (news != null) {
							title.setText(news.getTitle());
							text.setText(Html.fromHtml(news.getText()));
							link.setText(Html.fromHtml(news.getLinkUrl()));
							mApplication.pUnreadNews = false;
							((ImageButton) findViewById(R.id.btNews))
									.setImageResource(R.drawable.news);
						}
					}
				});

		((ImageButton) findViewById(R.id.btHelp))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showHelpOverlay();
					}
				});

		final SeekBar sb1 = (SeekBar) findViewById(R.id.barCh1);
		final SeekBar sb2 = (SeekBar) findViewById(R.id.barCh2);

		sb1.setMax(OsciPrimeApplication.PROGRESS_MAX);
		sb2.setMax(OsciPrimeApplication.PROGRESS_MAX);

		SeekBar.OnSeekBarChangeListener sbchlstnr = new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (seekBar == sb1) {
					if (fromUser
							&& mApplication.pRunningAttenuationCh1 == mApplication.pAttenuationSettingCh1) {
						mApplication.changeAttenuationCalibration(progress,
								OsciPrimeApplication.CH1);
						resample();

					}
				} else {
					if (fromUser
							&& mApplication.pRunningAttenuationCh2 == mApplication.pAttenuationSettingCh2) {
						mApplication.changeAttenuationCalibration(progress,
								OsciPrimeApplication.CH2);
						resample();
					}
				}
			}
		};

		sb1.setOnSeekBarChangeListener(sbchlstnr);
		sb2.setOnSeekBarChangeListener(sbchlstnr);

		sSurfaceView.invalidate();

		OsciRating.prompt(this, mApplication.pInterfaceColor);

		SharedPreferences sp = getSharedPreferences("default", MODE_PRIVATE);
		boolean firstStart = sp.getBoolean("firstStart", true);
		if (firstStart) {
			showHelpOverlay();
			sp.edit().putBoolean("firstStart", false).commit();
		} else if (sRequestShowOverlay) {
			showHelpOverlay();
		}
		
		//getWindow().setBackgroundDrawable(new ColorDrawable(mApplication.pColorBackground)); 
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void showPanL(View v) {
		findViewById(R.id.panL).setVisibility(View.VISIBLE);
		((ImageButton) findViewById(R.id.btLB)).setVisibility(View.VISIBLE);
		v.setVisibility(View.INVISIBLE);
		sSurfaceView.invalidate();
	}

	private void hidePanL() {
		findViewById(R.id.panL).setVisibility(View.INVISIBLE);
		((ImageButton) findViewById(R.id.btL)).setVisibility(View.VISIBLE);
		((ImageButton) findViewById(R.id.btLB)).setVisibility(View.INVISIBLE);
		sSurfaceView.invalidate();
	}

	private void showPanR(View v) {
		findViewById(R.id.panR).setVisibility(View.VISIBLE);
		((ImageButton) findViewById(R.id.btRB)).setVisibility(View.VISIBLE);
		v.setVisibility(View.INVISIBLE);
		mApplication.pBarWidth = findViewById(R.id.panR).getWidth();
		sSurfaceView.invalidate();
	}

	private void hidePanR() {
		findViewById(R.id.panR).setVisibility(View.INVISIBLE);
		((ImageButton) findViewById(R.id.btR)).setVisibility(View.VISIBLE);
		((ImageButton) findViewById(R.id.btRB)).setVisibility(View.INVISIBLE);
		mApplication.pBarWidth = 0;
		sSurfaceView.invalidate();
	}

	private void showPanD(View v) {
		findViewById(R.id.panD).setVisibility(View.VISIBLE);
		((ImageButton) findViewById(R.id.btUB)).setVisibility(View.VISIBLE);
		v.setVisibility(View.INVISIBLE);
		sSurfaceView.invalidate();
	}

	private void hidePanD() {
		findViewById(R.id.panD).setVisibility(View.INVISIBLE);
		SeekBar sb1 = (SeekBar) findViewById(R.id.barCh1);
		SeekBar sb2 = (SeekBar) findViewById(R.id.barCh2);

		sb1.setVisibility(View.INVISIBLE);
		sb2.setVisibility(View.INVISIBLE);
		((ImageButton) findViewById(R.id.btU)).setVisibility(View.VISIBLE);
		((ImageButton) findViewById(R.id.btUB)).setVisibility(View.INVISIBLE);
		sSurfaceView.invalidate();
	}

	@Override
	public void onBackPressed() {
		View panD = findViewById(R.id.panD);
		View panR = findViewById(R.id.panR);
		View panL = findViewById(R.id.panL);
		View optionH = findViewById(R.id.layout_options_holder);
		View colorH = findViewById(R.id.layout_color_holder);
		boolean closed = false;

		if (colorH.getVisibility() == View.VISIBLE) {
			colorH.setVisibility(View.INVISIBLE);
			closed = true;
		}

		if (optionH.getVisibility() == View.VISIBLE && !closed) {
			optionH.setVisibility(View.INVISIBLE);
			closed = true;
		}

		if (sOsciPrimeOverlayHelpView.getVisibility() == View.VISIBLE
				&& !closed) {
			sOsciPrimeOverlayHelpView.setVisibility(View.GONE);
			sOsciPrimeOverlayHelpView.reset();
			hidePanD();
			hidePanR();
			hidePanL();
			closed = true;
		}

		if (!closed) {
			if (panD.getVisibility() == View.VISIBLE) {
				hidePanD();
				closed = true;
			}
			if (panR.getVisibility() == View.VISIBLE) {
				hidePanR();
				closed = true;
			}

			if (panL.getVisibility() == View.VISIBLE) {
				hidePanL();
				closed = true;
			}
		}

		if (!closed)
			super.onBackPressed();
	}

	private View showOptions(int layoutId) {
		ScrollView item = (ScrollView) findViewById(R.id.layout_options);
		item.setBackgroundColor(mApplication.pInterfaceColor);
		item.removeAllViews();
		View child = getLayoutInflater().inflate(layoutId, null);// change
																	// options
																	// for cont
																	// usb src
		item.addView(child);
		item.setVisibility(View.VISIBLE);
		((RelativeLayout) findViewById(R.id.layout_options_holder))
				.setVisibility(View.VISIBLE);
		return child;
	}

	@Override
	protected void onPause() {
		super.onPause();
	}  

	@Override
	protected void onResume() {
		if ((ViewGroup) sSurfaceView.getParent() != null)
			((ViewGroup) sSurfaceView.getParent()).removeView(sSurfaceView);
		sSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		((FrameLayout) findViewById(R.id.rootlayout)).addView(sSurfaceView, 0);

		switch (mApplication.pActiveOverlay) {
		case OsciPrimeApplication.OVERLAY_MEASURE:
			((ImageButton) findViewById(R.id.btMeasureOverlay))
					.setImageResource(R.drawable.measure_a);
			((ImageButton) findViewById(R.id.btDebugOverlay))
					.setImageResource(R.drawable.debug);
			((ImageButton) findViewById(R.id.btOffsetOverlay))
					.setImageResource(R.drawable.offset);
			break;
		case OsciPrimeApplication.OVERLAY_OFFSET:
			((ImageButton) findViewById(R.id.btMeasureOverlay))
					.setImageResource(R.drawable.measure);
			((ImageButton) findViewById(R.id.btDebugOverlay))
					.setImageResource(R.drawable.debug);
			((ImageButton) findViewById(R.id.btOffsetOverlay))
					.setImageResource(R.drawable.offset_a);
			break;
		case OsciPrimeApplication.OVERLAY_DEBUG:
			((ImageButton) findViewById(R.id.btMeasureOverlay))
					.setImageResource(R.drawable.measure);
			((ImageButton) findViewById(R.id.btDebugOverlay))
					.setImageResource(R.drawable.debug_a);
			((ImageButton) findViewById(R.id.btOffsetOverlay))
					.setImageResource(R.drawable.offset);
			break;
		default:
			break;
		}

		if (findViewById(R.id.panR).getVisibility() == View.VISIBLE) {
			mApplication.pBarWidth = findViewById(R.id.panR).getWidth();
		} else {
			mApplication.pBarWidth = 0;
		}

		if (mApplication.pUnreadNews) {
			((ImageButton) findViewById(R.id.btNews))
					.setImageResource(R.drawable.news_active);
		}

		super.onResume();
	}

	// BUTTON HANDLERS
	public void closeOptions(View v) {
		((RelativeLayout) findViewById(R.id.layout_options_holder))
				.setVisibility(View.INVISIBLE);
	}

	public void bgColor(View v) {
		findViewById(R.id.layout_color_holder).setVisibility(View.VISIBLE);
		((LinearLayout) findViewById(R.id.layout_color_wrapper))
				.removeAllViews();
		ColorPicker cp = new ColorPicker(this, new ColorListener() {
			@Override
			public void colorChanged(int c) {
				mApplication.pColorBackground = c;
				findViewById(R.id.layout_color_holder).setVisibility(
						View.INVISIBLE);
				//getWindow().setBackgroundDrawable(new ColorDrawable(c)); 
				sSurfaceView.postInvalidate();
			}
		});
		((LinearLayout) findViewById(R.id.layout_color_wrapper)).addView(cp);
	}

	public void ch1Color(View v) {
		findViewById(R.id.layout_color_holder).setVisibility(View.VISIBLE);
		((LinearLayout) findViewById(R.id.layout_color_wrapper))
				.removeAllViews();
		ColorPicker cp = new ColorPicker(this, new ColorListener() {
			@Override
			public void colorChanged(int c) {
				mApplication.pColorCh1 = c;
				findViewById(R.id.layout_color_holder).setVisibility(
						View.INVISIBLE);
				sSurfaceView.postInvalidate();
			}
		});
		((LinearLayout) findViewById(R.id.layout_color_wrapper)).addView(cp);
	}

	public void ch2Color(View v) {
		findViewById(R.id.layout_color_holder).setVisibility(View.VISIBLE);
		((LinearLayout) findViewById(R.id.layout_color_wrapper))
				.removeAllViews();
		ColorPicker cp = new ColorPicker(this, new ColorListener() {
			@Override
			public void colorChanged(int c) {
				mApplication.pColorCh2 = c;
				findViewById(R.id.layout_color_holder).setVisibility(
						View.INVISIBLE);
				sSurfaceView.postInvalidate();
			}
		});
		((LinearLayout) findViewById(R.id.layout_color_wrapper)).addView(cp);
	}

	public void gridColor(View v) {
		findViewById(R.id.layout_color_holder).setVisibility(View.VISIBLE);
		((LinearLayout) findViewById(R.id.layout_color_wrapper))
				.removeAllViews();
		ColorPicker cp = new ColorPicker(this, new ColorListener() {
			@Override
			public void colorChanged(int c) {
				mApplication.pColorGrid = c;
				findViewById(R.id.layout_color_holder).setVisibility(
						View.INVISIBLE);
				sSurfaceView.postInvalidate();
			}
		});
		((LinearLayout) findViewById(R.id.layout_color_wrapper)).addView(cp);
	}

	public void measurementColor(View v) {
		findViewById(R.id.layout_color_holder).setVisibility(View.VISIBLE);
		((LinearLayout) findViewById(R.id.layout_color_wrapper))
				.removeAllViews();
		ColorPicker cp = new ColorPicker(this, new ColorListener() {
			@Override
			public void colorChanged(int c) {
				mApplication.pColorMeasure = c;
				findViewById(R.id.layout_color_holder).setVisibility(
						View.INVISIBLE);
				sSurfaceView.postInvalidate();
			}
		});
		((LinearLayout) findViewById(R.id.layout_color_wrapper)).addView(cp);
	}

	public void interfaceColor(View v) {
		findViewById(R.id.layout_color_holder).setVisibility(View.VISIBLE);
		((LinearLayout) findViewById(R.id.layout_color_wrapper))
				.removeAllViews();
		ColorPicker cp = new ColorPicker(this, new ColorListener() {
			@Override
			public void colorChanged(int c) {
				mApplication.pInterfaceColor = c;
				findViewById(R.id.layout_color_holder).setVisibility(
						View.INVISIBLE);
				updateInterfaceColor();
			}
		});
		((LinearLayout) findViewById(R.id.layout_color_wrapper)).addView(cp);
	}

	public void triggerCH1(View v) {
		mApplication.pTriggerChannel = OsciPrimeApplication.CH1;
		sSurfaceView.invalidate();
	}

	public void triggerCH2(View v) {
		mApplication.pTriggerChannel = OsciPrimeApplication.CH2;
		sSurfaceView.invalidate();
	}

	public void triggerRising(View v) {
		if (mApplication.pTriggerChannel == OsciPrimeApplication.CH1) {
			mApplication.pEdgeCh1 = OsciPrimeApplication.RISING;
		} else {
			mApplication.pEdgeCh2 = OsciPrimeApplication.RISING;
		}
	}

	public void triggerFalling(View v) {
		if (mApplication.pTriggerChannel == OsciPrimeApplication.CH1) {
			mApplication.pEdgeCh1 = OsciPrimeApplication.FALLING;
		} else {
			mApplication.pEdgeCh2 = OsciPrimeApplication.FALLING;
		}
	}

	public void p256(View v) {
		mApplication.pPointsOnView = 256;
		updateFrameSize();
		sSurfaceView.invalidate();
	}

	public void p512(View v) {
		mApplication.pPointsOnView = 512;
		updateFrameSize();
		sSurfaceView.invalidate();
	}

	public void p1024(View v) {
		mApplication.pPointsOnView = 1024;
		updateFrameSize();
		sSurfaceView.invalidate();
	}

	public void p2048(View v) {
		mApplication.pPointsOnView = 2048;
		updateFrameSize();
		sSurfaceView.invalidate();
	}

	public void p4096(View v) {
		mApplication.pPointsOnView = 4096;
		updateFrameSize();
		sSurfaceView.invalidate();
	}

	public void interleave_up(View v) {
		mApplication.pInterleave = Math.min(mApplication.pMaxInterleave,
				mApplication.pInterleave << 1);
		updateFrameSize();
	}

	public void interleave_down(View v) {
		mApplication.pInterleave = Math.max(mApplication.pInterleave >> 1, 1);
		updateFrameSize();
	}

	public void ch1up(View v) {
		//TODO this is a temporary lock until we find a proper way to handle
		//changes to attenuation in stop mode
		if(mServiceState == OsciPrimeService.IDLE){
			return;
		}
		mApplication.pAttenuationSettingCh1 = Math.min(
				OsciPrimeApplication.NUM_ATTENUATION_SETTINGS - 1,
				mApplication.pAttenuationSettingCh1 + 1);
		updateSeekbars();

		try {
			mServiceMessenger.send(Message.obtain(null,
					OsciPrimeService.ATTENUATION_CHANGED));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void ch1down(View v) {
		if(mServiceState == OsciPrimeService.IDLE){
			return;
		}
		mApplication.pAttenuationSettingCh1 = Math.max(0,
				mApplication.pAttenuationSettingCh1 - 1);
		updateSeekbars();
		try {
			mServiceMessenger.send(Message.obtain(null,
					OsciPrimeService.ATTENUATION_CHANGED));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void ch2up(View v) {
		if(mServiceState == OsciPrimeService.IDLE){
			return;
		}
		mApplication.pAttenuationSettingCh2 = Math.min(
				OsciPrimeApplication.NUM_ATTENUATION_SETTINGS - 1,
				mApplication.pAttenuationSettingCh2 + 1);
		updateSeekbars();
		try {
			mServiceMessenger.send(Message.obtain(null,
					OsciPrimeService.ATTENUATION_CHANGED));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void ch2down(View v) {
		if(mServiceState == OsciPrimeService.IDLE){
			return;
		}
		mApplication.pAttenuationSettingCh2 = Math.max(0,
				mApplication.pAttenuationSettingCh2 - 1);
		updateSeekbars();
		try {
			mServiceMessenger.send(Message.obtain(null,
					OsciPrimeService.ATTENUATION_CHANGED));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void includeCh1(View v) {
		SeekBar sb1 = (SeekBar) findViewById(R.id.barCh1);
		if (sb1.getVisibility() == View.VISIBLE)
			mApplication.pIncludeCh1 = ((CheckBox) v).isChecked();
	}

	public void includeCh2(View v) {
		mApplication.pIncludeCh2 = ((CheckBox) v).isChecked();
	}

	public void includeGrid(View v) {
		mApplication.pIncludeGrid = ((CheckBox) v).isChecked();
	}

	public void includeTrigger(View v) {
		mApplication.pIncludeTrigger = ((CheckBox) v).isChecked();
	}

	public void width800(View v) {
		mApplication.pExportWidth = 800;
	}

	public void width1200(View v) {
		mApplication.pExportWidth = 1200;
	}

	public void width1600(View v) {
		mApplication.pExportWidth = 1600;
	}

	public void calibrate(View v) {
		if (mServiceMessenger != null) {
			try {
				mServiceMessenger.send(Message.obtain(null,
						OsciPrimeService.CALIBRATE));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void calgains(View v) {
		SeekBar sb1 = (SeekBar) findViewById(R.id.barCh1);
		SeekBar sb2 = (SeekBar) findViewById(R.id.barCh2);
		if (sb1.getVisibility() == View.VISIBLE) {
			sb1.setVisibility(View.INVISIBLE);
			sb2.setVisibility(View.INVISIBLE);
		} else {
			updateSeekbars();
			sb1.setVisibility(View.VISIBLE);
			sb2.setVisibility(View.VISIBLE);
		}
	}

	private void updateSeekbars() {
		SeekBar sb1 = (SeekBar) findViewById(R.id.barCh1);
		SeekBar sb2 = (SeekBar) findViewById(R.id.barCh2);
		sb1.setProgress(mApplication
				.progressFromSetting(OsciPrimeApplication.CH1));
		sb2.setProgress(mApplication
				.progressFromSetting(OsciPrimeApplication.CH2));
		sSurfaceView.postInvalidate();
		resample();
	}

	public void sourceAudio(View v) {
		if (mApplication.pActiveSource == SourceType.AUDIO)
			return;
		mApplication.pActiveSource = SourceType.AUDIO;

		if (mServiceState == OsciPrimeService.RUNNING) {
			((ImageButton) findViewById(R.id.btRunStop)).setEnabled(false);
			try {
				mServiceMessenger.send(mHandler
						.obtainMessage(OsciPrimeService.STOP));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void sourceUsb(View v) {
		if (mApplication.pActiveSource == SourceType.USB)
			return;
		mApplication.pActiveSource = SourceType.USB;
		if (mServiceState == OsciPrimeService.RUNNING) {
			((ImageButton) findViewById(R.id.btRunStop)).setEnabled(false);
			try {
				mServiceMessenger.send(mHandler
						.obtainMessage(OsciPrimeService.STOP));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void sourceNetwork(View v) {
		configIp(v);
		if (mApplication.pActiveSource == SourceType.NETWORK)
			return;
		//disable network sink then
		if(mApplication.pNetworkSinkSate != OsciPrimeApplication.NETWORK_SINK_DISABLED){
			try {
				mServiceMessenger.send(mHandler.obtainMessage(OsciPrimeService.STOP_SINK));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mApplication.pActiveSource = SourceType.NETWORK;
		if (mServiceState == OsciPrimeService.RUNNING) {
			((ImageButton) findViewById(R.id.btRunStop)).setEnabled(false);
			try {
				mServiceMessenger.send(mHandler
						.obtainMessage(OsciPrimeService.STOP));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void networkSink(View v) {
		boolean start = ((CheckBox) v).isChecked();
		if(mApplication.pActiveSource == SourceType.NETWORK){
			Toast.makeText(this, R.string.cannot_run_a_network_sink_while_acuiring_data_from_a_network_source, Toast.LENGTH_LONG).show();
			((CheckBox) v).setChecked(false);
			return;
		}
		try {
			mServiceMessenger.send(mHandler
					.obtainMessage(start ? OsciPrimeService.START_SINK
							: OsciPrimeService.STOP_SINK));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		sSurfaceView.postInvalidate();
	}

	public void configIp(View v) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("IP Address");
		alert.setMessage("Enter Server IP Address (activate \"Network Sink\" on other devices)");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setText(mApplication.pIpAddress);
		alert.setView(input);

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				mApplication.pIpAddress = input.getText().toString();
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}

	public void showTriggerLabel(View v) {
		mApplication.pDrawTriggerLabel = ((CheckBox) v).isChecked();
		sSurfaceView.postInvalidate();
	}

	public void killService(View v) {
		mApplication.pKillService = ((CheckBox) v).isChecked();
	}

	public void hardware(View v) {
		mApplication.pHardwareAccelerated = ((CheckBox) v).isChecked();
		Intent intent = getIntent();
		mApplication.onStop(this);
		finish();
		startActivity(intent);
	}

	public void showCh1(View v) {
		mApplication.pShowCh1 = ((CheckBox) v).isChecked();
		sSurfaceView.invalidate();
	}

	public void showCh2(View v) {
		mApplication.pShowCh2 = ((CheckBox) v).isChecked();
		sSurfaceView.invalidate();
	}

	public void showClippingCh1(View v) {
		mApplication.pDrawClipCh1 = ((CheckBox) v).isChecked();
		sSurfaceView.postInvalidate();
	}

	public void showClippingCh2(View v) {
		mApplication.pDrawClipCh2 = ((CheckBox) v).isChecked();
		sSurfaceView.postInvalidate();
	}
	
	public void showBufferPreview(View v){
		mApplication.pShowBufferPreview = ((CheckBox) v).isChecked();
		sSurfaceView.postInvalidate();
	}
	
	public void stopSamplingOnClose(View v){
		boolean val = ((CheckBox)v).isChecked();
		mApplication.pStopSamplingOnClose = val;
	}
	
	public void fullscreen(View v){
		boolean val = ((CheckBox)v).isChecked();
		mApplication.pFullScreenMode = val;
	}

	public void probeCompensation(View v) {
		mApplication.pProbeCopensation = ((CheckBox) v).isChecked();
		try {
			mServiceMessenger.send(Message.obtain(null,
					OsciPrimeService.ATTENUATION_CHANGED));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void fetchCalibration(View v) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Fetch Calibration for your OsciPrime by Serial");
		alert.setMessage("Enter your OsciPrime serial number");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setText("");
		alert.setView(input);

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String serial = input.getText().toString();
				if(serial.matches("[0-9a-zA-Z]{4}")){
					mApplication.fetchCalibration(serial.toUpperCase(), mHandler);
				}else{
					Toast.makeText(mApplication, "invalid serial number", Toast.LENGTH_SHORT).show();
				}
			}
		});
		alert.setNegativeButton("Cancel", null);
		alert.show();
	}
	
	public void debugUsb(View v){
		showOptions(R.layout.debug_usb);
	}
	
	public void toggleBit(View v){
		LinearLayout parent = (LinearLayout) v.getParent();
		for(int i = 0; i < 8; i++){
			boolean check = ((ToggleButton)parent.getChildAt(i)).isChecked();
			//0 is msb, 7 is lsb
			byte mask = (byte)(1 << (7-i));
			mDebugByte &= ~mask;
			mDebugByte |= (check ? 1 : 0) << (7-i);
		}
		L.d(String.format("Debug Byte %02X",mDebugByte));
	}	
	private UsbDevice mDebugDevice;
	private byte mDebugByte;
	@TargetApi(12)
	public void initUsb(View v){
		if (Build.VERSION.SDK_INT < 12) {
			Toast.makeText(mApplication, "Android Version does not support USB Host API", Toast.LENGTH_LONG).show();
			return;
		}
		
		UsbDebugHelper.initUsb(this, new IUsbConnectionHandler() {
			@Override
			public void onDeviceNotFound() {
				Toast.makeText(mApplication, R.string.could_not_initialize_usb_device, Toast.LENGTH_LONG).show();
			}
			
			@Override
			public void onDeviceInitialized(UsbDevice dev) {
				mDebugDevice = dev;
			}
		});
	}
	public void sendI2CCommand(View v){
		if(mDebugDevice != null){
			
			UsbDebugHelper.sendCommand(mDebugDevice, mDebugByte, (UsbManager)getSystemService(USB_SERVICE), mApplication);
		}else{
			Toast.makeText(this, "Initialize device first", Toast.LENGTH_LONG).show();
		}
			
	}

	private volatile boolean mExporting = false;

	public void screenshot(View v) {
		if (mExporting) {
			Toast.makeText(OsciPrimeICSActivity.this,
					"Please wait while exporting finishes...",
					Toast.LENGTH_LONG).show();
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				mExporting = true;
				Thread.currentThread().setName("Image Exporter");
				String datef = "dd_MM_yyyy_HH_mm_ss";
				SimpleDateFormat sdf = new SimpleDateFormat(datef);
				String fileName = sdf.format(new Date()) + ".png";
				Bitmap b = sSurfaceView.getBitmap(mApplication.pExportWidth);
				File ext = new File(Environment.getExternalStorageDirectory()
						+ File.separator + "osciprime");
				ext.mkdirs();

				final File f = new File(ext, fileName);
				FileOutputStream fos;

				try {
					if (!ext.exists()) {
						fos = openFileOutput(fileName, Context.MODE_PRIVATE);
						toast("No external directory found, trying internal instead.");
					} else
						fos = new FileOutputStream(f);

					b.compress(CompressFormat.PNG, 95, fos);
					fos.flush();
					fos.close();
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							toast("Exported " + f.getAbsolutePath());
							Intent intent = new Intent();
							intent.setAction(android.content.Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.fromFile(f), "image/png");
							startActivity(intent);
						}
					});
				} catch (FileNotFoundException e) {
					toast("Error exporting: File not found");
					e.printStackTrace();
				} catch (IOException e) {
					toast("Error exporting: IOException");
					e.printStackTrace();
				}
				mExporting = false;
			}
		}).start();

	}

	private void toast(final String msg) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(OsciPrimeICSActivity.this, msg,
						Toast.LENGTH_SHORT).show();
			}
		});

	}

	private void updateFrameSize() {
		mApplication.pFrameSize = Math.max(mApplication.pPointsOnView
				* mApplication.pInterleave * 2, mApplication.pMinFrameSize);
		resample();
	}

	private void resample() {
		if (mServiceState == OsciPrimeService.IDLE) {
			try {
				mServiceMessenger.send(Message.obtain(null,
						OsciPrimeService.RESAMPLE, -1, 0));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void resampleWithTrigger(int index){
		if (mServiceState == OsciPrimeService.IDLE) {
			try {
				mServiceMessenger.send(Message.obtain(null,
						OsciPrimeService.RESAMPLE, index, 0));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void onNews() {
		if (mHandler != null) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (mApplication.pUnreadNews) {
						((ImageButton) findViewById(R.id.btNews))
								.setImageResource(R.drawable.news_active);
						Toast.makeText(mApplication, "News available",
								Toast.LENGTH_LONG).show();
					}
				}
			});
		}
	}

	private static boolean sRequestShowOverlay = false;

	public void showHelpOverlay() {

		if (sOsciPrimeOverlayHelpView.getVisibility() == View.VISIBLE)
			return;

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			sRequestShowOverlay = true;
			return;
		}

		((FrameLayout) findViewById(R.id.rootlayout))
				.removeView(sOsciPrimeOverlayHelpView);
		findViewById(R.id.panL).setVisibility(View.VISIBLE);
		((ImageButton) findViewById(R.id.btLB)).setVisibility(View.INVISIBLE);
		findViewById(R.id.btL).setVisibility(View.INVISIBLE);

		findViewById(R.id.panR).setVisibility(View.VISIBLE);
		((ImageButton) findViewById(R.id.btRB)).setVisibility(View.INVISIBLE);
		findViewById(R.id.btR).setVisibility(View.INVISIBLE);

		findViewById(R.id.panD).setVisibility(View.INVISIBLE);
		((ImageButton) findViewById(R.id.btUB)).setVisibility(View.INVISIBLE);
		findViewById(R.id.btU).setVisibility(View.INVISIBLE);

		int index = ((FrameLayout) findViewById(R.id.rootlayout))
				.indexOfChild(findViewById(R.id.panDHolder));
		L.d("PanD index " + index);
		if ((ViewGroup) sOsciPrimeOverlayHelpView.getParent() != null)
			((ViewGroup) sOsciPrimeOverlayHelpView.getParent())
					.removeView(sOsciPrimeOverlayHelpView);
		sOsciPrimeOverlayHelpView.setLayoutParams(new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		((FrameLayout) findViewById(R.id.rootlayout)).addView(
				sOsciPrimeOverlayHelpView, index);
		// if(sRequestShowOverlay)
		// ((FrameLayout)
		// findViewById(R.id.rootlayout)).addView(sOsciPrimeOverlayHelpView, 2);
		// else
		// ((FrameLayout)
		// findViewById(R.id.rootlayout)).addView(sOsciPrimeOverlayHelpView, 3);
		sOsciPrimeOverlayHelpView.setRootPane(findViewById(R.id.rootlayout));
		sOsciPrimeOverlayHelpView.setBackgroundColor(Color.TRANSPARENT);
		sOsciPrimeOverlayHelpView.setVisibility(View.VISIBLE);

		sRequestShowOverlay = false;
	}

	public void updateInterfaceColor() {
		findViewById(R.id.panL)
				.setBackgroundColor(mApplication.pInterfaceColor);
		findViewById(R.id.panR)
				.setBackgroundColor(mApplication.pInterfaceColor);
		findViewById(R.id.panD)
				.setBackgroundColor(mApplication.pInterfaceColor);

		findViewById(R.id.btL).setBackgroundColor(mApplication.pInterfaceColor);
		findViewById(R.id.btR).setBackgroundColor(mApplication.pInterfaceColor);
		findViewById(R.id.btU).setBackgroundColor(mApplication.pInterfaceColor);

		findViewById(R.id.btLB)
				.setBackgroundColor(mApplication.pInterfaceColor);
		findViewById(R.id.btRB)
				.setBackgroundColor(mApplication.pInterfaceColor);
		findViewById(R.id.btUB)
				.setBackgroundColor(mApplication.pInterfaceColor);

		findViewById(R.id.layout_options).setBackgroundColor(
				mApplication.pInterfaceColor);
	}
	
	private void fixLowerBarHeight() {
		int dp = 5;
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
		HorizontalScrollView panD =	((HorizontalScrollView)findViewById(R.id.panD));
		RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) panD.getLayoutParams();
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(getResources(), R.drawable.scale_up, options);
		int h = options.outHeight;
		params.height = (int) (h + px);
		L.d("setting height "+ params.height);
		panD.setLayoutParams(params);
	}
	
}