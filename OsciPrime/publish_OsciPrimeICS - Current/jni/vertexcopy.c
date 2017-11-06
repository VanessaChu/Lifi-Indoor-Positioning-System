
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

//vertexcopy.c
#include <jni.h>
#include <stdlib.h>

void
Java_ch_serverbox_android_osciprime_VertexHolder_copyVerteces(JNIEnv * env, jclass clazz, jintArray ch1src, jintArray ch2src, jobject ch1dst, jobject ch2dst, jint len){
	int i = 0;
	float* ch1buf = (float*)(*env)->GetDirectBufferAddress(env,ch1dst);
	float* ch2buf = (float*)(*env)->GetDirectBufferAddress(env,ch2dst);
	signed int* ch1carr = (signed int*)(*env)->GetIntArrayElements(env, ch1src, NULL);
	signed int* ch2carr = (signed int*)(*env)->GetIntArrayElements(env, ch2src, NULL);
	
	for(i = 0; i < len; i++){
		ch1buf[2*i] = (float)i;
		ch1buf[2*i+1] = (float)ch1carr[i];
		ch2buf[2*i] = (float)i;
		ch2buf[2*i+1] = (float)ch2carr[i];
	}
	
	(*env)->ReleaseIntArrayElements(env, ch1src, ch1carr, 0);
	(*env)->ReleaseIntArrayElements(env, ch2src, ch2carr, 0);
}
