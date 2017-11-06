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
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import ch.serverbox.android.osciprime.sources.SourceConfiguration;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;

//TODO Link & update preferences as needed
public class VertexHolder {

	private SourceConfiguration mSourceConfiguration;
	private OsciPreferences mPreferences;
	private static VertexHolder sVertexHolder = null;
	private Handler mServiceHandler = null;

	private FloatBuffer mVertexBufferCh1;
	private FloatBuffer mVertexBufferCh2;

	private boolean mCalibrateNextRun = false;

	private boolean isDrawing = false;

	private GLSurfaceView mSurfaceView = null;
	
	
	public VertexHolder(SourceConfiguration sourceConfiguration){
		l(this);
		mSourceConfiguration = sourceConfiguration;
		ByteBuffer vbbCh1 = ByteBuffer.allocateDirect(OPC.NUM_POINTS_PER_PLOT *2* 4); // 2 dim float has 4 bytes
		ByteBuffer vbbCh2 = ByteBuffer.allocateDirect(OPC.NUM_POINTS_PER_PLOT *2* 4); // float has 4 bytes

		vbbCh1.order(ByteOrder.nativeOrder());
		vbbCh2.order(ByteOrder.nativeOrder());

		mVertexBufferCh1 = vbbCh1.asFloatBuffer();
		mVertexBufferCh2 = vbbCh2.asFloatBuffer();
 
		mVertexBufferCh1.position(0);
		mVertexBufferCh2.position(0);		

	}

	public static VertexHolder getVertexholder(SourceConfiguration config){
		if(config == null && sVertexHolder == null){
			return null;
		}
		if(sVertexHolder == null)
			sVertexHolder = new VertexHolder(config);
		return sVertexHolder;
	}

	public synchronized void put(int[] ch1, int[] ch2){
		//		if(!isDrawing)
		copyVerteces(ch1, ch2, mVertexBufferCh1, mVertexBufferCh2, ch1.length);
		if(mSurfaceView != null)
			mSurfaceView.requestRender();
		//else
			//l("surface view is null...");
	}

	public void linkServiceHandler(Handler h){
		if(sVertexHolder != null)
			sVertexHolder.mServiceHandler  = h;

	}

	public native void copyVerteces(int[] ch1, int[] ch2, Object ch1dst, Object ch2dst, int len);

	static{
		System.loadLibrary("vertexcopy");
	}
 
	public synchronized void draw(GL10 gl, float offset1, float offset2, float timeOff, ShortBuffer indexBuffer){
		//isDrawing = true;

		/*
		 * Calculate the offset
		 */
		if(mCalibrateNextRun == true){
			l("saved offset"+mPreferences.getCalibrationOffsetCh2());
			
			float meanCh1 = 0;
			float meanCh2 = 0;
			mVertexBufferCh1.position(0);
			mVertexBufferCh2.position(0);

			for(int i = 0; i < OPC.NUM_POINTS_PER_PLOT; i++){
				mVertexBufferCh1.get();
				mVertexBufferCh2.get();
				meanCh1 += mVertexBufferCh1.get()/((float)OPC.NUM_POINTS_PER_PLOT);
				meanCh2 += mVertexBufferCh2.get()/((float)OPC.NUM_POINTS_PER_PLOT);
			}			

			if(mServiceHandler != null){
				Bundle b = new Bundle();
				b.putFloat("ch1", meanCh1);
				b.putFloat("ch2", meanCh2);
				Message m = mServiceHandler.obtainMessage(OPC.OS_SET_CALIBRATION_OFFSET);
				m.setData(b);
				m.sendToTarget();
			}

			mVertexBufferCh1.position(0);
			mVertexBufferCh2.position(0);
			mCalibrateNextRun = false;

			l("CH1 Offset "+meanCh1);
			l("CH2 Offset "+meanCh2);
		}

		gl.glPushMatrix();

		gl.glTranslatef(timeOff-OPC.NUM_POINTS_PER_PLOT/4, offset1, 0);


		float correctionCh1 = 1.0f;
			
		if(mPreferences != null && mSourceConfiguration != null){
			if(mPreferences.getGainCh1Index() < mSourceConfiguration.cGainTrippletsCh1().length)
				correctionCh1 = mSourceConfiguration.cGainTrippletsCh1()[mPreferences.getGainCh1Index()].factor;
		} 
 
		if(mPreferences != null && mSourceConfiguration.cSignedNess() == SourceConfiguration.SIGNEDNESS_UNSIGNED && mSourceConfiguration.cRange() == SourceConfiguration.RANGE_BYTE)
			gl.glTranslatef(0, -(mPreferences.getCalibrationOffsetCh1()-128)*255*correctionCh1, 0); // Byte, unsigned

		//gl.glTranslatef(0, -(mPreferences.getCalibrationOffsetCh1()-128)*255*correctionCh1, 0); // Byte, unsigned



		if(mSourceConfiguration.cSignedNess() == SourceConfiguration.SIGNEDNESS_UNSIGNED)
			gl.glTranslatef(0, -32768*correctionCh1, 0);

		if(mSourceConfiguration.cRange() == SourceConfiguration.RANGE_BYTE)
			gl.glScalef(1, 255*correctionCh1, 1);

		//gl.glScalef(1, 1/mCorrectionCh1, 1);		

		mVertexBufferCh1.position(0);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBufferCh1);
		gl.glColor4f(0.2f, 0.2f, 0.5f, 1.0f);// blue
		if(mPreferences.isChannel1Visible()) 
		gl.glDrawElements(GL10.GL_LINE_STRIP, 2*OPC.NUM_POINTS_PER_PLOT/2, GL10.GL_UNSIGNED_SHORT, indexBuffer);
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glTranslatef(timeOff-OPC.NUM_POINTS_PER_PLOT/4, offset2, 0);
		//if(mPreferences != null)
		//	gl.glTranslatef(0, -mPreferences.getCalibrationOffsetCh2(), 0);

		float correctionCh2 = 1.0f;
		if(mPreferences != null && mSourceConfiguration != null){
			if(mPreferences.getGainCh2Index() < mSourceConfiguration.cGainTrippletsCh2().length)
				correctionCh2 = mSourceConfiguration.cGainTrippletsCh2()[mPreferences.getGainCh2Index()].factor;
		}

		if(mPreferences != null && mSourceConfiguration.cSignedNess() == SourceConfiguration.SIGNEDNESS_UNSIGNED && mSourceConfiguration.cRange() == SourceConfiguration.RANGE_BYTE)
			gl.glTranslatef(0, -(mPreferences.getCalibrationOffsetCh2()-128)*255*correctionCh2, 0); // Byte, unsigned

		if(mPreferences != null && mSourceConfiguration.cSignedNess() == SourceConfiguration.SIGNEDNESS_SIGNED && mSourceConfiguration.cRange() == SourceConfiguration.RANGE_SHORT)
			gl.glTranslatef(0, (-mPreferences.getCalibrationOffsetCh2()), 0); // Short, signed

		

		if(mSourceConfiguration.cSignedNess() == SourceConfiguration.SIGNEDNESS_UNSIGNED)
			gl.glTranslatef(0, -32768*correctionCh2, 0);

		if(mSourceConfiguration.cRange() == SourceConfiguration.RANGE_BYTE)
			gl.glScalef(1, 255*correctionCh2, 1);

		mVertexBufferCh2.position(0);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBufferCh2);
		gl.glColor4f(0.2f, 0.5f, 0.2f, 1.0f);// green
		if(mPreferences.isChannel2Visible())
			gl.glDrawElements(GL10.GL_LINE_STRIP, 2*OPC.NUM_POINTS_PER_PLOT/2, GL10.GL_UNSIGNED_SHORT, indexBuffer);
		gl.glPopMatrix();
		//isDrawing = false;
		

	}

	public synchronized void calibrate(){
		if(sVertexHolder != null)
			sVertexHolder.mCalibrateNextRun = true;
	}

	public synchronized void updateConfig(SourceConfiguration config, OsciPreferences preferences){
		if(sVertexHolder != null){
			sVertexHolder.mSourceConfiguration = config;
			sVertexHolder.mPreferences = preferences;
		}
	}
	
	public synchronized void linkSurfaceView(GLSurfaceView surfaceView){
		if(sVertexHolder != null){
			sVertexHolder.mSurfaceView = surfaceView;
		}else{
			l("trying to link surfaceview but it svertexholder is null");
		}
	} 



	private void l(Object s){
		Log.d(getClass().getSimpleName(), ">==< "+s.toString()+" >==<");
	}

}
