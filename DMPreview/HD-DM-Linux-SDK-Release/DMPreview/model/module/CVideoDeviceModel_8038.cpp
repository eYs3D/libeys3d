#include "CVideoDeviceModel_8038.h"
#include <QThread>
#include "RegisterSettings.h"
#include "CEtronDeviceManager.h"
#include "CVideoDeviceController.h"
#include "eSPDI.h"
#include "DepthFusionHelper.h"
#include "CImageDataModel.h"

CVideoDeviceModel_8038::CVideoDeviceModel_8038(DEVSELINFO *pDeviceSelfInfo):
CVideoDeviceModel(pDeviceSelfInfo),
CVideoDeviceModel_ColorWithDepth(pDeviceSelfInfo),
m_pDepthFusionHelper(nullptr)
{

}

CVideoDeviceModel_8038::~CVideoDeviceModel_8038()
{
    if (m_pDepthFusionHelper) delete m_pDepthFusionHelper;
}

int CVideoDeviceModel_8038::InitDeviceSelInfo()
{
    CVideoDeviceModel::InitDeviceSelInfo();

    if(m_deviceSelInfo.empty()) return ETronDI_NullPtr;

    DEVSELINFO *pDevSelfInfo = new DEVSELINFO;
    pDevSelfInfo->index = m_deviceSelInfo[0]->index + 1;
    m_deviceSelInfo.push_back(std::move(pDevSelfInfo));

    return ETronDI_OK;
}

int CVideoDeviceModel_8038::InitDeviceInformation()
{
    CVideoDeviceModel::InitDeviceInformation();

    m_deviceInfo.push_back(GetDeviceInformation(m_deviceSelInfo[1]));

    return ETronDI_OK;
}

void CVideoDeviceModel_8038::SetVideoDeviceController(CVideoDeviceController *pVideoDeviceController)
{
    CVideoDeviceModel_ColorWithDepth::SetVideoDeviceController(pVideoDeviceController);
    if(m_pVideoDeviceController){
        m_pVideoDeviceController->GetPreviewOptions()->SetZRange(0, 2000);
        m_pVideoDeviceController->GetPreviewOptions()->SetDefaultZRange(0, 2000);
    }
}

bool CVideoDeviceModel_8038::IsDepthDataTypeSupport(DEPTH_DATA_TYPE type)
{
    return DEPTH_DATA_11BIT == type;
}

int CVideoDeviceModel_8038::GetRectifyLogData(int nDevIndex, int nRectifyLogIndex, eSPCtrl_RectLogData *pRectifyLogData, STREAM_TYPE depthType)
{
    switch(depthType){
        case STREAM_DEPTH:
        case STREAM_DEPTH_30mm:
        case STREAM_DEPTH_FUSION:   nRectifyLogIndex = 0;   break;
        case STREAM_DEPTH_60mm:     nRectifyLogIndex = 1;   break;
        case STREAM_DEPTH_150mm:    nRectifyLogIndex = 2;   break;
        default: break;
    }

    return CVideoDeviceModel_ColorWithDepth::GetRectifyLogData(nDevIndex, nRectifyLogIndex, pRectifyLogData, depthType);
}

eSPCtrl_RectLogData &CVideoDeviceModel_8038::GetRectifyLogData(STREAM_TYPE depthType)
{
    switch(depthType){
        case STREAM_DEPTH:
        case STREAM_DEPTH_30mm:
        case STREAM_DEPTH_FUSION:   return m_rectifyLogData;
        case STREAM_DEPTH_60mm:     return m_rectifyLogData_60mm;
        case STREAM_DEPTH_150mm:    return m_rectifyLogData_150mm;
        default: return m_rectifyLogData;
    }
}

int CVideoDeviceModel_8038::UpdateZDTable()
{
    if (!m_pVideoDeviceController) return ETronDI_NullPtr;

    m_zdTableInfo.nZNear = EX8038_MIN_DEPTH_RANGE;

    int nZNear, nZFar;
    m_pVideoDeviceController->GetPreviewOptions()->GetZRange(nZNear, nZFar);
    m_pVideoDeviceController->GetPreviewOptions()->SetZRange(m_zdTableInfo.nZNear, nZFar);

    return ETronDI_OK;
}

int CVideoDeviceModel_8038::AdjustRegister()
{
    QThread::sleep(1);
    RegisterSettings::DM_Quality_Register_Setting_For6cm(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                             m_deviceSelInfo[0]);
    RegisterSettings::DM_Quality_Register_Setting_For15cm(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                             m_deviceSelInfo[1]);
    return CVideoDeviceModel_ColorWithDepth::AdjustRegister();
}

int CVideoDeviceModel_8038::PreparePointCloudInfo()
{
    GetRectifyLogData(0, m_pVideoDeviceController->GetPreviewOptions()->GetStreamIndex(STREAM_DEPTH), &m_rectifyLogData_60mm, STREAM_DEPTH_60mm);
    GetRectifyLogData(0, m_pVideoDeviceController->GetPreviewOptions()->GetStreamIndex(STREAM_DEPTH), &m_rectifyLogData_150mm, STREAM_DEPTH_150mm);
    return CVideoDeviceModel::PreparePointCloudInfo();
}

int CVideoDeviceModel_8038::PrepareOpenDevice()
{
    int ret;
    RETRY_ETRON_API(ret, EtronDI_SetControlCounterMode(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                       m_deviceSelInfo[0],
                                                       0x0));

    ret = CVideoDeviceModel_ColorWithDepth::PrepareOpenDevice();

    auto AssignDepthImageData = [&](ImageData &imageData){
        imageData.nWidth = m_imageData[STREAM_DEPTH].nWidth;
        imageData.nHeight = m_imageData[STREAM_DEPTH].nHeight;
        imageData.bMJPG = m_imageData[STREAM_DEPTH].bMJPG;
        imageData.depthDataType = m_imageData[STREAM_DEPTH].depthDataType;
        imageData.imageDataType = m_imageData[STREAM_DEPTH].imageDataType;

        unsigned short nBytePerPixel = 2;
        unsigned int nBufferSize = imageData.nWidth * imageData.nHeight * nBytePerPixel;
        if (imageData.imageBuffer.size() != nBufferSize){
            imageData.imageBuffer.resize(nBufferSize);
        }
        memset(&imageData.imageBuffer[0], 0, sizeof(nBufferSize));
    };

    bool bDepthStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH);
    if (bDepthStream){
        AssignDepthImageData(m_imageData[STREAM_DEPTH_30mm]);

        if(!m_pVideoDeviceController->GetPreviewOptions()->IsPointCloudViewer()){
            AssignDepthImageData(m_imageData[STREAM_DEPTH_60mm]);
            AssignDepthImageData(m_imageData[STREAM_DEPTH_150mm]);
            AssignDepthImageData(m_imageData[STREAM_DEPTH_FUSION]);

            if(m_pDepthFusionHelper) delete m_pDepthFusionHelper;
            std::vector<float> baselineDist = { 30.0f, 60.0f, 150.0f };
            m_pDepthFusionHelper = new CDepthFusionHelper(3,
                                                          m_imageData[STREAM_DEPTH_FUSION].nWidth, m_imageData[STREAM_DEPTH_FUSION].nHeight,
                                                          800.0 , baselineDist, 1,
                                                          CVideoDeviceModel_8038::DepthFusionCallbackFn, this);
            m_pDepthFusionHelper->SetSDKHandle(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                               m_deviceSelInfo[0]);
        }
    }

    return ret;
}

int CVideoDeviceModel_8038::OpenDevice()
{
    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    bool bDepthStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH);
    if(bColorStream || bDepthStream){
        int nFPS;
        if(bColorStream){
            nFPS = m_pVideoDeviceController->GetPreviewOptions()->GetStreamFPS(STREAM_COLOR);
        }else{
            nFPS = m_pVideoDeviceController->GetPreviewOptions()->GetStreamFPS(STREAM_DEPTH);
        }

        if(ETronDI_OK != EtronDI_OpenDeviceMBL(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                               m_deviceSelInfo[0],
                                               m_imageData[STREAM_COLOR].nWidth, m_imageData[STREAM_COLOR].nHeight,
                                               m_imageData[STREAM_COLOR].bMJPG,
                                               m_imageData[STREAM_DEPTH].nWidth, m_imageData[STREAM_DEPTH].nHeight,
                                               DEPTH_IMG_NON_TRANSFER,
                                               true, nullptr,
                                               &nFPS,
                                               IMAGE_SN_SYNC)){
            return ETronDI_OPEN_DEVICE_FAIL;
        }

        if(bColorStream){
            m_pVideoDeviceController->GetPreviewOptions()->SetStreamFPS(STREAM_COLOR, nFPS);
            if(bDepthStream) m_pVideoDeviceController->GetPreviewOptions()->SetStreamFPS(STREAM_DEPTH, nFPS);
        }else{
            m_pVideoDeviceController->GetPreviewOptions()->SetStreamFPS(STREAM_DEPTH, nFPS);
        }
    }
    return ETronDI_OK;
}

int CVideoDeviceModel_8038::StartStreamingTask()
{
    int ret = CVideoDeviceModel_ColorWithDepth::StartStreamingTask();

    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    bool bDepthStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH);

    if(bColorStream && bDepthStream){
        m_imageData[STREAM_COLOR].nWidth /= 2;
    }

    if (bDepthStream){
        CreateStreamTask(STREAM_DEPTH_60mm);
        CreateStreamTask(STREAM_DEPTH_150mm);
    }

    return ret;
}

int CVideoDeviceModel_8038::CloseDevice()
{
    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    bool bDepthStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH);

    if(bColorStream || bDepthStream){
        return EtronDI_CloseDeviceMBL(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                      m_deviceSelInfo[0]);
    }

    return ETronDI_OK;
}

int CVideoDeviceModel_8038::ClosePreviewView()
{
    CVideoDeviceModel::ClosePreviewView();

    bool bDepthStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH);
    if (bDepthStream){
        m_pVideoDeviceController->GetControlView()->ClosePreviewView(STREAM_DEPTH_30mm);
        m_pVideoDeviceController->GetControlView()->ClosePreviewView(STREAM_DEPTH_60mm);
        m_pVideoDeviceController->GetControlView()->ClosePreviewView(STREAM_DEPTH_150mm);
        m_pVideoDeviceController->GetControlView()->ClosePreviewView(STREAM_DEPTH_FUSION);
    }

    return ETronDI_OK;
}

int CVideoDeviceModel_8038::StopStreamingTask()
{
    if(m_pDepthFusionHelper){
        delete m_pDepthFusionHelper;
        m_pDepthFusionHelper = nullptr;
    }

    return CVideoDeviceModel_ColorWithDepth::StopStreamingTask();
}

int CVideoDeviceModel_8038::GetImage(STREAM_TYPE type)
{
    int ret;
    switch (type){
        case STREAM_COLOR:
        case STREAM_DEPTH:
            ret = GetColorImage();
            break;
        case STREAM_DEPTH_60mm:
        case STREAM_DEPTH_150mm:
            ret = GetMultiBaselineDepthImage(type);
            break;
        default:
            return ETronDI_NotSupport;
    }

    return ret;
}

int CVideoDeviceModel_8038::GetColorImage()
{
    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    bool bDepthStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH);
    int nDepthDataType = bColorStream ? m_imageData[STREAM_COLOR].depthDataType :
                                        m_imageData[STREAM_DEPTH_30mm].depthDataType;

    std::vector<BYTE> &buffer = bColorStream && bDepthStream ? m_colorWithDepthImageData.imageBuffer :
                                bDepthStream ? m_imageData[STREAM_DEPTH_30mm].imageBuffer :
                                               m_imageData[STREAM_COLOR].imageBuffer;

    unsigned long int nImageSize = 0;
    int nSerial = EOF;

    int ret = EtronDI_Get_Color_30_mm_depth(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                            m_deviceSelInfo[0],
                                            &buffer[0],
                                            &nImageSize,
                                            &nSerial,
                                            nDepthDataType);

    if (ETronDI_OK != ret) return ret;

    if(bColorStream && bDepthStream){
        unsigned short nHalfWidth = m_colorWithDepthImageData.nWidth / 2;
        unsigned short nRowBytes = nHalfWidth * 2;
        for (int i = 0 ; i < m_colorWithDepthImageData.nHeight ; i++) {
            memcpy(&m_imageData[STREAM_COLOR].imageBuffer[i * nRowBytes], &m_colorWithDepthImageData.imageBuffer[nRowBytes * ((2 * i))], nRowBytes);
            memcpy(&m_imageData[STREAM_DEPTH_30mm].imageBuffer[i * nRowBytes], &m_colorWithDepthImageData.imageBuffer[nRowBytes * ((2 * i) + 1)], nRowBytes);
        }

        if(m_pDepthFusionHelper){
            if(!m_imageData[STREAM_COLOR].bMJPG){
                m_pDepthFusionHelper->UpdateColorData(nSerial,  &m_imageData[STREAM_COLOR].imageBuffer[0], nImageSize / 2);
            }

            m_pDepthFusionHelper->UpdateDepthData(0, nSerial,  &m_imageData[STREAM_DEPTH_30mm].imageBuffer[0], nImageSize / 2);
        }

        ProcessImage(STREAM_COLOR, nImageSize / 2, nSerial);        
        ProcessImage(STREAM_DEPTH_30mm, nImageSize / 2, nSerial);

        return ETronDI_OK;
    }else if (bColorStream){
        return ProcessImage(STREAM_COLOR, nImageSize, nSerial);
    }else{
        return ProcessImage(STREAM_DEPTH_30mm, nImageSize, nSerial);
    }
}

int CVideoDeviceModel_8038::GetMultiBaselineDepthImage(STREAM_TYPE type)
{
    int ret;
    unsigned long int nImageSize = 0;
    int nSerial = EOF;

    switch (type){
        case STREAM_DEPTH_60mm:
            ret = EtronDI_Get_60_mm_depth(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                          m_deviceSelInfo[0],
                                          &m_imageData[type].imageBuffer[0],
                                          &nImageSize,
                                          &nSerial,
                                          m_imageData[type].depthDataType);
            break;
        case STREAM_DEPTH_150mm:
            ret = EtronDI_Get_150_mm_depth(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                           m_deviceSelInfo[0],
                                           &m_imageData[type].imageBuffer[0],
                                           &nImageSize,
                                           &nSerial,
                                           m_imageData[type].depthDataType);
            break;
        default: return ETronDI_NotSupport;
    }

    if (ETronDI_OK != ret) return ret;

    if(m_pDepthFusionHelper){
        switch (type){
            case STREAM_DEPTH_60mm:
                m_pDepthFusionHelper->UpdateDepthData(1, nSerial,  &m_imageData[STREAM_DEPTH_60mm].imageBuffer[0], nImageSize);
                break;
            case STREAM_DEPTH_150mm:
                m_pDepthFusionHelper->UpdateDepthData(2, nSerial,  &m_imageData[STREAM_DEPTH_150mm].imageBuffer[0], nImageSize);
                break;
            default: return ETronDI_NotSupport;
        }
    }

    return ProcessImage(type, nImageSize, nSerial);
}

int CVideoDeviceModel_8038::ProcessImageCallback(STREAM_TYPE streamType,
                         int nImageSize, int nSerialNumber)
{
    if (STREAM_DEPTH_FUSION == streamType){
        CEtronUIView *pView = m_pVideoDeviceController->GetControlView();
        if (!pView) return ETronDI_OK;

        return pView->ImageCallback(m_imageData[streamType].imageDataType, streamType,
                                    &m_imageData[streamType].imageBuffer[0],
                                    nImageSize,
                                    m_imageData[streamType].nWidth, m_imageData[streamType].nHeight,
                                    nSerialNumber, &m_depthFusionBuffer[0]);
    }

    return CVideoDeviceModel_ColorWithDepth::ProcessImageCallback(streamType, nImageSize, nSerialNumber);
}

void CVideoDeviceModel_8038::ProcessDepthFusion(unsigned char* depthBuf, unsigned char* selectedIndex,int depthSize, int width, int height, int serialNumber)
{
    const int nPixelCount = width * height;
    const int nImageSize = depthSize;
    const double dblMultiplier[] = { 30.0 / 30.0, 60.0 / 30.0, 150.0 / 30.0 };

    memcpy(&m_imageData[STREAM_DEPTH_FUSION].imageBuffer[0], depthBuf, nImageSize * sizeof(unsigned char));

    if(m_depthFusionBuffer.size() != (size_t)nImageSize){
        m_depthFusionBuffer.resize(nImageSize);
    }

    unsigned short *pBuf = (unsigned short *)depthBuf;
    for(int i = 0 ; i < nPixelCount ; ++i){
        unsigned short value = pBuf[i];
        if (selectedIndex[i] != 255){
            value = value * (unsigned short)dblMultiplier[selectedIndex[i]];
        }

        m_depthFusionBuffer[i * 2] = (value & 0xff);
        m_depthFusionBuffer[i * 2 + 1] = (value & 0xff00) >> 8;
    }

    ProcessImageCallback(STREAM_DEPTH_FUSION, nImageSize, serialNumber);
}

void CVideoDeviceModel_8038::DepthFusionCallbackFn(unsigned char* depthBuf, unsigned char* selectedIndex, int depthSize, int width, int height, int serialNumber, void* pParam)
{
    CVideoDeviceModel_8038 *pModel = static_cast<CVideoDeviceModel_8038 *>(pParam);
    pModel->ProcessDepthFusion(depthBuf, selectedIndex, depthSize, width, height, serialNumber);
}

CImageDataModel *CVideoDeviceModel_8038::GetPreivewImageDataModel(STREAM_TYPE streamType)
{
    if (STREAM_DEPTH != streamType) return CVideoDeviceModel::GetPreivewImageDataModel(streamType);

    if (!m_pVideoDeviceController || !m_pVideoDeviceController->GetControlView()) return nullptr;

    return m_pVideoDeviceController->GetControlView()->GetPreviewImageData(STREAM_DEPTH_30mm);
}
