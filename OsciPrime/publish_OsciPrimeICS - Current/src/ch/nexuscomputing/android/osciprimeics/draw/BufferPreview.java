
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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.nexuscomputing.android.osciprimeics.L;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;
import ch.nexuscomputing.android.osciprimeics.source.OsciAudioSource;
import ch.nexuscomputing.android.osciprimeics.source.OsciUsbSource;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.Canvas;
import android.graphics.Paint;

public class BufferPreview {
	
	private static final Paint sPaint = new Paint();
	private static Bitmap sCache;
	private static Canvas sOffscreen;
	private static int[] sCh1Preview;
	private static int[] sCh2Preview;
	
	private static Thread sPreviewRenderThread;
	private static OsciPrimeApplication sApplication;
	private static final boolean ASYNC = false;
	
	private static int sWidth, sHeight;
	
	public static void drawPreview(Canvas canvas, int[] preview, int color, int width, int height, OsciPrimeApplication app){
		sApplication = app;
		if(!ASYNC){
			sPaint.setColor(color);
			sPaint.setStyle(Style.FILL);
			int len = preview.length/2;
			float w = width/(float)len;
			float div = 1 << sApplication.pResolutionInBits;
			sPaint.setAlpha(20);
			canvas.drawRect(0,0,width,height, sPaint);
			sPaint.setAlpha(255);
			if(sApplication.pResolutionInBits == 10){//this is a horrible hack to find out if it is audio
				for(int i = 0; i < len; i++){
					float x1 = i*w;
					float x2 = (i+1)*w;
					float y1 = -preview[2*i]/div*height+height+5-height/2;
					float y2 = -preview[2*i+1]/div*height+height-5-height/2;
					canvas.drawRect(x1, y1, x2, y2, sPaint);
				}
			}else{
				for(int i = 0; i < len; i++){
					float x1 = i*w;
					float x2 = (i+1)*w;
					float y1 = -preview[2*i]/div*height+height+5;
					float y2 = -preview[2*i+1]/div*height+height-5;
					canvas.drawRect(x1, y1, x2, y2, sPaint);
				}
			}

			return;
		}
	}
	
	public static void recycle(){
		if(sPreviewRenderThread == null){
			if(sCache != null){
				sCache.recycle();
				sCache = null;
			}
		}
	}
	
	private static final Runnable sPreviewRenderRunnable = new Runnable(){
		@Override
		public void run() {
			L.d("calculating preview");
			sCache = Bitmap.createBitmap(sWidth, sHeight, Config.RGB_565);
			sOffscreen = new Canvas(sCache);
			sPaint.setColor(sApplication.pColorCh1);
			sPaint.setStyle(Style.FILL);
			sOffscreen.drawColor(sApplication.pColorBackground);
			int len = sCh1Preview.length/2;
			float width = sWidth/(len+1);
			float div = 1 << sApplication.pResolutionInBits;
			for(int i = 0; i < len; i++){
				float x1 = i*width;
				float x2 = (i+1)*width;
				float y1 = sCh1Preview[2*i]/div*sHeight;
				float y2 = sCh1Preview[2*i+1]/div*sHeight;
				sOffscreen.drawRect(x1, y1, x2, y2, sPaint);
			}
			L.d("preview rendered");
			sPreviewRenderThread = null;
		}
	};
	
	
//	private static final Runnable sPreviewRenderRunnable = new Runnable(){
//		@Override
//		public void run() {
//			L.d("calculating preview");
//			ByteBuffer copy = OsciUsbSource.getBufferCopy();
//			if(copy != null){
//				copy.position(0);
//				sCache = Bitmap.createBitmap(sWidth, sHeight, Config.RGB_565);
//				sOffscreen = new Canvas(sCache);
////				int[] lastChannel = OsciAudioSource.getLastCh1();
////				int len = OsciAudioSource.getLastLen();
//				sPaint.setColor(sApplication.pColorCh1);
//				sPaint.setAntiAlias(true);
//				sOffscreen.drawColor(sApplication.pColorBackground);
//				final int LEN = copy.limit()/2-2;
//				final int SKIP = 8;
//				for(int i = 0; i < (LEN-SKIP)/2; i += SKIP){
//					float x1 = i/(float)(LEN/(float)SKIP)*sWidth;
//					float x2 = (i+1)/(float)(LEN/(float)SKIP)*sWidth;
//					float y1 = -(128+copy.get(2*i)&0xff)/256f*sHeight+sHeight/2;
//					float y2 = -(128+copy.get(2*(i+SKIP))&0xff)/256f*sHeight+sHeight/2;
//					sOffscreen.drawLine(x1, y1, x2, y2, sPaint);
//				}
//			}
//			L.d("preview rendered");
//			sPreviewRenderThread = null;
//		}
//	};
}
