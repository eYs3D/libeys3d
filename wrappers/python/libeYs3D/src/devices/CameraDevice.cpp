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

#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <malloc.h>
#include <limits.h>

#include "Constants.h"
#include "EYS3DSystem.h"
#include "EYS3DSystem_def.h"
#include "devices/CameraDevice.h"
#include "video/coders.h"
#include "video/factory.h"
#include "utils.h"
#include "debug.h"
#include "macros.h"
#include "CameraDevice_8037.h"
#include "CameraDevice_8053.h"
#include "CameraDevice_8036.h"
#include "CameraDevice_8052.h"
#include "CameraDevice_8062.h"
#include "CameraDevice_Hypatia.h"
#include "base/threads/Thread.h"
#include "DMPreview_utility/RegisterSettings.h"

#define LOG_TAG "CameraDevice"

#define CHECK_DEVICE_INIT()                                                    \
    do {                                                                       \
        if(CD_INITIALIZED & mCameraDeviceState)    {                           \
            LOG_WARN(LOG_TAG, "Device has been initialized, "                  \
                     "release device before it can be init again...");         \
            return LIBEYS3D_DEVICE_INITIALIZED;                                \
        }                                                                      \
    } while(0);

#define CHECK_STREAM_INIT()                                                    \
    do {                                                                       \
        if(CD_STREAM_INITIALIZED & mCameraDeviceState)    {                    \
            LOG_WARN(LOG_TAG, "Device stream has been initialized, "           \
                     "close device stream before it can be enable again...");  \
            return LIBEYS3D_DEVICE_STREAM_INITIALIZED;                         \
        }                                                                      \
    } while(0);

#define CHECK_DEVICE_NOT_INIT()                                                \
    do {                                                                       \
        if(!(CD_INITIALIZED & mCameraDeviceState))    {                        \
            LOG_WARN(LOG_TAG, "Device has NOT been initialized, "              \
                     "initialize device before it can be used...");            \
            return LIBEYS3D_DEVICE_NOT_INITIALIZED;                            \
        }                                                                      \
    } while(0);

#define CHECK_STREAM_NOT_INIT()                                                \
    do {                                                                       \
        if(!((CD_STREAM_INITIALIZED) & mCameraDeviceState))    {               \
            LOG_WARN(LOG_TAG, "Device stream has NOT been initialized, "       \
                     "enable device stream before it can be used...");         \
            return LIBEYS3D_DEVICE_STREAM_NOT_INITIALIZED;                     \
        }                                                                      \
    } while(0);

namespace libeYs3D    {
namespace devices    {

CameraDevice::CameraDevice(DEVSELINFO *devSelInfo, DEVINFORMATION *deviceInfo)
#ifdef DEVICE_MEMORY_ALLOCATOR
    : mPixelByteMemoryAllocator(this), mPixelFloatMemoryAllocator(this),
#else
    :
#endif
      mCameraDeviceInfo(deviceInfo),
      mDepthFilterOptions(), mDepthAccuracyOptions(),
      mDepthInvalidBandPixel(0),
      mCameraDeviceProperties(this), mIRProperty(),
      mRegisterReadWriteOptions(), mRegisterReadWriteController(this),
      mBlockingRead(false), mCameraDeviceState(0), mInterleaveModeEnabled(false),
      mPlyFilterEnabled(false), mIMUDevice(nullptr),
      mColorFrameProducer(nullptr),
      mDepthFrameProducer(nullptr),
      mPCFrameProducer(nullptr)     {
    int i, length;
    unsigned char serialNumber[256] = {0};
    
    mDevSelInfo = *devSelInfo;
    mCameraDeviceInfo.cameraDevice = this;

    if(ETronDI_OK == RETRY_ETRON_API(EtronDI_GetFwVersion(
                                         libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                         &mDevSelInfo,
                                         mCameraDeviceInfo.firmwareVersion,
                                         sizeof(mCameraDeviceInfo.firmwareVersion),
                                         &length))) {
        mCameraDeviceInfo.firmwareVersion[length] = '\0';
    }
                         
    if(ETronDI_OK == RETRY_ETRON_API(EtronDI_GetSerialNumber(
                                         libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                         &mDevSelInfo,
                                         serialNumber, sizeof(serialNumber),
                                         &length)))    {
        for(i = 0 ; i < length / 2 ; ++i)
            mCameraDeviceInfo.serialNumber[i] = serialNumber[i * 2 + 1] * 256 + serialNumber[i * 2];
            
        mCameraDeviceInfo.serialNumber[i] = '\0';
    }
        
    if(ETronDI_OK == RETRY_ETRON_API(EtronDI_GetBusInfo(
                                         libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                         &mDevSelInfo,
                                         mCameraDeviceInfo.busInfo, &length)))    {
        mCameraDeviceInfo.busInfo[length] = '\0';
    }

    get_model_name(mCameraDeviceInfo.devInfo.strDevName,
                   mCameraDeviceInfo.modelName, sizeof(mCameraDeviceInfo.modelName),
                   LOG_TAG);
                   
    mCameraDeviceProperties.setDeviceName(mCameraDeviceInfo.modelName);

    mUsbPortType = get_usb_type(mCameraDeviceInfo.devInfo.strDevName);
    mCameraDeviceInfo.usbPortType = mUsbPortType;
    mCameraDeviceProperties.init();
    
    initStreamInfoList();
    { // report StreamInfoList
        char buffer[1024];
        int length = 0, index = 0;
        auto it = mColorStreamInfo.begin();
        length += snprintf(buffer, sizeof(buffer),
                           "---- Camera Device %s(%p) Stream Info --------\n",
                           mCameraDeviceInfo.modelName, this);
        for(; it != mColorStreamInfo.end(); ++it)    {
            if(length >= sizeof(buffer))    break;
            length += snprintf(buffer + length, sizeof(buffer) - length,
                               "---- Color Stream info : index(%d) ----------------\n"
                               "    nWidth:    : %d\n"
                               "    nHeight    : %d\n"
                               "    bFormatMJPG: %s\n",
                               index++, (*it).nWidth, (*it).nHeight,
                               (*it).bFormatMJPG ? "ture" : "false");
        }

        index = 0;
        it = mDepthStreamInfo.begin();
        for(; it != mDepthStreamInfo.end(); ++it)    {
           if(length >= sizeof(buffer))    break;
            length += snprintf(buffer + length, sizeof(buffer) - length,
                               "++++ Depth Stream info : index(%d) ----------------\n"
                               "    nWidth:    : %d\n"
                               "    nHeight    : %d\n"
                               "    bFormatMJPG: %s\n",
                               index++, (*it).nWidth, (*it).nHeight,
                               (*it).bFormatMJPG ? "ture" : "false");
        }
        
        LOG_INFO(LOG_TAG, "%s", buffer);
    }
    initRegisterReadWriteOptions();
    mRegisterReadWriteController.start();
}

int CameraDevice::getModuleID()    {
    unsigned short value;
    int ret = RETRY_ETRON_API(EtronDI_GetHWRegister(
                              libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                              &(this->mDevSelInfo),
                              0xf306, &value,
                              FG_Address_2Byte | FG_Value_1Byte));

    if(ETronDI_OK != ret)    return -1;

    return (int)value;
}

int CameraDevice::setModuleID(uint8_t nID)    {
    return RETRY_ETRON_API(EtronDI_SetHWRegister(
                               libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                               &(this->mDevSelInfo),
                               0xf306, (unsigned short)nID,
                               FG_Address_2Byte | FG_Value_1Byte));
}

CameraDevice::~CameraDevice()    {
    LOG_INFO(LOG_TAG, "Deleting camera device (%s)... ", mCameraDeviceInfo.modelName);

    this->closeStream();

    mRegisterReadWriteController.pause();
    mRegisterReadWriteController.stop();
}

int32_t CameraDevice::getZDTableDataType()    {
    if(mCameraDeviceInfo.devInfo.nDevType == PUMA) { // 8052, 8053 use the same ZD table
        return ETronDI_DEPTH_DATA_11_BITS;
    } else {
        return ETronDI_DEPTH_DATA_8_BITS;
    }
}

int32_t CameraDevice::getZDTableSize()    {
    if(mCameraDeviceInfo.devInfo.nDevType == PUMA) { // 8052, 8053 is used to same ZD table
        return ETronDI_ZD_TABLE_FILE_SIZE_11_BITS;
    } else {
        return ETronDI_ZD_TABLE_FILE_SIZE_8_BITS;
    }
}

int CameraDevice::initStreamInfoList()    {
    int i, ret = 0;
    
    mColorStreamInfo.resize(MAX_STREAM_INFO_COUNT, {0, 0, false});
    mDepthStreamInfo.resize(MAX_STREAM_INFO_COUNT, {0, 0, false});
    
    ret = RETRY_ETRON_API(EtronDI_GetDeviceResolutionList(
                              EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                              &mDevSelInfo,
                              MAX_STREAM_INFO_COUNT, &mColorStreamInfo[0],
                              MAX_STREAM_INFO_COUNT, &mDepthStreamInfo[0]));

    if (ETronDI_OK != ret)    {
        LOG_ERR(LOG_TAG, "Error EtronDI_GetDeviceResolutionList, ret = %d", ret);
        return ret; 
    }

    auto it = mColorStreamInfo.begin();
    for(; it != mColorStreamInfo.end(); ++it)    {
        if(0 == (*it).nWidth)    break;
    }    
    mColorStreamInfo.erase(it, mColorStreamInfo.end());
    mColorStreamInfo.shrink_to_fit();

    it = mDepthStreamInfo.begin();
    for(; it != mDepthStreamInfo.end(); ++it)    {
        if (0 == (*it).nWidth)    break;
    }    
    mDepthStreamInfo.erase(it, mDepthStreamInfo.end());
    mDepthStreamInfo.shrink_to_fit();

    return ETronDI_OK;
}

// adjust ZDTable index acording to depth resolution selection in initStream(...)
int CameraDevice::getZDTableIndex()    {
    int index = 0, depthWidth;
    
    if(EtronDIImageType::DEPTH_8BITS ==
           libeYs3D::video::depth_raw_type_to_depth_image_type(mDepthFormat))    {
        depthWidth = mDepthWidth >> 1;
    } else    {
        depthWidth = mDepthWidth;
    }

    for(size_t i = 0 ; i < mDepthStreamInfo.size() ; ++i)    {
        if(depthWidth == mDepthStreamInfo[i].nWidth &&
           mDepthHeight == mDepthStreamInfo[i].nHeight)    { // bIsMJPEG == streamInfoList[i].bFormatMJPG ???
               index = i;
               break;
        }
    }

    return index;
}

// source from: CVideoDeviceModel::UpdateZDTable()
int CameraDevice::updateZDTable()    {
    int ret = 0;
    uint16_t zValue;

    mZDTableInfo.nZDTableInfo.nDataType = getZDTableDataType();
    memset(mZDTableInfo.nZDTable, 0, sizeof(mZDTableInfo.nZDTable));
    mZDTableInfo.nZDTableInfo.nIndex = getZDTableIndex();

    ret = RETRY_ETRON_API(EtronDI_GetZDTable(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                              &mDevSelInfo,
                              mZDTableInfo.nZDTable, getZDTableSize(),
                              &(mZDTableInfo.nActualZDTableLength),
                              &(mZDTableInfo.nZDTableInfo)));
    if(ret != ETronDI_OK) {
        LOG_ERR(LOG_TAG, "Failed getting ZD Table, error(%d)", ret);
        return ret;
    }

    mZDTableInfo.nZDTable[0] = 0;
    mZDTableInfo.nZDTable[1] = 0;
    mZDTableInfo.nZDTableMaxNear = USHRT_MAX;
    mZDTableInfo.nZDTableMaxFar = 0; 
    for(int i = 0 ; i < mZDTableInfo.nActualZDTableLength / 2 ; ++i)    {
        zValue = (((uint16_t)mZDTableInfo.nZDTable[i * 2]) << 8) + mZDTableInfo.nZDTable[i * 2 + 1];
        if(zValue)    {
            mZDTableInfo.nZDTableMaxNear = std::min<uint16_t>(mZDTableInfo.nZDTableMaxNear, zValue);
            mZDTableInfo.nZDTableMaxFar = std::max<uint16_t>(mZDTableInfo.nZDTableMaxFar, zValue);
        }
    }

    if(mZDTableInfo.nZDTableMaxNear > mZDTableInfo.nZDTableMaxFar)
        mZDTableInfo.nZDTableMaxNear = mZDTableInfo.nZDTableMaxFar;
    if(mZDTableInfo.nZDTableMaxFar > 1000)
        mZDTableInfo.nZDTableMaxFar = 1000;

    LOG_INFO(LOG_TAG, "Get ZD Table: index(%d), actualLength(%d), mZDTableMaxNear(%d), mZDTableMaxFar(%d)",
             mZDTableInfo.nZDTableInfo.nIndex, mZDTableInfo.nActualZDTableLength,
             mZDTableInfo.nZDTableMaxNear, mZDTableInfo.nZDTableMaxFar);

    return ret;
}

// TODO: consider nZNear, nZFar in preview options
void CameraDevice::updateColorPalette()    {
    int nZNear, nZFar;
    
    memset(mColorPaletteZ14, 0, sizeof(mColorPaletteZ14));
    memset(mGrayPaletteZ14, 0, sizeof(mGrayPaletteZ14));

    if(mDepthAccuracyOptions.isEnabled()){
        nZNear = 0;
        nZFar = MAX_DEPTH_DISTANCE;
    } else    {
        nZNear = (int)mZDTableInfo.nZDTableMaxNear;
        nZFar = (int)mZDTableInfo.nZDTableMaxFar;
    }

    ColorPaletteGenerator::DmColorMode14(mColorPaletteZ14,
                                         (float)nZFar, (float)nZNear,
                                         true); // bool reverseRedtoBlue
    ColorPaletteGenerator::DmGrayMode14(mGrayPaletteZ14,
                                        (float)nZFar, (float)nZNear,
                                        false);
}

int CameraDevice::configurePointCloudInfo()    {
    int ret = ETronDI_OK;
    
    ret = EtronDI_GetRectifyMatLogData(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                       &mDevSelInfo, &mRectifyLogData, mRectifyLogIndex);
    if(ret != ETronDI_OK)    {
        LOG_ERR(LOG_TAG, "Unable to get RectifyMatLogData with index(%d), ret=%d",
                mRectifyLogIndex, ret);
        return ret;
    }
      
    memset(&mPointCloudInfo, 0, sizeof(PointCloudInfo));
    mPointCloudInfo.wDepthType = (unsigned short)mDepthFormat;

    const float ratio_Mat = ((float)mColorHeight) / mRectifyLogData.OutImgHeight;
    const float baseline  = 1.0f / mRectifyLogData.ReProjectMat[14];
    const float diff      = mRectifyLogData.ReProjectMat[15] * ratio_Mat;

    mPointCloudInfo.centerX = -1.0f * mRectifyLogData.ReProjectMat[3] * ratio_Mat;
    mPointCloudInfo.centerY = -1.0f * mRectifyLogData.ReProjectMat[7] * ratio_Mat;
    mPointCloudInfo.focalLength = mRectifyLogData.ReProjectMat[11] * ratio_Mat;
    switch(EtronDIImageType::DepthDataTypeToDepthImageType((unsigned short)mDepthFormat))    {
        case EtronDIImageType::DEPTH_14BITS:
            mPointCloudInfo.disparity_len = 0;
            break;
        case EtronDIImageType::DEPTH_11BITS:
            mPointCloudInfo.disparity_len = 2048;
            for(int i = 0 ; i < mPointCloudInfo.disparity_len ; ++i)    {
                mPointCloudInfo.disparityToW[i] = ( i * ratio_Mat / 8.0f ) / baseline + diff;
            }
            break;
        default:
            mPointCloudInfo.disparity_len = 256;
            for(int i = 0 ; i < mPointCloudInfo.disparity_len ; ++i)    {
                mPointCloudInfo.disparityToW[i] = (i * ratio_Mat) / baseline + diff;
            }
            break;
    }

    return ret;
}

void CameraDevice::closeStream()    {
    int ret = 0;

    LOG_INFO(LOG_TAG, "Closing CameraDevice(%p) stream...", this);

    if(!(mCameraDeviceState & CD_INITIALIZED))    goto end;
    if(!(mCameraDeviceState & CD_STREAM_INITIALIZED))    goto end;

    // Pause stream before we stop producers
    this->pauseStream();

    // release/delete IMU device
    if(isIMUDeviceSupported() && mIMUDevice)    {
        LOG_INFO(LOG_TAG, "IMU is supported, releasing & delete current IMU device...");
        releaseIMUDevice();
    }

    // mPCFrameProducer should be stopped
    if(mColorFrameProducer)    mColorFrameProducer->stop();
    if(mDepthFrameProducer)    mDepthFrameProducer->stop();
    if(mPCFrameProducer)       mPCFrameProducer->stop();
    
    mColorFrameProducer = nullptr;
    mDepthFrameProducer = nullptr;
    mPCFrameProducer = nullptr;
    
    ret = EtronDI_CloseDevice(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(), &mDevSelInfo);
    if(ETronDI_OK != ret)    {
        LOG_ERR(LOG_TAG, "Error closing device(ret=%d), force warm reset...", ret);
        ret = EtronDI_CloseDeviceEx(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(), &mDevSelInfo);
        if(ETronDI_OK != ret)
            LOG_ERR(LOG_TAG, "Error warm resetting device(ret=%d)...", ret);
    }
    
    mCameraDeviceState = CD_INITIALIZED;

end:
    LOG_INFO(LOG_TAG, "CameraDevice(%p) closed...", this);
}

int CameraDevice::initStream(libeYs3D::video::COLOR_RAW_DATA_TYPE colorFormat,
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
    int ret = 0;
    bool configChanged = false;
    libeYs3D::video::Producer::Callback lColorImageCallback = colorImageCallback;
    libeYs3D::video::PCProducer::PCCallback lPCFrameCallback = pcFrameCallback;

    CHECK_DEVICE_NOT_INIT();
    CHECK_STREAM_INIT();
    
    if((mColorFormat != colorFormat) || (mColorWidth != colorWidth) ||
       (mColorHeight != colorHeight))    {
       configChanged = true;
    }

    mColorFormat = colorFormat;
    mColorWidth = colorWidth;
    mColorHeight = colorHeight;
    mActualFps = actualFps;
    
    // for depth only stream configuration
    if((mColorWidth == 0) || (mColorHeight == 0))    {
        lColorImageCallback = nullptr;
        lPCFrameCallback = nullptr;
    }

    mDepthFormat = depthFormat;
    if(EtronDIImageType::DEPTH_8BITS ==
           libeYs3D::video::depth_raw_type_to_depth_image_type(mDepthFormat))    {
        mDepthWidth = depthWidth << 1;
    } else    {
        mDepthWidth = depthWidth;
    }
    mDepthHeight = depthHeight;

    //sgfhsgfhsfghsfg
    
    mDepthDataTransferCtrl = depthDataTransferCtrl;
    mCtrlMode = ctrlMode;
    mRectifyLogIndex = rectifyLogIndex;

    ret =  updateZDTable();
    if(ETronDI_OK != ret) {
        LOG_ERR(LOG_TAG, "Failed updating ZDtable, ret=(%d)", ret);
    }
    
    updateColorPalette();
    
    // this should be done before camera device is opened                                    
    ret = EtronDI_SetDepthDataType(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(), &mDevSelInfo,
                                   (unsigned short)mDepthFormat);
    if(ret == ETronDI_OK)    {
        LOG_INFO(LOG_TAG, "EYs3d_SetDepthDataType() succeeds...");
    } else    {
        LOG_ERR(LOG_TAG, "EYs3d_SetDepthDataType() fails, ret=%d", ret);
        return ret;
    }
    
    // EtronDI_GetRectifyMatLogData before EtronDI_OpenDevice2
    ret = configurePointCloudInfo();
    if(ret == ETronDI_OK)    {
        LOG_INFO(LOG_TAG, "Configuring PointCloudInfo succeeded...");
    } else    {
        LOG_ERR(LOG_TAG, "Configuring PointCloudInfo failed, ret=%d", ret);
        return ret;
    }

    // EtronDI_OpenDevice2 (Color + Depth), always use DEPTH_IMG_NON_TRANSFER as default...
    ret = EtronDI_OpenDevice2(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(), &mDevSelInfo,
                              mColorWidth, mColorHeight,
                              (mColorFormat == libeYs3D::video::COLOR_RAW_DATA_TYPE::COLOR_RAW_DATA_MJPG), // if MJPG
                              depthWidth, mDepthHeight,
                              DEPTH_IMG_NON_TRANSFER, true,  NULL, &mActualFps, mCtrlMode);
    if(ret == ETronDI_OK)    {
        LOG_INFO(LOG_TAG, "EtronDI_OpenDevice2() succeeds... (FPS=%d)", mActualFps);
    } else {
        LOG_ERR(LOG_TAG, "EtronDI_OpenDevice2() fails, ret=%d", ret);
        return ret;
    }
    
    // It is safer to wait for about 2 seconds after opening device
    libeYs3D::base::Thread::sleepMs(1600);

    {    
        initDepthFilterOptions();
        initDepthAccuracyOptions();
    }

#if 1
    { // set module ID for HID/IMU device mapping
        // Writing HW Register 0xf306 (Module ID) of IMU_9_AXIS device won't be
        // synced to IMU device until camera OpenDevice is issued
        uint16_t id = libeYs3D::EYS3DSystem::getEYS3DSystem()->generateModelID();
        if(ETronDI_OK == setModuleID(id))
            LOG_INFO(LOG_TAG, "Setting camera device module ID: %" PRIu16 "", id);
        else
            LOG_ERR(LOG_TAG, "Error setting camera device module ID...");
    }
#endif

    // default set the device in blocking read mode
    ret = enableBlockingRead(true);
    if(ret != ETronDI_OK)    {
        return ret;
    }

    { // reporting camera device depth filter options
        char buffer[2048] = {0};
        int length = 0;
        length += snprintf(buffer, sizeof(buffer),
                           "---- Camera Device %s(%p) Depth Filter Options --------\n",
                           mCameraDeviceInfo.modelName, this);
        //mDepthFilterOptions.toString(buffer + length, sizeof(buffer) - length);
        LOG_INFO(LOG_TAG, "%s", buffer);
    }

    // preallocate memories
    if(configChanged)    {
#ifdef DEVICE_MEMORY_ALLOCATOR
        releasePreallocatedMemory();
        if(preallocateMemory() != 0)    return -1;
#endif
    }

    // mColorFrameProducer & mDepthFrameProducer must be ready before creating mPCFrameProducer
    if(lColorImageCallback)   mCameraDeviceState |= CD_COLOR_STREAM_CB;
    if(depthImageCallback)    mCameraDeviceState |= CD_DEPTH_STREAM_CB;
    if(lPCFrameCallback)      mCameraDeviceState |= CD_PC_STREAM_CB;
    if(imuDataCallback)       mCameraDeviceState |= CD_IMU_STREAM_CB;

    if(lColorImageCallback || lPCFrameCallback)    {
        mColorFrameProducer = video::createColorFrameProducer(this);
        mColorFrameProducer->attachCallback(lColorImageCallback);
    }

    if(depthImageCallback || lPCFrameCallback)    {
        mDepthFrameProducer = video::createDepthFrameProducer(this);
        mDepthFrameProducer->attachCallback(depthImageCallback);
    }

    if(lPCFrameCallback)    {
        mPCFrameProducer = video::createPCFrameProducer(this);
        mPCFrameProducer->attachCallback(lPCFrameCallback);
    }
    
    // Load register settings after stream init and before frame producers start
    adjustRegisters();

    // mPCFrameProducer should start first...
    if(mPCFrameProducer)   mPCFrameProducer->start();
    if(mColorFrameProducer)    mColorFrameProducer->start();
    if(mDepthFrameProducer)    mDepthFrameProducer->start();

    // create and initialize IMU device
    if(isIMUDeviceSupported())    {
        LOG_INFO(LOG_TAG, "IMU is supported, creating and initializing IMU device..."); 
        initIMUDevice();
        
        if(mIMUDevice)    {
            { // print IMU device info
                char buffer[1024];
                IMUDevice::IMUDeviceInfo imuDeviceInfo = getIMUDeviceInfo();
                imuDeviceInfo.toString(buffer, sizeof(buffer));
                LOG_INFO(LOG_TAG, "IMU device (%s) created...\n%s", mIMUDevice->getName(), buffer);
            }

            mIMUDevice->initStream(imuDataCallback,
                                   (mColorFrameProducer ? mColorFrameProducer->mIMUDataCallback : nullptr),
                                   (mDepthFrameProducer ? mDepthFrameProducer->mIMUDataCallback : nullptr));
        }
    } else    {
        LOG_INFO(LOG_TAG, "IMU is not supported..."); 
    }

    mCameraDeviceState |= CD_STREAM_INITIALIZED;
    
    base::Thread::sleepMs(48);

    return ret;
}

void CameraDevice::enableStream()    {
    enablePCStream();
    enableColorStream();
    enableDepthStream();

    enableIMUStream();
}

void CameraDevice::enableColorStream()    {
    if(!(mCameraDeviceState & CD_COLOR_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "Color frame callback is not provided...");
        return;
    }
    
    if(mCameraDeviceState & CD_COLOR_STREAM_ACTIVATED)    return;
    
    mCameraDeviceState |= CD_COLOR_STREAM_ACTIVATED;
    
    mColorFrameProducer->enableCallback();
}

void CameraDevice::enableDepthStream()    {
    if(!(mCameraDeviceState & CD_DEPTH_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "Depth frame callback has not been provided...");
        return;
    }
    
    if(mCameraDeviceState & CD_DEPTH_STREAM_ACTIVATED)    return;
    
    mCameraDeviceState |= CD_DEPTH_STREAM_ACTIVATED;
    
    mDepthFrameProducer->enableCallback();
}

void CameraDevice::enablePCStream()    {
    if(!(mCameraDeviceState & CD_PC_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "Point cloud frame callback has not been provided...");
        return;
    }
    
    if(mCameraDeviceState & CD_PC_STREAM_ACTIVATED)    return;
    
    mCameraDeviceState |= CD_PC_STREAM_ACTIVATED;
    
    mPCFrameProducer->enableCallback();
}

void CameraDevice::enableIMUStream()    {
    if(!isIMUDevicePresent())    {
        LOG_INFO(LOG_TAG, "IMU device is not present...");
        return;
    }
    
    if(!(mCameraDeviceState & CD_IMU_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "IMU stream callback has not been provided...");
        return;
    }
    
    if(mCameraDeviceState & CD_IMU_STREAM_ACTIVATED)    return;
    
    mCameraDeviceState |= CD_IMU_STREAM_ACTIVATED;
    
    mIMUDevice->enableAppStream();
}

void CameraDevice::pauseStream()    {
    pauseIMUStream();
    
    pauseColorStream();
    pauseDepthStream();
    pausePCStream();
}

void CameraDevice::pauseColorStream()    {
    if(!(mCameraDeviceState & CD_COLOR_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "Color frame callback is not provided...");
        return;
    }
    
    if(!(mCameraDeviceState & CD_COLOR_STREAM_ACTIVATED))    return;
    
    mCameraDeviceState &= ~CD_COLOR_STREAM_ACTIVATED;

    mColorFrameProducer->pauseCallback();
}

void CameraDevice::pauseDepthStream()    {
    if(!(mCameraDeviceState & CD_DEPTH_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "Depth frame callback is not provided...");
        return;
    }
    
    if(!(mCameraDeviceState & CD_DEPTH_STREAM_ACTIVATED))    return;
    
    mCameraDeviceState &= ~CD_DEPTH_STREAM_ACTIVATED;

    mDepthFrameProducer->pauseCallback();
}

void CameraDevice::CameraDevice::pausePCStream()    {
    if(!(mCameraDeviceState & CD_PC_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "Point cloud frame callback is not provided...");
        return;
    }
    
    if(!(mCameraDeviceState & CD_PC_STREAM_ACTIVATED))    return;
    
    mCameraDeviceState &= ~CD_PC_STREAM_ACTIVATED;

    mPCFrameProducer->pauseCallback();
}

void CameraDevice::CameraDevice::pauseIMUStream()    {
    if(!isIMUDevicePresent())    {
        LOG_INFO(LOG_TAG, "IMU device is not present...");
        return;
    }
    
    if(!(mCameraDeviceState & CD_IMU_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "IMU stream callback has not been provided...");
        return;
    }
    
    if(!(mCameraDeviceState & CD_IMU_STREAM_ACTIVATED))    return;
    
    mCameraDeviceState &= ~CD_IMU_STREAM_ACTIVATED;
    
    mIMUDevice->pauseAppStream();
}
    
int CameraDevice::enableBlockingRead(bool blocking)    {
    int ret = 0;

    CHECK_DEVICE_NOT_INIT();

    if(mBlockingRead == blocking)    return 0;

    ret = EtronDI_SetupBlock(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                             &mDevSelInfo, blocking);
    if(ret == ETronDI_OK) {
        LOG_INFO(LOG_TAG, "Successfully setting device in %s read mode...",
                 blocking ? "blocking" : "non-blocking");
        mBlockingRead = blocking;
    } else {
        LOG_INFO(LOG_TAG, "Failed setting device in %s read mode...",
                 blocking ? "blocking" : "non-blocking");
    }

    return ret;
}

int CameraDevice::initDepthFilterOptions()    {
    LOG_INFO(LOG_TAG, "initDepthFilterOptions Class...");
    mDepthFilterOptions.resetDefault();
    mDepthFilterOptions.enableSubSample(true);
    mDepthFilterOptions.enableEdgePreServingFilter(true);
    mDepthFilterOptions.enableHoleFill(true);
    mDepthFilterOptions.setHorizontal(true);
    mDepthFilterOptions.enableTemporalFilter(true);
    mDepthFilterOptions.enableFlyingDepthCancellation(false);
    
    switch(libeYs3D::video::depth_raw_type_to_depth_image_type(mDepthFormat))    {
        case EtronDIImageType::DEPTH_8BITS:
            mDepthFilterOptions.setType(1);
            mDepthFilterOptions.setBytesPerPixel(1);
            break;
        case EtronDIImageType::DEPTH_11BITS:
            mDepthFilterOptions.setType(2);
            mDepthFilterOptions.setBytesPerPixel(2);
            break;
        case EtronDIImageType::DEPTH_14BITS:
            mDepthFilterOptions.setType(3);
            mDepthFilterOptions.setBytesPerPixel(2);
            break;
        default:
            // According to DMPreview/model/CImageDataModel.cpp::DepthFilter(BYTE *pData),
            // it seems EtronDIImageType::DEPTH_8BITS_0x80 does NOT support depth filtering
            mDepthFilterOptions.enable(false);
    }
    
    mDepthFilterOptions.enable(true);
    
    return 0;
}

int CameraDevice::initDepthAccuracyOptions()    {
    mDepthAccuracyOptions.mEnabled = false;
    mDepthAccuracyOptions.mRegionRatio = 0.0f;
    mDepthAccuracyOptions.mGroundTruthDistanceMM = 0.0f;

    return 0;
}

int CameraDevice::initRegisterReadWriteOptions()    {
    mRegisterReadWriteController.start();
    
    return 0;
}

void CameraDevice::adjustDepthInvalidBandPixel()    {
    if(mDepthAccuracyOptions.mGroundTruthDistanceMM == 0.0f)    return;
    
    float ratio_Mat = ((float)mDepthHeight) / mRectifyLogData.OutImgHeight;
    float focalLength = mRectifyLogData.ReProjectMat[11] * ratio_Mat;
    float baseline = 1.0f / mRectifyLogData.ReProjectMat[14];

    mDepthInvalidBandPixel = baseline * focalLength / mDepthAccuracyOptions.mGroundTruthDistanceMM;
}

bool CameraDevice::isHWPPEnabled()    {
    unsigned short nHWPP;
    int ret = 0;
    
    if(!isHWPPSupported())    return false;
    
    ret = RETRY_ETRON_API(EtronDI_GetHWRegister(
                              EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                              &mDevSelInfo,
                              0xf424,
                              &nHWPP,
                              FG_Address_2Byte | FG_Value_1Byte));

    if (ret != ETronDI_OK) return false;

    return (nHWPP & 0x30) == 0;
}

int CameraDevice::enableHWPP(bool enable)    {
    int ret = 0;
    unsigned short value = 0;
    
    if(!isHWPPSupported())    return ETronDI_OK;

    ret = RETRY_ETRON_API(EtronDI_GetHWRegister(
                              EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                              &mDevSelInfo,
                              0xf424, &value,
                              FG_Address_2Byte | FG_Value_1Byte));
    if(ETronDI_OK != ret)    return ret;
    
    value &= 0x0F;
    value |= (enable ? 0x40 : 0x70);
    ret = RETRY_ETRON_API(EtronDI_SetHWRegister(
                              EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                              &mDevSelInfo,
                              0xf424, value,
                              FG_Address_2Byte | FG_Value_1Byte));

#if 0
    if(0 == i && m_deviceSelInfo.size() > 1){
        EtronDI_SetSensorRegister(pEtronDI, m_deviceSelInfo[i],
                                  0xC2, 0x9024, value,
                                  FG_Address_2Byte | FG_Value_1Byte,
                                  SENSOR_A);
    }
#endif

    return ret;
}

// TODO:
int CameraDevice::adjustRegisters()    {
    //if(GetState() != STREAMING) return ETronDI_OK;

    int ret =  RegisterSettings::DM_Quality_Register_Setting(
                                     EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                     &mDevSelInfo,
                                     mCameraDeviceInfo.devInfo.wPID);
    if(0 != ret)
        LOG_WARN(LOG_TAG, "Error adjusting device registers, ret=%d", ret);

    //AdjustFocalLength();

    if(isHWPPSupported() && (!isHWPPEnabled()))   {
       enableHWPP(true);
    }    

    //m_pVideoDeviceController->GetControlView()->UpdateUI();

    return ret; 
}

void CameraDevice::dumpFrameInfo(int frameCount)    {
    if(mCameraDeviceState & CD_PC_STREAM_ACTIVATED)    {
        mPCFrameProducer->dumpFrameInfo(frameCount);
    }
    
    if(mCameraDeviceState & CD_COLOR_STREAM_ACTIVATED)    {
        mColorFrameProducer->dumpFrameInfo(frameCount);
    }
    
    if(mCameraDeviceState & CD_DEPTH_STREAM_ACTIVATED)    {
        mDepthFrameProducer->dumpFrameInfo(frameCount);
    }    
}

void CameraDevice::doSnapshot()    {
    if(mCameraDeviceState & CD_PC_STREAM_ACTIVATED)    {
        mPCFrameProducer->doSnapshot();
        return;
    }
    
    if(mCameraDeviceState & CD_COLOR_STREAM_ACTIVATED)    {
        mColorFrameProducer->doSnapshot();
    }
    
    if(mCameraDeviceState & CD_DEPTH_STREAM_ACTIVATED)    {
        mDepthFrameProducer->doSnapshot();
    }
}

void CameraDevice::enablePlyFilter(bool enable)    {
    if(isPlyFilterSupported())    mPlyFilterEnabled = enable;
}

void CameraDevice::dumpIMUData(int recordCount)    {
    if(isIMUDevicePresent())    { // isIMUDeviceSupported() && (mIMUDevice != nullptr)
        mIMUDevice->dumpIMUData(recordCount);
    }
}

IMUDevice::IMUDeviceInfo CameraDevice::getIMUDeviceInfo()    {
    IMUDevice::IMUDeviceInfo imuDeviceInfo = { 0, 0, IMUDevice::IMU_UNKNOWN, 
                                               { 'N', '/', 'A', '\0' },
                                               { 'N', '/', 'A', '\0' },
                                               { 'N', '/', 'A', '\0' },
                                               false, false};
    
    if(isIMUDevicePresent())    {
        return mIMUDevice->getIMUDeviceInfo();
    } else    {
        LOG_WARN(LOG_TAG, "IMU device is not present...");
        return imuDeviceInfo;
    }
}

int CameraDevice::initIMUDevice()    {
    return 0;
}

void CameraDevice::releaseIMUDevice()    {
    if(isIMUDeviceSupported() && mIMUDevice)    {
        mIMUDevice->closeStream();

        delete mIMUDevice;
        mIMUDevice = nullptr;
    }
}

int CameraDevice::postInitialize()    {
    CHECK_DEVICE_INIT();

    initIRProperty();
    initRegisterReadWriteOptions();
    
    mCameraDeviceState |= CD_INITIALIZED;
    
    return 0;
}

int CameraDevice::readColorFrame(uint8_t *buffer, uint64_t bufferLength,
                                 uint64_t *actualSize, uint32_t *serial)    {
    int ret = 0;
    
    CHECK_DEVICE_NOT_INIT();
    CHECK_STREAM_NOT_INIT();
    
    do    {
        //LOG_INFO(LOG_TAG, "Before readColorFrame....");
        ret = EtronDI_GetColorImage(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                    &mDevSelInfo, buffer, actualSize, (int *)serial,
                                    0 /* nDepthDataType reserved, not used */);
        //LOG_INFO(LOG_TAG, "After readColorFrame, sn=%" PRIu32 "....", *serial);
    } while(ret == ETronDI_DEVICE_TIMEOUT);
    
    if(ret != ETronDI_OK)
        LOG_ERR(LOG_TAG, "Error reading color image from device, ret=%d", ret);
    
    return ret;
}
    
int CameraDevice::readDepthFrame(uint8_t *buffer, uint64_t bufferLength,
                                 uint64_t *actualSize, uint32_t *serial)    {
    int ret = 0;
    
    CHECK_DEVICE_NOT_INIT();
    CHECK_STREAM_NOT_INIT();
    
    do    {
        //LOG_INFO(LOG_TAG, "Before readDepthFrame ------....");
        ret = EtronDI_GetDepthImage(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                    &mDevSelInfo, buffer, actualSize, (int *)serial,
                                    mDepthFormat);
        //LOG_INFO(LOG_TAG, "After readDepthFrame ---- , sn=%" PRIu32 "....", *serial);
    } while(ret == ETronDI_DEVICE_TIMEOUT);
    
    if(ret != ETronDI_OK)
        LOG_ERR(LOG_TAG, "Error reading depth image from device, ret=%d", ret);
    
    return ret;
}

int CameraDevice::readPCFrame(const uint8_t *rgbBuffer, const uint8_t *depthBuffer,
                              uint8_t *rgbDataBuffer, float *xyzDataBuffer)    {
    int ret = 0;
    
    // TODO: calculate mZDTableMaxNear just once...
    ret = EtronDI_GetPointCloud(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                &mDevSelInfo, 
                                (uint8_t *)rgbBuffer, mColorWidth, mColorHeight,
                                (uint8_t *)depthBuffer, mDepthWidth, mDepthHeight,
                                &mPointCloudInfo,
                                rgbDataBuffer,
                                xyzDataBuffer,
                                (((float)mZDTableInfo.nZDTableMaxNear) * 1.0f) > 0 ? (float)mZDTableInfo.nZDTableMaxNear : 0.1f,
                                (((float)mZDTableInfo.nZDTableMaxFar) * 1.0f) > 0 ? (float)mZDTableInfo.nZDTableMaxFar : 1000.0f);
    if(ret != ETronDI_OK)
        LOG_ERR(LOG_TAG, "Error reading point cloud image from device, ret=%d", ret);
        
    return ret;
}

bool CameraDevice::isInterleaveModeEnabled()    {
    //if(isInterleaveModeSupported() == false)    return false;
    
    return mInterleaveModeEnabled;
}

int CameraDevice::enableInterleaveMode(bool enable)    {
    if(isInterleaveModeSupported() == false)    return ETronDI_NotSupport;
    
    if(mInterleaveModeEnabled == enable)    return ETronDI_OK;
    
    mInterleaveModeEnabled = enable;
    int ret = RETRY_ETRON_API(EtronDI_EnableInterleave(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                       &mDevSelInfo,
                                                       enable));
    if(ETronDI_OK == ret)    {
        LOG_INFO(LOG_TAG, "Interleave mode %s...", enable ? "enabled" : "disabled");
    } else    {
        LOG_ERR(LOG_TAG, "Error EtronDI_EnableInterleave(%s), ret = %d",
                enable ? "true" : "false", ret);
    }

    return ret;
}

DepthFilterOptions CameraDevice::getDepthFilterOptions()    {
    return mDepthFilterOptions;
}

void CameraDevice::setDepthFilterOptions(DepthFilterOptions depthFilterOptions)    {
    // TODO: need to check validity of depthFilterOptions ???
    mDepthFilterOptions = depthFilterOptions;
}

DepthAccuracyOptions CameraDevice::getDepthAccuracyOptions()    {
    return mDepthAccuracyOptions;
}

void CameraDevice::setDepthAccuracyOptions(DepthAccuracyOptions depthAccuracyOptions)    {
    bool wasEnabled = mDepthAccuracyOptions.mEnabled;
    
    // TODO: need to check validity of depthAccuracyOptions ???
    if(mDepthAccuracyOptions == depthAccuracyOptions)    return;
    
    mDepthAccuracyOptions = depthAccuracyOptions;
    adjustDepthInvalidBandPixel();
    updateColorPalette();
    
    { // adjust mDepthAccuracyRegion
        const int nHorizontalMargin =
                      (int)((( 1.0f - mDepthAccuracyOptions.mRegionRatio) *
                             (mDepthWidth - mDepthInvalidBandPixel * 2)) / 2);
        const int nVerticalMargin =
                      (int)((( 1.0f - mDepthAccuracyOptions.mRegionRatio ) *
                             mDepthHeight) / 2);
        mDepthAccuracyRegion.x = nHorizontalMargin + mDepthInvalidBandPixel;                 
        mDepthAccuracyRegion.y = nVerticalMargin;
        mDepthAccuracyRegion.width = mDepthWidth - nHorizontalMargin - mDepthAccuracyRegion.x;
        mDepthAccuracyRegion.height = mDepthHeight - nVerticalMargin - mDepthAccuracyRegion.y;
    }
}

CameraDeviceProperties::CameraPropertyItem
CameraDevice::getCameraDeviceProperty(CameraDeviceProperties::CAMERA_PROPERTY type)    {
    return mCameraDeviceProperties.getCameraProperty(type);
}

int CameraDevice::setCameraDevicePropertyValue(CameraDeviceProperties::CAMERA_PROPERTY type,
                                                int32_t value)    {
    return mCameraDeviceProperties.setCameraPropertyValue(type, value);
}

int CameraDevice::reloadCameraDeviceProperties()    {
    return mCameraDeviceProperties.update();
}

//TODO: really restore from previous saved value
int CameraDevice::initIRProperty()    {
    char buffer[1024];
    int ret = 0;
    IRProperty property = getIRProperty();
    property.enableExtendIR(false);
    property.setIRValue(0);
    
    ret = setIRProperty(property);
    
    mIRProperty.toString(buffer, sizeof(buffer));
    LOG_INFO(LOG_TAG, "IR property initialized...\n%s", buffer);
    
    return ret;
}

 // return a copy of current device IR property
IRProperty CameraDevice::getIRProperty()    {
    return mIRProperty;
}

int CameraDevice::setIRProperty(IRProperty property)    {
    int ret = ETronDI_OK;
    
    if(mIRProperty == property)    return ETronDI_OK;
   
    { 
        // 2 bits on for opening both 2 ir
        ret = RETRY_ETRON_API(EtronDI_SetIRMode(
                                  libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                  &mDevSelInfo, 0x03));
        if (ret != ETronDI_OK)    goto end;

            ret = RETRY_ETRON_API(EtronDI_SetIRMaxValue(
                                      libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                      &mDevSelInfo,
                                      property.mExtendIREnabled ? EXTEND_IR_MAX : DEFAULT_IR_MAX));
            if (ret != ETronDI_OK)    goto end;
    }
    
    { // set IR value
        if(property.mIRValue != 0)    {
            // 6 bits on for opening both 6 ir
            ret = RETRY_ETRON_API(EtronDI_SetIRMode(
                                      libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                      &mDevSelInfo, 0x63)); 
            ret = RETRY_ETRON_API(EtronDI_SetCurrentIRValue(
                                      libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                      &mDevSelInfo, property.mIRValue));
        } else    {
            ret = RETRY_ETRON_API(EtronDI_SetCurrentIRValue(
                                      libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                      &mDevSelInfo, property.mIRValue));
            // turn off ir                          
            ret = RETRY_ETRON_API(EtronDI_SetIRMode(
                                      libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                      &mDevSelInfo, 0x00)); 
        }
        
        if (ret != ETronDI_OK)    ret;
    }

    { // really update mIRProperty
        mIRProperty.mExtendIREnabled = property.mExtendIREnabled;
        
        ret = RETRY_ETRON_API(EtronDI_GetFWRegister(
                                    libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                    &mDevSelInfo,
                                    0xE2, &mIRProperty.mIRMax,
                                    FG_Address_1Byte | FG_Value_1Byte));
        if(ETronDI_OK != ret)    goto end;

        ret = RETRY_ETRON_API(EtronDI_GetFWRegister(
                                  libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                  &mDevSelInfo,
                                  0xE1, &mIRProperty.mIRMin,
                                  FG_Address_1Byte | FG_Value_1Byte));
        if(ETronDI_OK != ret)    goto end;;

        ret = RETRY_ETRON_API(EtronDI_GetCurrentIRValue(
                                  libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                  &mDevSelInfo,
                                  &mIRProperty.mIRValue));
    }
    
end:
    if(ret != ETronDI_OK)    LOG_ERR(LOG_TAG, "Error setting IR property, ret=%d", ret);
        
    return ret;
}

float CameraDevice::getManuelExposureTimeMs()    {
    return mCameraDeviceProperties.getManuelExposureTimeMs();
}

void CameraDevice::setManuelExposureTimeMs(float fMS)    {
    mCameraDeviceProperties.setManuelExposureTimeMs(fMS);
}

float CameraDevice::getManuelGlobalGain()    {
    return mCameraDeviceProperties.getManuelGlobalGain();
}

void CameraDevice::setManuelGlobalGain(float fGlobalGain)    {
    mCameraDeviceProperties.setManuelGlobalGain(fGlobalGain);
}

// return a copy of current device RegisterReadWriteOptions
RegisterReadWriteOptions CameraDevice::getRegisterReadWriteOptions()    {
    return mRegisterReadWriteOptions;
}

void CameraDevice::setRegisterReadWriteOptionsForRead(RegisterReadWriteOptions registerReadWriteOptions)    {
    int i = 0;
    
    //if(mRegisterReadWriteOptions == registerReadWriteOptions)    return;
    
    mRegisterReadWriteController.pause();
    {
        mRegisterReadWriteOptions = registerReadWriteOptions;
        mRegisterReadWriteController.commitReadRegisters();
    }
    mRegisterReadWriteController.resume();
}

void CameraDevice::setRegisterReadWriteOptionsForWrite(RegisterReadWriteOptions registerReadWriteOptions)    {
    int i = 0;
    
    //if(mRegisterReadWriteOptions == registerReadWriteOptions)    return;
    
    mRegisterReadWriteController.pause();
    {
        mRegisterReadWriteOptions = registerReadWriteOptions;
        mRegisterReadWriteController.commitWriteRegisters();
    }
    mRegisterReadWriteController.resume();
}

void CameraDevice::getDepthOfField(uint16_t *nZDTableMaxNear, uint16_t *nZDTableMaxFar)    {
    *nZDTableMaxNear = mZDTableInfo.nZDTableMaxNear;
    *nZDTableMaxFar = mZDTableInfo.nZDTableMaxFar;
}

void CameraDevice::setDepthOfField(uint16_t nZDTableMaxNear, uint16_t nZDTableMaxFar)    {
    // TODO: check teh validity of nZDTableMaxNear, nZDTableMaxFar
    mZDTableInfo.nZDTableMaxNear = nZDTableMaxNear;
    mZDTableInfo.nZDTableMaxFar = nZDTableMaxFar;
    
    updateColorPalette();
}

void CameraDevice::release()    {
}

#ifdef DEVICE_MEMORY_ALLOCATOR

void* CameraDevice::requestMemory(size_t size)    {
    size_t pageSize = sysconf(_SC_PAGESIZE);
    size_t pixel = (((mColorWidth * mColorHeight) << 2) + ((pageSize) - 1)) & (~(pageSize - 1));
    for(auto iter = mMemories.begin(); iter != mMemories.end(); ++iter)    {
        if(size > pixel)    { // request for xyz
            if(iter->second >= size)    {
                mMemories.erase(iter);
                return iter->first;
            }
        } else    { // request for pixel
            if(iter->second > pixel)    continue;
            
            mMemories.erase(iter);
            return iter->first;
        }
    }
    
    LOG_ERR(LOG_TAG, "Out of memory error...");
    return nullptr;
}

void CameraDevice::returnMemory(const void *memory, size_t size)    {
    std::map<void *, size_t>::iterator iter;
    
    iter = mMemories.find((void *)memory);
    if(iter == mMemories.end())    {
        mMemories[(void *)memory] = *((size_t *)(((uint8_t *)memory) - sizeof(size_t)));
    } else    {
        LOG_WARN(LOG_TAG, "Memory(%p) had been returned, ignore...", memory);
    }
}

// Full Page allocate with 1 extra page for storing frame attributes
int CameraDevice::preallocateMemory()    {
    size_t pageSize = sysconf(_SC_PAGESIZE);
    size_t size = (((mColorWidth * mColorHeight) << 2) + ((pageSize << 1) - 1)) & (~(pageSize - 1));
    for(int i = 0; i < MAX_PREALLOCATE_PIXEL_FRAME_COUNT; i++)    {
        uint8_t *p = (uint8_t *)memalign(pageSize, size);
        if(p == nullptr)    {
            LOG_ERR(LOG_TAG, "Error allocating memory of size(%" PRIu32 ")", (uint32_t)size);
            return -1;
        }
        
        p = p + pageSize;
        *((size_t *)(p - sizeof(size_t))) = (size - pageSize);
        mMemories[(void *)p] = (size - pageSize);
    }
    
    // xyz
    size = (((mColorWidth * mColorHeight) * 3 * sizeof(float)) + ((pageSize << 1) - 1)) & (~(pageSize - 1));
    for(int i = 0; i < MAX_PREALLOCATE_XYZ_FRAME_COUNT; i++)    {
        uint8_t *p = (uint8_t *)memalign(pageSize, size);
        if(p == nullptr)    {
            LOG_ERR(LOG_TAG, "Error allocating memory of size(%" PRIu32 ")", (uint32_t)size);
            return -1;
        }
        
        p = p + pageSize;
        *((size_t *)(p - sizeof(size_t))) = (size - pageSize);
        mMemories[(void *)p] = (size - pageSize);
    }
    
    return 0;
}

void CameraDevice::releasePreallocatedMemory()    {
    size_t pageSize = sysconf(_SC_PAGESIZE);
    for(auto iter = mMemories.begin(); iter != mMemories.end(); ++iter)    {
        uint8_t *p = (uint8_t *)iter->first;
        free((void*)(p - pageSize));
    }
    
    mMemories.clear();
}

#endif

int CameraDeviceInfo::toString(char *buffer, int bufferLength) const    {
    return snprintf(buffer, (size_t)bufferLength,
                    "---- Device Info. (%p) ----\n"
                    "        Product ID:       0X%04X\n"
                    "        Vendor ID:        0X%04X\n"
                    "        Dev Path:         %s\n"
                    "        Chip ID:          0X%04X\n"
                    "        Device Type:      0X%04X\n"
                    "        Model Name:       %s\n"
                    "        Bus Info:         %s\n"
                    "        USB Type:         %s\n"
                    "        Serial Number:    %s\n"
                    "        Firmware Version: %s",
                    this, (int)devInfo.wPID, (int)devInfo.wVID, devInfo.strDevName,
                    (int)devInfo.nChipID, (int)devInfo.nDevType,
                    modelName, busInfo,
                    ((usbPortType == USB_PORT_TYPE_2_0) ?
                       "USB 2" : ((usbPortType == USB_PORT_TYPE_3_0) ? "USB 3" : "Unknow USB Type")),
                    serialNumber, firmwareVersion);
}

int CameraDeviceInfo::toString(std::string &string) const    {
    int ret = 0;
    char buffer[1024];
    
    ret = toString(buffer, sizeof(buffer));
    string.append(buffer);
    
    return ret;
}

CameraDevice* CameraDeviceFactory::createCameradevice(DEVSELINFO *devSelInfo, DEVINFORMATION *deviceInfo)    {
    CameraDevice *cameraDevice = nullptr;
    
    switch(deviceInfo->wPID)    {
    case ETronDI_PID_8053:
        cameraDevice = new CameraDevice8053(devSelInfo, deviceInfo);
        break;
//    case ETronDI_PID_8036:
//        cameraDevice = new CameraDevice8036(devSelInfo, deviceInfo);
//        break;
//    case ETronDI_PID_8037:
//        cameraDevice = new CameraDevice8037(devSelInfo, deviceInfo);
//        break;
//    case ETronDI_PID_8052:
//        cameraDevice = new CameraDevice8052(devSelInfo, deviceInfo);
//        break;
    case ETronDI_PID_8062:
        cameraDevice = new CameraDevice8062(devSelInfo, deviceInfo);
        break;
//    case ETronDI_PID_HYPATIA:
//        cameraDevice = new CameraDeviceHypatia(devSelInfo, deviceInfo);
//        break;
    default:
        LOG_ERR("CameraDeviceFactory", "Undefined PID(0X%04X)...", (int)deviceInfo->wPID);
    }
    
    if(cameraDevice)    {
        if(0 != cameraDevice->postInitialize())    {
            delete cameraDevice;
            cameraDevice = nullptr;
        }
    }
    
    return cameraDevice;
}

} // end of namespace devices
} // end of namespace libeYs3D
