#include "CVideoDeviceModel_Grap.h"
#include "CVideoDeviceController.h"
#include "CEtronDeviceManager.h"

CVideoDeviceModel_Grap::CVideoDeviceModel_Grap(DEVSELINFO *pDeviceSelfInfo):
CVideoDeviceModel(pDeviceSelfInfo)
{

}

int CVideoDeviceModel_Grap::InitDeviceSelInfo()
{
    CVideoDeviceModel::InitDeviceSelInfo();

    if(m_deviceSelInfo.empty()) return ETronDI_NullPtr;    

    for (int i = 1 ; i <= 3 ; ++i){
        DEVSELINFO *pDevSelfInfo = new DEVSELINFO;
        pDevSelfInfo->index = m_deviceSelInfo[0]->index + i;
        m_deviceSelInfo.push_back(pDevSelfInfo);
    }

    return ETronDI_OK;
}

int CVideoDeviceModel_Grap::InitDeviceInformation()
{
    SetPlumAR0330(false);
    CVideoDeviceModel::InitDeviceInformation();
    m_deviceInfo.push_back(GetDeviceInformation(m_deviceSelInfo[1]));
    if(ETronDI_OK == SetPlumAR0330(true)){
        m_deviceInfo.push_back(GetDeviceInformation(m_deviceSelInfo[0]));
        SetPlumAR0330(false);
    }

    m_deviceInfo.push_back(GetDeviceInformation(m_deviceSelInfo[2]));
    m_deviceInfo.push_back(GetDeviceInformation(m_deviceSelInfo[3]));
    if(ETronDI_OK == SetPlumAR0330(true)){
        m_deviceInfo.push_back(GetDeviceInformation(m_deviceSelInfo[2]));
        SetPlumAR0330(false);
    }

    return ETronDI_OK;
}

bool CVideoDeviceModel_Grap::IsStreamSupport(STREAM_TYPE type)
{
    switch (type){
        case STREAM_COLOR:
        case STREAM_COLOR_SLAVE:
        case STREAM_KOLOR:
        case STREAM_KOLOR_SLAVE:
            return true;
        default:
            return false;
    }
}

int CVideoDeviceModel_Grap::InitStreamInfoList()
{
    auto AddStreamInfoList = [&](DEVSELINFO *pDevSelInfo, STREAM_TYPE type) -> int
    {
        m_streamInfo[type].resize(MAX_STREAM_INFO_COUNT, {0, 0, false});
        int ret;
        RETRY_ETRON_API(ret, EtronDI_GetDeviceResolutionList(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                             pDevSelInfo,
                                                             MAX_STREAM_INFO_COUNT, &m_streamInfo[type][0],
                                                             MAX_STREAM_INFO_COUNT, nullptr));

        if (ETronDI_OK != ret) return ret;

        auto it = m_streamInfo[type].begin();
        for ( ; it != m_streamInfo[type].end() ; ++it){
            if (0 == (*it).nWidth){
                break;
            }
        }
        m_streamInfo[type].erase(it, m_streamInfo[type].end());
        m_streamInfo[type].shrink_to_fit();

        return ret;
    };

    AddStreamInfoList(m_deviceSelInfo[0], STREAM_COLOR);    
    AddStreamInfoList(m_deviceSelInfo[2], STREAM_COLOR_SLAVE);
    AddStreamInfoList(m_deviceSelInfo[1], STREAM_KOLOR);
    AddStreamInfoList(m_deviceSelInfo[3], STREAM_KOLOR_SLAVE);

    return ETronDI_OK;
}

bool CVideoDeviceModel_Grap::IsStreamAvailable()
{
    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    bool bKolorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_KOLOR);

    return bColorStream || bKolorStream;
}

int CVideoDeviceModel_Grap::PrepareOpenDevice()
{
    auto PrepareImageData = [&](STREAM_TYPE type){
        bool bStreamEnable = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(type);
        if(bStreamEnable){
            std::vector<ETRONDI_STREAM_INFO> streamInfo = GetStreamInfoList(type);
            int index = m_pVideoDeviceController->GetPreviewOptions()->GetStreamIndex(type);
            m_imageData[type].nWidth = streamInfo[index].nWidth;
            m_imageData[type].nHeight  = streamInfo[index].nHeight;
            m_imageData[type].bMJPG = streamInfo[index].bFormatMJPG;
            m_imageData[type].depthDataType = GetDepthDataType();
            m_imageData[type].imageDataType = m_imageData[type].bMJPG ?
                                              EtronDIImageType::COLOR_MJPG :
                                              EtronDIImageType::COLOR_YUY2;

            unsigned short nBytePerPixel = 2;
            unsigned int nBufferSize = m_imageData[type].nWidth * m_imageData[type].nHeight * nBytePerPixel;
            if (m_imageData[type].imageBuffer.size() != nBufferSize){
                m_imageData[type].imageBuffer.resize(nBufferSize);
            }
            memset(&m_imageData[type].imageBuffer[0], 0, sizeof(nBufferSize));
        }
    };

    PrepareImageData(STREAM_COLOR);
    m_imageData[STREAM_COLOR_SLAVE] = m_imageData[STREAM_COLOR];

    PrepareImageData(STREAM_KOLOR);
    m_imageData[STREAM_KOLOR_SLAVE] = m_imageData[STREAM_KOLOR];

    return ETronDI_OK;
}

int CVideoDeviceModel_Grap::OpenDevice()
{
    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    if(bColorStream)
    {
        int nFPS = m_pVideoDeviceController->GetPreviewOptions()->GetStreamFPS(STREAM_COLOR);
        if(ETronDI_OK != EtronDI_OpenDevice2(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                             m_deviceSelInfo[0],
                                             m_imageData[STREAM_COLOR].nWidth, m_imageData[STREAM_COLOR].nHeight, m_imageData[STREAM_COLOR].bMJPG,
                                             0, 0,
                                             DEPTH_IMG_NON_TRANSFER,
                                             true, nullptr,
                                             &nFPS,
                                             IMAGE_SN_SYNC)){
            return ETronDI_OPEN_DEVICE_FAIL;
        }

        nFPS = m_pVideoDeviceController->GetPreviewOptions()->GetStreamFPS(STREAM_COLOR);
        if(ETronDI_OK != EtronDI_OpenDevice2(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                             m_deviceSelInfo[2],
                                             m_imageData[STREAM_COLOR_SLAVE].nWidth, m_imageData[STREAM_COLOR_SLAVE].nHeight, m_imageData[STREAM_COLOR_SLAVE].bMJPG,
                                             0, 0,
                                             DEPTH_IMG_NON_TRANSFER,
                                             true, nullptr,
                                             &nFPS,
                                             IMAGE_SN_SYNC)){
            return ETronDI_OPEN_DEVICE_FAIL;
        }

        m_pVideoDeviceController->GetPreviewOptions()->SetStreamFPS(STREAM_COLOR, nFPS);
    }

    bool bKolorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_KOLOR);
    if(bKolorStream)
    {
        int nFPS = m_pVideoDeviceController->GetPreviewOptions()->GetStreamFPS(STREAM_KOLOR);
        if(ETronDI_OK != EtronDI_OpenDevice2(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                             m_deviceSelInfo[1],
                                             m_imageData[STREAM_KOLOR].nWidth, m_imageData[STREAM_KOLOR].nHeight, m_imageData[STREAM_KOLOR].bMJPG,
                                             0, 0,
                                             DEPTH_IMG_NON_TRANSFER,
                                             true, nullptr,
                                             &nFPS,
                                             IMAGE_SN_SYNC)){
            return ETronDI_OPEN_DEVICE_FAIL;
        }

        nFPS = m_pVideoDeviceController->GetPreviewOptions()->GetStreamFPS(STREAM_KOLOR);
        if(ETronDI_OK != EtronDI_OpenDevice2(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                             m_deviceSelInfo[3],
                                             m_imageData[STREAM_KOLOR_SLAVE].nWidth, m_imageData[STREAM_KOLOR_SLAVE].nHeight, m_imageData[STREAM_KOLOR_SLAVE].bMJPG,
                                             0, 0,
                                             DEPTH_IMG_NON_TRANSFER,
                                             true, nullptr,
                                             &nFPS,
                                             IMAGE_SN_SYNC)){
            return ETronDI_OPEN_DEVICE_FAIL;
        }
        m_pVideoDeviceController->GetPreviewOptions()->SetStreamFPS(STREAM_KOLOR, nFPS);
    }

    return ETronDI_OK;
}

int CVideoDeviceModel_Grap::StartStreamingTask()
{
    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    if (bColorStream){
        CreateStreamTask(STREAM_COLOR);
        CreateStreamTask(STREAM_COLOR_SLAVE);
    }

    bool bKolorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_KOLOR);
    if (bKolorStream){
        CreateStreamTask(STREAM_KOLOR);
        CreateStreamTask(STREAM_KOLOR_SLAVE);
    }

    return ETronDI_OK;
}

int CVideoDeviceModel_Grap::CloseDevice()
{
    int ret;

    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    if(bColorStream){
        if(ETronDI_OK == EtronDI_CloseDevice(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                             m_deviceSelInfo[0])){
            ret = ETronDI_OK;
        }

        if(ETronDI_OK == EtronDI_CloseDevice(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                             m_deviceSelInfo[2])){
            ret = ETronDI_OK;
        }
    }

    bool bKolorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_KOLOR);
    if(bKolorStream){
        if(ETronDI_OK == EtronDI_CloseDevice(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                             m_deviceSelInfo[1])){
            ret = ETronDI_OK;
        }

        if(ETronDI_OK == EtronDI_CloseDevice(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                             m_deviceSelInfo[3])){
            ret = ETronDI_OK;
        }
    }

    return ret;
}

int CVideoDeviceModel_Grap::ClosePreviewView()
{
    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    if (bColorStream){
        m_pVideoDeviceController->GetControlView()->ClosePreviewView(STREAM_COLOR);
        m_pVideoDeviceController->GetControlView()->ClosePreviewView(STREAM_COLOR_SLAVE);
    }

    bool bKolorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_KOLOR);
    if (bKolorStream){
        m_pVideoDeviceController->GetControlView()->ClosePreviewView(STREAM_KOLOR);
        m_pVideoDeviceController->GetControlView()->ClosePreviewView(STREAM_KOLOR_SLAVE);
    }

    return ETronDI_OK;
}

int CVideoDeviceModel_Grap::GetImage(STREAM_TYPE type)
{
    int ret;
    switch (type){
        case STREAM_COLOR:
        case STREAM_COLOR_SLAVE:
        case STREAM_KOLOR:
        case STREAM_KOLOR_SLAVE:
            ret = GetColorImage(type);
            break;
        default:
            return ETronDI_NotSupport;
    }

    return ret;
}

int CVideoDeviceModel_Grap::GetColorImage(STREAM_TYPE type)
{
    DEVSELINFO *deviceSelInfo;

    switch(type){
        case STREAM_COLOR: deviceSelInfo = m_deviceSelInfo[0]; break;
        case STREAM_COLOR_SLAVE: deviceSelInfo = m_deviceSelInfo[2]; break;
        case STREAM_KOLOR: deviceSelInfo = m_deviceSelInfo[1]; break;
        case STREAM_KOLOR_SLAVE: deviceSelInfo = m_deviceSelInfo[3]; break;
        default: return ETronDI_NotSupport;
    }

    unsigned long int nImageSize = 0;
    int nSerial = EOF;
    int ret = EtronDI_GetColorImage(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                    deviceSelInfo,
                                    &m_imageData[type].imageBuffer[0],
                                    &nImageSize,
                                    &nSerial,
                                    0);

    if (ETronDI_OK != ret) return ret;

    return ProcessImage(type, nImageSize, nSerial);
}
