/*
 * base.h
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
 
 #include <libusb.h>
 
#define EP1_OUT 0x01 //ep1 IN
#define VID 0x04B4
#define PID 0x1004

static struct libusb_device_handle* handle;
static struct libusb_transfer* commandTransfer;// the transfers to be pupulated

void sendI2CCommand(unsigned char command);
void commandTransferCompleted(struct libusb_transfer *transfer);
void populateTransfer(struct libusb_transfer* bulkTransfer, unsigned char endpoint, unsigned char* buffer, int length, void* callback);
int initLibusb();
void deInitLibusb();
double now_ms(void);


