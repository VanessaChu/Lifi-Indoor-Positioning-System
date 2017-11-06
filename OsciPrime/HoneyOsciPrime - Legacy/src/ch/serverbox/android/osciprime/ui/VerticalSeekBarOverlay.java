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

import ch.serverbox.android.osciprime.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class VerticalSeekBarOverlay extends VerticalSeekBar{
	
	private int mPosition;
	private int mColor = 0xff0000ff;
	private boolean mDrawGround = false;
	
	final int LEFT = 5;
	final int RIGHT = 6;
	
	final int THUMB_WIDTH = 60;
	final int THUMB_HEIGHT = 30;
	
	Drawable thumb;

	public VerticalSeekBarOverlay(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SeekBar);
        thumb = a.getDrawable(R.styleable.SeekBar_android_thumb);
        thumb.setBounds(0, 0, THUMB_WIDTH, THUMB_HEIGHT);
		
		if(getTag().equals("left")){
			mPosition = LEFT;
		} else if (getTag().equals("right"))
		{
			mPosition = RIGHT;
		}
		
	}
	
	public void setColor(int color){
		this.mColor = color;
	}
	
	public VerticalSeekBarOverlay drawGround(boolean arg){
		this.mDrawGround = arg;
		return this;
	}
	
	@Override
	protected synchronized void onDraw(Canvas canvas) {
		
		float progress = (float)getProgress()/getMax()*getHeight();
		float ground = (float)getSecondaryProgress()/getMax()*getHeight();
		
		Paint paintProgress = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintProgress.setColor(mColor);
		paintProgress.setAlpha(128);
		canvas.drawLine(0, getHeight()-progress, getWidth(), getHeight()-progress, paintProgress);
		
		
		paintProgress.setPathEffect(new DashPathEffect(new float[] {5,5}, 0));
		if(mDrawGround)
			canvas.drawLine(0, getHeight()-ground, getWidth(), getHeight()-ground, paintProgress);

		
		canvas.save();
		switch(mPosition){
		case LEFT:
			canvas.translate(0, getHeight()-progress-THUMB_HEIGHT/2);
			thumb.draw(canvas);
			break;
			
		case RIGHT:
			canvas.translate(getWidth()-THUMB_WIDTH, getHeight()-progress-THUMB_HEIGHT/2);
			thumb.draw(canvas);
			break;
		
		}
		canvas.restore();
		
		
	}

}
