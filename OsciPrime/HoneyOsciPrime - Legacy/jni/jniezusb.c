/*
 * jniezusb.c
 * This file is part of Android EZLoader
 *
 * Copyright (C) 2011 - Manuel Di Cerbo
 *
 * Android EZLoader is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Android EZLoader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Android EZLoader; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, 
 * Boston, MA  02110-1301  USA
 */
#include <jni.h>
#include <android/log.h>
#include <fcntl.h>
#include <sys/types.h>
#include <errno.h>
#include "ezusb.h"

jint
Java_ch_serverbox_android_osciprime_sources_UsbContinuousSource_nativeload( JNIEnv* env,
                                                  jobject thiz, jint handle, jstring path )
{
	//ezusb_load_ram (fd=filehandle on opened device, ihex_path=path to hexfile on sd card, fx2=1, config=1);
	static char pathBuf[1024] = {0};
	int status;

	int len = (*env)->GetStringLength(env, path);
	if(len > 1024)
		return -1;
    (*env)->GetStringUTFRegion(env, path, 0, len, pathBuf);

	return ezusb_load_ram (handle, pathBuf, 1, 1);
}

void
Java_ch_serverbox_android_osciprime_sources_UsbContinuousSource_split( JNIEnv* env,
jobject thiz, jobject buffer, jint len, jobject ch1, jobject ch2){
	int i = 0;
	unsigned char* buf = (unsigned char*)(*env)->GetDirectBufferAddress(env,buffer);
	jint* ch1buf = (jint*)(*env)->GetDirectBufferAddress(env,ch1);
	jint* ch2buf = (jint*)(*env)->GetDirectBufferAddress(env,ch2);

	for(i = 0; i < len/2; i++){
		ch1buf[i] = buf[2*i];
		ch2buf[i] = buf[2*i+1];
	}
}


