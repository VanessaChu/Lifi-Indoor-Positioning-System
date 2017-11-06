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
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class SeekBarOverlay extends SeekBar{

	private int mPosition;
	final int TOP = 5;
	final int BOTTOM = 6;
	
	final int THUMB_WIDTH = 30;
	final int THUMB_HEIGHT = 60;

	Drawable thumb;


	public SeekBarOverlay(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SeekBar);
        thumb = a.getDrawable(R.styleable.SeekBar_android_thumb);
        thumb.setBounds(0, 0, THUMB_WIDTH, THUMB_HEIGHT);

		if(getTag().equals("top")){
			mPosition = TOP;
		} else if (getTag().equals("bottom"))
		{
			mPosition = BOTTOM;
		}


	}

	@Override
	protected synchronized void onDraw(Canvas canvas) {

		float progress = (float)getProgress()/getMax()*getWidth();

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(0xffff0000);
		paint.setAlpha(128);
		canvas.drawLine(progress, 0, progress, getHeight(), paint);


		canvas.save();

		switch(mPosition){
		case TOP:
			canvas.translate(progress-THUMB_WIDTH/2, 0);
			thumb.draw(canvas);
			break;

		case BOTTOM:
			canvas.translate(progress-THUMB_WIDTH/2, getHeight()-THUMB_HEIGHT);
			thumb.draw(canvas);
			break;
		}
		canvas.restore();

		//super.onDraw(canvas);
	}

}
