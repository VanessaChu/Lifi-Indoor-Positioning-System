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
package ch.serverbox.android.osciprime;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Currency;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera.Size;
import android.opengl.GLSurfaceView;
import android.test.IsolatedContext;
import android.util.Log;

public class OsciPrimeRenderer implements GLSurfaceView.Renderer{
	public final static int DIMENSIONS = 2;

	private ShortBuffer mIndexBuffer;
	private ByteBuffer mIbb;

	//private FloatBuffer mVertexBufferCh1;
	//private FloatBuffer mVertexBufferCh2;
	//private ByteBuffer mVbbCh1;
	//private ByteBuffer mVbbCh2;

	private float mWidth = -1;
	private float mHeight = -1;

	private PreviewOverlay mPreviewOverlay;
	//private Object mLock = null;

	private static int mOffsetCh1 = 0;
	private static int mOffsetCh2 = 0;
	private static int mOffsetTime = 0;

	private static boolean sIsDrawing = false;

	Grid mGrid;

	private long mTime0, mTime1;
	private int mCurrentFrame;

	private VertexHolder mVertexHolder = null;

	@Override
	public void onDrawFrame(GL10 gl) {
		sIsDrawing = true;
		mCurrentFrame++;
		if(mCurrentFrame == 0){
			mTime0 = System.currentTimeMillis();
		} else if(mCurrentFrame == 100){
			mTime1 = System.currentTimeMillis();
			float meanTime = (float)(mTime1 - mTime0)/100;

			l("avg frames per second = "+1000/meanTime);
			l("offset1 = "+mOffsetCh1+" offset2= "+mOffsetCh2);

			mCurrentFrame = -1;
		}

		gl.glLoadIdentity();
		gl.glOrthof(0f, OPC.NUM_POINTS_PER_PLOT / 2, -32768, 32768, -1f, 250f);
		//gl.glOrthof(1/4.0f*OPC.NUM_POINTS_PER_PLOT, 3/4.0f*OPC.NUM_POINTS_PER_PLOT, -32768, 32768, -1f, 250f);
		gl.glClearColor(0.9f, 0.9f, 0.9f, 1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		if(mGrid != null)
			mGrid.drawGrid(gl);

		if(mVertexHolder != null)
			mVertexHolder.draw(gl, mOffsetCh1, mOffsetCh2, mOffsetTime, mIndexBuffer);
		else{
			mVertexHolder = VertexHolder.getVertexholder(null);
		}
		sIsDrawing = false;
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mWidth = width;
		mHeight = height;
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		mGrid = new Grid(OPC.NUM_POINTS_PER_PLOT/2, 32768);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glViewport(0, 0, (int) mWidth, (int) mHeight);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glEnable(GL10.GL_LINE_SMOOTH);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_DONT_CARE);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
	}

	public static void setOffsetCh1(int offset){
		mOffsetCh1 = offset;
	}

	public static void setOffsetCh2(int offset){
		mOffsetCh2 = offset;
	}

	public static void setOffsetTime(int offset){
		mOffsetTime = offset;
	}

	public void init(){
		short[] mIndices = new short[OPC.NUM_POINTS_PER_PLOT];
		//Vertexholder
		//mVbbCh1 = ByteBuffer.allocateDirect(OPC.NUM_POINTS_PER_PLOT *DIMENSIONS* 4); // float has 4 bytes
		//mVbbCh2 = ByteBuffer.allocateDirect(OPC.NUM_POINTS_PER_PLOT *DIMENSIONS* 4); // float has 4 bytes

		mIbb = ByteBuffer.allocateDirect(OPC.NUM_POINTS_PER_PLOT * 2); // short has 2 bytes
		// set indices
		for(int i=0;i < mIndices.length; i++){
			mIndices[i] = (short)i;
		}

		// float has 4 bytes, coordinate * 4 bytes
		//mVbbCh1.order(ByteOrder.nativeOrder());
		//mVertexBufferCh1 = mVbbCh1.asFloatBuffer();

		//mVbbCh2.order(ByteOrder.nativeOrder());
		//mVertexBufferCh2 = mVbbCh2.asFloatBuffer();

		// short has 2 bytes, indices * 2 bytes
		mIbb.order(ByteOrder.nativeOrder());

		mIndexBuffer = mIbb.asShortBuffer();
		mIndexBuffer.put(mIndices);

		//mVertexBufferCh1.position(0);
		//mVertexBufferCh2.position(0);
		mIndexBuffer.position(0);

		mPreviewOverlay = new PreviewOverlay();
	}


	public void updateBuffers(int[] ch1, int[] ch2){
		Log.d("THREAD", Thread.currentThread().getName());
		/*
		if(sIsDrawing){
			l("drawing ... skipping ...");
			return;
		}
		if(2*ch1.length < OPC.NUM_POINTS_PER_PLOT){
			e("data length smaller than OPC.NUM_POINTS_PER_PLOT");
			return;
		}
		mVertexBufferCh1.position(0);
		mVertexBufferCh2.position(0);

		//int increment = data.length/OPC.NUM_POINTS_PER_PLOT;
		for(int i = 0; i < OPC.NUM_POINTS_PER_PLOT; i++){
			mVertexBufferCh1.put(i);
			mVertexBufferCh1.put(ch1[i]); 
			mVertexBufferCh2.put(i);
			mVertexBufferCh2.put(ch2[i]);
		}
		 */
		//mPreviewOverlay.updatePreview(data);
	}

	public OsciPrimeRenderer(){
		super();
		init();
		l("constructor called");
	}

	private void e(String msg){
		Log.e("Renderer", ">==< "+msg+" >==<");
	}
	private void l(String msg){
		Log.d("Renderer", ">==< "+msg+" >==<");
	}



	public static Bitmap screenshot(int x, int y, int w, int h, GL10 gl){  
		int b[]=new int[w*h];
		int bt[]=new int[w*h];
		IntBuffer ib=IntBuffer.wrap(b);
		ib.position(0);
		gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);
		for(int i=0; i<h; i++)
		{ 
			for(int j=0; j<w; j++)
			{
				int pix=b[i*w+j];
				int pb=(pix>>16)&0xff;
				int pr=(pix<<16)&0x00ff0000;
				int pix1=(pix&0xff00ff00) | pr | pb;
				bt[(h-i-1)*w+j]=pix1;
			}
		}     

		Bitmap sb=Bitmap.createBitmap(bt, w, h, Bitmap.Config.RGB_565);
		return sb;
	}

	private void l(Object s){
		Log.d(getClass().getSimpleName(), ">==< "+s.toString()+" >==<");
	}




}