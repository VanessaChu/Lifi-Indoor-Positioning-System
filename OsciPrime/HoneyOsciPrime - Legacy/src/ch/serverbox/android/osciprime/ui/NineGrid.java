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

import android.view.MotionEvent;
import android.widget.SeekBar;

public class NineGrid {
	SeekBar sTop, sBottom;
	VerticalSeekBar sLeft, sRight;
	
	final float th = (float) 0.2;
	final int NONE = -1;
	final int TOP = 0;
	final int BOTTOM = 1;
	final int LEFT = 2;
	final int RIGHT = 3;
	
	public NineGrid(SeekBar sTop, SeekBar sBottom, VerticalSeekBar sLeft, VerticalSeekBar sRight){
			this.sTop = sTop;
			this.sBottom = sBottom;
			this.sLeft = sLeft;
			this.sRight = sRight;
	}
	
	
/* [1][2][3]
 * [4][5][6]
 * [7][8][9]
 * 
 */
	
	public int resolve(MotionEvent event, int width, int height){
		if( (event.getX() < th*width) && (event.getY() < th*height) ){
			// [1]
			if(sLeft == null)
				return TOP;
			else if(sTop == null)
				return LEFT;
			else{
				if(((float)sTop.getProgress()/sTop.getMax()) < th){
					return TOP;
				} else{
					return LEFT;
				}
			}
		}
		if( (event.getX()>th*width) && (event.getX()<(1-th)*width) && (event.getY()<th*height) )
			// [2]
			return TOP;
		if(((1-th)*width<event.getX()) && (event.getY()<th*height)) {
			// [3]
			if(sRight == null)
				return TOP;
			else if(sTop == null)
				return RIGHT;
			else{
				if(((float)sTop.getProgress()/sTop.getMax()) > (1-th)){
					return TOP;
				} else{
					return RIGHT;
				}
			}
		}
		if( (event.getY()>th*height) && (event.getY()<(1-th)*height) && (event.getX()<th*width))
			// [4]
			return LEFT;
		if( (event.getY()>th*height) && (event.getY()<(1-th)*height) && (event.getX()>th*width) && (event.getX()<(1-th)*width))
			// [5]
			return NONE;
		if( (event.getY()>th*height) && (event.getY()<(1-th)*height) && (event.getX()>(1-th)*width))
			// [6]
			return RIGHT;
		if((1-th)*height < event.getY() && (event.getX()<th*width)){
			// [7]
			if(sLeft == null)
				return BOTTOM;
			else if(sBottom == null)
				return LEFT;
			else{
				if(((float)sBottom.getProgress()/sTop.getMax()) < th){
					return BOTTOM;
				} else{
					return LEFT;
				}
			}
		}
		if((1-th)*height < event.getY() && (event.getX()>th*width) && (event.getX()<(1-th)*width))
			// [8]
			return BOTTOM;
		if((1-th)*height < event.getY() && (event.getX()>(1-th)*width)){
			// [9]
			if(sRight == null)
				return BOTTOM;
			else if(sBottom == null)
				return RIGHT;
			else{
				if(((float)sBottom.getProgress()/sTop.getMax()) > (1-th)){
					return BOTTOM;
				} else{
					return RIGHT;
				}
			}
		}
		
		return NONE;
	}
}
