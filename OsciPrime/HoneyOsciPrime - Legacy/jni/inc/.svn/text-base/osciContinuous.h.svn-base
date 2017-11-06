/*
 * osciContinuous.h
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

#define EP2_IN 0x82 //ep 2 is configured IN

#define NUM_BUFFERS 4
#define BUFFER_SIZE 396288 // 64000

// ======= Naming convention: ===========
// A BufferTransfer is an IN transfer that collects data from the FX2
// A CommandTransfer is an OUT transfer that sends a command with a certain payload to the FX2
//=======================================


/* Methods */
void continuousTransferCompleted(struct libusb_transfer*);//callback method
void start();
void stop();

/* Fields */
static volatile char _stop;//used to break out of the main loop

/* Transfers*/
static struct libusb_transfer* continousTransfers[NUM_BUFFERS]; // the transfers to be pupulated

/* Buffers */
static unsigned char buffers[NUM_BUFFERS][BUFFER_SIZE]; // the buffers for each transfer




