
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
import android.graphics.RectF;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;

public class InfoTextScreenShot {
	static final Paint sPaint = new Paint();
	static final RectF r = new RectF();
	public static void draw(Canvas c, OsciPrimeApplication app, int width, int offsety, IInfoText infoText){
		sPaint.reset();
		sPaint.setTypeface(Typeface.MONOSPACE);
		float size = 20;
		String[] debugTexts = infoText.getInfoText(app);
		String text = "";
		
		sPaint.setTextSize(size);
		sPaint.setTextAlign(Align.LEFT);
		
		for(String s : debugTexts){
			if(s.length() > text.length())
				text = s;
		}
		int W = (int) (sPaint.measureText(text)+20);
		int H = (int) (1.6*(debugTexts.length+1)*size);
		
		int x = 0;
		if(infoText instanceof OverlayOffset)
			x = width-W;
		else
			x = 20;
			
		c.translate(x, offsety);
		
		sPaint.setStyle(Style.FILL);
		sPaint.setColor(app.pColorBackground);
		
		r.set(0,0, W,H);
		c.drawRect(r, sPaint);
		sPaint.setColor(app.pColorMeasure);

		sPaint.setAntiAlias(true);		
		//draw debug stuff
		for(int i = 0; i < debugTexts.length; i++){
			c.drawText(debugTexts[i], 10, (int)(1.6*(i+1)*size), sPaint);
		}
		c.translate(-x, -offsety);
	}
}
