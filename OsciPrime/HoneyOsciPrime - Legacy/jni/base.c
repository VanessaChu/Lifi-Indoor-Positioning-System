/*
 * base.c
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

/* PROJECT */
#include "inc/base.h"
#include "inc/usberror.h"


 void populateTransfer(struct libusb_transfer* bulkTransfer, unsigned char endpoint, unsigned char* buffer, int length, void* callback){
	libusb_fill_bulk_transfer( //fill the transfer with data
		bulkTransfer,
		handle,
		endpoint,
		buffer, 
		length, //how many samples to request
		callback, //this is the callback method
		NULL, // user data
		0
	);
}
 
 void sendI2CCommand(unsigned char command){
 	//check first if we have a handle
 	LOGD("Sending command %02X",command);
	if(handle != NULL){
		static unsigned char cmdBuffer[1];
		cmdBuffer[0] = (unsigned char)command;
		populateTransfer(commandTransfer, EP1_OUT, &cmdBuffer[0], 1, &commandTransferCompleted);
		libusb_submit_transfer(commandTransfer);
	}
}

void commandTransferCompleted(struct libusb_transfer *transfer){
	if (transfer->status == LIBUSB_TRANSFER_COMPLETED){//the transfer was successful
		LOGD("Command ack, sent %02X", transfer->buffer[0]);
	}else{
		//the transfer was not successful, handle errors here
		status(transfer->status);
		//TODO we don't actually have direct access to the "stop's" of 
		//osciSingleShot.c or osciContinuous
		//possible solution: maybe implement and register an error callback 
	}
}

int initLibusb(){
	libusb_init(NULL);
	if((handle = libusb_open_device_with_vid_pid(NULL, VID, PID)) == NULL){
		LOGE("Problem acquireing handle\n");
		return -1;
	}	
	int a = libusb_claim_interface(handle, 0);
	commandTransfer = libusb_alloc_transfer(0);//don't send a command transfer yet
	return a;
}

void deInitLibusb(){
	int ret;
	LOGE("Deinitializing usb");
	//ret = libusb_cancel_transfer(commandTransfer);
	//	if(ret  < 0){error(ret);}
	LOGE("Deinitializing usb: transfer cancelled");
	libusb_free_transfer(commandTransfer);
	LOGE("Deinitializing usb: transfer freed");
	if(handle != NULL){
		libusb_release_interface(handle, 0);
		LOGE("Deinitializing usb: interface released");
	}
	handle = NULL;
	libusb_exit(NULL);
	LOGE("Deinitializing usb: done");
}

/* return current time in milliseconds */
double now_ms(void){
    struct timespec res;
    clock_gettime(CLOCK_REALTIME, &res);
    return 1000.0*res.tv_sec + (double)res.tv_nsec/1e6;
}
