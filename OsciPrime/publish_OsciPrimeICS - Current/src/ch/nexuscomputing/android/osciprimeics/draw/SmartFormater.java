
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

import android.util.Pair;

public class SmartFormater {
	
	/**
	 * 
	 * @param t time value ins [s]
	 * @return positive time value and matching unit in brackets [ms] for instance 
	 */
	public static Pair<String, Float> formatTime(float t){
		String unit = "[s]";
		t = Math.abs(t);
		float retval = t;
		
		if(t < 1 && t >= (float)1E-3){
			unit = "[ms]";
			retval = (float) (1E3*t);
		}else if(t < (float)1E-3 && t >= (float)1E-6){
			unit = "[us]";
			retval = (float) (1E6*t);
		}else if(t < (float)1E-6 && t >= (float)1E-9){
			unit = "[ns]";
			retval = (float) (1E9*t);
		}else if(t < (float)1E-9){
			unit = "[ps]";
			retval = (float) (1E12*t);
		}
		return new Pair<String, Float>(unit, retval);
	}
	
	public static Pair<String, Float> formatVoltage(float t){
		String unit = "[V]";
		t = Math.abs(t);
		float retval = t;
		
		if(t < 1 && t >= (float)1E-3){
			unit = "[mV]";
			retval = (float) (1E3*t);
		}else if(t < (float)1E-3 && t >= (float)1E-6){
			unit = "[uV]";
			retval = (float) (1E6*t);
		}else if(t < (float)1E-6 && t >= (float)1E-9){
			unit = "[nV]";
			retval = (float) (1E9*t);
		}else if(t < (float)1E-9){
			unit = "[pV]";
			retval = (float) (1E12*t);
		}
		return new Pair<String, Float>(unit, retval);
	}
	
}