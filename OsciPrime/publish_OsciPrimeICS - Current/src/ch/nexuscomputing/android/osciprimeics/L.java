
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

import android.util.Log;


public class L {
	private static final boolean SHUT_UP = false;

	public static void d(Object o){
		if(BuildConfig.DEBUG && !SHUT_UP)
			Log.d(">==< OsciPrime ICS >==<", String.valueOf(o));
	}
	public static void d(String s, Object ... args){
		if(BuildConfig.DEBUG && !SHUT_UP)
			Log.d(">==< OsciPrime ICS >==<", String.format(s,args));
	}
	
	public static void e(Object o){
		if(BuildConfig.DEBUG && !SHUT_UP)
			Log.e(">==< OsciPrime ICS >==<", String.valueOf(o));
	}
}
