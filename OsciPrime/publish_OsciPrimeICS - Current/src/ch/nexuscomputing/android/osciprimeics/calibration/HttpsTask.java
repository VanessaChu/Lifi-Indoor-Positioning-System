
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpsTask {

	private static final int THREAD_JOIN_TIMEOUT = 1000;

	public interface IResponseCallback {
		void onResponse(Calibration calibration);
		void onFinish();
		void onError(String msg);
		void onStartException(String msg);
		void onKillException(String msg);
	}

	private final HttpClient mHttpClient;
	private final HttpPost mHttpPost = new HttpPost();
	private final Thread mTaskThread;
	private final IResponseCallback mCallback;

	private final String mUrl;

	/**
	 * used for several requests, for instance send outbox
	 * 
	 * @param url
	 * @param params
	 * @param callback
	 */
	public HttpsTask(String url, IResponseCallback callback) {
		mUrl = url;
		mCallback = callback;
		mTaskThread = initThread();
		mTaskThread.setName("HttpTask");
		mHttpClient = new DefaultHttpClient();

	}

	public void start() {
		try {
			mTaskThread.start();
		} catch (IllegalStateException e) {
			mCallback
					.onStartException("Thread has already been started before");
		}

	}

	public void kill() {
		mHttpPost.abort();
		try {
			mTaskThread.join(THREAD_JOIN_TIMEOUT);
		} catch (InterruptedException e) {
			mCallback.onKillException("Could not join thread!");
		}
	}

	private Thread initThread() {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String resp = connect();
					Calibration calibration = CalibrationParser.parse(resp);
					if(calibration != null)
						mCallback.onResponse(calibration);
					else{
						mCallback.onError("could not parse news");
					}
				} catch (URISyntaxException e) {
					if (e.getMessage() != null)
						mCallback.onError("URISyntaxException: "
								+ e.getMessage());
					else
						mCallback
								.onError("unclassified error occured during the http task");
					return;
				} catch (UnsupportedEncodingException e) {
					if (e.getMessage() != null)
						mCallback.onError("UnsupportedEncodingException: "
								+ e.getMessage());
					else
						mCallback
								.onError("unclassified error occured during the http task");
					return;
				} catch (ClientProtocolException e) {
					if (e.getMessage() != null)
						mCallback.onError("ClientProtocolException: "
								+ e.getMessage());
					else
						mCallback
								.onError("unclassified error occured during the http task");
					return;
				} catch (IOException e) {
					if (e.getMessage() != null)
						mCallback.onError("IOException: " + e.getMessage());
					else
						mCallback
								.onError("unclassified error occured during the http task");
					return;
				} catch (IllegalStateException e) {
					if (e.getMessage() != null)
						mCallback.onError("IOException: " + e.getMessage());
					else
						mCallback
								.onError("unclassified error occured during the http task");
					return;
				}
				mCallback.onFinish();
			}
		});
	}

	private String connect()
			throws URISyntaxException, ClientProtocolException, IOException {
		mHttpPost.setURI(new URI(mUrl));
		HttpResponse resp = mHttpClient.execute(mHttpPost);
		BufferedReader brdr = new BufferedReader(new InputStreamReader(resp
				.getEntity().getContent()));
		StringBuffer sb = new StringBuffer();
		String line = "";
		while ((line = brdr.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}

	public String getUrl() {
		return mUrl;
	}

	public IResponseCallback getCallback() {
		return mCallback;
	}


}
