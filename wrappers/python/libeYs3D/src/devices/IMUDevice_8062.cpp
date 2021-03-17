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


#include "devices/IMUDevice_8062.h"

#define LOG_TAG "IMUDevice8062"

namespace libeYs3D    {
namespace devices    {

IMUDevice8062::IMUDevice8062(IMUDeviceInfo info, CameraDevice *cameraDevice)
    : IMUDevice(info, cameraDevice)    {
}

std::vector<IMUDevice::IMU_DATA_FORMAT> IMUDevice8062::getSupportDataFormat()    {
    return { IMUDevice::IMU_DATA_FORMAT::QUATERNION_DATA };
}

int IMUDevice8062::selectDataFormat(IMU_DATA_FORMAT format)    {
    return ETronDI_NotSupport;
}

void IMUDevice8062::readDataOutputFormat()    {
    mCurrentIMUDataFormat = IMUDevice::IMU_DATA_FORMAT::QUATERNION_DATA;
}

IMUDevice8062::~IMUDevice8062()    {
}

} // end of namespace devices
} // end of namespace libeYs3D
