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

#include "sensors/IMUDataProducer.h"
#include "devices/IMUDevice.h"

#define LOG_TAG "IMUDataProducer"

namespace libeYs3D    {
namespace sensors    {

IMUDataProducer::IMUDataProducer(libeYs3D::devices::IMUDevice *imuDevice)
    : SensorDataProducer(SensorData::SensorDataType::IMU_DATA),
      mIMUDevice(imuDevice)    {
}

// returns the actual number of bytes read and negative on error. 
int IMUDataProducer::readSensorData(SensorData* sensorData)    {
    int ret = 0;
    
    sensorData->type = SensorData::SensorDataType::IMU_DATA;
    ret = mIMUDevice->readIMUData((IMUData *)sensorData->data);
    if(ret > 0)    {
        sensorData->serialNumber = (uint32_t)((IMUData *)sensorData->data)->_frameCount;
    }
    
    return ret;
}

}  // namespace sensors
}  // namespace libeYs3D
