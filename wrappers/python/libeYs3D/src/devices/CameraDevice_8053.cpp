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

#include "CameraDevice_8053.h"
#include "debug.h"
#include "DMPreview_utility/RegisterSettings.h"
#include "eSPDI.h"

#define LOG_TAG "CameraDevice8053"

namespace libeYs3D    {
namespace devices    {

CameraDevice8053::CameraDevice8053(DEVSELINFO *devSelInfo, DEVINFORMATION *deviceInfo) :
        CameraDevice(devSelInfo, deviceInfo)    {
    LOG_INFO(LOG_TAG, "Constructing CameraDevice8053...");
}

int CameraDevice8053::initStream(libeYs3D::video::COLOR_RAW_DATA_TYPE colorFormat,
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

bool CameraDevice8053::isInterleaveModeSupported()    {
    return true;
}

// TODO:
int CameraDevice8053::adjustRegisters()    {
    //if (EtronDIImageType::DEPTH_8BITS == GetDepthImageType() &&
    //    m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR) &&
    //    m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH)){

        if(1280 == mColorWidth && 720 == mColorHeight &&
           640 == mDepthWidth && 360 == mDepthHeight)    {
            RegisterSettings::ForEx8053Mode9(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                             &mDevSelInfo);
        }
    //}   

    return CameraDevice::adjustRegisters();
}

int CameraDevice8053::getZDTableIndex()    {
    if(mDepthHeight == 720)    {
        return 0;
    } else if((mUsbPortType != USB_PORT_TYPE_3_0) && (mDepthHeight == 360)) {
        return 0;
    }

    return 0;
}

} // end of namespace devices
} // end of namespace libeYs3D
