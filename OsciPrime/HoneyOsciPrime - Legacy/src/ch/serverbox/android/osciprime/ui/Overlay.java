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

import ch.serverbox.android.osciprime.OsciPrimeRenderer;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

public class Overlay extends View{
	SeekBar seekBarTop = null, seekBarBottom = null;
	VerticalSeekBar seekBarLeft = null, seekBarRight = null;
	NineGrid nineGrid;

	protected int mFocused;

	protected static final int NONE = -1;
	protected static final int TOP = 0;
	protected static final int BOTTOM = 1;
	protected static final int LEFT = 2;
	protected static final int RIGHT = 3;


	public Overlay(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void attachViews(SeekBar seekBarTop, SeekBar seekBarBottom, VerticalSeekBar seekBarLeft, VerticalSeekBar seekBarRight){
		this.seekBarTop = seekBarTop;
		this.seekBarBottom = seekBarBottom;
		this.seekBarLeft = seekBarLeft;
		this.seekBarRight = seekBarRight;
		
		init();

		nineGrid = new NineGrid(seekBarTop, seekBarBottom, seekBarLeft, seekBarRight);
	}
	
	protected void init(){
		
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN){
			mFocused = nineGrid.resolve(event, getWidth(), getHeight());
			return actionDown(event);
		}
		if(event.getAction() == MotionEvent.ACTION_MOVE){
			return actionMove(event);
		}
		if(event.getAction() == MotionEvent.ACTION_UP){
			return actionUp(event);
		}
		return super.onTouchEvent(event);
	}
	

	
	private boolean actionDown(MotionEvent event){
		switch(mFocused){
		case LEFT:
			return (seekBarLeft!=null)?seekBarLeft.dispatchTouchEvent(event):false;
		case RIGHT:
			return (seekBarRight!=null)?seekBarRight.dispatchTouchEvent(event):false;
		case TOP:
			return (seekBarTop!=null)?seekBarTop.dispatchTouchEvent(event):false;
		case BOTTOM:
			return (seekBarBottom!=null)?seekBarBottom.dispatchTouchEvent(event):false;
		case NONE:
			return true;
		default:
			return false;
		}
	}
	
	protected boolean actionMove(MotionEvent event){
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
	
	protected boolean actionUp(MotionEvent event){
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
