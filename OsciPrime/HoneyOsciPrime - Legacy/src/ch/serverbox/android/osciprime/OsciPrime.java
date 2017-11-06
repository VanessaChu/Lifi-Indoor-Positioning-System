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

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import ch.serverbox.android.osciprime.sources.SourceConfiguration;
import ch.serverbox.android.osciprime.sources.TriggerProcessor;
import ch.serverbox.android.osciprime.ui.Overlay;
import ch.serverbox.android.osciprime.ui.OverlayChannels;
import ch.serverbox.android.osciprime.ui.OverlayCursors;
import ch.serverbox.android.osciprime.ui.OverlayTrigger;
import ch.serverbox.android.osciprime.ui.VerticalSeekBar;
import ch.serverbox.android.osciprime.ui.VerticalSeekBarOverlay;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard.Key;
import android.nfc.tech.MifareClassic;
import android.opengl.GLSurfaceView;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Toast;

public class OsciPrime extends Activity {

	private Messenger mServiceMessenger = null;
	private Messenger mActivityMessenger = null;// new Messenger(new
	// ActivityHandler());
	private WorkerThread mWorkerThread = null;
	private Handler mWorkerHandler = null;
	private Handler mUiHandler = new Handler();// activity handler
	private VertexHolder mVertexHolder;
	MenuItem tst;
	public static Context sAppContext = null;

	private Button mBtOn = null;
	private Button mBtOff = null;
	private Button mBtScreenshot = null;
	private Button mBtSources = null;
	private Button mBtRun = null;
	private Button mBtCursors = null;
	private Button mBtChannels = null;
	private Button mBtTrigger = null;
	private Button mBtCalibrate = null;

	private ArrayList<IPreferenceListener> mPreferenceListeners = new ArrayList<IPreferenceListener>();
	private GLSurfaceView mSurfaceView = null;

	private SourceConfiguration mSourceConfiguration = null;

	final int OVERLAY_MEASURE = 10;
	final int OVERLAY_CHANNELS = 20;
	final int OVERLAY_TRIGGER = 30;
	private int mOverlaySelected = OVERLAY_CHANNELS;

	private OsciPrimeRenderer mOsciPrimeRenderer = null;

	private final static String THREAD_ACTIVITY = "Activity Thread";

	private Overlay mOverlayCursors, mOverlayChannels, mOverlayTrigger;
	private RelativeLayout mOverlayCursorsContainer, mOverlayChannelsContainer,
			mOverlayTriggerContainer, mOverlayMenuContainer;

	private LinearLayout mMenu;
	private FrameLayout mAdvanced;

	private ScrollView mAdvancedSources, mAdvancedTrigger, mAdvancedChannels;
	private RadioGroup mRadioSourceSelection;

	private OsciMenu mOsciMenu;

	private boolean mIsMenuVisible = false;
	private boolean mIsRunning = false;
	private RadioGroup mRadioTriggerPolarity;

	private OsciPreferences mOsciPreferences;
	private RadioGroup mRadioTriggerChannel;

	private class ActivityHandler extends Handler {
		public ActivityHandler(Looper l) {
			super(l);
		}

		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case OPC.BI_ECHO:
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e("Interrupted!");
					e.printStackTrace();
				}
				l("echo from " + Thread.currentThread().getName());
				break;
			case OPC.SA_ANSWER_STATE:
				l("SA_ANSWER_STATE");
				final int state = msg.arg1;
				mSourceConfiguration = (SourceConfiguration) msg.obj;
				mUiHandler.post(new Runnable() {
					@Override
					public void run() {
						mRadioSourceSelection.setEnabled(false);
						for (int i = 0; i < mRadioSourceSelection
						.getChildCount(); i++) {
							((RadioButton) mRadioSourceSelection.getChildAt(i))
							.setEnabled(false);
						}
						switch (mSourceConfiguration.cSourceId()) {
						case OPC.SOURCE_AUDIO:
							mRadioSourceSelection.check(R.id.source_audio);
							break;
						case OPC.SOURCE_GENERATOR:
							mRadioSourceSelection.check(R.id.source_generator);
							break;
						case OPC.SOURCE_USB:
							mRadioSourceSelection.check(R.id.source_usb);
							break;
						}
						mVertexHolder = VertexHolder
						.getVertexholder(mSourceConfiguration);
						mOsciMenu.setSourceConfiguration(mSourceConfiguration);
						for (int i = 0; i < mRadioSourceSelection
						.getChildCount(); i++) {
							((RadioButton) mRadioSourceSelection.getChildAt(i))
							.setEnabled(true);
						}
						mRadioSourceSelection.setEnabled(true);
						mBtOn.setEnabled(state == OPC.STATE_IDLE ? true : false);
						if(mInflatedMenu != null){
							mInflatedMenu.findItem(R.id.mni_start).setEnabled(
									state == OPC.STATE_IDLE ? true : false);

							((MenuItem)mInflatedMenu.findItem(R.id.mni_start)).setIcon(
									state == OPC.STATE_IDLE ? R.drawable.start
											: R.drawable.start_disabled);
						}

						mBtOff.setEnabled(state == OPC.STATE_RUNNING ? true
								: false);

						if(mInflatedMenu != null){
							mInflatedMenu.findItem(R.id.mni_stop).setEnabled(
									state == OPC.STATE_RUNNING ? true : false);
							((MenuItem)mInflatedMenu.findItem(R.id.mni_stop)).setIcon(state == OPC.STATE_RUNNING ? R.drawable.stop
									: R.drawable.stop_disabled);
						}
					}
				});
				break;
			case OPC.SA_SOURCE_NOT_AVAILABLE:
				mUiHandler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								"Source not Available", Toast.LENGTH_SHORT)
								.show();
					}
				});
				break;
			case OPC.SA_PREFERENCES_CHANGED:
				mOsciPreferences = (OsciPreferences) msg.obj;
				mUiHandler.post(new Runnable() {
					@Override
					public void run() {
						for (IPreferenceListener l : mPreferenceListeners) {
							l.onPreferenceChanged(mOsciPreferences);
						}
					}
				});
				break;
			case OPC.SA_LINKVIEW:
				VertexHolder.getVertexholder(null)
				.linkSurfaceView(mSurfaceView);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		l("onCreate");
		sAppContext = getApplicationContext();
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		setTheme(android.R.style.Theme_Holo_Light);
		setContentView(R.layout.main);
		getActionBar().setBackgroundDrawable(
				new ColorDrawable(Color.rgb(220, 220, 220)));
		Thread.currentThread().setName(THREAD_ACTIVITY);
		mWorkerThread = new WorkerThread("Activity Worker");
		mWorkerHandler = new ActivityHandler(mWorkerThread.getLooper());
		mActivityMessenger = new Messenger(mWorkerHandler);

		mSurfaceView = (GLSurfaceView) findViewById(R.id.mSurfaceView);
		mOsciPrimeRenderer = new OsciPrimeRenderer();
		mSurfaceView.setRenderer(mOsciPrimeRenderer);
		mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		mSurfaceView.requestRender();
		mBtOn = (Button) findViewById(R.id.btOn);
		mBtOn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mServiceMessenger != null) {
					try {
						mServiceMessenger.send(Message.obtain(null,
								OPC.AS_START_SAMPLING));
						holdOffButtons();
						mServiceMessenger.send(Message.obtain(null,
								OPC.AS_QUERY_STATE));
					} catch (RemoteException e) {
						e("could not send message to service, seems to be dead");
						e.printStackTrace();
					}
				}
			}
		});

		mBtOff = (Button) findViewById(R.id.btOff);
		mBtOff.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mServiceMessenger != null) {
					try {
						mServiceMessenger.send(Message.obtain(null,
								OPC.AS_STOP_SAMPLING));
						holdOffButtons();
						mServiceMessenger.send(Message.obtain(null,
								OPC.AS_QUERY_STATE));
					} catch (RemoteException e) {
						e("could not send message to service, seems to be dead");
						e.printStackTrace();
					}
				}
			}
		});

		holdOffButtons();

		mOverlayCursorsContainer = (RelativeLayout) findViewById(R.id.overlay_cursors_container);
		mOverlayCursors = (Overlay) findViewById(R.id.overlay_cursors);
		SeekBar overlayCursorsSeekBarTop = (SeekBar) findViewById(R.id.overlay_cursors_top);
		SeekBar overlayCursorsSeekBarBottom = (SeekBar) findViewById(R.id.overlay_cursors_bottom);
		VerticalSeekBar overlayCursorsSeekBarLeft = (VerticalSeekBar) findViewById(R.id.overlay_cursors_left);
		VerticalSeekBar overlayCursorsSeekBarRight = (VerticalSeekBar) findViewById(R.id.overlay_cursors_right);
		mOverlayCursors.attachViews(overlayCursorsSeekBarTop,
				overlayCursorsSeekBarBottom, overlayCursorsSeekBarLeft,
				overlayCursorsSeekBarRight);

		mOverlayChannelsContainer = (RelativeLayout) findViewById(R.id.overlay_channels_container);
		mOverlayChannels = (Overlay) findViewById(R.id.overlay_channels);
		SeekBar overlayChannelsSeekBarTop = (SeekBar) findViewById(R.id.overlay_channels_top);
		VerticalSeekBar overlayChannelsSeekBarLeft = (VerticalSeekBar) findViewById(R.id.overlay_channels_left);
		((VerticalSeekBarOverlay) overlayChannelsSeekBarLeft).drawGround(true)
				.setColor(getResources().getColor(R.color.blue));
		VerticalSeekBar overlayChannelsSeekBarRight = (VerticalSeekBar) findViewById(R.id.overlay_channels_right);
		((VerticalSeekBarOverlay) overlayChannelsSeekBarRight).drawGround(true)
				.setColor(getResources().getColor(R.color.green));
		mOverlayChannels.attachViews(overlayChannelsSeekBarTop, null,
				overlayChannelsSeekBarLeft, overlayChannelsSeekBarRight);

		mOverlayTriggerContainer = (RelativeLayout) findViewById(R.id.overlay_trigger_container);
		mOverlayTrigger = (Overlay) findViewById(R.id.overlay_trigger);
		SeekBar overlayTriggerSeekBarTop = (SeekBar) findViewById(R.id.overlay_trigger_top);
		VerticalSeekBar overlayTriggerSeekBarLeft = (VerticalSeekBar) findViewById(R.id.overlay_trigger_left);
		((VerticalSeekBarOverlay) overlayTriggerSeekBarLeft).drawGround(true)
				.setColor(getResources().getColor(R.color.blue));
		VerticalSeekBar overlayTriggerSeekBarRight = (VerticalSeekBar) findViewById(R.id.overlay_trigger_right);
		((VerticalSeekBarOverlay) overlayTriggerSeekBarRight).drawGround(true)
				.setColor(getResources().getColor(R.color.green));
		mOverlayTrigger.attachViews(overlayTriggerSeekBarTop, null,
				overlayTriggerSeekBarLeft, overlayTriggerSeekBarRight);

		((OverlayCursors) findViewById(R.id.overlay_cursors)).attachOsci(this);
		((OverlayTrigger) findViewById(R.id.overlay_trigger)).attachOsci(this);
		((OverlayTrigger) findViewById(R.id.overlay_trigger)).attachChannels(
				overlayChannelsSeekBarTop, overlayChannelsSeekBarLeft,
				overlayChannelsSeekBarRight);
		((OverlayChannels) findViewById(R.id.overlay_channels))
				.attachOsci(this);
		((OverlayChannels) findViewById(R.id.overlay_channels)).attachTriggers(
				overlayTriggerSeekBarLeft, overlayTriggerSeekBarRight,
				overlayTriggerSeekBarTop);

		mMenu = (LinearLayout) findViewById(R.id.menu);
		mOverlayMenuContainer = (RelativeLayout) findViewById(R.id.overlay_menu);

		mAdvanced = (FrameLayout) findViewById(R.id.advanced);
		mAdvancedTrigger = (ScrollView) findViewById(R.id.advanced_trigger);
		mAdvancedSources = (ScrollView) findViewById(R.id.advanced_sources);
		mAdvancedChannels = (ScrollView) findViewById(R.id.advanced_channels);

		mBtRun = (Button) findViewById(R.id.menu_button_run);
		mBtRun.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (!mIsRunning) {
					if (mServiceMessenger != null) {
						try {
							mServiceMessenger.send(Message.obtain(null,
									OPC.AS_START_SAMPLING));
							holdOffButtons();
							mServiceMessenger.send(Message.obtain(null,
									OPC.AS_QUERY_STATE));
							mIsRunning = true;
						} catch (RemoteException e) {
							e("could not send message to service, seems to be dead");
							e.printStackTrace();
						}
					}
				} else {
					if (mServiceMessenger != null) {
						try {
							mServiceMessenger.send(Message.obtain(null,
									OPC.AS_STOP_SAMPLING));
							holdOffButtons();
							mServiceMessenger.send(Message.obtain(null,
									OPC.AS_QUERY_STATE));
							mIsRunning = false;
						} catch (RemoteException e) {
							e("could not send message to service, seems to be dead");
							e.printStackTrace();
						}
					}
				}

			}
		});

		mBtCalibrate = (Button) findViewById(R.id.menu_button_calibrate);
		mBtCalibrate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				VertexHolder vh = VertexHolder.getVertexholder(null);
				if (vh != null)
					vh.calibrate();
			}
		});

		mOsciMenu = new OsciMenu(getApplicationContext(), this);

		mBtCursors = (Button) findViewById(R.id.menu_button_cursors);
		mBtChannels = (Button) findViewById(R.id.menu_button_channels);
		mBtTrigger = (Button) findViewById(R.id.menu_button_trigger);
		mBtSources = (Button) findViewById(R.id.menu_button_sources);

		mOsciMenu.add(mBtSources, null, mAdvancedSources);
		mOsciMenu.add(mBtTrigger, mOverlayTriggerContainer, mAdvancedTrigger);
		mOsciMenu.add(mBtCursors, mOverlayCursorsContainer, null);
		mOsciMenu
				.add(mBtChannels, mOverlayChannelsContainer, mAdvancedChannels);

		mPreferenceListeners.add(mOsciMenu);

		mRadioSourceSelection = (RadioGroup) findViewById(R.id.source_selection);
		mRadioSourceSelection.setEnabled(false);
		mRadioSourceSelection
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						if (mRadioSourceSelection.isEnabled()) {
							switch (checkedId) {
							case R.id.source_audio:
								l("Source audio clicked");
								if (mServiceMessenger != null) {
									try {
										mServiceMessenger.send(Message.obtain(
												null, OPC.AS_SET_SOURCE,
												OPC.SOURCE_AUDIO, 0));
										holdOffButtons();
										mServiceMessenger.send(Message.obtain(
												null, OPC.AS_QUERY_STATE));
									} catch (RemoteException e) {
										e("could not send message to service, seems to be dead");
										e.printStackTrace();
									}
								}
								break;

							case R.id.source_generator:
								l("Source generator clicked");
								if (mServiceMessenger != null) {
									try {
										mServiceMessenger.send(Message.obtain(
												null, OPC.AS_SET_SOURCE,
												OPC.SOURCE_GENERATOR, 0));
										holdOffButtons();
										mServiceMessenger.send(Message.obtain(
												null, OPC.AS_QUERY_STATE));
									} catch (RemoteException e) {
										e("could not send message to service, seems to be dead");
										e.printStackTrace();
									}
								}
								break;

							case R.id.source_usb:
								l("Source usb clicked");
								if (mServiceMessenger != null) {
									try {
										mServiceMessenger.send(Message.obtain(
												null, OPC.AS_SET_SOURCE,
												OPC.SOURCE_USB, 0));
										holdOffButtons();
										mServiceMessenger.send(Message.obtain(
												null, OPC.AS_QUERY_STATE));
									} catch (RemoteException e) {
										e("could not send message to service, seems to be dead");
										e.printStackTrace();
									}
								}
								break;
							default:

							}
							for (int i = 0; i < group.getChildCount(); i++) {
								((RadioButton) group.getChildAt(i))
										.setEnabled(false);
							}
						}
					}
				});

		mRadioTriggerPolarity = (RadioGroup) findViewById(R.id.trigger_polarity);
		mRadioTriggerPolarity
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {

						switch (checkedId) {
						case R.id.trigger_falling:
							l("Source audio clicked");
							if (mServiceMessenger != null) {
								try {
									mServiceMessenger.send(Message.obtain(null,
											OPC.AS_SET_POLARITY,
											TriggerProcessor.POLARITY_NEGATIVE,
											0));
									holdOffButtons();
									mServiceMessenger.send(Message.obtain(null,
											OPC.AS_QUERY_STATE));
								} catch (RemoteException e) {
									e("could not send message to service, seems to be dead");
									e.printStackTrace();
								}
							}
							break;

						case R.id.trigger_rising:
							l("Source generator clicked");
							if (mServiceMessenger != null) {
								try {
									mServiceMessenger.send(Message.obtain(null,
											OPC.AS_SET_POLARITY,
											TriggerProcessor.POLARITY_POSITIVE,
											0));
									holdOffButtons();
									mServiceMessenger.send(Message.obtain(null,
											OPC.AS_QUERY_STATE));
								} catch (RemoteException e) {
									e("could not send message to service, seems to be dead");
									e.printStackTrace();
								}
							}
							break;

						default:

						}

					}
				});

		mRadioTriggerChannel = (RadioGroup) findViewById(R.id.trigger_channel);
		mRadioTriggerChannel
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {

						switch (checkedId) {
						case R.id.trigger_channel1:

							if (mServiceMessenger != null) {
								try {
									mServiceMessenger.send(Message.obtain(null,
											OPC.AS_SET_TRIGGER_CHANNEL,
											TriggerProcessor.CHANNEL_1, 0));
									mServiceMessenger.send(Message.obtain(null,
											OPC.AS_QUERY_STATE));
								} catch (RemoteException e) {
									e("could not send message to service, seems to be dead");
									e.printStackTrace();
								}
							}
							break;

						case R.id.trigger_channel2:

							if (mServiceMessenger != null) {
								try {
									mServiceMessenger.send(Message.obtain(null,
											OPC.AS_SET_TRIGGER_CHANNEL,
											TriggerProcessor.CHANNEL_2, 0));
									mServiceMessenger.send(Message.obtain(null,
											OPC.AS_QUERY_STATE));
								} catch (RemoteException e) {
									e("could not send message to service, seems to be dead");
									e.printStackTrace();
								}
							}
							break;

						default:

						}

					}
				});

	}

	@Override
	protected void onDestroy() {
		l("onDestroy");
		mWorkerThread.quit();
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		l("onStart");

		startService(new Intent(OsciPrimeService.class.getName()));
		bindService(new Intent(OsciPrimeService.class.getName()),
				mServiceConnection, Service.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		l("onStop");
		unbindService(mServiceConnection);
		super.onStop();
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName arg0) {

		}

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			mServiceMessenger = new Messenger(arg1);
			try {
				Message m = Message.obtain(null, OPC.AS_REGISTER_ACTIVITY);
				m.replyTo = mActivityMessenger;
				mServiceMessenger.send(m);

				// mServiceMessenger.send(Message.obtain(null,
				// OPC.AS_START_SAMPLING));
				mServiceMessenger
						.send(Message.obtain(null, OPC.AS_QUERY_STATE));

			} catch (RemoteException e) {
				e("Error sending the Register Message to the Service");
				e.printStackTrace();
			}
		}
	};

	private void holdOffButtons() {
		mUiHandler.post(new Runnable() {
			@Override
			public void run() {
				mBtOn.setEnabled(false);
				mBtOff.setEnabled(false);

				if (mInflatedMenu != null) {

					mInflatedMenu.findItem(R.id.mni_start).setEnabled(false);
					((MenuItem) mInflatedMenu.findItem(R.id.mni_start))
							.setIcon(R.drawable.start_disabled);
					mInflatedMenu.findItem(R.id.mni_stop).setEnabled(false);
					((MenuItem) mInflatedMenu.findItem(R.id.mni_stop))
							.setIcon(R.drawable.stop_disabled);
				} else {
					l("menu not inflated yet ...");
				}

			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {

			if (!mIsMenuVisible) {
				AnimationSet set = new AnimationSet(true);
				set.setFillEnabled(true);
				set.setFillAfter(true);
				set.setInterpolator(new OvershootInterpolator());

				mOverlayMenuContainer.setVisibility(View.VISIBLE);

				Animation animationA = new TranslateAnimation(
						Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, 100);
				Animation animationB = new TranslateAnimation(
						Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, -100);
				Animation animationC = new AlphaAnimation(0, 1);

				animationA.setDuration(30);
				animationB.setDuration(500);
				animationB.setStartOffset(30);
				animationC.setDuration(500);
				animationC.setStartOffset(30);

				set.addAnimation(animationA);
				set.addAnimation(animationB);
				set.addAnimation(animationC);

				animationA.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationStart(Animation animation) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onAnimationRepeat(Animation animation) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onAnimationEnd(Animation animation) {
						for (int i = 0; i < mMenu.getChildCount(); i++) {
							mMenu.getChildAt(i).setVisibility(View.VISIBLE);
						}
					}
				});
				mMenu.startAnimation(set);
				mIsMenuVisible = true;

				return true;
			} else {
				AnimationSet set = new AnimationSet(true);
				set.setFillEnabled(true);
				set.setFillAfter(true);
				set.setInterpolator(new OvershootInterpolator());

				Animation animationB = new TranslateAnimation(
						Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, 100);
				Animation animationC = new AlphaAnimation(0, 1);

				animationB.setDuration(1000);
				animationC.setDuration(1000);

				set.addAnimation(animationB);
				// set.addAnimation(animationC);

				animationB.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationStart(Animation animation) {
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

					@Override
					public void onAnimationEnd(Animation animation) {
						for (int i = 0; i < mMenu.getChildCount(); i++) {
							mMenu.getChildAt(i).setVisibility(View.INVISIBLE);
						}
						mOverlayMenuContainer.setVisibility(View.INVISIBLE);

					}
				});
				mMenu.startAnimation(set);
				mAdvanced.setVisibility(View.INVISIBLE);
				mIsMenuVisible = false;

				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	private Menu mInflatedMenu = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.main, menu);
		mInflatedMenu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		l("onOptionsItemSelected");
		if (item.getItemId() == R.id.mni_measure) {
			l("Measure clicked");
			mOverlayCursorsContainer.setVisibility(View.VISIBLE);
			mOverlayChannelsContainer.setVisibility(View.INVISIBLE);
			mOverlayTriggerContainer.setVisibility(View.INVISIBLE);
		}
		if (item.getItemId() == R.id.mni_offset) {
			l("Offset clicked");
			mOverlayCursorsContainer.setVisibility(View.INVISIBLE);
			mOverlayTriggerContainer.setVisibility(View.INVISIBLE);
			mOverlayChannelsContainer.setVisibility(View.VISIBLE);
		}
		if (item.getItemId() == R.id.mni_source) {
			mOverlayMenuContainer.setVisibility(View.VISIBLE);
			mBtSources.performClick();
		}
		if (item.getItemId() == R.id.mni_configure) {
			mOverlayMenuContainer.setVisibility(View.VISIBLE);
			mBtChannels.performClick();
		}
		if (item.getItemId() == R.id.mni_trigger) {
			mOverlayMenuContainer.setVisibility(View.VISIBLE);
			mBtTrigger.performClick();
		}
		if (item.getItemId() == R.id.mni_callibrate) {
			mBtCalibrate.performClick();
		}
		if (item.getItemId() == R.id.mni_start) {
			mBtOn.performClick();
		}
		if (item.getItemId() == R.id.mni_stop) {
			mBtOff.performClick();
		}
		mOverlayCursorsContainer.invalidate();
		mOverlayChannelsContainer.invalidate();

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		l("onResume");
		super.onResume();
		Timer timerHideViews = new Timer(false);
		timerHideViews.schedule(new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// Toast.makeText(getApplicationContext(), "1",
						// Toast.LENGTH_SHORT).show();
						if (mIsMenuVisible) {
							// Toast.makeText(getApplicationContext(), "2",
							// Toast.LENGTH_SHORT).show();
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									mOverlayMenuContainer
											.setVisibility(View.VISIBLE);
									for (int i = 0; i < mMenu.getChildCount(); i++) {
										mMenu.getChildAt(i).setVisibility(
												View.VISIBLE);
									}
									// Toast.makeText(getApplicationContext(),
									// "3", Toast.LENGTH_SHORT).show();
								}
							});
						}

						mOverlayCursorsContainer.setVisibility(View.INVISIBLE);
						mOverlayChannelsContainer.setVisibility(View.INVISIBLE);
						mOverlayTriggerContainer.setVisibility(View.INVISIBLE);
						switch (mOverlaySelected) {
						case OVERLAY_MEASURE:
							mOverlayCursorsContainer
									.setVisibility(View.VISIBLE);
							break;
						case OVERLAY_CHANNELS:
							mOverlayChannelsContainer
									.setVisibility(View.VISIBLE);
							break;
						case OVERLAY_TRIGGER:
							mOverlayTriggerContainer
									.setVisibility(View.VISIBLE);
							break;
						default:
						}

					}
				});
			}
		}, 100);

		/*
		 * Timer timerHideViews = new Timer(false); timerHideViews.schedule(new
		 * TimerTask() {
		 * 
		 * @Override public void run() { runOnUiThread(new Runnable() {
		 * 
		 * @Override public void run() {
		 * mOverlayMeasurementContainer.setVisibility(View.INVISIBLE);
		 * mOverlayOffsetContainer.setVisibility(View.INVISIBLE);
		 * 
		 * } }); } }, 3000);
		 */

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		l("onSaveInstanceState");
		outState.putBoolean("mIsMenuVisible", mIsMenuVisible);
		outState.putBoolean("mIsRunning", mIsRunning);
		outState.putInt("mOverlaySelected", mOverlaySelected);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		l("onRestoreInstanceState");
		mIsMenuVisible = savedInstanceState.getBoolean("mIsMenuVisible");
		mIsRunning = savedInstanceState.getBoolean("mIsRunning");
		mOverlaySelected = savedInstanceState.getInt("mOverlaySelected");
	}

	public void requestRender() {
		mSurfaceView.requestRender();
	}

	private void e(String msg) {
		Log.e("Activity", ">==< " + msg + " >==<");
	}

	private void l(String msg) {
		Log.d("Activity", ">==< " + msg + " >==<");
	}

	public void sendMsgTrigger(int t, int channel) {
		if (mServiceMessenger != null) {
			try {
				mServiceMessenger.send(Message.obtain(null,
						OPC.AS_SET_TRIGGER_LEVEL, t, channel));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendMsgGain(int idx, int channel) {
		if (mServiceMessenger != null) {
			try {
				mServiceMessenger.send(Message.obtain(null, OPC.AS_SET_GAIN,
						idx, channel));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendMsgInterleave(int n, int idx) {
		if (mServiceMessenger != null) {
			try {
				mServiceMessenger.send(Message.obtain(null,
						OPC.AS_SET_INTERLEAVE, n, idx));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void requestPreferencesUpdate() {
		if (mServiceMessenger != null) {
			try {
				mServiceMessenger.send(Message.obtain(null,
						OPC.AS_QUERY_PREFERENCES));
			} catch (RemoteException e) {
			}
		}
	}

	public void setChannel1Visible(boolean isVisible) {
		if (mServiceMessenger != null) {
			try {
				mServiceMessenger.send(Message.obtain(null,
						OPC.AS_SET_CHANNELVISIBLE, (isVisible ? 1 : 0), 1));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	public void setChannel2Visible(boolean isVisible) {
		if (mServiceMessenger != null) {
			try {
				mServiceMessenger.send(Message.obtain(null,
						OPC.AS_SET_CHANNELVISIBLE, (isVisible ? 1 : 0), 2));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	public void updateCursors(int dv, int dt) {

	}

}