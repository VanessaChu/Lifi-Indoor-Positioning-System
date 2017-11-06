/**
 * This file is part of OsciPrime
 *
 * Copyright (C) 2011 - Manuel Di Cerbo, Andreas Rudolf
 * 
 * Nexus-Computing GmbH, Switzerland 2011
 *
 * OsciPrime is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * OsciPrime is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OsciPrime; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, 
 * Boston, MA  02110-1301  USA
 */
package ch.serverbox.android.osciprime.sources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Handler;
import android.text.GetChars;
import android.util.Log;
import ch.serverbox.android.osciprime.OPC;
import ch.serverbox.android.osciprime.OsciPreferences;
import ch.serverbox.android.osciprime.OsciPrime;
import ch.serverbox.android.osciprime.hw.UsbContinuousInterface;

public class UsbContinuousSource extends SourceBase{
	public static final byte BM_AMP_CH1 = 0x07;
	public static final byte BM_AMP_CH2 = 0x38;
	public static final String ACTION_USB_PERMISSION = "ch.serverbox.android.USB";

	public static final int NUM_BUFFERS = 4;
	public static final int NUM_SUBBUFFERS = 25;
	public static final int MAX_BUFFERSIZE = 16384;//Framework restriction (silly? kind of...)
	/*
	 * Dear friends at Google ... WTF! Why do you only mention this restriction on the ADK
	 * Documentation page and not on the USB-Host Doc page? I mean really, WTF!
	 * Srsly... 
	 */


	private boolean mPaused = true;
	private boolean mRunning = false;
	private UsbDeviceConnection mConnection = null;
	private UsbEndpoint cmdEp = null;

	private ProgressDialog mProgressDialog = null;

	public UsbContinuousSource(Handler sink, OsciPreferences pref) {
		super(sink,pref);
	}

	@Override
	public void loop() {
		mPaused = false;
		mRunning = true;
		enumerate();
	}

	@Override
	public void stop() {
		l("stop");
		mPaused = true;
	}

	@Override
	public void quit() {
		l("quit");
		/*
		if(mRunning)
			mUsb.stop();*/
		mPaused = true;
		mRunning = false;
		 
		 
	}

	public void callback(int[] ch1, int[] ch2){
		/*
    	if(mPaused){
    		return;
    	}
    	onNewSamples(ch1, ch2);
		 */ 
	}

	@Override
	public int cBlocksize() {
		return NUM_SUBBUFFERS*MAX_BUFFERSIZE/2;
		//return 396288/2;
	}

	@Override
	public int cSignedNess() {
		return SIGNEDNESS_UNSIGNED;
	}

	@Override
	public int cRange() {
		return RANGE_BYTE;
	}

	@Override
	public GainTripplet[] cGainTrippletsCh1() {//float[] correctionCh1 = new float[] {1.1f,1.16f,1.57f,1.70f,1.73f};
		return new GainTripplet[]{
				new GainTripplet((byte)0x3F, "4.0[V]", 0.95625f, 4.0f),
				new GainTripplet((byte)0x12, "2.0[V]", 0.96875f, 2.0f),
				new GainTripplet((byte)0x09, "1.0[V]", 1.3235f, 1.0f),
				new GainTripplet((byte)0x00, "0.5[V]", 1.45f, 0.5f),
				new GainTripplet((byte)0x1B, "0.25[V]", 1.73f, 0.25f)
		};
	}

	@Override
	public GainTripplet[] cGainTrippletsCh2() {
		return new GainTripplet[]{
				new GainTripplet((byte)0x3F, "4.0[V]", 0.95625f, 4.0f),
				new GainTripplet((byte)0x12, "2.0[V]", 0.96875f, 2),
				new GainTripplet((byte)0x09, "1.0[V]", 1.35f, 1),
				new GainTripplet((byte)0x00, "0.5[V]", 1.45f, .5f),
				new GainTripplet((byte)0x1B, "0.25[V]", 1.73f, .25f)
		};
	}

	@Override
	public TimeDivisionPair[] cTimeDivisionPairs() {
		return new TimeDivisionPair[]{
				new TimeDivisionPair("2.5[us]", 1, 2.5f),
				new TimeDivisionPair("5[us]", 2, 5),
				new TimeDivisionPair("10[us]", 4, 10),
				new TimeDivisionPair("25[us]", 10, 25),
				new TimeDivisionPair("50[us]", 20, 50),
				new TimeDivisionPair("100[us]", 40, 100),
				new TimeDivisionPair("250[us]", 100, 250),
				new TimeDivisionPair("500[us]", 200, 500),
				new TimeDivisionPair("1[ms]", 400, 1000),
		};
	}

	private void l(Object s){
		Log.d(getClass().getSimpleName(), ">==< "+s.toString()+" >==<");
	}

	public void callback(ByteBuffer ch1, ByteBuffer ch2) {
		if(mPaused){
			return;
		}
		onNewSamples(ch1, ch2); 
	}

	public void initGain(byte g1, byte g2){
		byte mod = g1;
		byte ch1Cmd = g2;
		byte v = (byte)(ch1Cmd & ~BM_AMP_CH2);
		v |= mod & BM_AMP_CH2;
		sendCommand(v);
	}

	@Override
	public int cSourceId() {
		return OPC.SOURCE_USB;
	}

	@Override
	public void sendCommand(byte cmd) {
		if(mConnection != null){
			mConnection.bulkTransfer(cmdEp, new byte[]{cmd}, 1, 1000);
		}
	}

	private void enumerate(){
		l("enumerating");
		UsbManager usbman = (UsbManager)OsciPrime.sAppContext.getSystemService(Context.USB_SERVICE);

		HashMap<String, UsbDevice> devlist = usbman.getDeviceList();
		Iterator<UsbDevice> deviter = devlist.values().iterator();
		PendingIntent pi = PendingIntent.getBroadcast(OsciPrime.sAppContext, 0, new Intent(
				ACTION_USB_PERMISSION), 0);

		preparefile();

		while (deviter.hasNext()) {
			UsbDevice d = deviter.next();
			l("Found device: "
					+ String.format("%04X:%04X", d.getVendorId(),
							d.getProductId()));
			if (String.format("%04X:%04X", d.getVendorId(), d.getProductId())
					.equals("04B4:8613")) {
				// we need to upload the hex file, first request permission
				l("Device under: " + d.getDeviceName());
				OsciPrime.sAppContext.registerReceiver(mPermissionReceiver, new IntentFilter(
						ACTION_USB_PERMISSION));
				if (!usbman.hasPermission(d))
					usbman.requestPermission(d, pi);
				else
					uploadFxHex(d);
				break;
			} else if (String.format("%04X:%04X", d.getVendorId(),
					d.getProductId()).equals("04B4:1004")) {
				// this is the right hex, we are going to communicate
				l("Device under: " + d.getDeviceName());
				OsciPrime.sAppContext.registerReceiver(mPermissionReceiver, new IntentFilter(
						ACTION_USB_PERMISSION));
				if (!usbman.hasPermission(d))
					usbman.requestPermission(d, pi);
				else
					mainloop(d);
				break;
			}
		}
		l("no more devices found");
	}

	private static UsbDevice sDevice = null;
	private void mainloop(UsbDevice d) {
		sDevice = d;// not really nice...
		new Thread(mLoop).start();
	}

	private Runnable mLoop = new Runnable() {
		ByteBuffer[][] mByteBuffers = new ByteBuffer[NUM_BUFFERS][NUM_SUBBUFFERS];
		ByteBuffer[] mBigBuffer  = new ByteBuffer[NUM_BUFFERS];
		@Override
		public void run() {
			Thread.currentThread().setName("UsbContinuous");
			for(int i = 0; i < NUM_BUFFERS; i++){
				mBigBuffer[i] = ByteBuffer.allocateDirect(MAX_BUFFERSIZE*NUM_SUBBUFFERS);
				mBigBuffer[i].order(ByteOrder.nativeOrder());
				mBigBuffer[i].position(0);
				mBigBuffer[i].put(new byte[MAX_BUFFERSIZE*NUM_SUBBUFFERS]);//zero
				for(int j = 0; j < NUM_SUBBUFFERS; j++){
					mBigBuffer[i].position(j*MAX_BUFFERSIZE);
					mByteBuffers[i][j] = mBigBuffer[i].slice();
				}
			}

			//TEST
			l("Value is: "+mBigBuffer[2].get(200000));
			UsbDevice dev = sDevice;
			try {
				Thread.sleep(1000);// wait for reenumeration
			} catch (InterruptedException e) {
				e(e);
			}
			if (dev == null)
				return; 
			UsbManager usbm = (UsbManager) OsciPrime.sAppContext.getSystemService(Context.USB_SERVICE);
			mConnection = usbm.openDevice(dev);
			l("Interface Count: "+dev.getInterfaceCount());
			l("Using "+String.format("%04X:%04X", sDevice.getVendorId(), sDevice.getProductId()));

			if(!mConnection.claimInterface(dev.getInterface(0), true)) 
				return;



			UsbInterface usbIf = dev.getInterface(0);				
			for(int i = 0; i < usbIf.getEndpointCount(); i++){
				l("EP: "+String.format("0x%02X", usbIf.getEndpoint(i).getAddress()));
				if(usbIf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK){
					l("Bulk Endpoint");
				}else{
					l("Not Bulk");
				}
			}


			 cmdEp = usbIf.getEndpoint(0);
			if(mConnection.bulkTransfer(cmdEp, new byte[]{0x40}, 1, 10000) != 1){
				e("could not submit command bulk transfer");
				return; 
			} 
			l("Successfully submitted command bulk transfer");

			long t = System.currentTimeMillis();
			UsbEndpoint dataEp = usbIf.getEndpoint(2); 

			UsbRequest req[][] = new UsbRequest[NUM_BUFFERS][NUM_SUBBUFFERS];
			t = System.currentTimeMillis();
			l("initializing request ...");
			for(int i = 0; i < NUM_BUFFERS; i++){
				for(int j = 0; j < NUM_SUBBUFFERS; j++){
					req[i][j] = new UsbRequest();
					req[i][j].initialize(mConnection, dataEp);
					if(req[i][j].queue(mByteBuffers[i][j], MAX_BUFFERSIZE) == false){
						e("could not queue transfer");
					}
					req[i][j].setClientData(new String("Buffer "+i+" Subbuffer "+j));
				}
			}
			l("... done");

			ByteBuffer mChannelLeft = ByteBuffer.allocateDirect(4*MAX_BUFFERSIZE*NUM_SUBBUFFERS/2);//4 because int
			ByteBuffer mChannelRight = ByteBuffer.allocateDirect(4*MAX_BUFFERSIZE*NUM_SUBBUFFERS/2);
			mChannelLeft.order(ByteOrder.nativeOrder());
			mChannelRight.order(ByteOrder.nativeOrder());

			int dbgcnt = 0;
			for(;;){				
				if(!mRunning || mPaused){
					l("Returning from Sampling Thread");
					return;
				}
				for(int i = 0; i < NUM_BUFFERS; i++){
					for(int j = 0; j < NUM_SUBBUFFERS; j++){
						UsbRequest r = mConnection.requestWait();
						if(r == null)
							return;
						r.queue(mByteBuffers[i][j], MAX_BUFFERSIZE);
					}
					//START
					t = System.currentTimeMillis();
					mBigBuffer[i].position(0);
					mChannelLeft.position(0);
					mChannelRight.position(0);
					split(mBigBuffer[i], NUM_SUBBUFFERS*MAX_BUFFERSIZE, mChannelLeft, mChannelRight);
					callback(mChannelLeft, mChannelRight);
					//DONE
					if(dbgcnt % 256 == 0){
						l("Processing took: "+(System.currentTimeMillis()-t)+" [ms]");
					}
					dbgcnt++;
				}
			}
		}
	};

	private native void split(ByteBuffer buffer, int len, ByteBuffer ch1, ByteBuffer ch2);
	private final BroadcastReceiver mPermissionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
				if (!intent.getBooleanExtra(
						UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					e("Permission not granted :(");
				} else {
					l("Permission granted");
					UsbDevice dev = (UsbDevice) intent
					.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (dev != null) {
						if (String.format("%04X:%04X", dev.getVendorId(),
								dev.getProductId()).equals("04B4:8613")) {
							l("device present => uploading hex");
							uploadFxHex(dev);//has new thread
						} else if (String.format("%04X:%04X",
								dev.getVendorId(), dev.getProductId()).equals(
								"04B4:1004")) {
							l("device present => hex ok, communicating");
							mainloop(dev);//has new thread
						}
					} else {
						e("device not present!");
					}
				}
			}
		}
	};

	private void uploadFxHex(final UsbDevice dev) {
		//need to do this in a new thread...
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (dev == null)
					return;
				UsbManager usbman = (UsbManager)OsciPrime.sAppContext.getSystemService(Context.USB_SERVICE);
				UsbDeviceConnection conn = usbman.openDevice(dev);
				if (conn == null) {
					e("cannot open device!");
					return;
				}
				int handle = conn.getFileDescriptor();
				if (nativeload(handle, "/sdcard/fifo.hex") < 0)
					e("failed");
				else {
					l("successfully uploaded hex"); 
					//wait before enumerating next time
					try {
						for (int i = 0; i < 100; i++){
							Thread.sleep(50);
						}
					} catch (InterruptedException e) {e(e);return;}//exit on interrupt
					enumerate();// Enumerate again! Crucial here!
				}
			}
		}).start();		
	}

	private void preparefile() {
		try {
			InputStream fis = OsciPrime.sAppContext.getAssets().open("fifo.hex");
			FileOutputStream fos = new FileOutputStream("/sdcard/fifo.hex");

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
	public native int nativeload(int handle, String path);
	static {
		System.loadLibrary("ezusb");
	}


	private static void e(Object s) {
		Log.e("UsbContinuousSource", ">==< " + s.toString() + " >==<");
	}

}
