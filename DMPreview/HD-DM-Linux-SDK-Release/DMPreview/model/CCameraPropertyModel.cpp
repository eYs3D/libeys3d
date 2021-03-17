#include "CCameraPropertyModel.h"
#include "CVideoDeviceModel.h"
#include "CEtronDeviceManager.h"
#include "eSPDI.h"
#include <math.h>

CCameraPropertyModel::CCameraPropertyModel(QString sDeviceName,
                                           CVideoDeviceModel *pVideoDeviceModel,
                                           DEVSELINFO *pDeviceSelfInfo):
m_sDeviceName(sDeviceName),
m_pVideoDeviceModel(pVideoDeviceModel),
m_pDeviceSelfInfo(pDeviceSelfInfo)
{

}

CCameraPropertyModel::~CCameraPropertyModel()
{

}

int CCameraPropertyModel::Init()
{
    InitCameraProperty();
    return ETronDI_OK;
}

int CCameraPropertyModel::Update()
{
    for (int i = 0 ; i < CAMERA_PROPERTY_COUNT ; ++i){
        UpdateCameraProperty((CAMERA_PROPERTY)i);
    }

    return ETronDI_OK;
}

int CCameraPropertyModel::Reset()
{
    Init();
    return ETronDI_OK;
}

int CCameraPropertyModel::InitCameraProperty()
{

    for (int i = 0 ; i < CAMERA_PROPERTY_COUNT ; i++){
        int nID;
        bool bIsCTProperty;
        GetCameraPropertyFlag((CAMERA_PROPERTY)i, nID, bIsCTProperty);

        CameraPropertyItem &item = m_cameraPropertyItems[i];
        if (!item.bSupport) continue;

        item.bValid = false;

        void *pEtron = CEtronDeviceManager::GetInstance()->GetEtronDI();
        int ret;
        if (bIsCTProperty){
            RETRY_ETRON_API(ret, EtronDI_GetCTRangeAndStep(pEtron, m_pDeviceSelfInfo, nID,
                                                           &item.nMax, &item.nMin, &item.nStep, &item.nDefault, &item.nFlags));
        }else{
            RETRY_ETRON_API(ret, EtronDI_GetPURangeAndStep(pEtron, m_pDeviceSelfInfo, nID,
                                                           &item.nMax, &item.nMin, &item.nStep, &item.nDefault, &item.nFlags));
        }

        if (ETronDI_OK != ret) continue;

        if(EXPOSURE_TIME == (CAMERA_PROPERTY)i){
            item.nMax = log2(item.nMax / 10000.0);
            item.nMin = log2(item.nMin  / 10000.0);
            item.nDefault = log2(item.nDefault / 10000.0);
        }

        DataToInfo((CAMERA_PROPERTY)i, item.nMax);
        DataToInfo((CAMERA_PROPERTY)i, item.nMin);
        DataToInfo((CAMERA_PROPERTY)i, item.nDefault);

        item.bValid = true;

        UpdateCameraProperty((CAMERA_PROPERTY)i);
    }

    for (int i = 0 ; i < CAMERA_PROPERTY_COUNT ; ++i){
        if(m_cameraPropertyItems[i].bSupport && !m_cameraPropertyItems[i].bValid) return ETronDI_Init_Fail;
    }

    return ETronDI_OK;

}

int CCameraPropertyModel::UpdateCameraProperty(CAMERA_PROPERTY type)
{
    int nID;
    bool bIsCTProperty;
    GetCameraPropertyFlag(type, nID, bIsCTProperty);

    void *pEtron = CEtronDeviceManager::GetInstance()->GetEtronDI();
    int ret;
    long int nValue;
    if (bIsCTProperty){
        RETRY_ETRON_API(ret, EtronDI_GetCTPropVal(pEtron, m_pDeviceSelfInfo, nID,
                                                  &nValue));
    }else{
        RETRY_ETRON_API(ret, EtronDI_GetPUPropVal(pEtron, m_pDeviceSelfInfo, nID,
                                                  &nValue));
    }

    if(ETronDI_OK == ret){
        m_cameraPropertyItems[type].nValue = nValue;
        DataToInfo(type, m_cameraPropertyItems[type].nValue);
    }

    return ret;
}

int CCameraPropertyModel::SetDefaultCameraProperty()
{
    int ret = ETronDI_OK;

    for (int i = 0 ; i < CAMERA_PROPERTY_COUNT ; ++i){
        bool bNeedRestoreAutoState = false;
        if(EXPOSURE_TIME == (CAMERA_PROPERTY)i){
            if(1 == m_cameraPropertyItems[AUTO_EXPOSURE].nValue){
                SetCameraPropertyValue(AUTO_EXPOSURE, 0);
                bNeedRestoreAutoState = true;
            }
        }else if(WHITE_BLANCE_TEMPERATURE == (CAMERA_PROPERTY)i){
            if(1 == m_cameraPropertyItems[AUTO_WHITE_BLANCE].nValue){
                SetCameraPropertyValue(AUTO_WHITE_BLANCE, 0);
                bNeedRestoreAutoState = true;
            }
        }

        ret = SetCameraPropertyValue((CAMERA_PROPERTY)i, m_cameraPropertyItems[i].nDefault);

        if(bNeedRestoreAutoState){
            if(EXPOSURE_TIME == (CAMERA_PROPERTY)i){
                SetCameraPropertyValue(AUTO_EXPOSURE, 1);
            }else if(WHITE_BLANCE_TEMPERATURE == (CAMERA_PROPERTY)i){
                SetCameraPropertyValue(AUTO_WHITE_BLANCE, 1);
            }
        }
    }

    return ret;
}

int CCameraPropertyModel::SetCameraPropertyValue(CAMERA_PROPERTY type, int nValue)
{
    if (!m_cameraPropertyItems[type].bSupport || !m_cameraPropertyItems[type].bValid){
        return ETronDI_NotSupport;
    }

    int nID;
    bool bIsCTProperty;
    GetCameraPropertyFlag(type, nID, bIsCTProperty);
    InfoToData(type, nValue);

    void *pEtron = CEtronDeviceManager::GetInstance()->GetEtronDI();

    int ret;
    if (bIsCTProperty){
        RETRY_ETRON_API(ret, EtronDI_SetCTPropVal(pEtron, m_pDeviceSelfInfo, nID, nValue));
    }else{
        RETRY_ETRON_API(ret, EtronDI_SetPUPropVal(pEtron, m_pDeviceSelfInfo, nID, nValue));
    }

    if (AUTO_EXPOSURE == type){
        SetCameraPropertyValue(EXPOSURE_TIME, m_cameraPropertyItems[EXPOSURE_TIME].nValue);
    }else if (AUTO_WHITE_BLANCE == type){
        SetCameraPropertyValue(WHITE_BLANCE_TEMPERATURE, m_cameraPropertyItems[WHITE_BLANCE_TEMPERATURE].nValue);
    }

    UpdateCameraProperty(type);

    return ret;
}

void CCameraPropertyModel::GetCameraPropertyFlag(CAMERA_PROPERTY type, int &nID, bool &bIsCTProperty)
{
    switch (type){
        case AUTO_EXPOSURE:
            nID = CT_PROPERTY_ID_AUTO_EXPOSURE_MODE_CTRL;
            bIsCTProperty = true;
            break;
        case AUTO_WHITE_BLANCE:
            nID = PU_PROPERTY_ID_WHITE_BALANCE_AUTO_CTRL;
            bIsCTProperty = false;
            break;
        case LOW_LIGHT_COMPENSATION:
            nID = CT_PROPERTY_ID_AUTO_EXPOSURE_PRIORITY_CTRL;
            bIsCTProperty = true;
            break;
        case LIGHT_SOURCE:
            nID = PU_PROPERTY_ID_POWER_LINE_FREQUENCY_CTRL;
            bIsCTProperty = false;
            break;
        case EXPOSURE_TIME:
            nID = CT_PROPERTY_ID_EXPOSURE_TIME_ABSOLUTE_CTRL;
            bIsCTProperty = true;
            break;
        case WHITE_BLANCE_TEMPERATURE:
            nID = PU_PROPERTY_ID_WHITE_BALANCE_CTRL;
            bIsCTProperty = false;
            break;
        default:
            break;
    }
}

void CCameraPropertyModel::DataToInfo(CAMERA_PROPERTY type, int &nValue)
{
    switch(type){
        case AUTO_EXPOSURE:
            nValue = (nValue == 3) ? 1 : 0;
            break;
        case LIGHT_SOURCE:
            nValue = (1 == nValue) ? VALUE_50HZ : VALUE_60HZ;
            break;
        case AUTO_WHITE_BLANCE:
        case LOW_LIGHT_COMPENSATION:
        case EXPOSURE_TIME:
        case WHITE_BLANCE_TEMPERATURE:
        default:
            break;
    }
}

void CCameraPropertyModel::InfoToData(CAMERA_PROPERTY type, int &nValue)
{
    switch(type){
        case AUTO_EXPOSURE:
            nValue = (1 == nValue) ? 3 : 1;
            break;
        case LIGHT_SOURCE:
            nValue = (VALUE_50HZ == nValue) ? 1 : 2;
            break;
        case AUTO_WHITE_BLANCE:
        case LOW_LIGHT_COMPENSATION:
        case EXPOSURE_TIME:
        case WHITE_BLANCE_TEMPERATURE:
        default:
            break;
    }
}

float CCameraPropertyModel::GetManuelExposureTimeMs()
{
    float fExposureTimeMs = 0.0f;
    int ret;
    RETRY_ETRON_API(ret, EtronDI_GetExposureTime(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                 m_pDeviceSelfInfo,
                                                 SENSOR_BOTH,
                                                 &fExposureTimeMs));

    return fExposureTimeMs;
}

void CCameraPropertyModel::SetManuelExposureTimeMs(float fMs)
{
    int ret;
    RETRY_ETRON_API(ret, EtronDI_SetExposureTime(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                 m_pDeviceSelfInfo,
                                                 SENSOR_BOTH,
                                                 fMs));

}

float CCameraPropertyModel::GetManuelGlobalGain()
{
    float fGlobalGain = 0.0f;
    int ret;
    RETRY_ETRON_API(ret, EtronDI_GetGlobalGain(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                               m_pDeviceSelfInfo,
                                               SENSOR_BOTH,
                                               &fGlobalGain));

    return fGlobalGain;
}

void CCameraPropertyModel::SetManuelGlobalGain(float fGlobalGain)
{
    int ret;
    RETRY_ETRON_API(ret, EtronDI_SetGlobalGain(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                               m_pDeviceSelfInfo,
                                               SENSOR_BOTH,
                                               fGlobalGain));
}
