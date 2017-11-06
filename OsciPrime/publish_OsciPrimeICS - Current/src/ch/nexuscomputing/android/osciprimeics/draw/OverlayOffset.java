
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

import java.util.ArrayList;

import android.util.Pair;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;

public class OverlayOffset implements IInfoText{

	final ArrayList<String> mOutput = new ArrayList<String>();

	@Override
	public String[] getInfoText(OsciPrimeApplication app) {
		mOutput.clear();
		float T = (float)app.pPointsOnView/(float)app.pSamplingFrequency*app.pInterleave;//time in [s]
		Pair<String, Float> time = SmartFormater.formatTime(T/OsciPrimeApplication.GRID_COLS);
		Pair<String,Float> ch1 = SmartFormater.formatVoltage(app.pVoltageDivCh1);
		Pair<String,Float> ch2 = SmartFormater.formatVoltage(app.pVoltageDivCh2);
		
		mOutput.add(String.format("t   Div %10.5f %s", time.second, time.first));
		
		if(app.pShowCh1){
			mOutput.add(String.format("CH1 Div %10.5f %s", ch1.second, ch1.first));
		}
		
		if(app.pShowCh2){
			mOutput.add(String.format("CH2 Div %10.5f %s", ch2.second, ch2.first));
		}
		return mOutput.toArray(new String[]{});
	}

}
