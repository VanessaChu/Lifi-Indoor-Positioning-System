
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

import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication.SourceType;

public class Calibration {
	private float[] mCh1Offsets;
	private float[] mCh2Offsets;
	private float[] mCh1AttenuationValues;
	private float[] mCh2AttenuationValues;
	private final SourceType mType;
	
	public Calibration(SourceType type){
		mType = type;
	}
	
	public float[] getCh1Offsets() {
		return mCh1Offsets;
	}
	public void setCh1Offsets(float[] ch1Offsets) {
		this.mCh1Offsets = ch1Offsets;
	}
	public float[] getCh2Offsets() {
		return mCh2Offsets;
	}
	public void setCh2Offsets(float[] ch2Offsets) {
		this.mCh2Offsets = ch2Offsets;
	}
	public float[] getCh1AttenuationValues() {
		return mCh1AttenuationValues;
	}
	public void setCh1AttenuationValues(float[] ch1AttenuationValues) {
		this.mCh1AttenuationValues = ch1AttenuationValues;
	}
	public float[] getCh2AttenuationValues() {
		return mCh2AttenuationValues;
	}
	public void setCh2AttenuationValues(float[] ch2AttenuationValues) {
		this.mCh2AttenuationValues = ch2AttenuationValues;
	}
	
	public SourceType getType(){
		return mType;
	}
}
