
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

package ch.nexuscomputing.android.osciprimeics;

public class Stats {
	
	private static float avgPathing;
	private static long minPathing = 1000;
	private static long maxPathing;
	
	private static float avgProc;
	private static long minProc = 1000;
	private static long maxProc;
	private static long criticalProc;
	private static float skipProc;
	
	private static float avgCopy;
	private static long minCopy = 1000;
	private static long maxCopy;
	
	private static float avgDraw;
	private static long minDraw = 1000;
	private static long maxDraw;
	
	
	private static int dbgCnt = 0;
	private static int framesPerStat = 64;
	private static final int SECS_PER_STAT = 5;//every 2 secs a log
	//every time the service's onSourceSamples is called, we call it a frame
	//30 fps target, does not count for audio
	
	//avgPathing is not copy in onDraw
	public static void stat(OsciPrimeApplication app){
		float secsPerFrame = app.pFrameSize/(float)app.pSamplingFrequency;
		int newFramesPerStat = (int) (SECS_PER_STAT/secsPerFrame);
		if(newFramesPerStat != framesPerStat){
			dbgCnt  = 0;
		}
		framesPerStat = newFramesPerStat; 
		
		if(dbgCnt % framesPerStat == 0){
			skipProc = criticalProc/((float)framesPerStat)*100;
			L.d("========AVG==MIN==MAX====CRIT=======================");
			L.d(String.format("PROC %6.3f %4d %4d  %4d (%4.2f%%)",avgProc, minProc, maxProc, criticalProc, skipProc));
			L.d(String.format("DCPY %6.3f %4d %4d", avgPathing, minPathing, maxPathing));
			L.d(String.format("COPY %6.3f %4d %4d", avgCopy, minCopy, maxCopy));
			L.d(String.format("DRAW %6.3f %4d %4d", avgDraw, minDraw, maxDraw));
			
			dbgCnt = 0;
			
			avgPathing  = 0;
			avgProc = 0;
			avgCopy = 0;
			avgDraw = 0;
			
			minPathing = 1000;
			minProc = 1000;
			minCopy = 1000;
			minDraw = 1000;
			
			maxPathing  = 0;
			maxProc = 0;
			maxCopy = 0;
			maxDraw = 0;
			
			criticalProc = 0;
		}
		dbgCnt++;
	}
	
	public static void proc(long time){
		avgProc += time/(float)framesPerStat;
		if(time > maxProc)
			maxProc = time;
		if(time < minProc)  
			minProc = time;
		
		if(time > 25){
			criticalProc++;
		}
	}
	
	public static void path(long time){
		avgPathing += time/(float)framesPerStat;
		if(time > maxPathing)
			maxPathing = time;
		if(time < minPathing)  
			minPathing = time;
	}
	
	public static void copy(long time){
		avgCopy += time/(float)framesPerStat;
		if(time > maxCopy)
			maxCopy = time;
		if(time < minCopy)  
			minCopy = time;
	}
	
	public static void draw(long time){
		avgDraw += time/(float)framesPerStat;
		if(time > maxDraw)
			maxDraw = time;
		if(time < minDraw)  
			minDraw = time;
	}
	
	
}
