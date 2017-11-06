
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
public final class Header {
	public static final byte CMD_START = 0;
	public static final byte CMD_STOP = 1;
	public static final byte CMD_SHARED_PARAMS = 2;
	public static final byte CMD_HOST_PARAMS = 3;
	public static final byte CMD_CLIENT_PARAMS = 4;
	public static final byte CMD_SAMPLES = 5;
	public static final byte CMD_RESAMPLE = 6;

	public int command;// command
	public int bodyLength;// len in bytes
}
