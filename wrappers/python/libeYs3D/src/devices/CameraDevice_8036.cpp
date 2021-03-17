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

#include "EYS3DSystem.h"
#include "CameraDevice_8036.h"
#include "debug.h"
#include "macros.h"
#include "eSPDI.h"

#define LOG_TAG "CameraDevice8036"

namespace libeYs3D    {
namespace devices    {

CameraDevice8036::CameraDevice8036(DEVSELINFO *devSelInfo, DEVINFORMATION *deviceInfo) :
        CameraDevice(devSelInfo, deviceInfo)    {
    LOG_INFO(LOG_TAG, "Constructing CameraDevice8036...");

    int ret = 0;
    unsigned short value;
    ret = RETRY_ETRON_API(EtronDI_GetFWRegister(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                &(mDevSelInfo),
                                                0xe5, &value,
                                                FG_Address_1Byte | FG_Value_1Byte));

    mSupportingInterleave = (ETronDI_OK == ret) && (1 == value);
}

int CameraDevice8036::initStream(libeYs3D::video::COLOR_RAW_DATA_TYPE colorFormat,
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

int CameraDevice8036::getZDTableIndex()    {
    int index = CameraDevice::getZDTableIndex();
    
    if((mUsbPortType != USB_PORT_TYPE_3_0) && 
       mColorHeight && mDepthHeight && (mColorHeight % mDepthHeight != 0) ) {
        // For mode 34 35 on PIF
        return 2;
    }

    if (mDepthHeight == 720){
        return 0;
    } else if (mDepthHeight >= 480) {
        return 1;
    }

    return index;
}

bool CameraDevice8036::isInterleaveModeSupported()    {
    return mSupportingInterleave;
}

} // endo of namespace devices
} // end of namespace libeYs3D
