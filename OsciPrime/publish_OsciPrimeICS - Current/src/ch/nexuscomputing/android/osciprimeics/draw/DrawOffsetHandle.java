
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
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;

public class DrawOffsetHandle {
	
	private static Typeface sTypeFace = null;
	
	public static void ch1(Canvas c, OsciPrimeApplication app){
		draw(c, app, app.mHandleCh1, app.pColorCh1,"CH1");
	}
	
	public static void ch2(Canvas c, OsciPrimeApplication app){
		draw(c, app, app.mHandleCh2, app.pColorCh2, "CH2");
	}
	
	static final Paint sPaint = new Paint();
	
	public static void draw(Canvas c, OsciPrimeApplication app, RectF handle, int col, String text){
		sPaint.reset();
		if(sTypeFace == null){
			sTypeFace = Typeface.createFromAsset(app.getAssets(),"Chewy.ttf");//this was responsible for a memory leak in Android 2.3
		}
		
		sPaint.setStyle(Style.FILL);
		sPaint.setColor(col);
		//c.drawRect(handle, paint);
		
		if(app.pDrawTriggerLabel){
			sPaint.setTypeface(sTypeFace);
			sPaint.setAntiAlias(true);
			sPaint.setTextSize(90);
			float offset = sPaint.measureText(text)+40;
			c.drawText(text, handle.left-offset, handle.bottom-5, sPaint);
		}
	}
}
