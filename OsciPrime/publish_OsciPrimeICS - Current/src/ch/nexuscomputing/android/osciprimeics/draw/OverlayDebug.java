
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

import android.os.Build;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;
import ch.nexuscomputing.android.osciprimeics.source.OsciUsbSource;

public class OverlayDebug implements IInfoText{

	@Override
	public String[] getInfoText(OsciPrimeApplication app) {
		return  new String[]{
				String.format("%-6s%-10s%-10s%-10s%-15s", "interl", "framesize","minframe","max interl","freq"),
				String.format("%-6d%-10d%-10d%-10d%-15d", app.pInterleave, app.pFrameSize, app.pMinFrameSize, app.pMaxInterleave,app.pSamplingFrequency),
				String.format("%-10s%-10s%-10s", "zoom", "x","y"),
				String.format("%-10.2f%-10.2f%-10.2f",OsciPrimeApplication.dZoom, OsciPrimeApplication.dOffx, OsciPrimeApplication.dOffy),
				String.format("%-10s%-10s%-10s", "pathing", "drawing", "processing"),
				String.format("%-10d%-10d%-10d", OsciPrimeApplication.dPathing,OsciPrimeApplication.dDrawingTime, OsciPrimeApplication.dProcessingTook),
				String.format("%-10s%-10s%-10s%-10s%-6s", "offch1", "offch2", "trlvl ch1", "trlvl ch2", "points"),
				String.format("%-10.2f%-10.2f%-10.2f%-10.2f%-6d", app.getActiveCalibration().getCh1Offsets()[app.pAttenuationSettingCh1], app.getActiveCalibration().getCh2Offsets()[app.pAttenuationSettingCh2], app.pTriggerLevelCh1, app.pTriggerLevelCh2, app.pPointsOnView),
				String.format("%-10s%-10s%-10s%-10s", "usb avg", "usb max","usb min","MBps"),
				String.format("%-10.2f%-10.2f%-10.2f%-10.2f", OsciPrimeApplication.dUsbBenchmark[0], OsciPrimeApplication.dUsbBenchmark[1], OsciPrimeApplication.dUsbBenchmark[2],  (OsciUsbSource.NUM_BUFFERS*OsciUsbSource.NUM_SUBBUFFERS*OsciUsbSource.MAX_BUFFERSIZE/(OsciPrimeApplication.dUsbBenchmark[0]/1000.0))/1E6),
				String.format("%-20s%-20s%-10s","OEM","model", "version"),
				String.format("%-20s%-20s%-10s", Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE)
		};
	}

}
