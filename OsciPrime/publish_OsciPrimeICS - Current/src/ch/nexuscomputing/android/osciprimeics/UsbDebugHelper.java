
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

package ch.nexuscomputing.android.osciprimeics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.widget.Toast;
import ch.nexuscomputing.android.usb.IUsbConnectionHandler;
import ch.nexuscomputing.android.usb.UsbController;

public class UsbDebugHelper {

	public static void initUsb(Context ctx, final IUsbConnectionHandler handler) {
		UsbController usbCont = new UsbController(ctx, new IServiceInterface() {
			@Override
			public boolean stopNow() {
				return false;
			}

			@Override
			public void onSourceSamples(int[] ch1, int[] ch2, int[] previewCh1, int[] previewCh2, int len,
					int found, float offch1, float offch2) {

			}

			@Override
			public void onError() {
				handler.onDeviceNotFound();
			}

			@Override
			public boolean isRunning() {
				return false;
			}

			@Override
			public OsciPrimeApplication getContext() {
				return null;
			}
		}, handler);
	}

	@SuppressLint("NewApi")
	public static void sendCommand(UsbDevice dev, byte command,
			UsbManager usbman, OsciPrimeApplication app) {
		UsbDeviceConnection connection = usbman.openDevice(dev);
		if (!connection.claimInterface(dev.getInterface(0), true)) {
			Toast.makeText(app, "Could not claim device interface",
					Toast.LENGTH_LONG).show();
			return;
		}
		UsbInterface usbIf = dev.getInterface(0);
		UsbEndpoint ep = usbIf.getEndpoint(0);
		connection.bulkTransfer(ep, new byte[] { command }, 1, 0);
		connection.releaseInterface(usbIf);
		connection.close();
	}

}
