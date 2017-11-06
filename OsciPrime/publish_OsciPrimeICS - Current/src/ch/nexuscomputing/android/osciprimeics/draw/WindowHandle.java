
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

package ch.nexuscomputing.android.osciprimeics.draw;

import android.graphics.Canvas;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import ch.nexuscomputing.android.osciprimeics.L;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication.SourceType;

public class WindowHandle extends HandleDrawable{

	final static Path sDoubleHeighPath = new Path();
	final static Path sSingleHeightPath = new Path();
	final float mInitHeight;
	final float mInitWidth;
	
	
	public WindowHandle(Path src, ICallback callback, float x, float y,
			float offX, float offY, float w, float h, OsciPrimeApplication app,
			int layerFlags, boolean lockX, boolean lockY, int color, int type) {
		super(src, callback, x, y, offX, offY, w, h, app, layerFlags, lockX, lockY,
				color, type);
		mInitHeight = h;
		mInitWidth = w;
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(5);
		sSingleHeightPath.addRect(0,0,w,h,Direction.CCW);
		sDoubleHeighPath.addRect(0,0,w,2*h,Direction.CCW);
	}
	
	@Override
	public void draw(Canvas canvas) {
		if(mApplication.pActiveSource == SourceType.NETWORK){
			return;
		}
		if(!mApplication.pShowBufferPreview)
			return; 
		
		if(mApplication.pFrameSize != 0 && mApplication.pCapturedFrameSize > 0){
			float width = (mApplication.pInterleave*mApplication.pPointsOnView)/(float)mApplication.pCapturedFrameSize*OsciPrimeApplication.WIDTH;
			sDoubleHeighPath.reset();
			sDoubleHeighPath.addRect(0, 0, width, 2*mInitHeight, Direction.CCW); 
			sSingleHeightPath.reset();
			sSingleHeightPath.addRect(0, 0, width, mInitHeight, Direction.CCW); 
			box.right = width;
			offX = -width/2;
		}
		
		
		if(mApplication.pShowCh1 && mApplication.pShowCh2){
			mPath.set(sDoubleHeighPath);
			box.bottom = mInitHeight*2 + offY;
		}else{
			mPath.set(sSingleHeightPath);
			box.bottom = mInitHeight + offY;
		}
		

		
		if(mApplication.getLastTrigger() > -1){
			float tr = mApplication.getLastTrigger();
			float pos = (float)tr/(mApplication.pCapturedFrameSize)*OsciPrimeApplication.WIDTH-OsciPrimeApplication.WIDTH/2;
			x = pos;
		}
		super.draw(canvas);
	}

}
