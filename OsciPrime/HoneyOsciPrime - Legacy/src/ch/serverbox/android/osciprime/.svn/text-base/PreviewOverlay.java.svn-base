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

import android.util.Log;

public class PreviewOverlay {
	
	private ShortBuffer mIndexBuffer;
	private FloatBuffer mPreviewCh1;
	private FloatBuffer mPreviewCh2;

	
	public void render(GL10 gl, int x, int y){
		gl.glLoadIdentity();
		gl.glOrthof(-200, OPC.NUM_POINTS_PER_PREVIEW_PLOT+1, -32768, 132767, -1f, 250f);
		
		synchronized(this){
			mPreviewCh1.position(0);
			gl.glVertexPointer(OsciPrimeRenderer.DIMENSIONS, GL10.GL_FLOAT, 0, mPreviewCh1);
			gl.glColor4f(0.2f, 0.2f, 0.5f, 1.0f);// green
			gl.glDrawElements(GL10.GL_LINE_STRIP, 2*OPC.NUM_POINTS_PER_PREVIEW_PLOT/OsciPrimeRenderer.DIMENSIONS, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);

			mPreviewCh2.position(0);
			gl.glVertexPointer(OsciPrimeRenderer.DIMENSIONS, GL10.GL_FLOAT, 0, mPreviewCh2);
			gl.glColor4f(0.2f, 0.5f, 0.2f, 1.0f);// green
			gl.glDrawElements(GL10.GL_LINE_STRIP, 2*OPC.NUM_POINTS_PER_PREVIEW_PLOT/OsciPrimeRenderer.DIMENSIONS, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
		}	
	}
	
	public PreviewOverlay(){
		short[] indices = new short[OPC.NUM_POINTS_PER_PREVIEW_PLOT];
		for(short i = 0; i < indices.length; i++){
			indices[i] = i;
		}
		ByteBuffer indexBase =  ByteBuffer.allocateDirect(OPC.NUM_POINTS_PER_PREVIEW_PLOT * 2);
		indexBase.order(ByteOrder.nativeOrder());
		mIndexBuffer = indexBase.asShortBuffer();
		mIndexBuffer.put(indices);
		
		ByteBuffer vertexBaseCh1 = ByteBuffer.allocateDirect(OPC.NUM_POINTS_PER_PREVIEW_PLOT * OsciPrimeRenderer.DIMENSIONS * 4);		
		ByteBuffer vertexBaseCh2 = ByteBuffer.allocateDirect(OPC.NUM_POINTS_PER_PREVIEW_PLOT * OsciPrimeRenderer.DIMENSIONS * 4);	
		
		vertexBaseCh1.order(ByteOrder.nativeOrder());
		vertexBaseCh2.order(ByteOrder.nativeOrder());
		
		mPreviewCh1 = vertexBaseCh1.asFloatBuffer();
		mPreviewCh2 = vertexBaseCh2.asFloatBuffer();
		
		mPreviewCh1.position(0);
		mPreviewCh2.position(0);
		mIndexBuffer.position(0);
	}
	
	public void updatePreview(int[] data){
		if(2*data.length < OPC.NUM_POINTS_PER_PREVIEW_PLOT){
			e("data length smaller than OPC.NUM_POINTS_PER_PLOT");
			return;
		}
		mPreviewCh1.position(0);
		mPreviewCh2.position(0);
		
		int increment = data.length/OPC.NUM_POINTS_PER_PREVIEW_PLOT;
		for(int i = 0; i < OPC.NUM_POINTS_PER_PREVIEW_PLOT; i+=increment){
			mPreviewCh1.put(i);
			mPreviewCh1.put(data[2*i]);
			mPreviewCh2.put(i);
			mPreviewCh2.put(data[2*i+1]);
		}
	}
	
	
	
	private void e(String msg){
		Log.e("Preview", ">==< "+msg+" >==<");
	}
	private void l(String msg){
		Log.d("Preview", ">==< "+msg+" >==<");
	}
}
