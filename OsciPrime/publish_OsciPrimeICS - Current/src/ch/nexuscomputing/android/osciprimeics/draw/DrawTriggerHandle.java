
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

public class DrawTriggerHandle {
	
	private static Typeface sTypeFace = null;
	public static void ch1(Canvas c, OsciPrimeApplication app){
		draw(c, app, app.mHandleTriggerCH1, app.pColorCh1);
	}
	
	public static void ch2(Canvas c, OsciPrimeApplication app){
		draw(c, app, app.mHandleTriggerCH2, app.pColorCh2);
	}
	
	static final Paint sPaint = new Paint();
	
	public static void draw(Canvas c, OsciPrimeApplication app, RectF handle, int col){
		sPaint.reset();
		if(sTypeFace == null){
			sTypeFace = Typeface.createFromAsset(app.getAssets(),"Chewy.ttf");//this was responsible for a memory leak in Android 2.3
		}
		
		sPaint.setStyle(Style.FILL);
		sPaint.setColor(col);
		//c.drawRect(handle, paint);
		sPaint.setAlpha(128);
		sPaint.setColor(col);
		if(app.pDrawTriggerLabel){
			sPaint.setTypeface(sTypeFace);
			sPaint.setAntiAlias(true);
			sPaint.setTextSize(70);
			c.drawText("Trigger", handle.right+20, handle.bottom, sPaint);
		}
		c.drawLine(-OsciPrimeApplication.WIDTH/2, 0, OsciPrimeApplication.WIDTH/2+OsciPrimeApplication.TRIGGER_HANDLE_PADDING, 0, sPaint);
	}
}
