/*
 * osciContinuous.c
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
#include <time.h>
#include <utils/Log.h>
#include <libusb.h>
#include "jni.h"

/* PROJECT */
#include "inc/base.h"
#include "inc/usberror.h"
#include "inc/osciContinuous.h"
#include "inc/jniOsciContinuous.h"

/* Methods */
void start(){
	if(initLibusb()== -1)
		return;
	_stop = 0;
	int i = 0;
	int ret = 0;
	for(i = 0; i < NUM_BUFFERS; i++){
		continousTransfers[i] = libusb_alloc_transfer(0);
		populateTransfer(continousTransfers[i], EP2_IN, buffers[i], BUFFER_SIZE, &continuousTransferCompleted);
		int ret = libusb_submit_transfer(continousTransfers[i]);
		if(ret  < 0){error(ret);}
	}
	//send continuous command
	sendI2CCommand(0x40);
	while(!_stop){libusb_handle_events(NULL);}
	deInitLibusb();
}
void stop(){
	int i = 0;
	if(continousTransfers != NULL){
		for(i = 0; i < NUM_BUFFERS; i++)
			continousTransfers[i]->flags = continousTransfers[i]->flags | LIBUSB_TRANSFER_FREE_TRANSFER;
	}
	_stop = 1;
}

void continuousTransferCompleted(struct libusb_transfer* transfer){
	int i = 0;
	static int times = 0;
//	static unsigned char ch1 [BUFFER_SIZE/2];
//	static unsigned char ch2 [BUFFER_SIZE/2];

	static jint ch1 [BUFFER_SIZE/2];
	static jint ch2 [BUFFER_SIZE/2];

	double t1;
	if (transfer->status == LIBUSB_TRANSFER_COMPLETED){//the transfer was successful
		//access to the buffer is guaranted even after passing the resubmission until
		//an unkown critiacal point where the kernel will access the buffer...
		t1 = now_ms();
		unsigned char* b = transfer->buffer;
		for(i = 0; i < BUFFER_SIZE/2; i++){
			ch1[i] = b[2*i];
			ch2[i] = b[2*i+1];
		}
		//continuousCallback(ch1,ch2);//jniOsciContinuous.c
		continuousCallback_2(ch1, ch2);
		if(times > 30){
			LOGD("Processing took %f [ms]\n", (now_ms()-t1));
			times = 0;
		}
		times++;
		populateTransfer(transfer, EP2_IN, transfer->buffer, BUFFER_SIZE,  &continuousTransferCompleted);
		int ret = libusb_submit_transfer(transfer);//resubmission
		if(ret  < 0){error(ret);}
	}else{//the transfer was not successful, handle errors here
		status(transfer->status);
		stop();
	}
}
