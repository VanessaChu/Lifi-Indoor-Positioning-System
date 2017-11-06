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

import ch.serverbox.android.osciprime.OPC;
import ch.serverbox.android.osciprime.OsciPreferences;
import ch.serverbox.android.osciprime.audio.AudioAdapter;
import android.os.Handler;

public class AudioSource extends SourceBase{

	private final AudioAdapter mAudioAdapter;
	public AudioSource(Handler sink, OsciPreferences pref) {
		super(sink,pref);
		mAudioAdapter = new AudioAdapter(this);
	}

	@Override
	public int cBlocksize() {
		return 1470;
	}

	@Override
	public int cSignedNess() {
		return SIGNEDNESS_SIGNED;
	}

	@Override
	public int cRange() {
		return RANGE_SHORT;
	}

	@Override 
	public GainTripplet[] cGainTrippletsCh1() {
		return new GainTripplet[]{
				new GainTripplet((byte)0x00, "1[V]", 1.0f, 1.0f)		
		};
	}
	@Override
	public GainTripplet[] cGainTrippletsCh2() {
		return new GainTripplet[]{
				new GainTripplet((byte)0x00, "1[V]", 1.0f, 1.0f)		
		};
	}

	@Override
	public TimeDivisionPair[] cTimeDivisionPairs() {
		return new TimeDivisionPair[]{
				new TimeDivisionPair(1, String.format("%2d [ms]", 100), 1000000)
		};
	}

	@Override
	public void loop() {
		mAudioAdapter.startSampling();
	}

	@Override
	public void stop() {
		mAudioAdapter.stopSampling();
	}

	@Override
	public void quit() {
		mAudioAdapter.quit();
	}

	@Override
	public int cSourceId() {
		return OPC.SOURCE_AUDIO;
	}

	@Override
	public void sendCommand(byte cmd) {
		// TODO Auto-generated method stub
		
	}
	
	
}
