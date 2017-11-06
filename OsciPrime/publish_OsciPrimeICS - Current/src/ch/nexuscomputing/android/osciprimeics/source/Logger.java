
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

package ch.nexuscomputing.android.osciprimeics.source;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import ch.nexuscomputing.android.osciprimeics.L;

/**
 * Logging enables to observe a signal over a large period of time and export
 * the data collected.
 * @author mdc
 *
 */
public class Logger {
	private static ByteBuffer sBuffer;
	private static IntBuffer sIntBuffer;
	private static final int[] SIZES = new int[]{
		1000000,
		2000000,
		4000000,
		8000000,
		16000000,
		32000000,
		64000000,
		128000000,
		256000000,
		512000000,
		1024000000,
		16000
	};
	
	public static void allocate(){
		if(sBuffer == null){
			for(int i = SIZES.length-1;i >= 0; i--)
				try{
					sBuffer = ByteBuffer.allocateDirect(SIZES[i]);
					sIntBuffer = sBuffer.asIntBuffer();
					L.d("allocated "+SIZES[i]+" bytes");
					break;
				}catch(OutOfMemoryError e){
					L.d("failed to allocate "+SIZES[i]+" bytes");
				}
		}
	}
	
	
	private static final byte[] testArray = new byte[409600];
	
	public static void testWrite(){
		write(ByteBuffer.wrap(testArray));
	}
	public static void write(ByteBuffer buffer){
		if(sBuffer == null){
			return;
		}
		long t = System.currentTimeMillis();
		try{
			sBuffer.put(buffer);
		}catch(Exception e){
			L.e("could not copy buffer");
		}
		L.d("writing took "+(System.currentTimeMillis()-t));
	}
	
	public static void write(IntBuffer buffer){
		L.d("remaining "+buffer.remaining());
		L.d("position "+sIntBuffer.position());
		L.d("remaining 2 "+sIntBuffer.remaining());
		if(sBuffer == null){
			return;
		}
		long t = System.currentTimeMillis();
		int curPos = 0;
		try{
			int remaining = sIntBuffer.remaining();
			int overflow = buffer.remaining() - remaining;
			if(overflow > 0){//circular buffer
				buffer.limit(buffer.position()+remaining);
				sIntBuffer.put(buffer);
				sIntBuffer.position(0);
				buffer.limit(buffer.position()+overflow);
				L.d("after: remaining "+buffer.remaining());
				L.d("after: position "+sIntBuffer.position());
				L.d("after: remaining 2 "+sIntBuffer.remaining());
				sIntBuffer.put(buffer);
			}else{
				sIntBuffer.put(buffer);
			}
			curPos = sIntBuffer.position();
		}catch(Exception e){
			L.e("could not copy buffer");
		}
		L.d("writing took "+(System.currentTimeMillis()-t));
	}
}
