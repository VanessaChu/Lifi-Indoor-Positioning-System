
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
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;

public class DrawMeasureOverlay {
	static final Paint sPaint = new Paint();
	
	public static void draw(Canvas c, OsciPrimeApplication app){
		sPaint.setAntiAlias(true);
		sPaint.setColor(app.pColorMeasure);
		sPaint.setStrokeWidth(1.5f);
		c.drawLine(-OsciPrimeApplication.WIDTH/2, app.pMeasureHandleHor1, OsciPrimeApplication.WIDTH/2, app.pMeasureHandleHor1, sPaint);
		c.drawLine(-OsciPrimeApplication.WIDTH/2, app.pMeasureHandleHor2, OsciPrimeApplication.WIDTH/2, app.pMeasureHandleHor2, sPaint);
		c.drawLine(app.pMeasureHandleVert1, -OsciPrimeApplication.HEIGHT/2, app.pMeasureHandleVert1, OsciPrimeApplication.HEIGHT/2,sPaint);
		c.drawLine(app.pMeasureHandleVert2, -OsciPrimeApplication.HEIGHT/2, app.pMeasureHandleVert2, OsciPrimeApplication.HEIGHT/2,sPaint);
	}
}
