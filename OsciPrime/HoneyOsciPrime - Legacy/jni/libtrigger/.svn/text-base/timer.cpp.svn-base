#include "timer.h"
#include <stdio.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

Timer::Timer(std::string ctx, bool debug){
	this->ctx = ctx;
	this->debug = debug;
	this->startTime = 0;
	this->stopTime = 0;
}

void Timer::start(){
	struct timeval tv;
	gettimeofday(&tv,0);
	this->startTime = tv.tv_sec+1E-6*tv.tv_usec;
}

double Timer::stop(){
	if(this->startTime == 0){
		fprintf(stderr,"[%s] [Error] Call to stop without proir call to start\n", this->ctx);
		return 0;
	}
	struct timeval tv;
	gettimeofday(&tv,0);
	this->stopTime = tv.tv_sec+1E-6*tv.tv_usec;
	if(debug)
		fprintf(stdout,"[%s] Time taken: %d [s]\n", this->ctx, this->stopTime - this->startTime);
	return this->stopTime - this->startTime;
}

double Timer::getTime(){
	if(this->startTime == 0){
		fprintf(stderr,"[%s] [Error] Call to stop without proir call to start\n", this->ctx);
		return 0;
	}
	if(this->stopTime == 0){
		fprintf(stderr,"[%s] [Error] Call to stop without proir call to start\n", this->ctx);
		return 0;
	}
	return this->stopTime - this->startTime;
}
