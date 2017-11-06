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
package ch.serverbox.android.osciprime.ui;

import ch.serverbox.android.osciprime.OPC;
import ch.serverbox.android.osciprime.OsciPrime;
import ch.serverbox.android.osciprime.OsciPrimeRenderer;
import ch.serverbox.android.osciprime.OsciTransformer;
import ch.serverbox.android.osciprime.R;
import ch.serverbox.android.osciprime.sources.TriggerProcessor;
import ch.serverbox.android.osciprime.ui.VerticalSeekBar.OnSeekBarChangeListener;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayCursors extends Overlay{

	TextView tv;

	public OverlayCursors(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	private OsciPrime mOsciPrime = null;

	public void attachOsci(OsciPrime op){
		mOsciPrime = op;
	}


	protected void init(){
		seekBarLeft.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			

			@Override
			public void onStopTrackingTouch(VerticalSeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(VerticalSeekBar seekBar) {
				
			}

			@Override
			public void onProgressChanged(VerticalSeekBar seekBar, int progress,
					boolean fromUser) {
				updateCursorsVoltage(seekBarLeft.getProgress()-seekBarRight.getProgress());
			}
		});

		seekBarRight.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			int delta = 0;

			@Override
			public void onStopTrackingTouch(VerticalSeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(VerticalSeekBar seekBar) {
				

			}

			@Override
			public void onProgressChanged(VerticalSeekBar seekBar, int progress,
					boolean fromUser) {
				updateCursorsVoltage(seekBarLeft.getProgress()-seekBarRight.getProgress());

			}
		});

		seekBarTop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				updateCursorsTime(seekBarBottom.getProgress()-seekBarTop.getProgress());

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}


		});
		
		seekBarBottom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				updateCursorsTime(seekBarBottom.getProgress()-seekBarTop.getProgress());

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}


		});


	}
	
	private void updateCursorsVoltage(int dvp){
		OsciTransformer ot = OsciTransformer.getInstance();
		if(ot != null){
			if(mOsciPrime != null){
				float[] dvv = ot.transformDeltaVoltage(dvp);
				((TextView)mOsciPrime.findViewById(R.id.display_dch1)).setText("dCH1: "+dvv[0]);
				((TextView)mOsciPrime.findViewById(R.id.display_dch2)).setText("dCH2: "+dvv[1]);
				

			}
		}
	}
	
	private void updateCursorsTime(int dtp){
		OsciTransformer ot = OsciTransformer.getInstance();
		if(ot != null){
			if(mOsciPrime != null){
				float dtv = ot.transformDeltaTime(dtp);
				if(Math.abs(dtv)>=1000){
					((TextView)mOsciPrime.findViewById(R.id.display_dt)).setText("dT: "+dtv/1000+"ms");
					((TextView)mOsciPrime.findViewById(R.id.display_freq)).setText("f: "+(dtv!=0?(1000000/dtv):"-")+"Hz");
				} else {
					((TextView)mOsciPrime.findViewById(R.id.display_dt)).setText("dT: "+dtv+"us");
					((TextView)mOsciPrime.findViewById(R.id.display_freq)).setText("f: "+(dtv!=0?(1000000/dtv):"-")+"Hz");
				}
			}
		}
	}

	protected boolean actionMove(MotionEvent event){		
		return action(event);
	}

	protected boolean actionDown(MotionEvent event){		
		return action(event);
	}

	protected boolean actionUp(MotionEvent event){		
		return action(event);
	}

	protected boolean action(MotionEvent event){
		switch(mFocused){
		case TOP:
			return (seekBarTop!=null)?seekBarTop.dispatchTouchEvent(event):false;
		case BOTTOM:
			return (seekBarBottom!=null)?seekBarBottom.dispatchTouchEvent(event):false;
		case LEFT:
			return (seekBarLeft!=null)?seekBarLeft.dispatchTouchEvent(event):false;
		case RIGHT:
			return (seekBarRight!=null)?seekBarRight.dispatchTouchEvent(event):false;
		case NONE:
			return false;
		default:
			return false;
		}
	}

	private void l(String msg){
		Log.d("Activity", ">==< "+msg+" >==<");
	}

}
