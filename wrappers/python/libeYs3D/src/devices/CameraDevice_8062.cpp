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

#include "devices/CameraDevice_8062.h"
#include "devices/IMUDevice_8062.h"
#include "debug.h"
#include "eSPDI.h"

#define LOG_TAG "CameraDevice8062"

namespace libeYs3D    {
namespace devices    {

CameraDevice8062::CameraDevice8062(DEVSELINFO *devSelInfo, DEVINFORMATION *deviceInfo) :
        CameraDevice(devSelInfo, deviceInfo)    {
    LOG_INFO(LOG_TAG, "Constructing CameraDevice8062...");
}

int CameraDevice8062::initStream(libeYs3D::video::COLOR_RAW_DATA_TYPE colorFormat,
                                 int32_t colorWidth, int32_t colorHeight, int32_t actualFps,
                                 libeYs3D::video::DEPTH_RAW_DATA_TYPE depthFormat,
                                 int32_t depthWidth, int32_t depthHeight, 
                                 DEPTH_TRANSFER_CTRL depthDataTransferCtrl,
                                 CONTROL_MODE ctrlMode,
                                 int rectifyLogIndex,
                                 libeYs3D::video::Producer::Callback colorImageCallback,
                                 libeYs3D::video::Producer::Callback depthImageCallback,
                                 libeYs3D::video::PCProducer::PCCallback pcFrameCallback,
                                 libeYs3D::sensors::SensorDataProducer::AppCallback imuDataCallback)    {
    // TODO: check the validation of parameters here...
    return CameraDevice::initStream(colorFormat, colorWidth, colorHeight,
                                    actualFps,
                                    depthFormat, depthWidth, depthHeight, 
                                    depthDataTransferCtrl,
                                    ctrlMode,
                                    rectifyLogIndex,
                                    colorImageCallback,
                                    depthImageCallback,
                                    pcFrameCallback,
                                    imuDataCallback);
}

bool CameraDevice8062::isInterleaveModeSupported()    {
    return true;
}

int CameraDevice8062::getZDTableIndex()    {
    int index = CameraDevice::getZDTableIndex();
    
    if((mUsbPortType == USB_PORT_TYPE_2_0) && (360 == mDepthHeight)) {
        index = 0;
    }
    
    return index;
}

int CameraDevice8062::initIMUDevice()    {
    int ret = 0;
    IMUDevice::IMUDeviceInfo imuDeviceInfo = { 0x1E4E, 0x0163, IMUDevice::IMU_9_AXIS, 
                                               { 'N', '/', 'A', '\0' },
                                               { 'N', '/', 'A', '\0' },
                                               { 'N', '/', 'A', '\0' },
                                               false, false};

    mIMUDevice = new IMUDevice8062(imuDeviceInfo, this);

    ret = mIMUDevice->initialize();
    if(ETronDI_OK == ret)
        ret = mIMUDevice->postInitialize();
    
    if(ETronDI_OK != ret)    {
        LOG_ERR(LOG_TAG, "Unable to create IMU device (%s)...", mIMUDevice->getName());
        delete mIMUDevice;
        mIMUDevice = nullptr;
    }

    return ret;
}

} // end of namespace devices
} // end of namespace libeYs3D
