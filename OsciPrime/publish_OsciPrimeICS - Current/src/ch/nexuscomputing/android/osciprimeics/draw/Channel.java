
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

import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Channel {
	
	
	public static void draw(Canvas canvas, float[] values, float att, Paint paint){
		float x0 = -OsciPrimeApplication.WIDTH / 2;
		canvas.drawLines(values,paint);
		canvas.drawLine(x0, 0, -x0, 0, paint);
	}
}
