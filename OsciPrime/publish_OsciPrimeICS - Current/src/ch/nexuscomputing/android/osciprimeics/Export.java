
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;

public class Export {
	
	public static void export(OsciPrimeApplication app, int[] ch1, int[] ch2){
		String datef = "dd.MM.yyyy-HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(datef);
		String fileName = sdf.format(new Date())+".dat";
		File ext = new File(Environment.getExternalStorageDirectory()+File.separator+"osciprime");
		ext.mkdirs();
		final File f = new File(ext, fileName);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(f);
			BufferedWriter wrtr = new BufferedWriter(new OutputStreamWriter(fos));
			for(int i = 0; i < ch1.length; i++){
				wrtr.write(i+"\t"+ch1[i]+"\t"+ch2[i]+"\n");
			}
			wrtr.close();
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
