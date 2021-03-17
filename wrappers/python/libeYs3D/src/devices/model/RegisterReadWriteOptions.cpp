/*
 * Copyright (C) 2015-2019 ICL/ITRI
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
 
#include "devices/model/RegisterReadWriteOptions.h"
#include "debug.h"

#define LOG_TAG "RegisterReadWriteOptions"

namespace libeYs3D    {
namespace devices    {

RegisterReadWriteOptions::RegisterReadWriteOptions()    {
    for (int i = 0 ; i < REGISTER_REQUEST_MAX_COUNT ; ++i)   {
        mRequestValue[i] = EOF;
        mRequestAddress[i] = EOF;
    }
}

bool RegisterReadWriteOptions::operator==(const RegisterReadWriteOptions &rhs) const    {
    if(mRegisterType != rhs.mRegisterType ||
       mSlaveID != rhs.mSlaveID ||
       mAddressSize != rhs.mAddressSize ||
       mValueSize != rhs.mValueSize ||
       mSensorMode != rhs.mSensorMode ||
       mPeriodicRead != rhs.mPeriodicRead ||
       mPeriodTimeMs != rhs.mPeriodTimeMs ||
       mSaveLog != rhs.mSaveLog)    {
        return false;
    }
    
    for(int i = 0; i < REGISTER_REQUEST_MAX_COUNT; ++i)    {
        if(mRequestAddress[i] != rhs.mRequestAddress[i] ||
           mRequestValue[i] != rhs.mRequestValue[i])    {
            return false;
        }
    }

    return true;
}

bool RegisterReadWriteOptions::operator>=(const RegisterReadWriteOptions &rhs) const    {
    for(int i = 0; i < REGISTER_REQUEST_MAX_COUNT; ++i)    {
        if(mRequestAddress[i] != rhs.mRequestAddress[i] ||
           mRequestValue[i] != rhs.mRequestValue[i])    {
            return true;
        }
    }
    
    return false;
}

void RegisterReadWriteOptions::operator<<(const RegisterReadWriteOptions &rhs)    {
    mRegisterType = rhs.mRegisterType;
    mSlaveID = rhs.mSlaveID;
    mAddressSize = rhs.mAddressSize;
    mValueSize = rhs.mValueSize;
    mSensorMode = rhs.mSensorMode;
    mPeriodicRead = rhs.mPeriodicRead;
    mPeriodTimeMs = rhs.mPeriodTimeMs;
    mSaveLog = rhs.mSaveLog;
}

int RegisterReadWriteOptions::toString(char *buffer, int bufferLength) const    {
    int length = 0;
    char const *type = "NONE";
    switch(mRegisterType)    {
        case TYPE::IC2:
            type = "IC2";
            break;
        case TYPE::ASIC:
            type = "ASIC";
            break;
        case TYPE::FW:
            type = "FW";
            break;
        default:
            break;
    }

    length += snprintf(buffer + length, bufferLength - length, "[Type] %s \n", type);
    if(length >= bufferLength)    return bufferLength;

    if (EOF != mSlaveID)    {
        length += snprintf(buffer + length, bufferLength - length,
                           "[ID] 0x%x \n", mSlaveID);
        if(length >= bufferLength)    return bufferLength;
    }

    length += snprintf(buffer + length, bufferLength - length,
                       "[Size] Address_%dByte | Value_%dByte \n",
                       FG_Address_1Byte == mAddressSize ? 1 : 2,
                       FG_Value_1Byte == mValueSize ? 1 : 2);
    if(length >= bufferLength)    return bufferLength;

    char const *sensorMode = "Unknown";
    switch (mSensorMode){
        case SENSOR_A:
            sensorMode = "Sensor 1";
            break;
        case SENSOR_B:
            sensorMode = "Sensor 2";
            break;
        case SENSOR_C:
            sensorMode = "Sensor 3";
            break;
        case SENSOR_D:
            sensorMode = "Sensor 4";
            break;
        case SENSOR_BOTH:
            sensorMode = "Sensor all";
            break;
        default:
            break;
    }

    length += snprintf(buffer + length, bufferLength - length,
                       "[Sensor] %s \n", sensorMode);
    if(length >= bufferLength)    return bufferLength;

    for(int i = 0; i < REGISTER_REQUEST_MAX_COUNT; ++i)    {
        int address = mRequestAddress[i];
        int value = this->mRequestValue[i];
        if (EOF == address || EOF == value) continue;

        length += snprintf(buffer + length, bufferLength - length,
                           "[Address, Value] 0x%04x, 0x%04x \n", address, value);
        if(length >= bufferLength)    return bufferLength;
    }
    
    return length;
}

} // end of namespace devices
} // end of namespace libeYs3D
