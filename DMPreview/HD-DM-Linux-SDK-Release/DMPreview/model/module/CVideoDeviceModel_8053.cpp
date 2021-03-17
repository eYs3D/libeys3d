#include "CVideoDeviceModel_8053.h"
#include "CVideoDeviceController.h"
#include "RegisterSettings.h"
#include "CEtronDeviceManager.h"

CVideoDeviceModel_8053::CVideoDeviceModel_8053(DEVSELINFO *pDeviceSelfInfo):
CVideoDeviceModel_8053_8059(pDeviceSelfInfo)
{

}

int CVideoDeviceModel_8053::AdjustZDTableIndex(int &nIndex)
{
    if (!m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH)){
        return ETronDI_OK;
    }

    std::vector<ETRONDI_STREAM_INFO> streamInfo = GetStreamInfoList(STREAM_DEPTH);
    int nDepthIndex = m_pVideoDeviceController->GetPreviewOptions()->GetStreamIndex(STREAM_DEPTH);

    if (720 == streamInfo[nDepthIndex].nHeight &&
        1280 == streamInfo[nDepthIndex].nWidth &&
        EtronDIImageType::DEPTH_8BITS == GetDepthImageType()){
        nIndex = 0;
    }

    return ETronDI_OK;
}

int CVideoDeviceModel_8053::AdjustRegister()
{
    if (EtronDIImageType::DEPTH_8BITS == GetDepthImageType() &&
        m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR) &&
        m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH)){

        std::vector<ETRONDI_STREAM_INFO> colorStreamInfo = GetStreamInfoList(STREAM_COLOR);
        int nColorIndex = m_pVideoDeviceController->GetPreviewOptions()->GetStreamIndex(STREAM_COLOR);

        std::vector<ETRONDI_STREAM_INFO> depthStreamInfo = GetStreamInfoList(STREAM_DEPTH);
        int nDepthIndex = m_pVideoDeviceController->GetPreviewOptions()->GetStreamIndex(STREAM_DEPTH);

        if(1280 == colorStreamInfo[nColorIndex].nWidth &&
           720 == colorStreamInfo[nColorIndex].nHeight &&
           640 == depthStreamInfo[nDepthIndex].nWidth &&
           360 == depthStreamInfo[nDepthIndex].nHeight ){
            RegisterSettings::ForEx8053Mode9(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                             m_deviceSelInfo[0]);
        }
    }

    return CVideoDeviceModel_8053_8059::AdjustRegister();
}
