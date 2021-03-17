#include "CVideoDeviceModel_Hypatia.h"
#include "CVideoDeviceController.h"

CVideoDeviceModel_Hypatia::CVideoDeviceModel_Hypatia(DEVSELINFO *pDeviceSelfInfo):
CVideoDeviceModel(pDeviceSelfInfo)
{

}

void CVideoDeviceModel_Hypatia::SetVideoDeviceController(CVideoDeviceController *pVideoDeviceController)
{
    CVideoDeviceModel::SetVideoDeviceController(pVideoDeviceController);
    if(m_pVideoDeviceController){
        m_pVideoDeviceController->GetPreviewOptions()->SetIRLevel(48);
    }
}
