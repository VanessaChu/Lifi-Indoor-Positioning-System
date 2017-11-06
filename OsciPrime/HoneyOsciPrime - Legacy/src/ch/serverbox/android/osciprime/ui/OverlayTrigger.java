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
import ch.serverbox.android.osciprime.sources.TriggerProcessor;
import ch.serverbox.android.osciprime.ui.VerticalSeekBar.OnSeekBarChangeListener;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class OverlayTrigger extends Overlay{

	private SeekBar mChannelTop;
	private VerticalSeekBar mChannelLeft;
	private VerticalSeekBar mChannelRight;
	
	public OverlayTrigger(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private OsciPrime mOsciPrime = null;

	public void attachOsci(OsciPrime op){
		mOsciPrime = op;
	}
	
	public void attachChannels(SeekBar top, VerticalSeekBar left, VerticalSeekBar right){
		this.mChannelTop = top;
		this.mChannelLeft = left;
		this.mChannelRight = right;
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
				if(fromUser){
					sendTrigger(seekBarLeft.getProgress(),seekBarLeft.getSecondaryProgress(),TriggerProcessor.CHANNEL_1);
					mChannelLeft.setSecondaryProgress(progress);
				}
			}
		});

		seekBarRight.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(VerticalSeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(VerticalSeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(VerticalSeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser){
					sendTrigger(seekBarRight.getProgress(), seekBarRight.getSecondaryProgress(),TriggerProcessor.CHANNEL_2);
					mChannelRight.setSecondaryProgress(progress);
				}
			}
		});

		seekBarTop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser){
					mChannelTop.setProgress(progress);
					float timeOffset = OPC.NUM_POINTS_PER_PLOT/2*(float)(progress-seekBar.getMax()/2)/seekBar.getMax();
					OsciPrimeRenderer.setOffsetTime((int) timeOffset);
				}

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}


		});


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

	private boolean action(MotionEvent event){
		switch(mFocused){
		case TOP:
			return (seekBarTop!=null)?seekBarTop.dispatchTouchEvent(event):false;
		case BOTTOM:
			return (seekBarBottom!=null)?seekBarBottom.dispatchTouchEvent(event):false;
		case LEFT:
			l("progress1 "+seekBarLeft.getProgress());
			return (seekBarLeft!=null)?seekBarLeft.dispatchTouchEvent(event):false;
		case RIGHT:
			l("progress2 "+seekBarRight.getProgress());
			return (seekBarRight!=null)?seekBarRight.dispatchTouchEvent(event):false;
		case NONE:
			return false;
		default:
			return false;
		}
	}

	private void sendTrigger(int progress, int offset, int channel){
		OsciTransformer ot = OsciTransformer.getInstance();
		if(ot != null){
			if(mOsciPrime != null){
				int t = ot.transformTrigger(progress, offset, channel);
				mOsciPrime.sendMsgTrigger(t, channel);
			}
		}
	}


	private void l(String msg){
		Log.d("Activity", ">==< "+msg+" >==<");
	}


}
