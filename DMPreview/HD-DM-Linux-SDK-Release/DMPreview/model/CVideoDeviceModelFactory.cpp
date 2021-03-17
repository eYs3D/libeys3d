#include "CVideoDeviceModelFactory.h"
#include "eSPDI.h"
#include "CEtronDeviceManager.h"
#include "CVideoDeviceModel_8029.h"
#include "CVideoDeviceModel_8053_8059.h"
#include "CVideoDeviceModel_8053.h"
#include "CVideoDeviceModel_8037.h"
#include "CVideoDeviceModel_8040_8054.h"
#include "CVideoDeviceModel_8054.h"
#include "CVideoDeviceModel_8040.h"
#include "CVideoDeviceModel_8038.h"
#include "CVideoDeviceModel_8060.h"
#include "CVideoDeviceModel_8051.h"
#include "CVideoDeviceModel_8052.h"
#include "CVideoDeviceModel_8036_8052.h"
#include "CVideoDeviceModel_Hypatia.h"
#include "CVideoDeviceModel_8062.h"
#include "CVideoDeviceModel_8036.h"
#include "CVideoDeviceModel_Grap.h"

CVideoDeviceModel *CVideoDeviceModelFactory::CreateVideoDeviceModel(DEVSELINFO *pDeviceSelfInfo)
{
    DEVINFORMATION deviceInfomation;
    if (ETronDI_OK != EtronDI_GetDeviceInfo(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                           pDeviceSelfInfo, &deviceInfomation)){
        return nullptr;
    }

    CVideoDeviceModel *pModel = nullptr;
    switch (deviceInfomation.wPID){
        case ETronDI_PID_8029:
            pModel = new CVideoDeviceModel_8029(pDeviceSelfInfo); break;
        case ETronDI_PID_8036:
            pModel = new CVideoDeviceModel_8036(pDeviceSelfInfo); break;
        case ETronDI_PID_8052:
            pModel = new CVideoDeviceModel_8052(pDeviceSelfInfo); break;
        case ETronDI_PID_8037:
            pModel = new CVideoDeviceModel_8037(pDeviceSelfInfo); break;
        case ETronDI_PID_8053:
            pModel = new CVideoDeviceModel_8053(pDeviceSelfInfo); break;
        case ETronDI_PID_8059:
            pModel = new CVideoDeviceModel_8053_8059(pDeviceSelfInfo); break;
        case ETronDI_PID_8040S:
            pModel = new CVideoDeviceModel_8040(pDeviceSelfInfo); break;
        case ETronDI_PID_8054:
            pModel = new CVideoDeviceModel_8054(pDeviceSelfInfo); break;
        case ETronDI_PID_8038:
            pModel = new CVideoDeviceModel_8038(pDeviceSelfInfo); break;
        case ETronDI_PID_8051:
            pModel = new CVideoDeviceModel_8051(pDeviceSelfInfo); break;
        case ETronDI_PID_8060:
            pModel = new CVideoDeviceModel_8060(pDeviceSelfInfo); break;
        case ETronDI_PID_8062:
            pModel = new CVideoDeviceModel_8062(pDeviceSelfInfo); break;
        case ETronDI_PID_GRAP:
            pModel = new CVideoDeviceModel_Grap(pDeviceSelfInfo); break;
        case ETronDI_PID_HYPATIA:
            pModel = new CVideoDeviceModel_Hypatia(pDeviceSelfInfo); break;
        case ETronDI_PID_8040S_K:
        case ETronDI_PID_8054_K:
        case ETronDI_PID_8060_K:
        case ETronDI_PID_8060_T:
        case ETronDI_PID_GRAP_SLAVE:
        case ETronDI_PID_GRAP_K:
        case ETronDI_PID_GRAP_SLAVE_K:
        case ETronDI_PID_8038_M1: break;

        default:
            pModel = new CVideoDeviceModel(pDeviceSelfInfo); break;
    }

    if (pModel) pModel->Init();

    return pModel;
}

void CVideoDeviceModelFactory::ReleaseModels(std::vector<CVideoDeviceModel *> models)
{
    for (CVideoDeviceModel *pModel : models) delete pModel;
}
