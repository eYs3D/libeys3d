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


#include "devices/IMUDevice.h"
#include "devices/CameraDevice.h"
#include "sensors/IMUDataProducer.h"
#include "base/threads/Thread.h"
#include "debug.h"
#include "utils.h"
#include "macros.h"

#include <limits.h>

#define LOG_TAG "IMUDevice"

namespace libeYs3D    {
namespace devices    {

int IMUDevice::IMUDeviceInfo::toString(char *buffer, int bufferLength)    {
    return snprintf(buffer, (size_t)bufferLength,
                    "---- IMU Device Info. ----\n"
                    "              Product ID: 0X%04X\n"
                    "               Vendor ID: 0X%04X\n"
                    "             Device Type: %s\n"
                    "             Module Name: %s\n"
                    "           Serial Number: %s\n"
                    "        Firmware Version: %s\n"
                    "                  status: %s\n"
                    "                 isValid: %s",
                    (int)nVID, (int)nPID,
                    (nType == IMU_6_AXIS) ? "IMU_6_AXIS" : ((nType == IMU_9_AXIS) ? "IMU_9_AXIS" : "IMU_UNKNOWN"),
                    moduleName, serialNumber, fwVersion,
                    (status ? "enabled" : "disabled"),
                    (isValid ? "true" : "false"));
                      
}

const char GET_MODULE_NAME_0[8] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_MODULE_NAME_1[8] = { 0x00, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_MODULE_NAME_2[8] = { 0x00, 0x02, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_MODULE_NAME_3[8] = { 0x00, 0x03, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00 };

const char GET_FW_VERSION_0[8] = { 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_FW_VERSION_1[8] = { 0x00, 0x05, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_FW_VERSION_2[8] = { 0x00, 0x06, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_FW_VERSION_3[8] = { 0x00, 0x07, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_FW_VERSION_4[8] = { 0x00, 0x08, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_FW_VERSION_5[8] = { 0x00, 0x09, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_FW_VERSION_6[8] = { 0x00, 0x0A, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_FW_VERSION_7[8] = { 0x00, 0x0B, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00 };

const char READ_OUTPUT_STATUS[8] = { 0x00, 0x11, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char DISABLE_OUTPUT[8] = { 0x00, 0x11, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char ENABLE_OUTPUT[8] = { 0x00, 0x11, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00 };

const char READ_OUTPUT_FORMAT[8] = { 0x00, 0x12, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

const char SET_OUTPUT_FORMAT_1[8] = { 0x00, 0x12, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00 };
const char SET_OUTPUT_FORMAT_2[8] = { 0x00, 0x12, 0x01, 0x02, 0x00, 0x00, 0x00, 0x00 };
const char SET_OUTPUT_FORMAT_3[8] = { 0x00, 0x12, 0x01, 0x03, 0x00, 0x00, 0x00, 0x00 };
const char SET_OUTPUT_FORMAT_4[8] = { 0x00, 0x12, 0x02, 0x04, 0x00, 0x00, 0x00, 0x00 };
const char SET_OUTPUT_FORMAT_5[8] = { 0x00, 0x12, 0x02, 0x05, 0x00, 0x00, 0x00, 0x00 };

const char CHECK_CALIBRATING_STATUS[8] = { 0x00, 0x13, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char START_CALIBRATION[8] = { 0x00, 0x13, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char READ_CALIBRATED[8] = { 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

const char GET_SERIAL_NUMBER_0[8] = { 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_SERIAL_NUMBER_1[8] = { 0x00, 0x14, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_SERIAL_NUMBER_2[8] = { 0x00, 0x14, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_SERIAL_NUMBER_3[8] = { 0x00, 0x14, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char GET_SERIAL_NUMBER_4[8] = { 0x00, 0x14, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00 };

const char CHECK_FLASH_WRITING_STATUS[8] = { 0x00, 0x1B, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
const char START_WRITE_FLASH[8] = { 0x00, 0x1B, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 };

void IMUDevice::checkCalibratingStatus(char *calibratingStatus)    {
    FeatureConfigItem setFeatureData =
        { &CHECK_CALIBRATING_STATUS[0], (sizeof(CHECK_CALIBRATING_STATUS) / sizeof(CHECK_CALIBRATING_STATUS[0])) };

    char status[8] = { 0 };
    sendFeatureReport(setFeatureData.pData, setFeatureData.nDataLength);
    getFeatureReport(&status[0], setFeatureData.nDataLength);

    *calibratingStatus = status[0];
}

void IMUDevice::startCalibration()    {
    FeatureConfigItem setFeatureData =
        { &START_CALIBRATION[0], (sizeof(START_CALIBRATION) / sizeof(START_CALIBRATION[0])) };
    sendFeatureReport(setFeatureData.pData, setFeatureData.nDataLength);
}

void IMUDevice::readCalibrated(char *calibrated)    {
    FeatureConfigItem setFeatureData =
        { &READ_CALIBRATED[0], (sizeof(READ_CALIBRATED) / sizeof(READ_CALIBRATED[0])) };

    char status[8] = { 0 };
    sendFeatureReport(setFeatureData.pData, setFeatureData.nDataLength);
    getFeatureReport(&status[0], setFeatureData.nDataLength);

    *calibrated = status[0];
}

IMUDevice::IMUDeviceInfo IMUDevice::getIMUDeviceInfo()    {
    if(!(mIMUDeviceState & IMUD_INITIALIZED))    {
        LOG_WARN(LOG_TAG, "IMUDevice has not been initialized yet...");
    }
    
    getStatus();
    mIMUDeviceInfo.isValid = isValid();
    
    return mIMUDeviceInfo;
}

const char* IMUDevice::getFWVersion()    {
    FeatureConfigItem setFeatureData[8] = {
        { &GET_FW_VERSION_0[0], (sizeof(GET_FW_VERSION_0) / sizeof(GET_FW_VERSION_0[0])) },
        { &GET_FW_VERSION_1[0], (sizeof(GET_FW_VERSION_1) / sizeof(GET_FW_VERSION_1[0])) },
        { &GET_FW_VERSION_2[0], (sizeof(GET_FW_VERSION_2) / sizeof(GET_FW_VERSION_2[0])) },
        { &GET_FW_VERSION_3[0], (sizeof(GET_FW_VERSION_3) / sizeof(GET_FW_VERSION_3[0])) },
        { &GET_FW_VERSION_4[0], (sizeof(GET_FW_VERSION_4) / sizeof(GET_FW_VERSION_4[0])) },
        { &GET_FW_VERSION_5[0], (sizeof(GET_FW_VERSION_5) / sizeof(GET_FW_VERSION_5[0])) },
        { &GET_FW_VERSION_6[0], (sizeof(GET_FW_VERSION_6) / sizeof(GET_FW_VERSION_6[0])) },
        { &GET_FW_VERSION_7[0], (sizeof(GET_FW_VERSION_7) / sizeof(GET_FW_VERSION_7[0])) }
    };
    
    char *fwVersion = mIMUDeviceInfo.fwVersion;
    fwVersion[0] = '\0';
    for(int i = 0; i < 8; i++)    {
        sendFeatureReport(setFeatureData[i].pData, setFeatureData[i].nDataLength);
        getFeatureReport(fwVersion, 8);
        fwVersion += 8;
    }
    
    *fwVersion= '\0';

    return mIMUDeviceInfo.fwVersion;
}

const char* IMUDevice::getModuleName()    {
    FeatureConfigItem setFeatureData[4] = {
        { &GET_MODULE_NAME_0[0], (sizeof(GET_MODULE_NAME_0) / sizeof(GET_MODULE_NAME_0[0])) },
        { &GET_MODULE_NAME_1[0], (sizeof(GET_MODULE_NAME_1) / sizeof(GET_MODULE_NAME_1[0])) },
        { &GET_MODULE_NAME_2[0], (sizeof(GET_MODULE_NAME_2) / sizeof(GET_MODULE_NAME_2[0])) },
        { &GET_MODULE_NAME_3[0], (sizeof(GET_MODULE_NAME_3) / sizeof(GET_MODULE_NAME_3[0])) }
    };

    char *moduleName = mIMUDeviceInfo.moduleName;
    moduleName[0] = '\0';
    for(int i = 0; i < 4; i++) {
        sendFeatureReport(setFeatureData[i].pData, setFeatureData[i].nDataLength);
        getFeatureReport(moduleName, 8);
        moduleName += 8;
    }

    *moduleName = '\0';
    
    return mIMUDeviceInfo.moduleName;
}

int IMUDevice::getStatus()    {
    FeatureConfigItem setFeatureData =
        { &READ_OUTPUT_STATUS[0], (sizeof(READ_OUTPUT_STATUS) / sizeof(READ_OUTPUT_STATUS[0])) };

    char status[8] = { 0 };
    sendFeatureReport(setFeatureData.pData, setFeatureData.nDataLength);
    getFeatureReport(&status[0], setFeatureData.nDataLength);

    if(0 == status[0])    {
        mIMUDeviceInfo.status = 0;
    } else    {
        mIMUDeviceInfo.status = 1;
    }
    
    return mIMUDeviceInfo.status;
}

IMUDevice::IMU_TYPE IMUDevice::getType()    {
    return mIMUDeviceInfo.nType;
}

const char* IMUDevice::getSerialNumber()    {
    if(IMU_6_AXIS != mIMUDeviceInfo.nType)    {
        mIMUDeviceInfo.serialNumber[0] = 'N'; mIMUDeviceInfo.serialNumber[1] =  '/';
        mIMUDeviceInfo.serialNumber[2] = 'A', mIMUDeviceInfo.serialNumber[3] = '\0';
        return mIMUDeviceInfo.serialNumber;
    }

    FeatureConfigItem setFeatureData[5] = {
        { &GET_SERIAL_NUMBER_0[0], (sizeof(GET_SERIAL_NUMBER_0) / sizeof(GET_SERIAL_NUMBER_0[0])) },
        { &GET_SERIAL_NUMBER_1[0], (sizeof(GET_SERIAL_NUMBER_1) / sizeof(GET_SERIAL_NUMBER_1[0])) },
        { &GET_SERIAL_NUMBER_2[0], (sizeof(GET_SERIAL_NUMBER_2) / sizeof(GET_SERIAL_NUMBER_2[0])) },
        { &GET_SERIAL_NUMBER_3[0], (sizeof(GET_SERIAL_NUMBER_3) / sizeof(GET_SERIAL_NUMBER_3[0])) },
        { &GET_SERIAL_NUMBER_4[0], (sizeof(GET_SERIAL_NUMBER_4) / sizeof(GET_SERIAL_NUMBER_4[0])) }
    };

    char serialNumber[256] = { 0 };
    int count = 0;
    char *sSerialNumber = mIMUDeviceInfo.serialNumber;
    for(int i = 0; i < 5; i++)    {
        sendFeatureReport(setFeatureData[i].pData, setFeatureData[i].nDataLength);
        getFeatureReport(&serialNumber[count], 8);
        int j = (0 == i) ? 2 : 0;
        for(; j < 8; j += 2)    {
            uint16_t unicode = serialNumber[count + j] | (serialNumber[count + j + 1]) << 8;
            if(unicode == 0)     break;
            
            *sSerialNumber++ = unicode & 0X007F;
        }

        count += 8;
    }
    
    *sSerialNumber = '\0';

    return mIMUDeviceInfo.serialNumber;
}

void IMUDevice::setSerialNumber(const char *serialNumber)    {
    if(IMU_6_AXIS != mIMUDeviceInfo.nType) return;

    char SET_SERIAL_NUMBER[6][8] = { {0x00, 0x15, 0x24, 0x03, 0x00, 0x00, 0x00, 0x00, },
                                     {0x00, 0x16, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, },
                                     {0x00, 0x17, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, },
                                     {0x00, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, },
                                     {0x00, 0x19, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, },
                                     {0x00, 0x1a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, }};

    FeatureConfigItem setFeatureData[6] = {
        { &SET_SERIAL_NUMBER[0][0], (sizeof(SET_SERIAL_NUMBER[0]) / sizeof(SET_SERIAL_NUMBER[0][0])) },
        { &SET_SERIAL_NUMBER[1][0], (sizeof(SET_SERIAL_NUMBER[1]) / sizeof(SET_SERIAL_NUMBER[1][0])) },
        { &SET_SERIAL_NUMBER[2][0], (sizeof(SET_SERIAL_NUMBER[2]) / sizeof(SET_SERIAL_NUMBER[2][0])) },
        { &SET_SERIAL_NUMBER[3][0], (sizeof(SET_SERIAL_NUMBER[3]) / sizeof(SET_SERIAL_NUMBER[3][0])) },
        { &SET_SERIAL_NUMBER[4][0], (sizeof(SET_SERIAL_NUMBER[4]) / sizeof(SET_SERIAL_NUMBER[4][0])) },
        { &SET_SERIAL_NUMBER[5][0], (sizeof(SET_SERIAL_NUMBER[5]) / sizeof(SET_SERIAL_NUMBER[5][0])) }
    };

    char *pSerialNumber = (char *)serialNumber;
    for(int i = 0; i < 6; i++) {
        int j = (0 == i) ? 4 : 2;
        for(; j < 8; j += 2)    {
            if(*pSerialNumber != '\0') {
                unsigned short unicode = (unsigned short)*pSerialNumber;
                SET_SERIAL_NUMBER[i][j] = unicode & 0xff;
                SET_SERIAL_NUMBER[i][j + 1] = (unicode & 0xff00) >> 8;
                pSerialNumber++;
            }
        }
        
        sendFeatureReport(setFeatureData[i].pData, setFeatureData[i].nDataLength);
    }
    
    FeatureConfigItem checkFlashWritingStatue =
        { &CHECK_FLASH_WRITING_STATUS[0],
          (sizeof(CHECK_FLASH_WRITING_STATUS) / sizeof(CHECK_FLASH_WRITING_STATUS[0])) };
    sendFeatureReport(checkFlashWritingStatue.pData, checkFlashWritingStatue.nDataLength);
    char status[8] = { 0 };
    getFeatureReport(&status[0], checkFlashWritingStatue.nDataLength);

    char nRetryCount = 10;
    while(0 != status[0] && --nRetryCount >= 0)    {
        libeYs3D::base::Thread::sleepMs(100);
        sendFeatureReport(checkFlashWritingStatue.pData, checkFlashWritingStatue.nDataLength);
        getFeatureReport(&status[0], checkFlashWritingStatue.nDataLength);
    }

    FeatureConfigItem writeSerialToFlash =
        { &START_WRITE_FLASH[0], (sizeof(START_WRITE_FLASH) / sizeof(START_WRITE_FLASH[0])) };
    sendFeatureReport(writeSerialToFlash.pData, writeSerialToFlash.nDataLength);
    
    // read S/N back to mIMUDeviceInfo.serialNumber
    getSerialNumber();
}

// common API similar to CameraDevice
int IMUDevice::initStream(libeYs3D::sensors::SensorDataProducer::AppCallback appCallback,
                          libeYs3D::sensors::SensorDataProducer::Callback colorFrameProducer,
                          libeYs3D::sensors::SensorDataProducer::Callback depthFrameProducer)    {
    if(appCallback)    {
        mIMUDeviceState |= IMUD_APP_STREAM_CB;
        mIMUDataProducer->attachAppCallback(appCallback);
    }
    
    if(colorFrameProducer)    {
        mIMUDeviceState |= IMUD_COLOR_STREAM_CB;
        mIMUDataProducer->attachColorCallback(colorFrameProducer);
    }
    
    if(depthFrameProducer)    {
        mIMUDeviceState |= IMUD_DEPTH_STREAM_CB;
        mIMUDataProducer->attachDepthCallback(depthFrameProducer);
    }
    
    mIMUDeviceState |= IMUD_STREAM_INITIALIZED;
    
    mIMUDataProducer->start();
    
    return 0;
}

bool IMUDevice::isStreamEnabled()    {
    uint32_t state = IMUD_APP_STREAM_ACTIVATED | IMUD_COLOR_STREAM_ACTIVATED |
                     IMUD_DEPTH_STREAM_ACTIVATED;
    
    return ((mIMUDeviceState & state) != 0);
}
                           
void IMUDevice::enableStream()    {
    enableAppStream();
    enableColorStream();
    enableDepthStream();
}

void IMUDevice::enableAppStream()    {
    if(!(mIMUDeviceState & IMUD_APP_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "App IMU data stream callback is not provided...");
        return;
    }
    
    if(mIMUDeviceState & IMUD_APP_STREAM_ACTIVATED)    return;
    
    if(!isStreamEnabled())    enableDataOutout(true);
    
    mIMUDeviceState |= IMUD_APP_STREAM_ACTIVATED;

    mIMUDataProducer->enableAppCallback();
}

void IMUDevice::enableColorStream()    {
    if(!(mIMUDeviceState & IMUD_COLOR_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "ColorFrameProducer IMU data set stream callback is not provided...");
        return;
    }
    
    if(mIMUDeviceState & IMUD_COLOR_STREAM_ACTIVATED)    return;
    
    if(!isStreamEnabled())    enableDataOutout(true);
    
    mIMUDeviceState |= IMUD_COLOR_STREAM_ACTIVATED;
    
    mIMUDataProducer->enableColorCallback();
}

void IMUDevice::enableDepthStream()    {
    if(!(mIMUDeviceState & IMUD_DEPTH_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "DepthFrameProducer IMU data set stream callback is not provided...");
        return;
    }
    
    if(mIMUDeviceState & IMUD_DEPTH_STREAM_ACTIVATED)    return;
    
    if(!isStreamEnabled())    enableDataOutout(true);
    
    mIMUDeviceState |= IMUD_DEPTH_STREAM_ACTIVATED;

    mIMUDataProducer->enableDepthCallback();
}

void IMUDevice::pauseStream()    {
    pauseAppStream();
    pauseColorStream();
    pauseDepthStream();
}

void IMUDevice::pauseAppStream()    {
    if(!(mIMUDeviceState & IMUD_APP_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "App IMU data stream callback is not provided...");
        return;
    }
    
    if(!(mIMUDeviceState & IMUD_APP_STREAM_ACTIVATED))    return;
    
    mIMUDeviceState &= ~IMUD_APP_STREAM_ACTIVATED;
    
    if(!isStreamEnabled())    enableDataOutout(false);
    
    mIMUDataProducer->pauseAppCallback();
}

void IMUDevice::pauseColorStream()    {
    if(!(mIMUDeviceState & IMUD_COLOR_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "ColorFrameProducer IMU data set stream callback is not provided...");
        return;
    }
    
    if(!(mIMUDeviceState & IMUD_COLOR_STREAM_ACTIVATED))    return;

    mIMUDeviceState &= ~IMUD_COLOR_STREAM_ACTIVATED;

    if(!isStreamEnabled())    enableDataOutout(false);

    mIMUDataProducer->pauseColorCallback();
}

void IMUDevice::pauseDepthStream()    {
    if(!(mIMUDeviceState & IMUD_DEPTH_STREAM_CB))    {
        LOG_INFO(LOG_TAG, "DepthFrameProducer IMU data set stream callback is not provided...");
        return;
    }
    
    if(!(mIMUDeviceState & IMUD_DEPTH_STREAM_ACTIVATED))    return;
    
    mIMUDeviceState &= ~IMUD_DEPTH_STREAM_ACTIVATED;
    
    if(!isStreamEnabled())    enableDataOutout(false);
    
    mIMUDataProducer->pauseDepthCallback();
}

void IMUDevice::dumpIMUData(int recordCount)    {
    if(mDataDumpCount > 0)    {
        LOG_INFO(LOG_TAG, "IMUDevice is dumping data, ignore it...");
        return;
    }
    
    if(recordCount <= 0)    {
        LOG_INFO(LOG_TAG, "Invalid IMU data record count: %d, ignore it...", recordCount);
        return;
    }
    
    { // prepare data log file
        char logPath[PATH_MAX], tmpBuffer[128];
        
        get_time_YYYY_MM_DD_HH_MM_SS(now_in_microsecond_unix_time() / 1000,
                                     tmpBuffer, sizeof(tmpBuffer));
        snprintf(logPath, PATH_MAX, "%s/imu-%s.log",
                 libeYs3D::EYS3DSystem::getEYS3DSystem()->getIMULogPath(), tmpBuffer);
        mDataLogFile = fopen(logPath, "wt");
        if(!mDataLogFile) {
            LOG_ERR(LOG_TAG, "Error opening %s", logPath);
            return;
        }
        
        mDataDumpCount = recordCount;
    }
}

void IMUDevice::logIMUDataRecord(uint8_t *imuRawData, int length)    {
    if(mDataDumpCount == 0)    return;
    
    mDataDumpCount -= 1;
    if(mDataLogFile)    {
        for(int i = 0; i < length; i++) {
            fprintf(mDataLogFile, "%02x ", imuRawData[i]);
        }
        
        fprintf(mDataLogFile, "\n");
        fflush(mDataLogFile);
        
        if(mDataDumpCount == 0)    {
            fclose(mDataLogFile);
            mDataLogFile = nullptr;
        }
    }
}
                                
void IMUDevice::closeStream()    {
    int ret = 0;

    LOG_INFO(LOG_TAG, "Closing IMUDevice(%p) stream...", this);

    if(!(mIMUDeviceState & (IMUD_INITIALIZED)))    goto end;
    if(!(mIMUDeviceState & (IMUD_STREAM_INITIALIZED)))    goto end;

    // Pause stream before we stop producers
    this->pauseStream();
    if(mIMUDataProducer)    {
        mIMUDataProducer->stop();
        delete mIMUDataProducer;
        mIMUDataProducer = nullptr;
    }

    if(mHidHandle)    {
        LOG_INFO(LOG_TAG, "Closing IMU HID............");
        hid_close(mHidHandle);
        mHidHandle = nullptr;
    }

    mIMUDeviceState = IMUD_INITIALIZED;

end:
    LOG_INFO(LOG_TAG, "IMUDevice(%p) closed...", this);
}

int IMUDevice::toString(char *buffer, int bufferLength)    {
    return snprintf(buffer, (size_t)bufferLength,
                    "---- %s Info. (%p), isValid(%s), status(%s) ----\n"
                    "        Product ID:       0X%04X\n"
                    "        Vendor ID:        0X%04X\n"
                    "        Device Type:      %s\n"
                    "        Module ID:        0X%04X\n"
                    "        Serial Number:    %s\n"
                    "        Module Name:      %s\n"
                    "        Firmware Version: %s",
                    getName(), this, (isValid() ? "true" : "false"),
                    (getStatus() ? "enabled" : "disabled"),  
                    (int)mIMUDeviceInfo.nVID, (int)mIMUDeviceInfo.nPID,
                    (mIMUDeviceInfo.nType == IMU_6_AXIS) ?
                        "IMU_6_AXIS" : ((mIMUDeviceInfo.nType == IMU_9_AXIS) ? "IMU_9_AXIS" : "IMU_UNKNOWN"),
                    mCameraDevice->getModuleID(), getSerialNumber(), getModuleName(), getFWVersion());
}

IMUDevice::IMUDevice(IMUDeviceInfo info, CameraDevice *cameraDevice)
    : mIMUDeviceInfo(info), mCameraDevice(cameraDevice),
      mHidHandle(nullptr),
      mCurrentIMUDataFormat(IMU_DATA_FORMAT::RAW_DATA_WITHOUT_OFFSET),
      mIMUDeviceState(0), mIMUDataProducer(nullptr),
      mDataDumpCount(0), mDataLogFile(nullptr)    {
}

int IMUDevice::initialize()    {
    std::vector<std::string> hidDeviceList;
    
    if(mHidHandle)    { // TODO: this should never happen ?
        hid_close(mHidHandle);
        mHidHandle = nullptr;
    }
    
    if(IMU_UNKNOWN == mIMUDeviceInfo.nType)    return ETronDI_NotSupport;

    hidDeviceList = libeYs3D::EYS3DSystem::getEYS3DSystem()->getHIDDeviceList(mIMUDeviceInfo.nVID, mIMUDeviceInfo.nPID);
    if(0 == hidDeviceList.size())    {
        return ETronDI_NoDevice;
    }

    { // Mapping IMU device with camera
        int retry = 8;
        bool isConnected = false;
        
        for(std::string devicePath : hidDeviceList)    {
            hid_device *device = hid_open_path(devicePath.c_str());
            if(!device)    {
                LOG_ERR(LOG_TAG, "Unable to open device:%s ...", devicePath.c_str());
                continue;
            }
            
            {
                if(0 != hid_set_nonblocking(device, 1))    {
                        LOG_WARN(LOG_TAG, "Unable to set device:%s into non-block mode...",
                                 devicePath.c_str());
                }
            
                isConnected = false;
                while(retry-- > 0)    {
                    isConnected = isIMUConnectedWithCamera(device);
                    if(isConnected)    {
                        LOG_INFO(LOG_TAG, "Device(%s) maps with camera device, retry...",
                                 devicePath.c_str());
                        break;
                    } else    {
                        LOG_INFO(LOG_TAG, "Device(%s) does NOT map with camera device, retry(%d)...",
                                 devicePath.c_str(), retry);
                        libeYs3D::base::Thread::sleepMs(48); // 3 frames/60FPS
                    }
                }
                
                //TODO
                isConnected = true;
    
                if(isConnected)    {
                    mHidHandle = device;
                    if(0 != hid_set_nonblocking(device, 0))    {
                        LOG_WARN(LOG_TAG, "Unable to set device:%s into block mode...",
                                 devicePath.c_str());
                    }
                
                    break;
                } else    {
                    hid_close(device);
                }
            }
        } // end of for
    }
    
    if(mHidHandle)    {
        enableDataOutout(false); // disable data output when initialized
        readDataOutputFormat();
        
        { // read data
            getFWVersion();
            getModuleName();
            getStatus();
            getSerialNumber();
            mIMUDeviceInfo.isValid = isValid();
        }
    }

    return mHidHandle ? ETronDI_OK : ETronDI_NoDevice;
}

int IMUDevice::postInitialize()    {
    mIMUDataProducer = new libeYs3D::sensors::IMUDataProducer(this);
    
    mIMUDeviceState |= IMUD_INITIALIZED;

    return ETronDI_OK;
}

bool IMUDevice::isValid()    {
    return mHidHandle != nullptr;
}

void IMUDevice::readDataOutputFormat()    {
    FeatureConfigItem setFeatureData =
        { &READ_OUTPUT_FORMAT[0], (sizeof(READ_OUTPUT_FORMAT) / sizeof(READ_OUTPUT_FORMAT[0])) };
    char status[8] = { 0 };

    sendFeatureReport(setFeatureData.pData, setFeatureData.nDataLength);
    getFeatureReport(&status[0], setFeatureData.nDataLength);

    mCurrentIMUDataFormat = status[0] != 0 ? (IMU_DATA_FORMAT)status[0] : RAW_DATA_WITHOUT_OFFSET;
    LOG_INFO(LOG_TAG, "IMU Device data output format read: %d", mCurrentIMUDataFormat);
}

void IMUDevice::enableDataOutout(bool bIsEnbale)    {
    FeatureConfigItem setFeatureData;

    if(bIsEnbale)    {
        setFeatureData = { &ENABLE_OUTPUT[0], (sizeof(ENABLE_OUTPUT) / sizeof(ENABLE_OUTPUT[0])) };
    } else    {
        setFeatureData = { &DISABLE_OUTPUT[0], (sizeof(DISABLE_OUTPUT) / sizeof(DISABLE_OUTPUT[0])) };
    }
    
    readDataOutputFormat();

    sendFeatureReport(setFeatureData.pData, setFeatureData.nDataLength);
}

int IMUDevice::selectDataFormat(IMU_DATA_FORMAT format)    {
    FeatureConfigItem setFeatureData;

    switch (format)    {
    case RAW_DATA_WITHOUT_OFFSET:
        setFeatureData = { &SET_OUTPUT_FORMAT_1[0], (sizeof(SET_OUTPUT_FORMAT_1) / sizeof(SET_OUTPUT_FORMAT_1[0])) };
        break;
    case RAW_DATA_WITH_OFFSET:
        setFeatureData = { &SET_OUTPUT_FORMAT_2[0], (sizeof(SET_OUTPUT_FORMAT_2) / sizeof(SET_OUTPUT_FORMAT_2[0])) };
        break;
    case OFFSET_DATA:
        setFeatureData = { &SET_OUTPUT_FORMAT_3[0], (sizeof(SET_OUTPUT_FORMAT_3) / sizeof(SET_OUTPUT_FORMAT_3[0])) };
        break;
    case DMP_DATA_WITHOT_OFFSET:
        setFeatureData = { &SET_OUTPUT_FORMAT_4[0], (sizeof(SET_OUTPUT_FORMAT_4) / sizeof(SET_OUTPUT_FORMAT_4[0])) };
        break;
    case DMP_DATA_WITH_OFFSET:
        setFeatureData = { &SET_OUTPUT_FORMAT_5[0], (sizeof(SET_OUTPUT_FORMAT_5) / sizeof(SET_OUTPUT_FORMAT_5[0])) };
        break;
    default:
        return ETronDI_NotSupport;
    }

    sendFeatureReport(setFeatureData.pData, setFeatureData.nDataLength);
    mCurrentIMUDataFormat = format;

    return ETronDI_OK;
}

IMUDevice::IMU_DATA_FORMAT IMUDevice::getDataFormat()    {
    return mCurrentIMUDataFormat;
}

// returns the actual number of bytes read and negative on error. 
int IMUDevice::readIMUData(IMUData *imuData)    {
    if(!mHidHandle)    {
        return ETronDI_NullPtr; // -2
    }

    readDataOutputFormat();

    uint8_t imuRawData[256] = {0};
    //int ret = hid_read(mHidHandle, imuRawData, sizeof(imuRawData));
    int ret = hid_read_timeout(mHidHandle, imuRawData, sizeof(imuRawData), 16);
    if(ret > 0)    {
        int nIMUDataByte = getIMUDataOutputBytes(mCurrentIMUDataFormat);

        logIMUDataRecord(imuRawData, nIMUDataByte);

        if(IMU_9_AXIS == mIMUDeviceInfo.nType)    {
            imuData->parsePacket_Quaternion(imuRawData);
        } else if (27 == nIMUDataByte)    {
            imuData->parsePacket(imuRawData, OFFSET_DATA != mCurrentIMUDataFormat);
        }else if (58 == nIMUDataByte)    {
            imuData->parsePacket_DMP(imuRawData);
        }
    }

    return ret;
}

int IMUDevice::getIMUDataOutputBytes(IMU_DATA_FORMAT format)    {
    switch(format)    {
        case RAW_DATA_WITHOUT_OFFSET:
        case RAW_DATA_WITH_OFFSET:
        case OFFSET_DATA:
            return 27;
        case DMP_DATA_WITHOT_OFFSET:
        case DMP_DATA_WITH_OFFSET:
            return 58;
        case QUATERNION_DATA:
            return 58;
        default: return 0;
    }
}

std::vector<IMUDevice::IMU_DATA_FORMAT> IMUDevice::getSupportDataFormat()    {
    switch(mIMUDeviceInfo.nType)    {
        case IMU_6_AXIS:
            return { RAW_DATA_WITHOUT_OFFSET, RAW_DATA_WITH_OFFSET, OFFSET_DATA };
        case IMU_9_AXIS:
            return { RAW_DATA_WITHOUT_OFFSET, RAW_DATA_WITH_OFFSET,
                     OFFSET_DATA, DMP_DATA_WITHOT_OFFSET, DMP_DATA_WITH_OFFSET };
        default:
            return {};
    }
}

void IMUDevice::getFeatureReport(char *pData, size_t data_lenght)    {
    if(mHidHandle)    {
        unsigned char *pBuf = nullptr;
        pBuf = (unsigned char *)calloc(data_lenght + 1, sizeof(unsigned char));
        pBuf[0] = { 0x0 };
        hid_get_feature_report(mHidHandle, pBuf, data_lenght + 1); 
        memcpy(pData, pBuf + 1, data_lenght);
        free( pBuf );
    }  
}

void IMUDevice::sendFeatureReport(const char *pData, size_t data_lenght)    {
    if(mHidHandle)    {
        unsigned char *pBuf = (unsigned char *)calloc(data_lenght + 1, sizeof(unsigned char));
        pBuf[0] = { 0x0 };
        memcpy(pBuf + 1, pData, data_lenght);
        hid_send_feature_report(mHidHandle, pBuf, data_lenght + 1); 
        free(pBuf);
    }
}

bool IMUDevice::isIMUConnectedWithCamera(hid_device *device)    {
    bool isConnected = false;

    mHidHandle = device;
    
    switch(mIMUDeviceInfo.nType)    {
        case IMU_6_AXIS:    {
            if(0 == strcmp(mCameraDevice->mCameraDeviceInfo.serialNumber, getSerialNumber()))
                isConnected = true;
            break;
        }
        case IMU_9_AXIS:    {
            IMUData data;
            readIMUData(&data);
                             
            isConnected = (data._hour == mCameraDevice->getModuleID());
            
            LOG_INFO(LOG_TAG, "isIMUConnectedWithCamera: %s, Camera Moudle ID(%d) : IMU Device ID(%d)",
                     (isConnected ? "yes" : "no"), mCameraDevice->getModuleID(), data._hour);
            
            break;
        }
        default:
            break;
    }

    mHidHandle = nullptr;

   return isConnected;
}

IMUDevice::~IMUDevice()    {
    closeStream();
}

} // end of namespace devices
} // end of namespace libeYs3D
