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

#include "macros.h"
#include "eSPDI.h"
#include "devices/model/CameraDeviceProperties.h"
#include "devices/CameraDevice.h"

#include <string.h>
#include <math.h>

#define LOG_TAG "CameraDeviceProperties"

namespace libeYs3D    {
namespace devices    {

CameraDeviceProperties::CameraDeviceProperties(CameraDevice *cameraDevice)
    : mCameraDevice(cameraDevice)    {
}

CameraDeviceProperties::~CameraDeviceProperties()    {
}

int CameraDeviceProperties::init()    {
    initCameraProperties();

    return ETronDI_OK;
}

int CameraDeviceProperties::update()    {
    for(int i = 0 ; i < CAMERA_PROPERTY_COUNT ; ++i)   {
        updateCameraProperty((CAMERA_PROPERTY)i);
    }

    return ETronDI_OK;
}

int CameraDeviceProperties::reset()    {
    initCameraProperties();
    
    return ETronDI_OK;
}

int CameraDeviceProperties::initCameraProperties()    {
    int nID;
    bool bIsCTProperty;
    
    for(int i = 0 ; i < CAMERA_PROPERTY_COUNT ; i++)    {
        getCameraPropertyFlag((CAMERA_PROPERTY)i, &nID, &bIsCTProperty);

        CameraPropertyItem &item = mCameraPropertyItems[i];
        if(!item.bSupport) continue;

        item.bValid = false;

        int ret;
        if(bIsCTProperty)    {
            ret = RETRY_ETRON_API(EtronDI_GetCTRangeAndStep(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                            &(mCameraDevice->mDevSelInfo),
                                                            nID,
                                                            &item.nMax, &item.nMin, &item.nStep, &item.nDefault, &item.nFlags));
            if(ret != ETronDI_OK)    {
                LOG_ERR(LOG_TAG, "Failed EtronDI_GetCTRangeAndStep, ret = %d", ret);
            }
        } else    {
            ret = RETRY_ETRON_API(EtronDI_GetPURangeAndStep(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                            &(mCameraDevice->mDevSelInfo),
                                                            nID,
                                                            &item.nMax, &item.nMin, &item.nStep, &item.nDefault, &item.nFlags));
            if(ret != ETronDI_OK)    {
                LOG_ERR(LOG_TAG, "Failed EtronDI_GetPURangeAndStep, ret = %d", ret);
            }
        }

        if (ETronDI_OK != ret) continue;

        if(EXPOSURE_TIME == (CAMERA_PROPERTY)i){
            item.nMax = log2(item.nMax / 10000.0);
            item.nMin = log2(item.nMin  / 10000.0);
            item.nDefault = log2(item.nDefault / 10000.0);
        }

        dataToInfo((CAMERA_PROPERTY)i, &(item.nMax));
        dataToInfo((CAMERA_PROPERTY)i, &(item.nMin));
        dataToInfo((CAMERA_PROPERTY)i, &(item.nDefault));

        item.bValid = true;

        updateCameraProperty((CAMERA_PROPERTY)i);
    }

    for(int i = 0 ; i < CAMERA_PROPERTY_COUNT ; ++i){
        if(mCameraPropertyItems[i].bSupport && !mCameraPropertyItems[i].bValid)
            return ETronDI_Init_Fail;
    }

    return ETronDI_OK;
}

int CameraDeviceProperties::updateCameraProperty(CAMERA_PROPERTY type)    {
    int nID;
    bool bIsCTProperty;
    getCameraPropertyFlag(type, &nID, &bIsCTProperty);

    int ret;
    long int nValue;
    if(bIsCTProperty)    {
        ret = RETRY_ETRON_API(EtronDI_GetCTPropVal(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                   &(mCameraDevice->mDevSelInfo),
                                                   nID, &nValue));
        if(ret != ETronDI_OK)    {
            LOG_ERR(LOG_TAG, "Failed EtronDI_GetCTPropVal, ret = %d", ret);
        }
    } else   {
        ret = RETRY_ETRON_API(EtronDI_GetPUPropVal(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                   &(mCameraDevice->mDevSelInfo),
                                                   nID, &nValue));
        if(ret != ETronDI_OK)    {
            LOG_ERR(LOG_TAG, "Failed EtronDI_GetPUPropVal, ret = %d", ret);
        }
    }

    if(ETronDI_OK == ret){
        mCameraPropertyItems[type].nValue = nValue;
        dataToInfo(type, &(mCameraPropertyItems[type].nValue));
    }

    return ret;
}

int CameraDeviceProperties::setDefaultCameraProperties()    {
    int ret = ETronDI_OK;

    for(int i = 0 ; i < CAMERA_PROPERTY_COUNT ; ++i)    {
        bool bNeedRestoreAutoState = false;
        if(EXPOSURE_TIME == (CAMERA_PROPERTY)i)    {
            if(1 == mCameraPropertyItems[AUTO_EXPOSURE].nValue){
                setCameraPropertyValue(AUTO_EXPOSURE, 0);
                bNeedRestoreAutoState = true;
            }
        } else if(WHITE_BLANCE_TEMPERATURE == (CAMERA_PROPERTY)i)    {
            if(1 == mCameraPropertyItems[AUTO_WHITE_BLANCE].nValue)    {
                setCameraPropertyValue(AUTO_WHITE_BLANCE, 0);
                bNeedRestoreAutoState = true;
            }
        }

        ret = setCameraPropertyValue((CAMERA_PROPERTY)i, mCameraPropertyItems[i].nDefault);

        if(bNeedRestoreAutoState)    {
            if(EXPOSURE_TIME == (CAMERA_PROPERTY)i)    {
                setCameraPropertyValue(AUTO_EXPOSURE, 1);
            } else if(WHITE_BLANCE_TEMPERATURE == (CAMERA_PROPERTY)i)    {
                setCameraPropertyValue(AUTO_WHITE_BLANCE, 1);
            }
        }
    }

    return ret;
}

int CameraDeviceProperties::setCameraPropertyValue(CAMERA_PROPERTY type, int32_t nValue)    {
    if (!mCameraPropertyItems[type].bSupport || !mCameraPropertyItems[type].bValid){
        return ETronDI_NotSupport;
    }

    int nID;
    bool bIsCTProperty;
    getCameraPropertyFlag(type, &nID, &bIsCTProperty);
    infoToData(type, &nValue);

    int ret;

    if(bIsCTProperty)    {
        ret = RETRY_ETRON_API(EtronDI_SetCTPropVal(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                   &(mCameraDevice->mDevSelInfo),
                                                   nID, nValue));
        /*if(ret != ETronDI_OK)    {
            LOG_ERR(LOG_TAG, "Failed EtronDI_SetCTPropVal, ret = %d", ret);
        }*/
    } else    {
        ret = RETRY_ETRON_API(EtronDI_SetPUPropVal(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                   &(mCameraDevice->mDevSelInfo),
                                                   nID, nValue));
        /*if(ret != ETronDI_OK)    {
            LOG_ERR(LOG_TAG, "Failed EtronDI_SetPUPropVal, ret = %d", ret);
        }*/
    }

    if(AUTO_EXPOSURE == type)    {
        ret = setCameraPropertyValue(EXPOSURE_TIME, mCameraPropertyItems[EXPOSURE_TIME].nValue);
    } else if (AUTO_WHITE_BLANCE == type){
        ret = setCameraPropertyValue(WHITE_BLANCE_TEMPERATURE, mCameraPropertyItems[WHITE_BLANCE_TEMPERATURE].nValue);
    }

    updateCameraProperty(type);

    return ret;
}

int CameraDeviceProperties::setCameraPropertySupport(CAMERA_PROPERTY type, bool bSupport)    {
    mCameraPropertyItems[type].bSupport = bSupport;
    
    return ETronDI_OK;
}

void CameraDeviceProperties::getCameraPropertyFlag(CAMERA_PROPERTY type, int32_t *nID, bool *bIsCTProperty)    {
    switch(type)    {
        case AUTO_EXPOSURE:
            *nID = CT_PROPERTY_ID_AUTO_EXPOSURE_MODE_CTRL;
            *bIsCTProperty = true;
            break;
        case AUTO_WHITE_BLANCE:
            *nID = PU_PROPERTY_ID_WHITE_BALANCE_AUTO_CTRL;
            *bIsCTProperty = false;
            break;
        case LOW_LIGHT_COMPENSATION:
            *nID = CT_PROPERTY_ID_AUTO_EXPOSURE_PRIORITY_CTRL;
            *bIsCTProperty = true;
            break;
        case LIGHT_SOURCE:
            *nID = PU_PROPERTY_ID_POWER_LINE_FREQUENCY_CTRL;
            *bIsCTProperty = false;
            break;
        case EXPOSURE_TIME:
            *nID = CT_PROPERTY_ID_EXPOSURE_TIME_ABSOLUTE_CTRL;
            *bIsCTProperty = true;
            break;
        case WHITE_BLANCE_TEMPERATURE:
            *nID = PU_PROPERTY_ID_WHITE_BALANCE_CTRL;
            *bIsCTProperty = false;
            break;
        default:
            break;
    }
}

void CameraDeviceProperties::dataToInfo(CAMERA_PROPERTY type, int32_t *nValue)    {
    switch(type)   {
        case AUTO_EXPOSURE:
            *nValue = (*nValue == 3) ? 1 : 0;
            break;
        case LIGHT_SOURCE:
            *nValue = (1 == *nValue) ? VALUE_50HZ : VALUE_60HZ;
            break;
        case AUTO_WHITE_BLANCE:
        case LOW_LIGHT_COMPENSATION:
        case EXPOSURE_TIME:
        case WHITE_BLANCE_TEMPERATURE:
        default:
            break;
    }
}

void CameraDeviceProperties::infoToData(CAMERA_PROPERTY type, int32_t *nValue)    {
    switch(type){
        case AUTO_EXPOSURE:
            *nValue = (1 == *nValue) ? 3 : 1;
            break;
        case LIGHT_SOURCE:
            *nValue = (VALUE_50HZ == *nValue) ? 1 : 2;
            break;
        case AUTO_WHITE_BLANCE:
        case LOW_LIGHT_COMPENSATION:
        case EXPOSURE_TIME:
        case WHITE_BLANCE_TEMPERATURE:
        default:
            break;
    }
}

float CameraDeviceProperties::getManuelExposureTimeMs()    {
    float fExposureTimeMs = 0.0f;
    int ret;
    
    ret = RETRY_ETRON_API(EtronDI_GetExposureTime(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                  &(mCameraDevice->mDevSelInfo),
                                                  SENSOR_BOTH,
                                                  &fExposureTimeMs));
    if(ret != ETronDI_OK)    {
        LOG_ERR(LOG_TAG, "Failed EtronDI_GetExposureTime, ret = %d", ret);
    }

    return fExposureTimeMs;
}

void CameraDeviceProperties::setManuelExposureTimeMs(float fMs)    {
    int ret;
    
    ret = RETRY_ETRON_API(EtronDI_SetExposureTime(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                  &(mCameraDevice->mDevSelInfo),
                                                  SENSOR_BOTH,
                                                  fMs));

    if(ret != ETronDI_OK)    {
        LOG_ERR(LOG_TAG, "Failed EtronDI_SetExposureTime, ret = %d", ret);
    }
}

float CameraDeviceProperties::getManuelGlobalGain()    {
    float fGlobalGain = 0.0f;
    int ret;
    ret = RETRY_ETRON_API(EtronDI_GetGlobalGain(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                &(mCameraDevice->mDevSelInfo),
                                                SENSOR_BOTH,
                                                &fGlobalGain));
                                                
    if(ret != ETronDI_OK)    {
        LOG_ERR(LOG_TAG, "Failed EtronDI_GetGlobalGain, ret = %d", ret);
    }

    return fGlobalGain;
}

void CameraDeviceProperties::setManuelGlobalGain(float fGlobalGain)    {
    int ret;
    ret = RETRY_ETRON_API(EtronDI_SetGlobalGain(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                &(mCameraDevice->mDevSelInfo),
                                                SENSOR_BOTH,
                                                fGlobalGain));

    if(ret != ETronDI_OK)    {
        LOG_ERR(LOG_TAG, "Failed EtronDI_SetGlobalGain, ret = %d", ret);
    }
}

void CameraDeviceProperties::setDeviceName(const char *deviceName)    {
    strncpy((char *)mDeviceName, deviceName, PATH_MAX);
}

std::string CameraDeviceProperties::getDeviceName()    {
    std::string value(mDeviceName);
    
    return value;
}

CameraDeviceProperties::CameraPropertyItem CameraDeviceProperties::getCameraProperty(CAMERA_PROPERTY type)    {
    return mCameraPropertyItems[type];
}

static const char *get_camera_property_name(CameraDeviceProperties::CAMERA_PROPERTY property)    {
    switch(property)    {
        case CameraDeviceProperties::CAMERA_PROPERTY::AUTO_EXPOSURE:
            return "AUTO_EXPOSURE";
        case CameraDeviceProperties::CAMERA_PROPERTY::AUTO_WHITE_BLANCE:
            return "AUTO_WHITE_BLANCE";
        case CameraDeviceProperties::CAMERA_PROPERTY::LOW_LIGHT_COMPENSATION:
            return "LOW_LIGHT_COMPENSATION";
        case CameraDeviceProperties::CAMERA_PROPERTY::LIGHT_SOURCE:
            return "LIGHT_SOURCE";
        case CameraDeviceProperties::CAMERA_PROPERTY::EXPOSURE_TIME:
            return "EXPOSURE_TIME";
        case CameraDeviceProperties::CAMERA_PROPERTY::WHITE_BLANCE_TEMPERATURE:
            return "WHITE_BLANCE_TEMPERATURE";
        default:
            return "unknown";
    };
}

int CameraDeviceProperties::toString(char *buffer, int bufferLength) const    {
    char *bufferIndex = buffer;
    int length = 0;
    
    length += snprintf(buffer + length, (size_t)(bufferLength),
                       "---- CameraDeviceProperties: %s(%p) ----", mDeviceName, this);
    for(int i = 0 ; i < CAMERA_PROPERTY_COUNT; i++)    {
        
        if(length > bufferLength)    break;
        
        const CameraPropertyItem *item = &mCameraPropertyItems[i];
        length += snprintf(buffer + length, (size_t)(bufferLength - length),
                           "\n    %s:\n"
                           "        bSupport: %s\n"
                           "        bValid:   %s\n"
                           "        nValue:   %" PRId32 "\n"
                           "        nMin:     %" PRId32 "\n"
                           "        nMax:     %" PRId32 "\n"
                           "        nDefault: %" PRId32 "\n"
                           "        nFlags:   %" PRId32 "\n"
                           "        nStep:    %" PRId32 "",
                           get_camera_property_name((CAMERA_PROPERTY)i),
                           item->bSupport ? "true" : "false",
                           item->bValid ? "true" : "false",
                           item->nValue, item->nMin, item->nMax,
                           item->nDefault, item->nFlags, item->nStep);
    }
    
    return (length > bufferLength) ? bufferLength : length;
}

int CameraDeviceProperties::toString(std::string &string) const    {
    int ret = 0;
    char buffer[2048];
    
    ret = toString(buffer, sizeof(buffer));
    string.append(buffer);
    
    return ret;
}

} // end of namespace devices
} // end of namespace libeYs3D
