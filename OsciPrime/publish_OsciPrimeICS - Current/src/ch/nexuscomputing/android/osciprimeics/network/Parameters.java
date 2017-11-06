
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

package ch.nexuscomputing.android.osciprimeics.network;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;


public class Parameters {
	public static class RemoteParameters{
		public int pInterleave = 1;
		public int pPointsOnView = 256;
		public int pFrameSize = 2048;
		public int pAttenuationSettingCh1 = 0;
		public int pAttenuationSettingCh2 = 0;
		public int pEdgeCh1 = 0;
		public int pEdgeCh2 = 0;
		public int pTriggerChannel = OsciPrimeApplication.CH1;
		public int pMode;
		
		public float pTriggerLevelCh1 = 0;
		public float pTriggerLevelCh2 = 0;
		
		
		public static void fromStream(DataInputStream stream, RemoteParameters param) throws IOException{
			param.pInterleave = stream.readInt();
			param.pPointsOnView = stream.readInt();
			param.pFrameSize = stream.readInt();
			param.pAttenuationSettingCh1 = stream.readInt();
			param.pAttenuationSettingCh2 = stream.readInt();
			param.pEdgeCh1 = stream.readInt();
			param.pEdgeCh2 = stream.readInt();
			param.pTriggerChannel = stream.readInt();
			param.pTriggerLevelCh1 = stream.readFloat();
			param.pTriggerLevelCh2 = stream.readFloat();
			param.pMode = stream.readInt();
			
		}
		
		public static void toStream(DataOutputStream stream, OsciPrimeApplication app) throws IOException{
			stream.writeInt(app.pInterleave);
			stream.writeInt(app.pPointsOnView);
			stream.writeInt(app.pFrameSize);
			stream.writeInt(app.pAttenuationSettingCh1);
			stream.writeInt(app.pAttenuationSettingCh2);
			stream.writeInt(app.pEdgeCh1);
			stream.writeInt(app.pEdgeCh2);
			stream.writeInt(app.pTriggerChannel);
			stream.writeFloat(app.pTriggerLevelCh1);
			stream.writeFloat(app.pTriggerLevelCh2);
			stream.writeInt(app.pMode);
		}
	}
	
	public static class HostParameters{
		public int pMinFrameSize = 2048;
		public int pSamplingFrequency = 44100;
		public int pMaxInterleave = 8;
		public int pCalibrationOffsetCh1;
		public int pCalibrationOffsetCh2;
		public int pResolutionInBits = 10;
		
		public float pVoltageDivCh1 = .16f;
		public float pVoltageDivCh2 = .16f;
		
		public final float[] pAttenuationValuesCh1 = new float[OsciPrimeApplication.NUM_ATTENUATION_SETTINGS];
		public final float[] pAttenuationValuesCh2 = new float[OsciPrimeApplication.NUM_ATTENUATION_SETTINGS];

		  
		public static void fromStream(DataInputStream stream, HostParameters param) throws IOException{
			param.pMinFrameSize = stream.readInt();
			param.pSamplingFrequency = stream.readInt();
			param.pMaxInterleave = stream.readInt();
			param.pCalibrationOffsetCh1 = stream.readInt();
			param.pCalibrationOffsetCh2 = stream.readInt();
			param.pResolutionInBits = stream.readInt();
			param.pVoltageDivCh1 = stream.readFloat();
			param.pVoltageDivCh2 = stream.readFloat();
			
			for(int i = 0; i < OsciPrimeApplication.NUM_ATTENUATION_SETTINGS; i++){
				param.pAttenuationValuesCh1[i] = stream.readFloat();
				param.pAttenuationValuesCh2[i] = stream.readFloat();
			}
		}
		
		public static void toStream(DataOutputStream stream, OsciPrimeApplication app) throws IOException{
			stream.writeInt(app.pMinFrameSize);
			stream.writeInt(app.pSamplingFrequency);
			stream.writeInt(app.pMaxInterleave);
			stream.writeInt((int)app.getActiveCalibration().getCh1Offsets()[app.pAttenuationSettingCh1]);
			stream.writeInt((int)app.getActiveCalibration().getCh2Offsets()[app.pAttenuationSettingCh2]);
			stream.writeInt(app.pResolutionInBits);
			stream.writeFloat(app.pVoltageDivCh1);
			stream.writeFloat(app.pVoltageDivCh2);
			
			for(int i = 0; i < OsciPrimeApplication.NUM_ATTENUATION_SETTINGS; i++){
				stream.writeFloat(app.getActiveCalibration().getCh1AttenuationValues()[i]);
				stream.writeFloat(app.getActiveCalibration().getCh2AttenuationValues()[i]);
			}
		}
	}
	
	public static class SharedParameters{
		public int pAttenuationSettingCh1 = 0;//set by remote, used by both
		public int pAttenuationSettingCh2 = 0;
		
		public static void fromStream(DataInputStream stream, SharedParameters param) throws IOException{
			param.pAttenuationSettingCh1 = stream.readInt();
			param.pAttenuationSettingCh2 = stream.readInt();
		}
		
		public static void toStream(DataOutputStream stream, OsciPrimeApplication app) throws IOException{
			stream.writeInt(app.pAttenuationSettingCh1);
			stream.writeInt(app.pAttenuationSettingCh2);
		}
	}
}
