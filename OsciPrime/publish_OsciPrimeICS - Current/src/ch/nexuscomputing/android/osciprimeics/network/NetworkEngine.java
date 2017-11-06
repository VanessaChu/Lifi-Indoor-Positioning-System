
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

package ch.nexuscomputing.android.osciprimeics.network;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.nexuscomputing.android.osciprimeics.L;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;

public class NetworkEngine {

	private static final int OSCI_PORT = 21337;
	private final IEngineCallback mEngineCallback;
	private final LinkedBlockingQueue<Data> mDataQueue = new LinkedBlockingQueue<Data>();
	private InputStream mInputStream;
	private OutputStream mOutputStream;
	private final static int BUF_SIZE = 100000;
	private final String mName;

	private static class Data {
		Header head;
		byte[] bytes;
	}

	public interface IEngineCallback {
		public void onReceive(Header head, DataInputStream dis);
		public void onTerminated();
		public void onConnected();
		public void onDisconnected();
		public OsciPrimeApplication getApplication();
	}

	public NetworkEngine(IEngineCallback callback, String name) {
		mEngineCallback = callback;
		mName = name;
	}

	public void send(int command, byte[] bytes) {
		if(mOutputStream == null)
			return;
		Data d = new Data();
		Header head = new Header();
		head.command = command;
		head.bodyLength = bytes.length;
		d.head = head;
		d.bytes = bytes;
		try {
			mDataQueue.put(d);
		} catch (InterruptedException e) {
			L.e(mName + ": interrupted while mDataQueue.put(d) data into queue");
		}
	}

	private final Runnable mReaderLoop = new Runnable() {
		@Override
		public void run() {
			if (mInputStream == null)
				return;

			byte[] buf = new byte[BUF_SIZE];
			DataInputStream dis = new DataInputStream(mInputStream);
			
			while (true) {
				try {
					Header head = new Header();

					head.command = dis.readInt();
					head.bodyLength = dis.readInt();
					

					int bodylen = head.bodyLength;
					
					if(bodylen > buf.length){
						L.d("bodylengt was to large "+bodylen);
						continue;
					}

					int read = 0, remain = bodylen;
					while (remain > 0) {
						read = dis.read(buf, bodylen - remain, remain);
						remain -= read;
					}

					mEngineCallback.onReceive(head, new DataInputStream(
							new ByteArrayInputStream(buf, 0, bodylen)));
				} catch (IOException e) {
					L.e(mName + ": iox in reader thread, self terminate? "
							+ !mTerminated.get());
					mEngineCallback.onDisconnected();
					if (!mTerminated.get()) {
						selfTerminate();
					}
					return;// quit on error
				}
			}// end try
		}// end while true
	};

	private final Runnable mWriterLoop = new Runnable() {
		public void run() {
			while (true) {
				try {
					Data d = mDataQueue.take();
					DataOutputStream dos = new DataOutputStream(mOutputStream);
					dos.writeInt(d.head.command);
					dos.writeInt(d.head.bodyLength);
					dos.write(d.bytes);
				} catch (IOException e) {
					L.e(mName
							+ ": ioex while writing to output stream, stopping writer thread");
					return;
				} catch (InterruptedException e) {
					L.e(mName + ": interrupted writing, stopping writer thread");
					return;// killed
				}
			}
		};
	};

	private AtomicBoolean mStarted = new AtomicBoolean(false);
	private AtomicBoolean mTerminated = new AtomicBoolean(false);

	private ServerSocket mServerSocket;
	private Socket mClientSocket;

	private Thread mWriterThread;
	private Thread mReaderThread;

	public void spawnServer() throws IOException, InterruptedException {
		if (mStarted.get()) {
			L.d("already started");
			return;
		}
		mServerSocket = new ServerSocket(OSCI_PORT);
		
		mEngineCallback.getApplication().pServerIp = "";
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4) {
	                	mEngineCallback.getApplication().pServerIp += " "+inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException e) {
	    	L.e(e);
	    }
			
		for(InetAddress a : InetAddress.getAllByName("localhost")){
			mEngineCallback.getApplication().pServerIp += " "+a.toString();
		}
		L.d("server accepting");
		mClientSocket = mServerSocket.accept();
		mEngineCallback.onConnected();
		mInputStream = mClientSocket.getInputStream();
		mOutputStream = mClientSocket.getOutputStream();
		startThreads();
	}


	public void spawnClient(String ip) throws IOException, InterruptedException {
		L.d("spawning client");
		if (mStarted.get()) {
			L.d("already started");
			return;
		}
		L.d("client connecting");
		mClientSocket = new Socket();
		mClientSocket.bind(null);
		mClientSocket.connect(new InetSocketAddress(ip, OSCI_PORT),1000);
		L.d("client connected");
		mEngineCallback.onConnected();
		mInputStream = mClientSocket.getInputStream();
		mOutputStream = mClientSocket.getOutputStream();
		startThreads();
	}
	
	private void startThreads() {
		synchronized (this) {
			if(mStarted.get())
				return;
			mReaderThread = new Thread(mReaderLoop);
			mWriterThread = new Thread(mWriterLoop);
			mReaderThread.start();
			mWriterThread.start();
			mReaderThread.setName("Network Reader Thread");
			mWriterThread.setName("Network Writer Thread");
			mStarted.set(true);
		}
	}

	public void terminate() throws IOException {
		L.d(mName + ": terminating connection");

		mTerminated.set(true);
		if (mOutputStream != null) {
			mOutputStream.close();
		}
		if (mInputStream != null) {
			mInputStream.close();
		}

		if (mClientSocket != null) {
			mClientSocket.close();
		}
		if (mServerSocket != null) {
			mServerSocket.close();
		}

		if (mWriterThread != null) {
			mWriterThread.interrupt();
		}

		try {
			L.d(mName + ": joining mWriterThread");
			if(mWriterThread != null)
				mWriterThread.join();
			L.d(mName + ": joining mReaderThread");
			if(mReaderThread != null)
				mReaderThread.join();
			L.d(mName + ": joined all");
			mEngineCallback.onTerminated();
			mStarted.set(false);
		} catch (InterruptedException e) {
			L.e(mName + ": interrupted while joining threads");
			e.printStackTrace();
		}
	}

	/**
	 * called by reader thread when the other side hangs up
	 */
	private void selfTerminate() {
		L.d(mName + " self terminate");
		try {
			if (mOutputStream != null) {
				mOutputStream.close();
			}
			if (mInputStream != null) {
				mInputStream.close();
			}

			if (mClientSocket != null) {
				mClientSocket.close();
			}
			if (mServerSocket != null) {
				mServerSocket.close();
			}

			if (mWriterThread != null) {
				mWriterThread.interrupt();
			}
			mWriterThread.join();
			L.d(mName + ": self termination successful!");
			mEngineCallback.onTerminated();
			mStarted.set(false);
		} catch (IOException e) {
			L.e(mName + " ioex, could not self terminate");
		} catch (InterruptedException e) {
			L.e(mName + " interrupted, in self terminate not able to join writer thread");
			e.printStackTrace();
		}
	}

	public boolean isAlive() {
		return false;
	}
}

// while((len = mInputStream.read(buf,off, BUF_SIZE-off)) > 0){
// L.d("read "+len);
// if(len < HEADER_LEN){//2 ints
// //that is bad, continue to read more;
// off = len;
// continue;
// }
// DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
//
// head.command = dis.readInt();
// head.bodyLength = dis.readInt();
//
// int dataLen = HEADER_LEN+head.bodyLength;
//
// if(len < dataLen){
// off = len;
// continue;//try again
// }
//
// //process data
//
// mEngineCallback.onReceive(head, dis);
//
// if(len > dataLen){//more bytes
// off = len - dataLen;
// for(int i = dataLen; i < len; i++){
// buf[i - dataLen] = buf[i];
// }
// continue;
// }
//
// //if len = HEADER_LEN+head.bodyLength
// off = 0;
// }

//
// int total = 0, len = 0;
// byte[] buf = new byte[d.head.bodyLength];
//
// while(total != d.head.bodyLength){
// len = d.dis.read(buf,total,d.head.bodyLength-total);
// mOutputStream.write(buf, total, len);
// total += len;
// }