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

import ch.serverbox.android.osciprime.sources.SourceConfiguration.GainTripplet;

public interface SourceConfiguration{
	
	public static final int SIGNEDNESS_UNSIGNED = 0;
	public static final int SIGNEDNESS_SIGNED = 1;
	
	public static final int RANGE_BYTE = 2;
	public static final int RANGE_SHORT = 3;
	
	int cBlocksize();
	int cSignedNess();
	int cRange();	
	int cSourceId();
	
	GainTripplet[] cGainTrippletsCh1();
	GainTripplet[] cGainTrippletsCh2();
	TimeDivisionPair[] cTimeDivisionPairs();
	
	public static class GainTripplet{
		public byte cfg;
		public String humanReadable;
		public float factor;
		public float val;
		
		public GainTripplet(byte cfg, String humanReadable, float factor, float val) {
			this.cfg = cfg;
			this.humanReadable = humanReadable;
			this.factor = factor;
			this.val = val;
		}
	}
	
	public static class TimeDivisionPair{
		public int interleave;
		public String humanRepresentation; 
		public float val;
		public TimeDivisionPair(int interleave, String humanRepresentation, float val) {
			this.interleave = interleave;
			this.humanRepresentation = humanRepresentation;
			this.val = val;
		}
		
		public TimeDivisionPair(String humanRepresentation,int interleave, float val) {
			this.interleave = interleave;
			this.humanRepresentation = humanRepresentation;
			this.val = val;
		}
	}

	
	
}
