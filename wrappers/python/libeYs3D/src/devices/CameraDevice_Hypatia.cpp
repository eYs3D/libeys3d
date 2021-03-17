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

#include "CameraDevice_Hypatia.h"
#include "debug.h"

#define LOG_TAG "CameraDeviceHypatia"

namespace libeYs3D    {
namespace devices    {

CameraDeviceHypatia::CameraDeviceHypatia(DEVSELINFO *devSelInfo, DEVINFORMATION *deviceInfo) :
        CameraDevice(devSelInfo, deviceInfo)    {
    LOG_INFO(LOG_TAG, "Constructing CameraDeviceHypatia...");
}

int CameraDeviceHypatia::initStream(libeYs3D::video::COLOR_RAW_DATA_TYPE colorFormat,
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

int CameraDeviceHypatia::getZDTableIndex()    {
    if(mDepthHeight == 400) {
        return 0;
    } else if(mDepthHeight == 200)    {
        return 1;
    } else if(mDepthHeight == 104)    {
        return 2;
    }

   return 0;
}

} // end of namespace devices
} // end of namespace libeYs3D
