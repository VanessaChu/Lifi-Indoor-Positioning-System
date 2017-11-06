/*
 * jniOsciContinuous.c
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

/* SYSTEM */
#include <stdio.h>
#include <utils/Log.h>
#include "jni.h"
#include "JNIHelp.h"

/* PROJECT */
#include "inc/jniOsciContinuous.h"
#include "inc/osciContinuous.h"
#include "inc/base.h"

//static const char *classPathNameContinous = "fhnw/oscione/service/source/OsciUsbContinuousSource";
static const char *classPathNameContinous = "ch/serverbox/android/osciprime/hw/UsbContinuousInterface";
static jobject callbackObject;
static jmethodID callbackContinuous;
static JNIEnv* callbackEnv;

/*		EXPOSED FUNCTIONS		*/
void mStart(JNIEnv * env, jobject obj){
	callbackObject = obj;
	callbackEnv = env;
	jclass cls = (*env)->GetObjectClass(env, obj);
	callbackContinuous = (*env)->GetMethodID(env, cls, "callbackContinous", "(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)V");
	//callbackContinuous = (*env)->GetMethodID(env, cls, "callbackContinous", "([I[I)V");
	if(callbackContinuous == NULL) LOGE("no cb method\n");
	if(cls == NULL) LOGE("no cls\n");
	
	//actually kick on the source
	start();//osciContinuous.c
}
void mStop(JNIEnv * env, jobject obj){
	stop();
}

void mSendCommand(JNIEnv * env, jobject obj,jchar cmd){
	sendI2CCommand((unsigned char)cmd);
}

/*		CALLBACK FOR osciContinous.c		*/
void continuousCallback(unsigned char* ch1, unsigned char* ch2){
	if(callbackContinuous == NULL) fprintf(stdout, "no cb method\n");
	jobject jbufCh1 = (*callbackEnv)->NewDirectByteBuffer(callbackEnv, &ch1[0], BUFFER_SIZE/2);
	jobject jbufCh2 = (*callbackEnv)->NewDirectByteBuffer(callbackEnv, &ch2[0], BUFFER_SIZE/2);
	(*callbackEnv)->CallVoidMethod(callbackEnv, callbackObject, callbackContinuous, jbufCh1, jbufCh2);
	(*callbackEnv)->DeleteLocalRef(callbackEnv, jbufCh1);
	(*callbackEnv)->DeleteLocalRef(callbackEnv, jbufCh2);
}

void continuousCallback_2(jint* ch1, jint* ch2){
	if(callbackContinuous == NULL){
		fprintf(stdout, "no cb method\n");
		return;
	}
	//jintArray resCh1 = (*callbackEnv)->NewIntArray(callbackEnv, BUFFER_SIZE/2);
	//jintArray resCh2 = (*callbackEnv)->NewIntArray(callbackEnv, BUFFER_SIZE/2);
	//(*callbackEnv)->SetIntArrayRegion(callbackEnv, resCh1, 0, BUFFER_SIZE/2, ch1);
	//(*callbackEnv)->SetIntArrayRegion(callbackEnv, resCh2, 0, BUFFER_SIZE/2, ch2);
	jobject resCh1 = (*callbackEnv)->NewDirectByteBuffer(callbackEnv, &ch1[0], 4*BUFFER_SIZE/2);
	jobject resCh2 = (*callbackEnv)->NewDirectByteBuffer(callbackEnv, &ch2[0], 4*BUFFER_SIZE/2);
	(*callbackEnv)->CallVoidMethod(callbackEnv, callbackObject, callbackContinuous, resCh1, resCh2);
	(*callbackEnv)->DeleteLocalRef(callbackEnv, resCh1);
	(*callbackEnv)->DeleteLocalRef(callbackEnv, resCh2);
}


/*		JNI STUFF		*/

static JNINativeMethod methods[] = {
  {"startSampling", "()V", (void*)mStart},
  {"stopSampling", "()V", (void*)mStop},
  {"sendCommand", "(C)V", (void*)mSendCommand}
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className, JNINativeMethod* gMethods, int numMethods){
    jclass clazz;
    clazz = (*env)->FindClass(env, className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env){
	if (!registerNativeMethods(env, classPathNameContinous, methods, sizeof(methods) / sizeof(methods[0])))
		return JNI_FALSE;
	return JNI_TRUE;
}

// ----------------------------------------------------------------------------

/*
 * This is called by the VM when the shared library is first loaded.
 */
 
typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;
    
    LOGI("JNI_OnLoad");

    if ((*vm)->GetEnv(vm, &uenv.venv, JNI_VERSION_1_6) != JNI_OK) {
        LOGE("ERROR: GetEnv failed");
        goto bail;
    }

    LOGI("JNI_OnLoad: Env passed");
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        LOGE("ERROR: registerNatives failed");
        goto bail;
    }
    
    result = JNI_VERSION_1_6;
    LOGI("JNI_OnLoad: Done");
bail:
    return result;
}


