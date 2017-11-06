/*
 * trigger.c
 * This file is part of OsciOne
 *
 * Copyright (C) 2010 - Manuel Di Cerbo
 *
 * OsciOne is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * OsciOne is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OsciOne; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, 
 * Boston, MA  02110-1301  USA
 */
 
 //Only Bytetrigger, SelectBytePoints is tested

#include <jni.h>
#include <math.h>
#include <stdlib.h>
#include <android/log.h>
#include <time.h>
#include <stdio.h>

#define RISING_EDGE 0

int trigger(signed int* buffer, jint length, signed int trigger, char edge);
static void selectPoints(signed int* buffer, int buflen, signed int* dest, int destlen, int triggerIndex, int nthVal);
static double now_ms(void);



/*
JNI STUFF
*/
jint
Java_ch_serverbox_android_osciprime_sources_TriggerProcessor_nativeTrigger(JNIEnv * env, jclass clazz, jintArray arr, jint len, jint tr, jboolean edge){
//	jint* buffer;
	int res = 0;
	signed int* buffer = (signed int*)(malloc(len*sizeof(signed int)));
	(*env)->GetIntArrayRegion(env, arr, 0, len, buffer);
	//buffer = (*env)->GetIntArrayElements(env, arr, NULL);
//	if (buffer == NULL) {
//		__android_log_write(ANDROID_LOG_ERROR,"TRIGGER","ERROR");
//		return 0; /* exception occurred */
//	} 

	res = trigger(buffer, len, (signed int)tr, (char)edge);
	free(buffer);
//	(*env)->ReleaseIntArrayElements(env, arr, buffer, 0);//release

	return res;
}

jint
Java_ch_serverbox_android_osciprime_sources_TriggerProcessor_nativeTriggerBuffer(JNIEnv * env, jclass clazz, jobject buf, jint len, jint tr, jboolean edge){
	int res = 0;
	signed int* buffer = (signed int*)(*env)->GetDirectBufferAddress(env,buf);
	res = trigger(buffer, len, tr, (char) edge);
	return res;
}




//select points
jintArray
Java_ch_serverbox_android_osciprime_sources_TriggerProcessor_nativeInterleave(JNIEnv * env, jclass clazz, jintArray arr, jint buflen, jint destlen, jint trigger, jint nthVal){
	jint* buffer;

	buffer = (*env)->GetIntArrayElements(env, arr, NULL);
	signed int* dest = (signed int*)(malloc(destlen*sizeof(signed int)));
	jintArray res = (*env)->NewIntArray(env, destlen);

	if(buffer == NULL){
		__android_log_write(ANDROID_LOG_ERROR,"selectShortPoints","buffer address is null");
		return;
	}

	selectPoints(buffer, buflen, dest, destlen, trigger, nthVal);

	(*env)->SetIntArrayRegion(env, res, 0, destlen, dest);
	(*env)->ReleaseIntArrayElements(env, arr, buffer, 0);//release

	//clean up the mess :)
	free(dest);

	return res;
}

//select points
jintArray
Java_ch_serverbox_android_osciprime_sources_TriggerProcessor_nativeInterleaveBuffer(JNIEnv * env, jclass clazz, jobject buf, jint buflen, jint destlen, jint trigger, jint nthVal){
	signed int* buffer = (signed int*)(*env)->GetDirectBufferAddress(env,buf);
	signed int* dest = (signed int*)(malloc(destlen*sizeof(signed int)));
	jintArray res = (*env)->NewIntArray(env, destlen);
	if(buffer == NULL){
		__android_log_write(ANDROID_LOG_ERROR,"selectShortPoints","buffer address is null");
		return;
	}
	selectPoints(buffer, buflen, dest, destlen, trigger, nthVal);
	(*env)->SetIntArrayRegion(env, res, 0, destlen, dest);
	free(dest);
	return res;
}



/*
	IMPLEMENTATION
*/

static int* indices;
static int len = 0;

int trigger(signed int* buffer, jint length, signed int trigger, char edge){	
	
	int ref = length/2;//this is the reference, "best" expected trigger
	signed int lastSample = 0;
	signed int n;
	int i = 0, j = 0;
	unsigned long long bestCost = ((unsigned long long)length)*((unsigned long long)length);
	unsigned long long curCost = 0;
	int triggerIndex = 0;
	static int start = 0;

	if(edge == RISING_EDGE){
		for(i = 0; i < length; i++){
			n = buffer[i];
			if(n >= trigger && lastSample < trigger){
				curCost = abs(i-ref);
				if(curCost < bestCost){
					bestCost = curCost;
					triggerIndex = i;
				}
			}
			lastSample = n;
		}
		start = 1;
	}else{
		for(i = 0; i < length; i++){
			n = buffer[i];
			if(n <= trigger && lastSample > trigger){
				curCost = abs(i-ref);
				if(curCost < bestCost){
					bestCost = curCost;
					triggerIndex = i;
				}
			}
			lastSample = n;
		}
	}	

	return triggerIndex;
}

static void selectPoints(signed int* buffer, int buflen, signed int* dest, int destlen, int triggerIndex, int nthVal){
	int i = 0, j = 0;
	int start = triggerIndex-destlen/2*nthVal;
	int stop = triggerIndex+destlen/2*nthVal;
	if(start < 0){
		start = 0;//no trigger found
	}
	if(stop > buflen-1){
		start = 0;//trigger to far away
	}
	
	j = start;
	for(i = 0; i < destlen; i++){
		dest[i] = buffer[j];
		j += nthVal;
		if(j > buflen-1){
			__android_log_print(ANDROID_LOG_ERROR,"selectShortPoints","access violation: index superceeds: %d",j);
			return;
		}
	}
}

/* return current time in milliseconds */
static double now_ms(void){
    struct timespec res;
    clock_gettime(CLOCK_REALTIME, &res);
    return 1000.0*res.tv_sec + (double)res.tv_nsec/1e6;
}

