
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
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.TypedValue;
import android.view.View;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;

public class InfoText {
	static final Paint sPaint = new Paint();
	static final Rect sBounds = new Rect();
	static RectF r = new RectF();
	
	static float px = -1;
	
	public static void draw(Canvas c, OsciPrimeApplication app, View v, IInfoText iInfo){
		
		sPaint.setTypeface(Typeface.MONOSPACE);
		
		if(px == -1){
			float size = 12;
			px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, app.getResources().getDisplayMetrics());
		}
		

		String[] debugTexts = iInfo.getInfoText(app);
		
		String text = "";
		
		sPaint.setTextSize(px);
		sPaint.setTextAlign(Align.LEFT);
		
		for(String s : debugTexts){
			if(s.length() > text.length())
				text = s;
		}
		int W = (int) (sPaint.measureText(text)+20);
		String lastText = debugTexts[debugTexts.length-1];
		sPaint.getTextBounds(lastText, 0, lastText.length(), sBounds);
		int H = (int) (1.5*(debugTexts.length+1)*px);
		c.translate(v.getWidth()-W-app.pBarWidth, v.getHeight()-H);
		sPaint.setStyle(Style.FILL);
		sPaint.setColor(app.pColorBackground);
		r.set(0,0, W,H);
		c.drawRect(r, sPaint);
		sPaint.setColor(app.pColorMeasure);
		sPaint.setAntiAlias(true);		
		//draw debug stuff
		for(int i = 0; i < debugTexts.length; i++){
			c.drawText(debugTexts[i], 10, (int)(1.6*(i+1)*px), sPaint);
		}
		c.translate(-W-app.pBarWidth, -(c.getHeight()-H));
	}
	
}
