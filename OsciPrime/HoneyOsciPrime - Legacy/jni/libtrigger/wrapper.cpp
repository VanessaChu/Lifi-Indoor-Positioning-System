/*
 * wrapper.cpp
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
#include "trigger.h"
#include <utils/Log.h>
#include "jni.h"
#include "JNIHelp.h"
#include <stdlib.h>
#include <utils/Log.h>
 
#define BUFFERSIZE 396288

jint mByteTrigger(JNIEnv * env, jclass clazz, jobject buf, jint len, jbyte tr){
	int res = 0;
	unsigned char* buffer = (unsigned char*)env->GetDirectBufferAddress(buf);
	//LOGD("Acquired Buffer");
	if(buffer == NULL){
		LOGE("Buffer is NULL");
		return 0;
	}
	res = trigger(buffer, len, (unsigned char)tr);
	return res;
}

jint mShortTrigger(JNIEnv * env, jclass clazz, jobject buf, jint len, jshort tr){
	int res = 0;
	unsigned short* buffer = (unsigned short*)env->GetDirectBufferAddress(buf);
	LOGD("Short buffer length: %d",len);
	len = (int)env->GetDirectBufferCapacity(buf);
	LOGD("Short buffer length: %d",len/2);
	//LOGD("Acquired Buffer");
	if(buffer == NULL){
		LOGE("Buffer is NULL");
		return 0;
	}
	res = trigger(buffer, len/2, (unsigned short)tr);
	return res;
}

/*
jobject allocByteBuffer(JNIEnv * env, jclass clazz, jint size){
	static unsigned char* buffer;
	if(buffer != NULL)
		delete(buffer);
	buffer = (unsigned char*)malloc(sizeof(char)*size);
	jobject jbuf = env->NewDirectByteBuffer(&buffer[0],size);
	return jbuf;
}
*/

static const char *classPathName = "fhnw/oscione/core/Trigger";

static JNINativeMethod methods[] = {
  {"byteTrigger", "(Ljava/nio/ByteBuffer;IB)I", (void*)mByteTrigger},
  {"shortTrigger", "(Ljava/nio/ShortBuffer;IS)I", (void*)mShortTrigger}
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className, JNINativeMethod* gMethods, int numMethods){
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
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
  if (!registerNativeMethods(env, classPathName,
                 methods, sizeof(methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }
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

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_6) != JNI_OK) {
        LOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        LOGE("ERROR: registerNatives failed");
        goto bail;
    }
    
    result = JNI_VERSION_1_6;
    
bail:
    return result;
}
