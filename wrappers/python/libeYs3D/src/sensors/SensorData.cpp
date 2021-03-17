/*
 * Copyright (C) 2015-2017 ICL/ITRI
 * All rights reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of ICL/ITRI and its suppliers, if any.
 * The intellectual and technical concepts contained
 * herein are proprietary to ICL/ITRI and its suppliers and
 * may be covered by Taiwan and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from ICL/ITRI.
 */

#include "sensors/SensorData.h"
#include "debug.h"
#include "utils.h"

#include <string.h>
#include <stdio.h>
#include <inttypes.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/limits.h>

#define LOG_TAG    "SensorData"

namespace libeYs3D    {
namespace sensors    {

SensorData::SensorData(SensorDataType type)    {
    this->type = type;
    serialNumber = UINT32_MAX;
    memset(this, 0, sizeof(data));
}

int SensorData::toString(char *buffer, int bufferLength) const    {
    return snprintf(buffer, (size_t)bufferLength,
                    "---- Sensor Data (%p) ----\n"
                    "    type:         %d\n"
                    "    serialNumber: %" PRIu32 "\n",
                    this, type, serialNumber);
}

int SensorData::toStringFull(char *buffer, int bufferLength) const    {
    char pool[2048] = { '\0' };
    
    if(this->type == SensorData::SensorDataType::IMU_DATA)    {
        IMUData *imuData = (IMUData *)this->data;    
        snprintf(pool, sizeof(pool),
                 "    time:           %d:%d:%d:%d\n"
                 "    _temprature:    %d\n"
                 "    _accel:         %.2f : %.2f : %.2f\n"
                 "    _gyroScope:     %.2f : %.2f : %.2f\n"
                 "    _compassX:      %.2f : %.2f : %.2f\n"
                 "    _compass_TBC:   %.2f : %.2f : %.2f\n"
                 "    _accuracy_FLAG: %d\n"
                 "    _quaternion:    %.2f : %.2f : %.2f : %.2f",
                 imuData->_hour, imuData->_min, imuData->_sec, imuData->_subSecond,
                 imuData->_temprature,
                 imuData->_accelX, imuData->_accelY, imuData->_accelZ,
                 imuData->_gyroScopeX, imuData->_gyroScopeY, imuData->_gyroScopeZ,
                 imuData->_compassX, imuData->_compassY, imuData->_compassZ,
                 imuData->_compassX_TBC, imuData->_compassY_TBC, imuData->_compassZ_TBC,
                 (int)imuData->_accuracy_FLAG,
                 imuData->_quaternion[0], imuData->_quaternion[1], imuData->_quaternion[2],imuData->_quaternion[3]);
    }
    
    return snprintf(buffer, (size_t)bufferLength,
                    "---- Sensor Data (%p) ----\n"
                    "    type:         %d\n"
                    "    serialNumber: %" PRIu32 "\n"
                    "%s",
                    this, type, serialNumber, pool);
}

void SensorData::clone(const SensorData *sensorData)    {
    type = sensorData->type;
    serialNumber = sensorData->serialNumber;
    toAppCallback = sensorData->toAppCallback;
    
    memcpy((void *)data, (void *)sensorData->data,
           (sizeof(SensorDataRawUnion) + 32) & (~32));
}

SensorDataSet::SensorDataSet(SensorData::SensorDataType type, int32_t cap)
    : sensorDataType(type), actualDataCount(0), sensorDataVec(cap)    {
}

void SensorDataSet::reset()    {
    tsUs = 0L;
    actualDataCount = 0;
    serialNumber = 0;
    sensorDataType = SensorData::SensorDataType::UNKNOWN;
    toColorCallback = false;
    toDepthCallback = false;
}

void SensorDataSet::addClone(const SensorData *sensorData)    {
    if(actualDataCount >= sensorDataVec.capacity())    { // drop it
        return;
    }

#if 0
    sensorDataVec[actualDataCount++].clone(sensorData);
#else
    memcpy((void *)&sensorDataVec[actualDataCount++], sensorData, sizeof(*sensorData));
#endif
}

void SensorDataSet::addClone(const SensorDataSet *sensorDataSet)    {
    for(int i = 0; i < sensorDataSet->actualDataCount; i++)
        this->addClone(&sensorDataSet->sensorDataVec[i]);
}

void SensorDataSet::clone(const SensorDataSet *sensorDataSet)    {
    this->tsUs = sensorDataSet->tsUs;
    this->serialNumber = sensorDataSet->serialNumber;
    this->sensorDataType = sensorDataSet->sensorDataType;
    this->actualDataCount = sensorDataSet->actualDataCount;
    
    if(this->sensorDataVec.capacity() < sensorDataSet->sensorDataVec.capacity())
        this->sensorDataVec.resize(sensorDataSet->sensorDataVec.capacity());
    
#if 0
    for(int i = 0; i < this->actualDataCount; i++)    {
        this->sensorDataVec[i].clone(&sensorDataSet->sensorDataVec[i]);
    }
#else          
    memcpy((void *)this->sensorDataVec.data(),
           (void *)sensorDataSet->sensorDataVec.data(),
           this->actualDataCount * sizeof(SensorData));
#endif
}

int SensorDataSet::toString(char *buffer, int bufferLength) const    {
    return snprintf(buffer, (size_t)bufferLength,
                    "---- Sensor Data Set (%p) ----\n"
                    "    tsUs:         %" PRIu64 "\n"
                    "    serialNumber: %" PRIu32 "\n"
                    "    actualDataCount: %d",
                    this, tsUs, serialNumber, actualDataCount);
}

}  // namespace sensors
}  // namespace libeYs3D
