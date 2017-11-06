
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

package ch.nexuscomputing.android.osciprimeics.calibration;

import ch.nexuscomputing.android.osciprimeics.L;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication.SourceType;

public class CalibrationParser {
	
	static Calibration parse(String contents){
		String[] floats = contents.split(",");
		if(floats.length != 20)
			return null;
		
		float[] ch1AttenuationValues = new float[5];
		float[] ch2AttenuationValues = new float[5];
		float[] ch1Offsets = new float[5];
		float[] ch2Offsets = new float[5];
		
		for(int i = 0; i < 5; i++){//ch1
			ch1AttenuationValues[i] = Float.valueOf(floats[2*i]).floatValue();//cal
			ch1Offsets[i] = Float.valueOf(floats[2*i+1]).floatValue();//offset
		}
		
		
		for(int i = 5; i < 10 ; i++){//ch2
			ch2AttenuationValues[i-5] = Float.valueOf(floats[2*i]).floatValue();//cal
			ch2Offsets[i-5] = Float.valueOf(floats[2*i+1]).floatValue();//offset
		}
		
		Calibration cal = new Calibration(SourceType.USB);
		cal.setCh1AttenuationValues(ch1AttenuationValues);
		cal.setCh2AttenuationValues(ch2AttenuationValues);
		cal.setCh1Offsets(ch1Offsets);
		cal.setCh2Offsets(ch2Offsets);
		return cal;
	}
}
