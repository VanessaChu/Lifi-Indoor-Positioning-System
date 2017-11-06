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

public class Grid {
  
	private static final int NUM_DIVISIONS = 10;
	
	FloatBuffer[] gridVertexBufferVert = new FloatBuffer[NUM_DIVISIONS-1];
	FloatBuffer[] gridVertexBufferHoriz = new FloatBuffer[NUM_DIVISIONS-1];

	ShortBuffer gridIndexBuffer;
	float[][] gridCoords = new float[NUM_DIVISIONS-1][4];
	short[] gridIndices = new short[2];


	public void drawGrid(GL10 gl){
		for(int i = 0; i < gridVertexBufferVert.length; i++){
			gridVertexBufferVert[i].position(0);
			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, gridVertexBufferVert[i]);
			gl.glColor4f(.8f, .8f, .8f, 1.0f);
			gl.glDrawElements(GL10.GL_LINES, 2, GL10.GL_UNSIGNED_SHORT, gridIndexBuffer); 
		}  
		for(int i = 0; i < gridVertexBufferHoriz.length; i++){
			gridVertexBufferHoriz[i].position(0);
			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, gridVertexBufferHoriz[i]);
			gl.glColor4f(.8f, .8f, .8f, 1.0f);
			gl.glDrawElements(GL10.GL_LINES, 2, GL10.GL_UNSIGNED_SHORT, gridIndexBuffer); 
		}  
	} 
	
	public Grid(int xlength, int ylength) {	
		for(int i = 0; i<gridIndices.length;i++){
			gridIndices[i] = (short)i;
		}
		ByteBuffer gibb = ByteBuffer.allocateDirect(gridIndices.length * 2);
		gibb.order(ByteOrder.nativeOrder());
		gridIndexBuffer = gibb.asShortBuffer();
		gridIndexBuffer.position(0);
		gridIndexBuffer.put(gridIndices);
		gridIndexBuffer.position(0);
		
		ByteBuffer[] gvbbv = new ByteBuffer[NUM_DIVISIONS-1];
		ByteBuffer[] gvbbh = new ByteBuffer[NUM_DIVISIONS-1];

		
		for(int i = 0; i < NUM_DIVISIONS-1; i++){
			gvbbv[i] = ByteBuffer.allocateDirect(gridCoords[i].length * 4); // float has 4 bytes
			gvbbv[i].order(ByteOrder.nativeOrder());
			gvbbh[i] = ByteBuffer.allocateDirect(gridCoords[i].length * 4); // float has 4 bytes
			gvbbh[i].order(ByteOrder.nativeOrder());
			
			gridVertexBufferVert[i] = gvbbv[i].asFloatBuffer();
			gridVertexBufferVert[i].position(0);
			gridVertexBufferVert[i].put(new float[]{0,(i+1)*(2*ylength)/NUM_DIVISIONS-ylength,xlength,(i+1)*(2*ylength)/NUM_DIVISIONS-ylength});
			
			gridVertexBufferHoriz[i] = gvbbh[i].asFloatBuffer();
			gridVertexBufferHoriz[i].position(0);
			gridVertexBufferHoriz[i].put(new float[]{(i+1)*xlength/NUM_DIVISIONS,-ylength,(i+1)*xlength/NUM_DIVISIONS,ylength});
		}
	} 
}



/*
public Grid() {
	for(int i = 0; i<gridIndices.length;i++){
		gridIndices[i] = (short)i;
	}
	ByteBuffer gibb = ByteBuffer.allocateDirect(gridIndices.length * 2);
	gibb.order(ByteOrder.nativeOrder());
	gridIndexBuffer = gibb.asShortBuffer();
	gridIndexBuffer.position(0);
	gridIndexBuffer.put(gridIndices);
	gridIndexBuffer.position(0);
	
	ByteBuffer[] gvbbv = new ByteBuffer[NUM_DIVISIONS-1];
	ByteBuffer[] gvbbh = new ByteBuffer[NUM_DIVISIONS-1];

	
	for(int i = 0; i < NUM_DIVISIONS-1; i++){
		gvbbv[i] = ByteBuffer.allocateDirect(gridCoords[i].length * 4); // float has 4 bytes
		gvbbv[i].order(ByteOrder.nativeOrder());
		gvbbh[i] = ByteBuffer.allocateDirect(gridCoords[i].length * 4); // float has 4 bytes
		gvbbh[i].order(ByteOrder.nativeOrder());
		
		gridVertexBufferVert[i] = gvbbv[i].asFloatBuffer();
		gridVertexBufferVert[i].position(0);
		gridVertexBufferVert[i].put(new float[]{0,(i+1)*256/NUM_DIVISIONS-128,Constant.NUM_PLOT_POINTS,(i+1)*256/NUM_DIVISIONS-128});
		
		gridVertexBufferHoriz[i] = gvbbh[i].asFloatBuffer();
		gridVertexBufferHoriz[i].position(0);
		gridVertexBufferHoriz[i].put(new float[]{(i+1)*Constant.NUM_PLOT_POINTS/NUM_DIVISIONS,-128,(i+1)*Constant.NUM_PLOT_POINTS/NUM_DIVISIONS,128});
	}
}  
*/
