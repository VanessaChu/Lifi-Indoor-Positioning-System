
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

/*
 * UsbController.java
 * This file is part of UsbController
 *
 * Copyright (C) 2012 - Manuel Di Cerbo
 *
 * UsbController is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * UsbController is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with UsbController. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.nexuscomputing.android.usb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import android.util.Log;
import ch.nexuscomputing.android.osciprimeics.IServiceInterface;
import ch.nexuscomputing.android.osciprimeics.L;

/**
 * (c) Neuxs-Computing GmbH Switzerland
 * @author Manuel Di Cerbo, 02.02.2012
 *
 */
@SuppressLint("NewApi")
public class UsbController {

	private final Context mApplicationContext;
	private final UsbManager mUsbManager;
	private final IServiceInterface mSvc;
	private final IUsbConnectionHandler mConnectionHandler;
	private final int VID_PRE = 0x04B4;
	private final int PID_PRE = 0x8613;
	private final int VID_POST = 0x04B4;
	private final int PID_POST = 0x1004;
	protected static final String ACTION_USB_PERMISSION = "ch.serverbox.android.USB";

	/**
	 * Activity is needed for onResult
	 * 
	 * @param parentContext
	 * @param svc 
	 */
	public UsbController(Context parentContext,
			IServiceInterface svc, IUsbConnectionHandler connectionHandler) {
		mApplicationContext = parentContext.getApplicationContext();
		mSvc = svc;
		mConnectionHandler = connectionHandler;
		mUsbManager = (UsbManager) mApplicationContext
				.getSystemService(Context.USB_SERVICE);
		init();
	}
	
	private final IPermissionListener mPermissionListener = new IPermissionListener() {
		@Override
		public void onPermissionDenied(UsbDevice d) {
			UsbManager usbman = (UsbManager) mApplicationContext
					.getSystemService(Context.USB_SERVICE);
			PendingIntent pi = PendingIntent.getBroadcast(
					mApplicationContext, 0, new Intent(
							ACTION_USB_PERMISSION), 0);
			mApplicationContext.registerReceiver(mPermissionReceiver,
					new IntentFilter(ACTION_USB_PERMISSION));
			//before we ask for permission, check if we need to get out...
			if(mSvc.stopNow()){
				L.d("mPermissionListener: returning!");
				return;
			}
			usbman.requestPermission(d, pi);
		}
	};

	private void init() {
		preparefile();
		enumerate(mPermissionListener);
	}


	private Thread mUsbThread;

	private void startHandler(UsbDevice d) {
		mConnectionHandler.onDeviceInitialized(d);
	}
	
	private void enumerate(IPermissionListener listener) {
		l("enumerating");
		HashMap<String, UsbDevice> devlist = mUsbManager.getDeviceList();
		Iterator<UsbDevice> deviter = devlist.values().iterator();
		while (deviter.hasNext()) {
			UsbDevice d = deviter.next();
			l("Found device: "
					+ String.format("%04X:%04X", d.getVendorId(),
							d.getProductId()));
			if (d.getVendorId() == VID_PRE && d.getProductId() == PID_PRE) {
				l("Device under: " + d.getDeviceName());
				if (!mUsbManager.hasPermission(d)){
					listener.onPermissionDenied(d);
					return;
				}else{
					dispatchUpload(d);
					return;
				}
			}
			if (d.getVendorId() == VID_POST && d.getProductId() == PID_POST) {
				l("Device under: " + d.getDeviceName());
				if (!mUsbManager.hasPermission(d)){
					l("no permission on scope");
					listener.onPermissionDenied(d);
					//it's ok, user will grant permission
					return;
				}else{
					l("starting handler");
					startHandler(d);
					return;
				}
			}
		}
		l("no more devices found");
		mConnectionHandler.onDeviceNotFound();
	}

	/**
	 * dispatch to a new thread if coming from broadcastreceiver ... well either way always dispatch...
	 * @param d
	 */
	private void uploadhex(UsbDevice d) {
		if (d == null)
			return;
		UsbDeviceConnection conn = mUsbManager.openDevice(d);
		if (conn == null) {
			e("cannot open device!");
			return;
		}
		int handle = conn.getFileDescriptor();
		if (nativeload(handle, new File(Environment.getExternalStorageDirectory()+File.separator+"osciprime"+File.separator+"fifo.hex").getAbsolutePath()) < 0)
			e("failed");
		else {
			l("successfully uploaded hex"); 
			//wait before enumerating next time
			try {
				for (int i = 0; i < 100; i++){
					if(mSvc.stopNow())
						return;
					Thread.sleep(50);
				}
			} catch (InterruptedException e) {e(e);return;}//exit on interrupt
			enumerate(mPermissionListener);// Enumerate again! Crucial here!
		}		
	}

	private class PermissionReceiver extends BroadcastReceiver {
		private final IPermissionListener mPermissionListener;

		public PermissionReceiver(IPermissionListener permissionListener) {
			mPermissionListener = permissionListener;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			mApplicationContext.unregisterReceiver(this);
			if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
				if (!intent.getBooleanExtra(
						UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					mPermissionListener.onPermissionDenied((UsbDevice) intent
							.getParcelableExtra(UsbManager.EXTRA_DEVICE));
				} else {
					l("Permission granted");
					final UsbDevice dev = (UsbDevice) intent
							.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (dev != null) {
						if (dev.getVendorId() == VID_PRE
								&& dev.getProductId() == PID_PRE) {
							dispatchUpload(dev);
						}else if(dev.getVendorId() == VID_POST && dev.getProductId() == PID_POST){
							startHandler(dev);
						}
					} else {
						e("device not present!");
					}
				}
			}
		}

	}

	private BroadcastReceiver mPermissionReceiver = new PermissionReceiver(
			new IPermissionListener() {
				@Override
				public void onPermissionDenied(UsbDevice d) {
					l("Permission denied on " + d.getDeviceId());
					mSvc.onError();
				}
			});

	private static interface IPermissionListener {
		void onPermissionDenied(UsbDevice d);
	}
	
	private void preparefile() {
		try {
			InputStream fis = mApplicationContext.getAssets().open("fifo.hex");
			File ext = new File(Environment.getExternalStorageDirectory()+File.separator+"osciprime");
			ext.mkdirs();
			File f = new File(ext, "fifo.hex");
			FileOutputStream fos = new FileOutputStream(f);
			byte[] buffer = new byte[1024];
			int numBytes = 0;
			while ((numBytes = fis.read(buffer)) > -1) {
				fos.write(buffer, 0, numBytes);
			}
			fos.close();
			fis.close();
		} catch (IOException e) {
			e(e);
		}
	}
	

	public final static String TAG = "USBController";

	private void l(Object msg) {
		Log.d(TAG, ">==< " + msg.toString() + " >==<");
	}

	private void e(Object msg) {
		Log.e(TAG, ">==< " + msg.toString() + " >==<");
	}
	
	private void dispatchUpload(final UsbDevice dev){
		new Thread(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("HexUploader");
				uploadhex(dev);// has new thread
			}
		}).start();
	}
	
	public native void split(ByteBuffer buffer, int len, ByteBuffer ch1, ByteBuffer ch2, int off1, int off2);
	public native int nativeload(int handle, String path);
	static {
		System.loadLibrary("ezusb");
	}
	
}
