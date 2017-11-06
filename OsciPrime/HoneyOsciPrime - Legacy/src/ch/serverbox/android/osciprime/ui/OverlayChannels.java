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
import android.widget.Toast;

public class OverlayChannels extends Overlay{

	private VerticalSeekBar mTriggerLeft, mTriggerRight;
	private SeekBar mTriggerTop;

	public OverlayChannels(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	private OsciPrime mOsciPrime = null;

	public void attachOsci(OsciPrime op){
		mOsciPrime = op;
	}

	public void attachTriggers(VerticalSeekBar left, VerticalSeekBar right, SeekBar top){
		this.mTriggerLeft = left;
		this.mTriggerRight = right;
		this.mTriggerTop = top;
	}

	protected void init(){
		seekBarLeft.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			int delta = 0;
			int deltaGround = 0;

			@Override
			public void onStopTrackingTouch(VerticalSeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(VerticalSeekBar seekBar) {
				delta = seekBar.getProgress() - mTriggerLeft.getProgress();
				deltaGround = seekBar.getProgress() - seekBar.getSecondaryProgress();

			}

			@Override
			public void onProgressChanged(VerticalSeekBar seekBar, int progress,
					boolean fromUser) {
				//l("progress1 "+seekBarLeft.getProgress());
				if(fromUser){
				float offset1 = 65536*(float)(progress-seekBar.getMax()/2)/seekBar.getMax();
				OsciPrimeRenderer.setOffsetCh1((int) offset1);
				mTriggerLeft.setProgress(progress-delta);
				mTriggerLeft.setSecondaryProgress(progress);
				//sendTrigger(seekBarLeft.getProgress(), TriggerProcessor.CHANNEL_1);
				
				mOsciPrime.requestRender();
				seekBar.setSecondaryProgress(progress - deltaGround);

				}

			}
		});

		seekBarRight.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			int delta = 0;
			int deltaGround = 0;

			@Override
			public void onStopTrackingTouch(VerticalSeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(VerticalSeekBar seekBar) {
				delta = seekBar.getProgress() - mTriggerRight.getProgress();
				deltaGround = seekBar.getProgress() - seekBar.getSecondaryProgress();

			}

			@Override
			public void onProgressChanged(VerticalSeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser){
				//l("progress2 "+seekBarRight.getProgress());
				float offset2 = 65536*(float)(progress-seekBar.getMax()/2)/seekBar.getMax();
				OsciPrimeRenderer.setOffsetCh2((int) offset2);
				mTriggerRight.setProgress(progress-delta);
				mTriggerRight.setSecondaryProgress(progress);
				//sendTrigger(seekBarRight.getProgress(), TriggerProcessor.CHANNEL_2);
				mOsciPrime.requestRender();
				seekBar.setSecondaryProgress(progress - deltaGround);
				}

			}
		});

		seekBarTop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser){
					mTriggerTop.setProgress(progress);
					float timeOffset = OPC.NUM_POINTS_PER_PLOT/2*(float)(progress-seekBar.getMax()/2)/seekBar.getMax();
					OsciPrimeRenderer.setOffsetTime((int) timeOffset);
					mOsciPrime.requestRender();

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
