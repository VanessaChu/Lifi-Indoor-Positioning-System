package ch.serverbox.android.osciprime;

import java.util.ArrayList;

import ch.serverbox.android.osciprime.sources.SourceConfiguration;
import ch.serverbox.android.osciprime.sources.TriggerProcessor;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

public class OsciMenu implements IPreferenceListener{

	ArrayList<View> mButtons;
	ArrayList<OnClickListener> mButtonOCLs;
	ArrayList<View> mOverlays;
	View mAdvanced;
	ArrayList<View> mAdvancedMenus;
	OsciPrime mOsciPrime;

	View mAdvancedChannels;

	SourceConfiguration mSourceConfiguration;

	Context mContext;

	Button mBtCalibrate;

	RadioGroup mTriggerPolarity;
	RadioGroup mTriggerChannel;
	
	private CheckBox mCheckboxCH1;
	private CheckBox mCheckboxCH2;


	public OsciMenu(ArrayList<View> buttons, ArrayList<OnClickListener> buttonOCLs, ArrayList<View> overlays, ArrayList<View> advancedMenus){
		mButtons = buttons;
		mButtonOCLs = buttonOCLs;
		mOverlays = overlays;
		mAdvancedMenus = advancedMenus;
	}

	public OsciMenu(ArrayList<View> buttons, ArrayList<View> overlays, ArrayList<View> advancedMenus, FrameLayout advanced){
		mButtons = buttons;
		mOverlays = overlays;
		mAdvancedMenus = advancedMenus;
		mAdvanced = advanced;

	}

	public OsciMenu(Context context, OsciPrime osciPrime){
		mButtons = new ArrayList<View>();
		mOverlays = new ArrayList<View>();
		mAdvancedMenus = new ArrayList<View>();


		mOsciPrime = osciPrime;
		mAdvancedChannels = osciPrime.findViewById(R.id.advanced_channels);
		mAdvanced = osciPrime.findViewById(R.id.advanced);

		mTriggerChannel = (RadioGroup) mOsciPrime.findViewById(R.id.trigger_channel);
		mTriggerPolarity = (RadioGroup) mOsciPrime.findViewById(R.id.trigger_polarity);
		
		mCheckboxCH1 = (CheckBox)mOsciPrime.findViewById(R.id.checkbox_show_channel1);
		mCheckboxCH2 = (CheckBox)mOsciPrime.findViewById(R.id.checkbox_show_channel2);

		mContext = context;
	}

	public void add(View b, View o, View a){
		b.setOnClickListener(defaultOCL);

		mButtons.add(b);
		mOverlays.add(o);
		mAdvancedMenus.add(a);
	}

	final OnClickListener defaultOCL = new OnClickListener() {
		@Override
		public void onClick(View v) {
			//l(v);
			showOverlay((Button)v);
			showAdvanced((Button)v);			
		}
	};


	public void showOverlay(Button button){
		int i = mButtons.indexOf(button);
		if(i != -1 && mOverlays.get(i) != null){
			for(View  o : mOverlays){
				if(o != null){
					o.setVisibility(View.INVISIBLE);
				}					
			}
			mOverlays.get(i).setVisibility(View.VISIBLE);
			/*
			if(vis == View.VISIBLE){//was visible before ...
				mOverlays.get(i).setVisibility(View.INVISIBLE);
				mAdvanced.setVisibility(View.INVISIBLE);
			}else{
				mOverlays.get(i).setVisibility(View.VISIBLE);
			}*/
			
		}
	}

	public void showAdvanced(Button button){
		int i = mButtons.indexOf(button);
		l("index "+i);
		if(i != -1 && mAdvancedMenus.get(i) != null){
			int vis = mAdvancedMenus.get(i).getVisibility();
			for(View a : mAdvancedMenus){
				if(a != null){
					a.setVisibility(View.INVISIBLE);
				}
			}
			mAdvanced.setVisibility(View.INVISIBLE);
			if(vis == View.INVISIBLE){//was visible before?
				l("Invisible");
				mAdvancedMenus.get(i).setVisibility(View.VISIBLE);
				mAdvanced.setVisibility(View.VISIBLE);
			}else{
				l("Visible");
				mAdvancedMenus.get(i).setVisibility(View.INVISIBLE);
				mAdvanced.setVisibility(View.INVISIBLE);
			}
		} else {
			
		}
	}

	public void setSourceConfiguration(SourceConfiguration sourceConfiguration){
		mSourceConfiguration = sourceConfiguration;
		populate();
	}


	private RadioGroup rgGain1, rgGain2;
	private RadioGroup rgInterleave;

	public void populate(){
		LinearLayout ll =(LinearLayout) mAdvancedChannels.findViewById(R.id.divisions);

		for(int i=0; i<ll.getChildCount();i++)
			((LinearLayout)ll.getChildAt(i)).removeAllViews();



		if(mSourceConfiguration == null){
			l("SOURCECONFIGURATION IS NULL..");
			return;
		}

		LinearLayout.LayoutParams layoutLParams = new LinearLayout.LayoutParams(
				RadioGroup.LayoutParams.WRAP_CONTENT,
				RadioGroup.LayoutParams.WRAP_CONTENT);
		LinearLayout.LayoutParams layoutRParams = new RadioGroup.LayoutParams(
				RadioGroup.LayoutParams.WRAP_CONTENT,
				RadioGroup.LayoutParams.WRAP_CONTENT);

		TextView titleGain1 = new TextView(mContext);
		titleGain1.setTextColor(Color.BLACK);
		titleGain1.setText("Voltage Division");
		titleGain1.setTextSize(18);

		rgGain1 = new RadioGroup(mContext);
		for(int i = 0; i<mSourceConfiguration.cGainTrippletsCh1().length;i++){
			RadioButton rb = new RadioButton(mContext);
			rb.setId(i);
			rb.setText(mSourceConfiguration.cGainTrippletsCh1()[i].humanReadable);
			rb.setTextColor(Color.BLACK);
			rgGain1.addView(rb, 0, layoutRParams);
		}

		rgGain1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(group.isEnabled()){
					mOsciPrime.sendMsgGain(checkedId, TriggerProcessor.CHANNEL_1);
					((TextView)mOsciPrime.findViewById(R.id.display_ch1)).setText("CH1: "+mSourceConfiguration.cGainTrippletsCh1()[checkedId].humanReadable);

				}
			}
		});

		((LinearLayout)ll.getChildAt(0)).addView(titleGain1, 0, layoutLParams);
		((LinearLayout)ll.getChildAt(0)).addView(rgGain1, 1, layoutLParams);


		TextView titleGain2 = new TextView(mContext);
		titleGain2.setText("Voltage Division");
		titleGain2.setTextColor(Color.BLACK);
		titleGain2.setTextSize(18);

		rgGain2 = new RadioGroup(mContext);
		for(int i = 0; i<mSourceConfiguration.cGainTrippletsCh1().length;i++){
			RadioButton rb = new RadioButton(mContext);
			rb.setId(i);
			rb.setTextColor(Color.BLACK);
			rb.setText(mSourceConfiguration.cGainTrippletsCh1()[i].humanReadable);
			rgGain2.addView(rb, 0, layoutRParams);
		}

		rgGain2.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(group.isEnabled()){
					mOsciPrime.sendMsgGain(checkedId, TriggerProcessor.CHANNEL_2);
					((TextView)mOsciPrime.findViewById(R.id.display_ch2)).setText("CH2: "+mSourceConfiguration.cGainTrippletsCh2()[checkedId].humanReadable);

				}
			}
		});

		((LinearLayout)ll.getChildAt(1)).addView(titleGain2, 0, layoutLParams);
		((LinearLayout)ll.getChildAt(1)).addView(rgGain2, 1, layoutLParams);


		TextView titleTime = new TextView(mContext);
		titleTime.setText("Time Division");
		titleTime.setTextColor(Color.BLACK);
		titleTime.setTextSize(18);

		rgInterleave = new RadioGroup(mContext);
		for(int i = 0; i<mSourceConfiguration.cTimeDivisionPairs().length;i++){
			RadioButton rb = new RadioButton(mContext);
			rb.setId(i);
			rb.setTextColor(Color.BLACK);
			rb.setText(mSourceConfiguration.cTimeDivisionPairs()[i].humanRepresentation);
			rgInterleave.addView(rb, 0, layoutRParams);
		}

		rgInterleave.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(group.isEnabled()){
					l("length "+mSourceConfiguration.cTimeDivisionPairs().length);
					mOsciPrime.sendMsgInterleave(mSourceConfiguration.cTimeDivisionPairs()[checkedId].interleave, checkedId);
					((TextView)mOsciPrime.findViewById(R.id.display_time)).setText("t: "+mSourceConfiguration.cTimeDivisionPairs()[checkedId].humanRepresentation);
				}
			}
		});

		((LinearLayout)ll.getChildAt(2)).addView(titleTime, 0, layoutLParams);
		((LinearLayout)ll.getChildAt(2)).addView(rgInterleave, 1, layoutLParams);
		
		mCheckboxCH1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(buttonView.isEnabled()){
					mOsciPrime.setChannel1Visible(isChecked);
				}
			}
		});
		
		ll.invalidate();
		mCheckboxCH2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(buttonView.isEnabled()){
					mOsciPrime.setChannel2Visible(isChecked);
				}
			}
		});
		//done, request the update
		mOsciPrime.requestPreferencesUpdate();
	}

	private void l(String msg){
		Log.d("Activity", ">==< "+msg+" >==<");
	}


	@Override
	public void onPreferenceChanged(OsciPreferences op) {
		l("onPreferenceChanged");
		if(rgInterleave != null){
			rgInterleave.setEnabled(false);
			rgGain1.setEnabled(false);
			rgGain2.setEnabled(false);
		}
		mTriggerChannel.setEnabled(false);
		mTriggerPolarity.setEnabled(false);



		switch(op.getChannel()){
		case TriggerProcessor.CHANNEL_1:
			mTriggerChannel.check(R.id.trigger_channel1);
			if(op.getPolarityCh1() == TriggerProcessor.POLARITY_POSITIVE)
				mTriggerPolarity.check(R.id.trigger_rising);
			else
				mTriggerPolarity.check(R.id.trigger_falling);
			break;
		case TriggerProcessor.CHANNEL_2:
			mTriggerChannel.check(R.id.trigger_channel2);
			if(op.getPolarityCh2() == TriggerProcessor.POLARITY_POSITIVE)
				mTriggerPolarity.check(R.id.trigger_rising);
			else
				mTriggerPolarity.check(R.id.trigger_falling);
			break;
		}

		if(rgGain1 != null){
			rgGain1.check(op.getGainCh1Index());
		}
		if(rgGain2 != null){
			rgGain2.check(op.getGainCh2Index());
		}
		if(rgInterleave != null){
			l("index: "+op.getInterleaveIndex());
			rgInterleave.check(op.getInterleaveIndex());
		}
		if(rgInterleave != null){
			rgInterleave.setEnabled(true);
			rgGain1.setEnabled(true);
			rgGain2.setEnabled(true);
		}
		
		if(mSourceConfiguration != null){
			((TextView)mOsciPrime.findViewById(R.id.display_ch1)).setText("CH1: "+mSourceConfiguration.cGainTrippletsCh1()[op.getGainCh1Index()].humanReadable);
			((TextView)mOsciPrime.findViewById(R.id.display_ch2)).setText("CH2: "+mSourceConfiguration.cGainTrippletsCh1()[op.getGainCh2Index()].humanReadable);
			((TextView)mOsciPrime.findViewById(R.id.display_time)).setText("t: "+mSourceConfiguration.cTimeDivisionPairs()[op.getInterleaveIndex()].humanRepresentation);
		}
		
		mTriggerChannel.setEnabled(true);
		mTriggerPolarity.setEnabled(true);
		
		mCheckboxCH1.setEnabled(false);
		mCheckboxCH2.setEnabled(false);
		mCheckboxCH1.setChecked(op.isChannel1Visible());
		mCheckboxCH2.setChecked(op.isChannel2Visible());
		mCheckboxCH1.setEnabled(true);
		mCheckboxCH2.setEnabled(true);


	}

	private void l(Object s){
		Log.d(getClass().getSimpleName(), ">==< "+s.toString()+" >==<");
	}

}
