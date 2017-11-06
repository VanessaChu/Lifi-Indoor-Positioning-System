/*
 * osciSingleShot.h
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
#define EP6_IN 0x86
#define BUFFER_SIZE_SINGLESHOT 2048


/* Methods */
void singleShotTransferCompleted(struct libusb_transfer*);//callback method
void start();
void stop();

/* Fields */
static volatile char _stop;//used to break out of the main loop

/* Transfers*/
static struct libusb_transfer* singleTransfer; // transfer for singleshot

/* Buffers */
static unsigned char singleBuffer[BUFFER_SIZE_SINGLESHOT];

 
 
