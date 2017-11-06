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
