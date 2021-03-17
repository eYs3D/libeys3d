#include "CVideoDeviceModel.h"
#include "eSPDI.h"
#include "CEtronDeviceManager.h"
#include "CVideoDeviceController.h"
#include "CThreadWorkerManage.h"
#include "CTaskInfoManager.h"
#include "PlyFilter.h"
#include "RegisterSettings.h"
#include "CImageDataModel.h"
#include <libudev.h>
#include "CFrameSyncManager.h"

CVideoDeviceModel::CVideoDeviceModel(DEVSELINFO *pDeviceSelfInfo):
m_state(CLOSED),
m_nIRMin(0),
m_nIRMax(0),
m_nIRValue(0),
m_usbPortType(USB_PORT_TYPE_UNKNOW),
m_depthDataType(ETronDI_DEPTH_DATA_DEFAULT),
m_pVideoDeviceController(nullptr),
m_pFrameGrabber(nullptr),
m_coldResetTask(nullptr),
m_pIMUModel(nullptr),
m_nLastInterLeaveColorSerial(0),
m_nLastInterLeaveDepthSerial(0),
m_serialNumberType(FRAME_COUNT)
{
    DEVSELINFO *pDevSelfInfo = new DEVSELINFO;
    pDevSelfInfo->index = pDeviceSelfInfo->index;
    m_deviceSelInfo.push_back(std::move(pDevSelfInfo));

    memset(m_nColdResetThresholdMs, 0, sizeof(m_nColdResetThresholdMs));
    memset(m_nLastestSuccessTime, 0, sizeof(m_nLastestSuccessTime));
}

CVideoDeviceModel::~CVideoDeviceModel()
{
    for (DEVSELINFO *pSelfInfo : m_deviceSelInfo){
        delete pSelfInfo;
    }

    if(m_pFrameGrabber){
        delete m_pFrameGrabber;
    }

    if(m_pIMUModel) delete m_pIMUModel;
}

void CVideoDeviceModel::AdjustDeviceSelfInfo(DEVSELINFO *pDeviceSelfInfo)
{
    m_deviceSelInfo[0]->index = pDeviceSelfInfo->index;
    Init();
}

bool CVideoDeviceModel::EqualModel(CVideoDeviceModel *pModel)
{
    std::vector<DeviceInfo> selfInfo = GetDeviceInformation();
    std::vector<DeviceInfo> modelInfo = pModel->GetDeviceInformation();

    if (selfInfo.empty() || modelInfo.empty()) return false;

    if (selfInfo.size() != modelInfo.size()) return false;

    for(size_t i = 0 ; i < selfInfo.size() ; ++i){
        DeviceInfo self = selfInfo[i];
        DeviceInfo another = modelInfo[i];

        if (self.deviceInfomation.nChipID != another.deviceInfomation.nChipID) return false;
        if (self.deviceInfomation.wPID != another.deviceInfomation.wPID) return false;
        if (self.deviceInfomation.wVID != another.deviceInfomation.wVID) return false;

        if (self.sFWVersion.compare(another.sFWVersion)) return false;
        if (self.sSerialNumber.compare(another.sSerialNumber)) return false;
        if (self.sBusInfo.compare(another.sBusInfo)) return false;
    }

    return true;
}

int CVideoDeviceModel::Init()
{
    Reset();
    InitDeviceSelInfo();
    InitDeviceInformation();
    InitStreamInfoList();
    InitUsbType();
    InitCameraproperty();
    InitIMU();
    Update();
    if (m_pVideoDeviceController) m_pVideoDeviceController->Init();
    return DataVerification();
}

int CVideoDeviceModel::Reset()
{
    for(size_t i = 1 ; i < m_deviceSelInfo.size() ; ++i){
        delete m_deviceSelInfo[i];
    }

    m_deviceSelInfo.resize(1);

    m_deviceInfo.clear();

    for(int i = 0 ; i < STREAM_TYPE_COUNT ; ++i){
        m_streamInfo[i].resize(MAX_STREAM_INFO_COUNT, {0, 0, false});
    }

    for(CCameraPropertyModel *pCameraPropertyModel : m_cameraPropertyModel){
        delete pCameraPropertyModel;
    }
    m_cameraPropertyModel.clear();

    return ETronDI_OK;
}

int CVideoDeviceModel::Update()
{
    UpdateIR();
    UpdateDepthDataType();
    UpdateZDTable();
    for(CCameraPropertyModel *pCameraPropertyModel : m_cameraPropertyModel){
        pCameraPropertyModel->Update();
    }
    return ETronDI_OK;
}

int CVideoDeviceModel::DataVerification()
{
    for (DeviceInfo deviceInfo : m_deviceInfo){
        ERROR_HANDLE(deviceInfo.deviceInfomation.nChipID != 0 &&
                     deviceInfo.deviceInfomation.wPID != 0 &&
                     deviceInfo.deviceInfomation.wVID != 0 &&
                     deviceInfo.deviceInfomation.strDevName != 0 &&
                     deviceInfo.sSerialNumber.length() != 0 &&
                     deviceInfo.sSerialNumber.length() != 0, "Get device information failed!!\n");
    }

    for (int i = 0 ; i < STREAM_TYPE_COUNT ; ++i){
        if (IsStreamSupport( (STREAM_TYPE)i )){
            ERROR_HANDLE(!m_streamInfo[i].empty(), "Get stream information failed!!\n");
        }
    }

    ERROR_HANDLE(true == (USB_PORT_TYPE_UNKNOW != m_usbPortType), "Get usb port type failed!!\n");

    return ETronDI_OK;
}

void CVideoDeviceModel::ChangeState(STATE state)
{
    if(RECONNECTING == state && RECONNECTING != m_state){
        m_restoreState = m_state;
        if (STREAMING == m_restoreState){
            StopStreaming(true);
        }
    }else if(RECONNECTED == state){
        state = m_restoreState;
        if (STREAMING == m_restoreState){
            StartStreaming();
        }
    }

    m_state = state;
}

int CVideoDeviceModel::InitDeviceSelInfo()
{
    return ETronDI_OK;
}

std::vector<DEVSELINFO *> CVideoDeviceModel::GetDeviceSelInfo() const
{
    return m_deviceSelInfo;
}

int CVideoDeviceModel::InitDeviceInformation()
{
    m_deviceInfo.push_back(GetDeviceInformation(m_deviceSelInfo[0]));
    return ETronDI_OK;
}

std::vector<CVideoDeviceModel::DeviceInfo> CVideoDeviceModel::GetDeviceInformation() const
{
    return m_deviceInfo;
}

CVideoDeviceModel::DeviceInfo CVideoDeviceModel::GetDeviceInformation(DEVSELINFO *pDeviceSelfInfo)
{
    void *pEtronDI = CEtronDeviceManager::GetInstance()->GetEtronDI();

    DeviceInfo deviceInfo;
    memset(&deviceInfo, 0, sizeof(DeviceInfo));
    if(!pDeviceSelfInfo) return deviceInfo;

    int ret;
    RETRY_ETRON_API(ret, EtronDI_GetDeviceInfo(pEtronDI, pDeviceSelfInfo, &deviceInfo.deviceInfomation));

    char pFWVersion[256] = {0};
    int  nFWLength;
    RETRY_ETRON_API(ret, EtronDI_GetFwVersion(pEtronDI, pDeviceSelfInfo, pFWVersion, 256, &nFWLength));
    if (ETronDI_OK == ret){
        deviceInfo.sFWVersion = pFWVersion;
    }


    unsigned char pSerialNumber[256] = {0};
    int  nSerialNumberLength;
    RETRY_ETRON_API(ret, EtronDI_GetSerialNumber(pEtronDI, pDeviceSelfInfo, pSerialNumber, 256, &nSerialNumberLength));
    if (ETronDI_OK == ret){
        deviceInfo.sSerialNumber.resize( nSerialNumberLength / 2 );
        for (int i = 0 ; i < nSerialNumberLength / 2 ; ++i)
            deviceInfo.sSerialNumber[i] = pSerialNumber[i * 2 + 1] * 256 + pSerialNumber[i * 2];
    }

    char pBusInfo[256] = {0};
    int  nBusInfoLength;
    RETRY_ETRON_API(ret, EtronDI_GetBusInfo(pEtronDI, pDeviceSelfInfo, pBusInfo, &nBusInfoLength));
    if (ETronDI_OK == ret){
        deviceInfo.sBusInfo = pBusInfo;
    }

    auto getModelName = [=](char *pDevV4lPath, char *pModelNameResult){
        FILE* f;
        char buffer[128];
        int i = 0;

        f = fopen(pDevV4lPath, "r" );
        if (!f) {
            printf("Could not open %s\n", pDevV4lPath);
            return -1;
        }

        fgets(buffer, sizeof(buffer), f);
        do {
            pModelNameResult[i] = buffer[i];
            i++;
        } while(buffer[i] != '\0');
        i--;
        pModelNameResult[i] = '\0';
        fclose( f );

        return 0;
    };

    char pDevBuf[128] = {0};
    char pDevBuf_v4l[128] = {0};
    char pModelName[128] = {0};
    strcpy(pDevBuf, &deviceInfo.deviceInfomation.strDevName[strlen("/dev/")]);
    sprintf(pDevBuf_v4l, "/sys/class/video4linux/%s/name", pDevBuf);
    getModelName(pDevBuf_v4l, pModelName);
    deviceInfo.sModelName = pModelName;

    return deviceInfo;
}

int  CVideoDeviceModel::InitUsbType()
{
    int ret;
    RETRY_ETRON_API(ret, EtronDI_GetDevicePortType(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                   m_deviceSelInfo[0],
                                                   &m_usbPortType));
    if (ETronDI_OK != ret){
        struct udev *udev;
        struct udev_enumerate *enumerate;
        struct udev_list_entry *devices, *dev_list_entry;
        struct udev_device *dev;

        udev = udev_new();

        enumerate = udev_enumerate_new(udev);
        udev_enumerate_add_match_subsystem(enumerate, "video4linux");
        udev_enumerate_scan_devices(enumerate);
        devices = udev_enumerate_get_list_entry(enumerate);

        char devPath[256] = {0};
        strcpy(devPath, &m_deviceInfo[0].deviceInfomation.strDevName[strlen("/dev/")]);
        udev_list_entry_foreach(dev_list_entry, devices) {
            const char *path = udev_list_entry_get_name(dev_list_entry);
            if (strcmp(&path[strlen(path) - strlen(devPath)], devPath)) continue;

            dev = udev_device_new_from_syspath(udev, path);
            const char *speed = udev_device_get_sysattr_value(dev, "speed");
            struct udev_device *nodeDev = dev;
            while (!speed){
                struct udev_device *parentDev = udev_device_get_parent(dev);
                if (!parentDev) break;
                dev = parentDev;
                speed = udev_device_get_sysattr_value(dev, "speed");
            }

            if (speed){
                if (!strcmp(speed, "5000")){
                    m_usbPortType = USB_PORT_TYPE_3_0;
                }else if (!strcmp(speed, "480")){
                    m_usbPortType = USB_PORT_TYPE_2_0;
                }

                ret = ETronDI_OK;
            }
            udev_device_unref(nodeDev);
            break;
        }
        udev_enumerate_unref(enumerate);
        udev_unref(udev);
    }

    return ret;
}

USB_PORT_TYPE CVideoDeviceModel::GetUsbType()
{
    return m_usbPortType;
}

bool CVideoDeviceModel::IsStreamSupport(STREAM_TYPE type)
{
    switch (type){
        case STREAM_COLOR:
        case STREAM_DEPTH:
            return true;
        default:
            return false;
    }
}

int CVideoDeviceModel::InitStreamInfoList()
{
    int ret;
    RETRY_ETRON_API(ret, EtronDI_GetDeviceResolutionList(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                         m_deviceSelInfo[0],
                                                         MAX_STREAM_INFO_COUNT, &m_streamInfo[STREAM_COLOR][0],
                                                         MAX_STREAM_INFO_COUNT, &m_streamInfo[STREAM_DEPTH][0]));

    if (ETronDI_OK != ret) return ret;

    auto it = m_streamInfo[STREAM_COLOR].begin();
    for ( ; it != m_streamInfo[STREAM_COLOR].end() ; ++it){
        if (0 == (*it).nWidth){
            break;
        }
    }
    m_streamInfo[STREAM_COLOR].erase(it, m_streamInfo[STREAM_COLOR].end());
    m_streamInfo[STREAM_COLOR].shrink_to_fit();

    it = m_streamInfo[STREAM_DEPTH].begin();
    for ( ; it != m_streamInfo[STREAM_DEPTH].end() ; ++it){
        if (0 == (*it).nWidth){
            break;
        }
    }
    m_streamInfo[STREAM_DEPTH].erase(it, m_streamInfo[STREAM_DEPTH].end());
    m_streamInfo[STREAM_DEPTH].shrink_to_fit();

    return ETronDI_OK;
}

std::vector<ETRONDI_STREAM_INFO> CVideoDeviceModel::GetStreamInfoList(STREAM_TYPE type)
{
    if (!IsStreamSupport(type)) return {};
    return m_streamInfo[type];
}

int CVideoDeviceModel::InitCameraproperty()
{
    AddCameraPropertyModels();

    for(CCameraPropertyModel *pCameraPropertyModel : m_cameraPropertyModel){
        pCameraPropertyModel->Init();
    }

    return ETronDI_OK;
}

int CVideoDeviceModel::SetPlumAR0330(bool bEnable)
{
    unsigned short nValue = 0;
    if(ETronDI_OK != EtronDI_GetFWRegister(CEtronDeviceManager::GetInstance()->GetInstance(),
                                           m_deviceSelInfo[0],
                                           0xF3,
                                           &nValue,
                                           FG_Address_1Byte | FG_Value_1Byte)){
        return ETronDI_WRITE_REG_FAIL;
    }

    nValue |= 0x11;
    nValue &= bEnable ? 0x10 : 0x01;
    if(ETronDI_OK != EtronDI_SetFWRegister(CEtronDeviceManager::GetInstance()->GetInstance(),
                                           m_deviceSelInfo[0],
                                           0xF3,
                                           nValue,
                                           FG_Address_1Byte | FG_Value_1Byte)){
        return ETronDI_WRITE_REG_FAIL;
    }

    return ETronDI_OK;
}

int CVideoDeviceModel::AddCameraPropertyModels()
{
    CCameraPropertyModel *pNewCameraPropertyModel = new CCameraPropertyModel("Color", this, m_deviceSelInfo[0]);
    m_cameraPropertyModel.push_back(std::move(pNewCameraPropertyModel));

    return ETronDI_OK;
}

std::vector<CCameraPropertyModel *> CVideoDeviceModel::GetCameraproperty()
{
    return m_cameraPropertyModel;
}

#define DEFAULT_IR_MAX 6
#define EXTEND_IR_MAX 15

int CVideoDeviceModel::SetIRValue(unsigned short nValue)
{
    void *pEtronDI = CEtronDeviceManager::GetInstance()->GetEtronDI();
    int ret;

    if (nValue != 0) {
        RETRY_ETRON_API(ret, EtronDI_SetIRMode(pEtronDI, m_deviceSelInfo[0], 0x3f)); // 6 bits on for opening both 6 ir
        RETRY_ETRON_API(ret, EtronDI_SetCurrentIRValue(pEtronDI, m_deviceSelInfo[0], nValue));
    } else {
        RETRY_ETRON_API(ret, EtronDI_SetCurrentIRValue(pEtronDI, m_deviceSelInfo[0], nValue));
    }

    UpdateIR();

    return ETronDI_OK;
}

int CVideoDeviceModel::ExtendIR(bool bEnable)
{
    void *pEtronDI = CEtronDeviceManager::GetInstance()->GetEtronDI();

    int ret;
    bool bDisableIR = (0 == m_nIRValue);
    if (bDisableIR){
        RETRY_ETRON_API(ret, EtronDI_SetIRMode(pEtronDI, m_deviceSelInfo[0], 0x03)); // 2 bits on for opening both 2 ir
    }

    RETRY_ETRON_API(ret, EtronDI_SetIRMaxValue(pEtronDI, m_deviceSelInfo[0], bEnable ? EXTEND_IR_MAX : DEFAULT_IR_MAX));

    if (bDisableIR){
        RETRY_ETRON_API(ret, EtronDI_SetIRMode(pEtronDI, m_deviceSelInfo[0], 0x00);); // turn off ir
    }

    if (ret != ETronDI_OK) return ret;

    ret = UpdateIR();

    return ret;
}

int CVideoDeviceModel::SetSerialNumberType(SERIAL_NUMBER_TYPE type)
{
    int ret;
    RETRY_ETRON_API(ret, EtronDI_SetControlCounterMode(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                       m_deviceSelInfo[0],
                                                       (SERIAL_COUNT == type) ? 0x1 : 0x0));

    unsigned char result;
    EtronDI_GetControlCounterMode(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                  m_deviceSelInfo[0],
                                  &result);
    m_serialNumberType = (1 == result) ? SERIAL_COUNT : FRAME_COUNT;

    return ret;
}

int CVideoDeviceModel::UpdateIR()
{
    void *pEtronDI = CEtronDeviceManager::GetInstance()->GetEtronDI();
    int ret;
    ret = EtronDI_GetFWRegister(pEtronDI, m_deviceSelInfo[0],
                                0xE2, &m_nIRMax,
                                FG_Address_1Byte | FG_Value_1Byte);
    if (ETronDI_OK != ret) return ret;

    ret = EtronDI_GetFWRegister(pEtronDI, m_deviceSelInfo[0],
                                0xE1, &m_nIRMin,
                                FG_Address_1Byte | FG_Value_1Byte);
    if (ETronDI_OK != ret) return ret;

    ret = EtronDI_GetCurrentIRValue(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                    m_deviceSelInfo[0], &m_nIRValue);
    if (ETronDI_OK != ret) return ret;

    return ETronDI_OK;
}

int CVideoDeviceModel::GetIRRange(unsigned short &nMin, unsigned short &nMax)
{
    nMin = m_nIRMin;
    nMax = m_nIRMax;
    return ETronDI_OK;
}

unsigned short CVideoDeviceModel::GetIRValue()
{
    return m_nIRValue;
}

bool CVideoDeviceModel::IsIRExtended()
{
    return EXTEND_IR_MAX == m_nIRMax;
}

int CVideoDeviceModel::TransformDepthDataType(int nDepthDataType, bool bRectifyData)
{
    switch (nDepthDataType){
        case ETronDI_DEPTH_DATA_8_BITS:
        case ETronDI_DEPTH_DATA_8_BITS_RAW:
        case ETronDI_DEPTH_DATA_ILM_8_BITS:
        case ETronDI_DEPTH_DATA_ILM_8_BITS_RAW:
            nDepthDataType = bRectifyData ? ETronDI_DEPTH_DATA_8_BITS : ETronDI_DEPTH_DATA_8_BITS_RAW; break;
        case ETronDI_DEPTH_DATA_8_BITS_x80:
        case ETronDI_DEPTH_DATA_8_BITS_x80_RAW:
        case ETronDI_DEPTH_DATA_ILM_8_BITS_x80:
        case ETronDI_DEPTH_DATA_ILM_8_BITS_x80_RAW:
            nDepthDataType = bRectifyData ? ETronDI_DEPTH_DATA_8_BITS_x80 : ETronDI_DEPTH_DATA_8_BITS_x80_RAW; break;
        case ETronDI_DEPTH_DATA_11_BITS:
        case ETronDI_DEPTH_DATA_11_BITS_RAW:
        case ETronDI_DEPTH_DATA_11_BITS_COMBINED_RECTIFY:
        case ETronDI_DEPTH_DATA_ILM_11_BITS:
        case ETronDI_DEPTH_DATA_ILM_11_BITS_RAW:
        case ETronDI_DEPTH_DATA_ILM_11_BITS_COMBINED_RECTIFY:
            nDepthDataType = bRectifyData ? ETronDI_DEPTH_DATA_11_BITS : ETronDI_DEPTH_DATA_11_BITS_RAW;  break;
        case ETronDI_DEPTH_DATA_14_BITS:
        case ETronDI_DEPTH_DATA_14_BITS_RAW:
        case ETronDI_DEPTH_DATA_14_BITS_COMBINED_RECTIFY:
        case ETronDI_DEPTH_DATA_ILM_14_BITS:
        case ETronDI_DEPTH_DATA_ILM_14_BITS_RAW:
        case ETronDI_DEPTH_DATA_ILM_14_BITS_COMBINED_RECTIFY:
            nDepthDataType = bRectifyData ? ETronDI_DEPTH_DATA_14_BITS : ETronDI_DEPTH_DATA_14_BITS_RAW;  break;
        case ETronDI_DEPTH_DATA_OFF_RAW:
        case ETronDI_DEPTH_DATA_OFF_RECTIFY:
        case ETronDI_DEPTH_DATA_ILM_OFF_RAW:
        case ETronDI_DEPTH_DATA_ILM_OFF_RECTIFY:
            nDepthDataType = bRectifyData ? ETronDI_DEPTH_DATA_OFF_RECTIFY : ETronDI_DEPTH_DATA_OFF_RAW;  break;
        default:
            nDepthDataType = bRectifyData ? ETronDI_DEPTH_DATA_DEFAULT : ETronDI_DEPTH_DATA_OFF_RECTIFY;  break;
    }

    if (IsInterleaveMode()) nDepthDataType += ETronDI_DEPTH_DATA_INTERLEAVE_MODE_OFFSET;

    return nDepthDataType;
}

int CVideoDeviceModel::SetDepthDataType(int nDepthDataType)
{
    void *pEtronDI = CEtronDeviceManager::GetInstance()->GetEtronDI();

    int ret;
    for (DEVSELINFO *devSelInfo : m_deviceSelInfo){
        ret = EtronDI_SetDepthDataType(pEtronDI, devSelInfo, nDepthDataType);
        if (ret != ETronDI_OK) return ret;
        UpdateDepthDataType();
    }

    return ret;
}

int CVideoDeviceModel::TransformDepthDataType(bool bRectifyData)
{
    return TransformDepthDataType(GetDepthDataType(), bRectifyData);
}

int CVideoDeviceModel::TransformDepthDataType(int nDepthDataType)
{
    return TransformDepthDataType(nDepthDataType, IsRectifyData());
}

int CVideoDeviceModel::UpdateDepthDataType()
{
    int ret;
    RETRY_ETRON_API(ret, EtronDI_GetDepthDataType(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                  m_deviceSelInfo[0], &m_depthDataType));

    if (ETronDI_OK != ret) return ret;
    return ETronDI_OK;
}

unsigned short CVideoDeviceModel::GetDepthDataType()
{
    return m_depthDataType;
}

bool CVideoDeviceModel::IsRectifyData()
{
    switch (GetDepthDataType()){
        case ETronDI_DEPTH_DATA_8_BITS:
        case ETronDI_DEPTH_DATA_8_BITS_x80:
        case ETronDI_DEPTH_DATA_11_BITS:
        case ETronDI_DEPTH_DATA_11_BITS_COMBINED_RECTIFY:
        case ETronDI_DEPTH_DATA_14_BITS:
        case ETronDI_DEPTH_DATA_14_BITS_COMBINED_RECTIFY:
        case ETronDI_DEPTH_DATA_OFF_RECTIFY:
        case ETronDI_DEPTH_DATA_ILM_8_BITS:
        case ETronDI_DEPTH_DATA_ILM_8_BITS_x80:
        case ETronDI_DEPTH_DATA_ILM_11_BITS:
        case ETronDI_DEPTH_DATA_ILM_11_BITS_COMBINED_RECTIFY:
        case ETronDI_DEPTH_DATA_ILM_14_BITS:
        case ETronDI_DEPTH_DATA_ILM_14_BITS_COMBINED_RECTIFY:
        case ETronDI_DEPTH_DATA_ILM_OFF_RECTIFY:
            return true;
        default:
            return false;
    }
}

EtronDIImageType::Value CVideoDeviceModel::GetDepthImageType()
{
    switch (GetDepthDataType()){
        case ETronDI_DEPTH_DATA_8_BITS:
        case ETronDI_DEPTH_DATA_8_BITS_RAW:
        case ETronDI_DEPTH_DATA_ILM_8_BITS:
        case ETronDI_DEPTH_DATA_ILM_8_BITS_RAW:
            return EtronDIImageType::DEPTH_8BITS;
        case ETronDI_DEPTH_DATA_8_BITS_x80:
        case ETronDI_DEPTH_DATA_8_BITS_x80_RAW:
        case ETronDI_DEPTH_DATA_ILM_8_BITS_x80:
        case ETronDI_DEPTH_DATA_ILM_8_BITS_x80_RAW:
            return EtronDIImageType::DEPTH_8BITS_0x80;
        case ETronDI_DEPTH_DATA_11_BITS:
        case ETronDI_DEPTH_DATA_11_BITS_RAW:
        case ETronDI_DEPTH_DATA_11_BITS_COMBINED_RECTIFY:
        case ETronDI_DEPTH_DATA_ILM_11_BITS:
        case ETronDI_DEPTH_DATA_ILM_11_BITS_RAW:
        case ETronDI_DEPTH_DATA_ILM_11_BITS_COMBINED_RECTIFY:
            return EtronDIImageType::DEPTH_11BITS;
        case ETronDI_DEPTH_DATA_14_BITS:
        case ETronDI_DEPTH_DATA_14_BITS_RAW:
        case ETronDI_DEPTH_DATA_14_BITS_COMBINED_RECTIFY:
        case ETronDI_DEPTH_DATA_ILM_14_BITS:
        case ETronDI_DEPTH_DATA_ILM_14_BITS_RAW:
        case ETronDI_DEPTH_DATA_ILM_14_BITS_COMBINED_RECTIFY:
            return EtronDIImageType::DEPTH_14BITS;
        default:
            return EtronDIImageType::IMAGE_UNKNOWN;
    }
}

bool CVideoDeviceModel::IsHWPP()
{
    unsigned short nHWPP;
    int ret = EtronDI_GetHWRegister(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                    m_deviceSelInfo[0],
                                    0xf424,
                                    &nHWPP,
                                    FG_Address_2Byte | FG_Value_1Byte);

    if (ret != ETronDI_OK) return false;

    return (nHWPP & 0x30) == 0;
}

int CVideoDeviceModel::SetHWPP(bool bEnable)
{
    unsigned short value = 0;
    void *pEtronDI = CEtronDeviceManager::GetInstance()->GetEtronDI();

    for(size_t i = 0 ; i < m_deviceSelInfo.size() ; ++i){
        value = 0;
        EtronDI_GetHWRegister(pEtronDI, m_deviceSelInfo[i],
                              0xf424, &value,
                              FG_Address_2Byte | FG_Value_1Byte);
        value &= 0x0F;
        value |= (bEnable ? 0x40 : 0x70);
        EtronDI_SetHWRegister(pEtronDI, m_deviceSelInfo[i],
                              0xf424, value,
                              FG_Address_2Byte | FG_Value_1Byte);

        if(0 == i && m_deviceSelInfo.size() > 1){
            EtronDI_SetSensorRegister(pEtronDI, m_deviceSelInfo[i],
                                      0xC2, 0x9024, value,
                                      FG_Address_2Byte | FG_Value_1Byte,
                                      SENSOR_A);
        }
    }

    return ETronDI_OK;
}

int CVideoDeviceModel::UpdateZDTable()
{
    if (!m_pVideoDeviceController) return ETronDI_NullPtr;
    if (GetCameraFocus() > 0.0) return ETronDI_OK;

    ZDTABLEINFO zdTableInfo;

    zdTableInfo.nDataType = ETronDI_DEPTH_DATA_11_BITS;
    memset(m_zdTableInfo.ZDTable, 0, sizeof(m_zdTableInfo.ZDTable));
    if (m_deviceInfo[0].deviceInfomation.nDevType == PUMA) {
        m_zdTableInfo.nTableSize = ETronDI_ZD_TABLE_FILE_SIZE_11_BITS;
    } else {
        m_zdTableInfo.nTableSize = ETronDI_ZD_TABLE_FILE_SIZE_8_BITS;
    }

    zdTableInfo.nIndex = m_pVideoDeviceController->GetPreviewOptions()->GetStreamIndex(STREAM_DEPTH);
    AdjustZDTableIndex(zdTableInfo.nIndex);

    int nActualLength = 0;
    int ret;
    RETRY_ETRON_API(ret, EtronDI_GetZDTable(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                            m_deviceSelInfo[0],
                                            m_zdTableInfo.ZDTable, m_zdTableInfo.nTableSize,
                                            &nActualLength, &zdTableInfo));

    if (ETronDI_OK != ret) return ret;

    m_zdTableInfo.ZDTable[0] = 0;
    m_zdTableInfo.ZDTable[1] = 0;
    m_zdTableInfo.nZNear = USHRT_MAX;
    m_zdTableInfo.nZFar = 0;

    unsigned short nZValue;
    for( int i = 0 ; i < nActualLength / 2 ; ++i) {

        nZValue = (((unsigned short)m_zdTableInfo.ZDTable[i * 2]) << 8) + m_zdTableInfo.ZDTable[i * 2 + 1];
        if (nZValue){
            m_zdTableInfo.nZNear = std::min(m_zdTableInfo.nZNear, nZValue);
            m_zdTableInfo.nZFar = std::max(m_zdTableInfo.nZFar, nZValue);
        }
    }

    int nZNear, nZFar;
    m_pVideoDeviceController->GetPreviewOptions()->GetZRange(nZNear, nZFar);
    m_pVideoDeviceController->GetPreviewOptions()->SetZRange(m_zdTableInfo.nZNear, nZFar);
    m_pVideoDeviceController->AdjustZRange();

    return ETronDI_OK;
}

CVideoDeviceModel::ZDTableInfo *CVideoDeviceModel::GetZDTableInfo()
{
    return &m_zdTableInfo;
}

int CVideoDeviceModel::GetRectifyLogData(int nDevIndex, int nRectifyLogIndex, eSPCtrl_RectLogData *pRectifyLogData, STREAM_TYPE depthType)
{
    if (!pRectifyLogData) return ETronDI_NullPtr;

    if (nDevIndex >= (int)m_deviceInfo.size()) return ETronDI_NullPtr;

    int ret;
    RETRY_ETRON_API(ret, EtronDI_GetRectifyMatLogData(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                      m_deviceSelInfo[nDevIndex],
                                                      pRectifyLogData,
                                                      nRectifyLogIndex));
    return ret;
}

bool CVideoDeviceModel::IsStreamAvailable()
{
    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    bool bDepthStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH);

    return bColorStream || bDepthStream;
}

int CVideoDeviceModel::StartStreaming()
{
    if (!m_pVideoDeviceController->GetControlView()) return ETronDI_NullPtr;
    if (!IsStreamAvailable()) return ETronDI_NullPtr;

    m_mapSerialCountLast.clear();
    m_mapSerialCountDiff.clear();
    m_mapSerialCount2FrameCount.clear();

    m_coldResetTask = nullptr;

    PrepareOpenDevice();

    if (m_pVideoDeviceController->GetPreviewOptions()->IsPointCloudViewer()){
        StartFrameGrabber();
    }

    if (m_pVideoDeviceController->GetPreviewOptions()->IsIMUSyncWithFrame()){
        m_pVideoDeviceController->StartIMUSyncWithFrame();
    }

    PreparePointCloudInfo();

    int ret = OpenDevice();
    if (ETronDI_OK != ret) {
        StopStreaming();
        return ret;
    }

    ChangeState(STREAMING);

    StartStreamingTask();

    return ret;
}

int CVideoDeviceModel::PrepareOpenDevice()
{
    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    bool bDepthStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH);

    SetIRValue(m_pVideoDeviceController->GetPreviewOptions()->GetIRLevel());
    if (bDepthStream){
        SetDepthDataType(TransformDepthDataType(GetDepthDataType()));
    }else{
        SetDepthDataType(TransformDepthDataType(ETronDI_DEPTH_DATA_OFF_RAW));
    }

    m_imageData[STREAM_COLOR].nWidth = 0;
    m_imageData[STREAM_COLOR].nHeight = 0;
    m_imageData[STREAM_COLOR].bMJPG = false;
    m_imageData[STREAM_DEPTH].nWidth = 0;
    m_imageData[STREAM_DEPTH].nHeight = 0;

    if (bColorStream){
        std::vector<ETRONDI_STREAM_INFO> streamInfo = GetStreamInfoList(STREAM_COLOR);
        int index = m_pVideoDeviceController->GetPreviewOptions()->GetStreamIndex(STREAM_COLOR);
        m_imageData[STREAM_COLOR].nWidth = streamInfo[index].nWidth;
        m_imageData[STREAM_COLOR].nHeight  = streamInfo[index].nHeight;
        m_imageData[STREAM_COLOR].bMJPG = streamInfo[index].bFormatMJPG;
        m_imageData[STREAM_COLOR].depthDataType = GetDepthDataType();
        m_imageData[STREAM_COLOR].imageDataType = m_imageData[STREAM_COLOR].bMJPG ?
                                                  EtronDIImageType::COLOR_MJPG :
                                                  EtronDIImageType::COLOR_YUY2;

        unsigned short nBytePerPixel = 2;
        unsigned int nBufferSize = m_imageData[STREAM_COLOR].nWidth * m_imageData[STREAM_COLOR].nHeight * nBytePerPixel;
        if (m_imageData[STREAM_COLOR].imageBuffer.size() != nBufferSize){
            m_imageData[STREAM_COLOR].imageBuffer.resize(nBufferSize);
        }
        memset(&m_imageData[STREAM_COLOR].imageBuffer[0], 0, sizeof(nBufferSize));
    }

    if (bDepthStream){
        std::vector<ETRONDI_STREAM_INFO> streamInfo = GetStreamInfoList(STREAM_DEPTH);
        int index = m_pVideoDeviceController->GetPreviewOptions()->GetStreamIndex(STREAM_DEPTH);
        m_imageData[STREAM_DEPTH].nWidth = streamInfo[index].nWidth;
        m_imageData[STREAM_DEPTH].nHeight = streamInfo[index].nHeight;
        m_imageData[STREAM_DEPTH].depthDataType = GetDepthDataType();
        m_imageData[STREAM_DEPTH].imageDataType = GetDepthImageType();

        unsigned short nBytePerPixel = 2;
        unsigned int nBufferSize = m_imageData[STREAM_DEPTH].nWidth * m_imageData[STREAM_DEPTH].nHeight * nBytePerPixel;
        if (m_imageData[STREAM_DEPTH].imageBuffer.size() != nBufferSize){
            m_imageData[STREAM_DEPTH].imageBuffer.resize(nBufferSize);
        }
        memset(&m_imageData[STREAM_DEPTH].imageBuffer[0], 0, sizeof(nBufferSize));
    }

    if (InterleaveModeSupport() &&
        ETronDI_OK != SetInterleaveModeEnable(IsInterleaveMode())){
        CTaskInfo *pInfo = CTaskInfoManager::GetInstance()->RequestTaskInfo(CTaskInfo::VIDEO_INTERLEAVE_MODE, this);
        CThreadWorkerManage::GetInstance()->AddTask(pInfo);
    }

    return ETronDI_OK;
}

CVideoDeviceModel::ImageData &CVideoDeviceModel::GetColorImageData()
{
    return m_imageData[STREAM_COLOR];
}

CVideoDeviceModel::ImageData &CVideoDeviceModel::GetDepthImageData()
{
    return m_imageData[STREAM_DEPTH];
}

CImageDataModel *CVideoDeviceModel::GetPreivewImageDataModel(STREAM_TYPE streamType)
{
    if (!m_pVideoDeviceController || !m_pVideoDeviceController->GetControlView()) return nullptr;

    return m_pVideoDeviceController->GetControlView()->GetPreviewImageData(streamType);
}

int CVideoDeviceModel::StartFrameGrabber()
{
    if(m_pFrameGrabber) {
        delete m_pFrameGrabber;
    }

    int nMaxPoolSize = 1;
    m_pFrameGrabber = new FrameGrabber(nMaxPoolSize, CVideoDeviceModel::FrameGrabberCallbackFn, this);
    return ETronDI_OK;

}

int CVideoDeviceModel::PreparePointCloudInfo()
{
    GetRectifyLogData(0, m_pVideoDeviceController->GetPreviewOptions()->GetStreamIndex(STREAM_DEPTH), &m_rectifyLogData);
    GetPointCloudInfo(&m_rectifyLogData, m_pointCloudInfo, GetColorImageData(), GetDepthImageData());
    return ETronDI_OK;
}

int CVideoDeviceModel::GetPointCloudInfo(eSPCtrl_RectLogData *pRectifyLogData, PointCloudInfo &pointCloudInfo,
                                         ImageData colorImageData, ImageData depthImageData)
{
    memset(&pointCloudInfo, 0, sizeof(PointCloudInfo));

    pointCloudInfo.wDepthType = depthImageData.depthDataType;

    const float ratio_Mat = (float)depthImageData.nHeight / pRectifyLogData->OutImgHeight;
    const float baseline  = 1.0f / pRectifyLogData->ReProjectMat[14];
    const float diff      = pRectifyLogData->ReProjectMat[15] * ratio_Mat;

    pointCloudInfo.centerX = -1.0f * pRectifyLogData->ReProjectMat[3] * ratio_Mat;
    pointCloudInfo.centerY = -1.0f * pRectifyLogData->ReProjectMat[7] * ratio_Mat;
    pointCloudInfo.focalLength = pRectifyLogData->ReProjectMat[11] * ratio_Mat;

    switch (depthImageData.imageDataType){
        case EtronDIImageType::DEPTH_14BITS: pointCloudInfo.disparity_len = 0; break;
        case EtronDIImageType::DEPTH_11BITS:
        {
            pointCloudInfo.disparity_len = 2048;
            for(int i = 0 ; i < pointCloudInfo.disparity_len ; ++i){
                pointCloudInfo.disparityToW[i] = ( i * ratio_Mat / 8.0f ) / baseline + diff;
            }
            break;
        }
        default:
            pointCloudInfo.disparity_len = 256;
            for(int i = 0 ; i < pointCloudInfo.disparity_len ; ++i){
                pointCloudInfo.disparityToW[i] = (i * ratio_Mat) / baseline + diff;
            }
            break;
    }

    return ETronDI_OK;
}

int CVideoDeviceModel::OpenDevice()
{
    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    bool bDepthStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH);

    int nFPS = bColorStream ? m_pVideoDeviceController->GetPreviewOptions()->GetStreamFPS(STREAM_COLOR) :
               bDepthStream ? m_pVideoDeviceController->GetPreviewOptions()->GetStreamFPS(STREAM_DEPTH) :
               0;

    int ret = EtronDI_OpenDevice2(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                  m_deviceSelInfo[0],
                                  m_imageData[STREAM_COLOR].nWidth, m_imageData[STREAM_COLOR].nHeight, m_imageData[STREAM_COLOR].bMJPG,
                                  m_imageData[STREAM_DEPTH].nWidth, m_imageData[STREAM_DEPTH].nHeight,
                                  DEPTH_IMG_NON_TRANSFER,
                                  true, nullptr,
                                  &nFPS,
                                  IMAGE_SN_SYNC);

    if(ETronDI_OK == ret){
        if(bColorStream){
            m_pVideoDeviceController->GetPreviewOptions()->SetStreamFPS(STREAM_COLOR, nFPS);
            if(bDepthStream) m_pVideoDeviceController->GetPreviewOptions()->SetStreamFPS(STREAM_DEPTH, nFPS);
        }else{
            m_pVideoDeviceController->GetPreviewOptions()->SetStreamFPS(STREAM_DEPTH, nFPS);
        }
    }

    return ret;
}

int CVideoDeviceModel::StartStreamingTask()
{
    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    bool bDepthStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH);

    if (bColorStream){
        CreateStreamTask(STREAM_COLOR);
    }

    if (bDepthStream){
        auto AdjustRealDepthWidth = [](int &nWidth, EtronDIImageType::Value type){
            if (EtronDIImageType::DEPTH_8BITS == type){
                nWidth *= 2;
            }
        };
        AdjustRealDepthWidth(m_imageData[STREAM_DEPTH].nWidth, m_imageData[STREAM_DEPTH].imageDataType);

        CreateStreamTask(STREAM_DEPTH);
    }

    if (InterleaveModeSupport()){

        if (IsInterleaveMode()){
            m_bPrevLowLightValue = m_cameraPropertyModel[0]->GetCameraProperty(CCameraPropertyModel::LOW_LIGHT_COMPENSATION).nValue;
            m_cameraPropertyModel[0]->SetCameraPropertyValue(CCameraPropertyModel::LOW_LIGHT_COMPENSATION, 0);
            m_cameraPropertyModel[0]->SetCameraPropertySupport(CCameraPropertyModel::LOW_LIGHT_COMPENSATION, false);
        }
    }


    return ETronDI_OK;
}

int CVideoDeviceModel::StopStreaming(bool bRestart)
{
    ChangeState(OPENED);

    if (m_pVideoDeviceController->GetPreviewOptions()->IsIMUSyncWithFrame()){
        m_pVideoDeviceController->StopIMUSyncWithFrame();
    }

    StopStreamingTask();
    if (m_pVideoDeviceController->GetPreviewOptions()->IsPointCloudViewer()){
        StopFrameGrabber();
    }

    if (!bRestart) ClosePreviewView();

    SetIRValue(0);

    CloseDevice();

    return ETronDI_OK;
}

int CVideoDeviceModel::StopStreamingTask()
{
    for (CTaskInfo *pTask : m_taskInfoStorage){
        CThreadWorkerManage::GetInstance()->RemoveTask(pTask);
    }

    m_taskInfoStorage.clear();

    if (InterleaveModeSupport()){

        if(IsInterleaveMode()){
            m_cameraPropertyModel[0]->SetCameraPropertySupport(CCameraPropertyModel::LOW_LIGHT_COMPENSATION, true);
            m_cameraPropertyModel[0]->SetCameraPropertyValue(CCameraPropertyModel::LOW_LIGHT_COMPENSATION, m_bPrevLowLightValue);
        }

        SetInterleaveModeEnable(false);
    }

    return ETronDI_OK;
}

int CVideoDeviceModel::CloseDevice()
{
    return EtronDI_CloseDevice(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                  m_deviceSelInfo[0]);
}

int CVideoDeviceModel::ClosePreviewView()
{
    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    bool bDepthStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH);

    if (bColorStream){
        m_pVideoDeviceController->GetControlView()->ClosePreviewView(STREAM_COLOR);
    }

    if (bDepthStream){
        m_pVideoDeviceController->GetControlView()->ClosePreviewView(STREAM_DEPTH);
    }

    if (m_pVideoDeviceController->GetPreviewOptions()->IsPointCloudViewer()){
        m_pVideoDeviceController->GetControlView()->ClosePointCloud();
    }

    return ETronDI_OK;
}

int CVideoDeviceModel::StopFrameGrabber()
{
    if(m_pFrameGrabber) {
        delete m_pFrameGrabber;
        m_pFrameGrabber = nullptr;
    }
    return ETronDI_OK;
}

int CVideoDeviceModel::AdjustRegister()
{
    if(GetState() != STREAMING) return ETronDI_OK;

    int ret =  RegisterSettings::DM_Quality_Register_Setting(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                             m_deviceSelInfo[0],
                                                             m_deviceInfo[0].deviceInfomation.wPID);

    AdjustFocalLength();

    if (!IsHWPP()){
        SetHWPP(true);
    }

    m_pVideoDeviceController->GetControlView()->UpdateUI();

    return ret;
}

int CVideoDeviceModel::ConfigDepthFilter()
{
    if (!m_pVideoDeviceController) return ETronDI_OK;

    DepthFilterOptions *pDepthFilterOptions = m_pVideoDeviceController->GetDepthFilterOptions();

    pDepthFilterOptions->EnableDepthFilter(true);
    pDepthFilterOptions->SetState(DepthFilterOptions::MIN);

    return ETronDI_OK;
}

int CVideoDeviceModel::DoImageGrabber(CTaskInfo::TYPE type)
{
    STREAM_TYPE streamType;
    switch (type){
        case CTaskInfo::GRABBER_VIDEO_IMAGE_COLOR:
            streamType = STREAM_COLOR;
            break;
        case CTaskInfo::GRABBER_VIDEO_IMAGE_COLOR_SLAVE:
            streamType = STREAM_COLOR_SLAVE;
            break;
        case CTaskInfo::GRABBER_VIDEO_IMAGE_DEPTH:
            streamType = STREAM_DEPTH;
            break;
        case CTaskInfo::GRABBER_VIDEO_IMAGE_DEPTH_60mm:
            streamType = STREAM_DEPTH_60mm;
            break;
        case CTaskInfo::GRABBER_VIDEO_IMAGE_DEPTH_150mm:
            streamType = STREAM_DEPTH_150mm;
            break;
        case CTaskInfo::GRABBER_VIDEO_IMAGE_KOLOR:
            streamType = STREAM_KOLOR;
            break;
        case CTaskInfo::GRABBER_VIDEO_IMAGE_KOLOR_SLAVE:
            streamType = STREAM_KOLOR_SLAVE;
            break;
        case CTaskInfo::GRABBER_VIDEO_IMAGE_TRACK:
            streamType = STREAM_TRACK;
            break;
        default:
            return ETronDI_NotSupport;
    }

    int ret = GetImage(streamType);
    HandleGetImageResult(streamType, ret);
    if(ETronDI_OK == ret && m_pVideoDeviceController->GetPreviewOptions()->IsPointCloudViewer() &&
       m_pFrameGrabber){
        UpdateFrameGrabberData(streamType);
    }

    return ret;
}

int CVideoDeviceModel::GetImage(STREAM_TYPE type)
{
    int ret;
    switch (type){
        case STREAM_COLOR:
            ret = GetColorImage();
            break;
        case STREAM_DEPTH:
            ret = GetDepthImage();
            break;
        default:
            return ETronDI_NotSupport;
    }

    return ret;
}

int CVideoDeviceModel::GetColorImage()
{
    QMutexLocker locker(&m_streamMutex[STREAM_COLOR]);
    unsigned long int nImageSize = 0;
    int nSerial = EOF;
    int ret = EtronDI_GetColorImage(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                    m_deviceSelInfo[0],
                                    &m_imageData[STREAM_COLOR].imageBuffer[0],
                                    &nImageSize,
                                    &nSerial,
                                    m_imageData[STREAM_COLOR].depthDataType);

    if (ETronDI_OK != ret) return ret;

    return ProcessImage(STREAM_COLOR, nImageSize, nSerial);
}

int CVideoDeviceModel::FirstSuccessGetImageCallback(STREAM_TYPE type)
{
    QMutexLocker locker(&m_streamMutex[type]);
    if (STREAM_DEPTH == type ||
        STREAM_DEPTH_30mm == type){
        CTaskInfo *pInfo = CTaskInfoManager::GetInstance()->RequestTaskInfo(CTaskInfo::VIDEO_QUALITY_REGISTER_SETTING, this);
        CThreadWorkerManage::GetInstance()->AddTask(pInfo);
    }

    return ETronDI_OK;
}

int CVideoDeviceModel::GetDepthImage()
{
    QMutexLocker locker(&m_streamMutex[STREAM_DEPTH]);
    unsigned long int nImageSize = 0;
    int nSerial = EOF;
    int ret = EtronDI_GetDepthImage(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                    m_deviceSelInfo[0],
                                    &m_imageData[STREAM_DEPTH].imageBuffer[0],
                                    &nImageSize,
                                    &nSerial,
                                    m_imageData[STREAM_DEPTH].depthDataType);

    if (ETronDI_OK != ret) return ret;

    return ProcessImage(STREAM_DEPTH, nImageSize, nSerial);
}

int CVideoDeviceModel::HandleGetImageResult(STREAM_TYPE type, int getImageResult)
{
    if (STREAMING != GetState()) return getImageResult;

    if (ETronDI_OK == getImageResult){
        if(GetColdResetThresholdMs(type) == FirstOpenDeviceColdeRestThresholdMs()){
            FirstSuccessGetImageCallback(type);
            SetColdResetThresholdMs(type, OpenDeviceColdeRestThresholdMs());
        }

        SetLastestSuccessTime(type, QTime::currentTime());

    }else{
        QTime lastestSuccessTime = GetLastestSuccessTime(type);
        if (lastestSuccessTime.msecsTo(QTime::currentTime()) > GetColdResetThresholdMs(type) ||
            lastestSuccessTime.msecsTo(QTime::currentTime()) < 0){
            static QMutex mutex;
            QMutexLocker locker(&mutex);
            if(!m_coldResetTask){
                SetColdResetStartTime(QTime::currentTime());
                m_coldResetTask = CTaskInfoManager::GetInstance()->RequestTaskInfo(CTaskInfo::VIDEO_COLD_RESET, this);
                CThreadWorkerManage::GetInstance()->AddTask(m_coldResetTask);
            }
        }
    }

    return getImageResult;
}

int CVideoDeviceModel::ProcessImageCallback(STREAM_TYPE streamType,
                                            int nImageSize, int nSerialNumber)
{
    if (IsInterleaveMode()){
        if (STREAM_COLOR == streamType){
            if (1 == nSerialNumber % 2) return ETronDI_OK;
            if (m_nLastInterLeaveColorSerial + 2 != nSerialNumber){
                m_nLastInterLeaveColorSerial = nSerialNumber;
                return ETronDI_OK;
            }
            m_nLastInterLeaveColorSerial = nSerialNumber;
        }

        if (STREAM_DEPTH == streamType){
            if (0 == nSerialNumber % 2) return ETronDI_OK;
            if (m_nLastInterLeaveDepthSerial + 2 != nSerialNumber){
                m_nLastInterLeaveDepthSerial = nSerialNumber;
                return ETronDI_OK;
            }
            m_nLastInterLeaveDepthSerial = nSerialNumber;
        }
    }

    CEtronUIView *pView = m_pVideoDeviceController->GetControlView();
    if (!pView) return ETronDI_OK;

    if (IsIMUSyncWithFrame()){
        return CFrameSyncManager::GetInstance()->SyncImageCallback(this,
                                                            m_imageData[streamType].imageDataType, streamType,
                                                            &m_imageData[streamType].imageBuffer[0],
                                                            nImageSize,
                                                            m_imageData[streamType].nWidth, m_imageData[streamType].nHeight,
                                                            nSerialNumber, nullptr);
    }else{
        return pView->ImageCallback(m_imageData[streamType].imageDataType, streamType,
                                    &m_imageData[streamType].imageBuffer[0],
                                    nImageSize,
                                    m_imageData[streamType].nWidth, m_imageData[streamType].nHeight,
                                    nSerialNumber, nullptr);
    }
}

int CVideoDeviceModel::ProcessImage(STREAM_TYPE streamType,
                                    int nImageSize, int nSerialNumber)
{
    SerialCountToFrameCount(streamType ,nSerialNumber);
    return ProcessImageCallback(streamType, nImageSize, nSerialNumber);
}

int CVideoDeviceModel::UpdateFrameGrabberData(STREAM_TYPE streamType)
{
    bool bIsDpethOutput = PreviewOptions::POINT_CLOUDE_VIEWER_OUTPUT_DEPTH == m_pVideoDeviceController->GetPreviewOptions()->GetPointCloudViewOutputFormat();
    switch(streamType){
        case STREAM_COLOR:
        case STREAM_KOLOR:
        {
            CImageDataModel *pColorImageData = GetPreivewImageDataModel(streamType);
            if (!pColorImageData) return ETronDI_NullPtr;

            QMutexLocker locker(&pColorImageData->GetDataMutex());

            BYTE *pBuffer = &pColorImageData->GetRGBData()[0];
            int nBytesPerPixelColor = 3;
            if (bIsDpethOutput){
                CImageDataModel *pDepthImageData = GetPreivewImageDataModel(STREAM_DEPTH);
                if (pDepthImageData &&
                    pDepthImageData->GetWidth() > 0 && pDepthImageData->GetHeight() > 0){
                    if (pDepthImageData->GetWidth() == pColorImageData->GetWidth() &&
                        pDepthImageData->GetHeight() == pColorImageData->GetHeight()){
                        pBuffer = &pDepthImageData->GetRGBData()[0];
                    }else{
                        if (m_imageData[STREAM_RESERVED].imageBuffer.size() != pColorImageData->GetWidth() * pColorImageData->GetHeight() * 3){
                            m_imageData[STREAM_RESERVED].imageBuffer.resize(pColorImageData->GetWidth() * pColorImageData->GetHeight() * 3);
                        }
                        PlyWriter::resampleImage(pDepthImageData->GetWidth(), pDepthImageData->GetHeight(), &pDepthImageData->GetRGBData()[0],
                                                 pColorImageData->GetWidth(), pColorImageData->GetHeight(), &m_imageData[STREAM_RESERVED].imageBuffer[0],
                                                 nBytesPerPixelColor);
                        pBuffer = &m_imageData[STREAM_RESERVED].imageBuffer[0];
                    }
                }
            }

            m_pFrameGrabber->SetFrameFormat(FrameGrabber::FRAME_POOL_INDEX_COLOR,
                                            pColorImageData->GetWidth(), pColorImageData->GetHeight(),
                                            nBytesPerPixelColor);
            m_pFrameGrabber->UpdateFrameData(FrameGrabber::FRAME_POOL_INDEX_COLOR,
                                             pColorImageData->GetSerialNumber(),
                                             pBuffer,
                                             pColorImageData->GetRGBData().size());
            break;
        }
        case STREAM_DEPTH:
        {
            CImageDataModel *pDepthData = GetPreivewImageDataModel(STREAM_DEPTH);
            if (!pDepthData) return ETronDI_NullPtr;

            int nBytesPerPixelDepth = 2;
            if(EtronDIImageType::DEPTH_8BITS == m_imageData[STREAM_DEPTH].imageDataType) nBytesPerPixelDepth = 1;
            m_pFrameGrabber->SetFrameFormat(FrameGrabber::FRAME_POOL_INDEX_DEPTH,
                                            pDepthData->GetWidth(), pDepthData->GetHeight(),
                                            nBytesPerPixelDepth);
            m_pFrameGrabber->UpdateFrameData(FrameGrabber::FRAME_POOL_INDEX_DEPTH,
                                             pDepthData->GetSerialNumber(),
                                             &pDepthData->GetRawData()[0],
                                             pDepthData->GetRawData().size());
            break;
        }
        default: return ETronDI_NotSupport;
    }

    return ETronDI_OK;
}

int CVideoDeviceModel::CreateStreamTask(STREAM_TYPE type)
{
    SetColdResetThresholdMs(type, FirstOpenDeviceColdeRestThresholdMs());
    SetLastestSuccessTime(type, QTime::currentTime());

    CTaskInfo::TYPE taskType;
    switch (type){
        case STREAM_COLOR: taskType = CTaskInfo::GRABBER_VIDEO_IMAGE_COLOR; break;
        case STREAM_COLOR_SLAVE: taskType = CTaskInfo::GRABBER_VIDEO_IMAGE_COLOR_SLAVE; break;
        case STREAM_DEPTH: taskType = CTaskInfo::GRABBER_VIDEO_IMAGE_DEPTH; break;
        case STREAM_DEPTH_60mm: taskType = CTaskInfo::GRABBER_VIDEO_IMAGE_DEPTH_60mm; break;
        case STREAM_DEPTH_150mm: taskType = CTaskInfo::GRABBER_VIDEO_IMAGE_DEPTH_150mm; break;
        case STREAM_KOLOR: taskType = CTaskInfo::GRABBER_VIDEO_IMAGE_KOLOR; break;
        case STREAM_KOLOR_SLAVE: taskType = CTaskInfo::GRABBER_VIDEO_IMAGE_KOLOR_SLAVE; break;
        case STREAM_TRACK: taskType = CTaskInfo::GRABBER_VIDEO_IMAGE_TRACK; break;
        default: return ETronDI_NotSupport;
    }

    CTaskInfo *pInfo = CTaskInfoManager::GetInstance()->RequestTaskInfo(taskType, this);
    CThreadWorkerManage::GetInstance()->AddTask(pInfo);
    m_taskInfoStorage.push_back(std::move(pInfo));

    return ETronDI_OK;
}

void CVideoDeviceModel::SerialCountToFrameCount(STREAM_TYPE streamType, int &nSerialNumber)
{
    QMutexLocker locker(&m_serialCountMutex);

    auto CheckSerialCountRecycle = [ & ] ( const STREAM_TYPE streamType , const int nSerialCount)
    {
        if (!m_mapSerialCountLast.count( streamType )) return;
        if ( nSerialCount < m_mapSerialCountLast[ streamType ] ) m_mapSerialCountLast[ streamType ] = m_mapSerialCountLast[ streamType ] - 65535;
    };

    auto ResetSerialCount = [ & ] ()
    {
        m_mapSerialCountLast.clear();
        m_mapSerialCountDiff.clear();
        m_mapSerialCount2FrameCount.clear();
    };

    if ( 0 == m_mapSerialCountDiff.count( streamType ))
    {
        if ( 1 == nSerialNumber ) // frame-count
        {
            m_mapSerialCountDiff[ streamType ] = 1;

            return;
        }
        else if ( 0 != m_mapSerialCountLast.count( streamType ))
        {
            CheckSerialCountRecycle( streamType , nSerialNumber);

            const int iDiff = nSerialNumber - m_mapSerialCountLast[ streamType ];

            if ( iDiff ) {
                m_mapSerialCountDiff[ streamType ] = nSerialNumber - m_mapSerialCountLast[ streamType ];
            }
        }else {
            m_mapSerialCount2FrameCount[ streamType ] = 0;
        }
    }

    if (m_mapSerialCountDiff.count( streamType ) > 0){

        if ( 1 == m_mapSerialCountDiff[ streamType ] ) // frame-count
        {
            return;
        }
        else // serial-count
        {
            CheckSerialCountRecycle( streamType, nSerialNumber );

            m_mapSerialCount2FrameCount[ streamType ]++;
            int nDiff = (nSerialNumber - m_mapSerialCountLast[ streamType ]) - m_mapSerialCountDiff[ streamType ];
            if ( nDiff >= SERIAL_THRESHOLD  ||  nDiff <= -SERIAL_THRESHOLD) { // reset count if count not match
                nSerialNumber = m_mapSerialCount2FrameCount[ streamType ];
                ResetSerialCount();
                return;
            }
        }
    }

    m_mapSerialCountLast[ streamType ] = nSerialNumber;

    if (m_mapSerialCount2FrameCount.count(streamType) > 0){
        nSerialNumber = m_mapSerialCount2FrameCount[ streamType ];
    }

}

std::vector<CloudPoint> CVideoDeviceModel::GeneratePointCloud(
        STREAM_TYPE depthType,
        std::vector<BYTE> &depthData, unsigned short nDepthWidth, unsigned short nDepthHeight,
        std::vector<BYTE> &colorData, unsigned short nColorWidth, unsigned short nColorHeight,
        bool bEnableFilter)
{
    EtronDIImageType::Value depthImageType = EtronDIImageType::DepthDataTypeToDepthImageType(GetDepthDataType());

    int nZNear, nZFar;
    m_pVideoDeviceController->GetPreviewOptions()->GetZRange(nZNear, nZFar);

    bool bUsePlyFilter = bEnableFilter &&
                         m_pVideoDeviceController->GetPreviewOptions()->IsPlyFilter() &&
                         PlyFilterSupprot();

    std::vector<float> imgFloatBufOut;
    if (bUsePlyFilter){
        if(ETronDI_OK != PlyFilterTransform(depthData, colorData,
                                            nDepthWidth, nDepthHeight,
                                            nColorWidth, nColorHeight,
                                            imgFloatBufOut, GetRectifyLogData(depthType),
                                            depthImageType)){
            return {};
        }
    }

    return GeneratePointCloud(depthData, colorData,
                              nDepthWidth, nDepthHeight,
                              nColorWidth, nColorHeight,
                              GetRectifyLogData(depthType),
                              depthImageType,
                              nZNear, nZFar,
                              bUsePlyFilter, imgFloatBufOut);
}

std::vector<CloudPoint> CVideoDeviceModel::GeneratePointCloud(std::vector<unsigned char> &depthData,
                                                              std::vector<unsigned char> &colorData,
                                                              unsigned short nDepthWidth,
                                                              unsigned short nDepthHeight,
                                                              unsigned short nColorWidth,
                                                              unsigned short nColorHeight,
                                                              eSPCtrl_RectLogData rectifyLogData,
                                                              EtronDIImageType::Value depthImageType,
                                                              int nZNear, int nZFar,
                                                              bool bUsePlyFilter, std::vector<float> imgFloatBufOut)
{
    std::vector<CloudPoint> cloudPoints;
    if(bUsePlyFilter && !imgFloatBufOut.empty()){
        PlyWriter::etronFrameTo3D_PlyFilterFloat(nDepthWidth, nDepthHeight,
                                                 imgFloatBufOut,
                                                 nColorWidth, nColorHeight,
                                                 colorData,
                                                 &rectifyLogData, depthImageType,
                                                 cloudPoints,
                                                 true, nZNear, nZFar,
                                                 true, false, 1.0f);
    }else{
        PlyWriter::etronFrameTo3D(nDepthWidth, nDepthHeight, depthData,
                                  nColorWidth, nColorHeight, colorData,
                                  &rectifyLogData, depthImageType,
                                  cloudPoints,
                                  true, nZNear, nZFar,
                                  true, false, 1.0f);
    }

    return cloudPoints;
}

int CVideoDeviceModel::PlyFilterTransform(std::vector<unsigned char> &depthData,
                                          std::vector<unsigned char> &colorData,
                                          unsigned short &nDepthWidth,
                                          unsigned short &nDepthHeight,
                                          unsigned short &nColorWidth,
                                          unsigned short &nColorHeight,
                                          std::vector<float> &imgFloatBufOut,
                                          eSPCtrl_RectLogData &rectifyLogData,
                                          EtronDIImageType::Value depthImageType)
{
    float ratio = (float)rectifyLogData.OutImgHeight / nDepthHeight;
    if (ratio != 1.0f) {
        int resampleWidthDepth = nDepthWidth * ratio;
        int resampleHeightDepth = nDepthHeight * ratio;

        int bufSize = resampleWidthDepth * resampleHeightDepth * 2;
        std::vector<unsigned char> dArrayResized(bufSize);
        if ( depthImageType == EtronDIImageType::DEPTH_8BITS )
            PlyWriter::MonoBilinearFineScaler( &depthData[0], &dArrayResized[0], nDepthWidth, nDepthHeight, resampleWidthDepth, resampleHeightDepth, 1);
        else
            PlyWriter::MonoBilinearFineScaler_short( (unsigned short*)&depthData[0], (unsigned short*)&dArrayResized[0], nDepthWidth, nDepthHeight, resampleWidthDepth, resampleHeightDepth, 1 );

        depthData.resize(bufSize);
        depthData.assign(dArrayResized.begin(), dArrayResized.end());

        nDepthWidth = resampleWidthDepth;
        nDepthHeight = resampleHeightDepth;
    }

    switch (depthImageType){
        case EtronDIImageType::DEPTH_8BITS:
        {
            //D8 TO D11 IMAGE +
            std::vector< BYTE > bufDepthTmpout;
            bufDepthTmpout.resize( depthData.size() * 2 );

            WORD* pDepthOut = ( WORD* )bufDepthTmpout.data();

            for ( size_t i = 0; i != depthData.size(); i++ )
            {
                pDepthOut[ i ] = ( ( WORD )depthData[ i ] ) << 3;
            }

            //D8 TO D11 IMAGE -
            PlyFilter::CF_FILTER(bufDepthTmpout, colorData,
                                 nDepthWidth, nDepthHeight,
                                 nColorWidth, nColorHeight,
                                 imgFloatBufOut,
                                 &rectifyLogData);
            break;
        }
        case EtronDIImageType::DEPTH_11BITS:
        {
            PlyFilter::UnavailableDisparityCancellation(depthData, nDepthWidth, nDepthHeight, 16383);
            PlyFilter::CF_FILTER(depthData, colorData,
                                 nDepthWidth, nDepthHeight,
                                 nColorWidth, nColorHeight,
                                 imgFloatBufOut,
                                 &rectifyLogData);
            break;
        }
        case EtronDIImageType::DEPTH_14BITS:
        {
            PlyFilter::CF_FILTER_Z14(depthData, colorData,
                                     nDepthWidth, nDepthHeight,
                                     nColorWidth, nColorHeight,
                                     imgFloatBufOut,
                                     &rectifyLogData);
            break;
        }
        default:
            break;
    }

    if (imgFloatBufOut.empty()){
        return ETronDI_NullPtr;
    }

    return ETronDI_OK;
}

void CVideoDeviceModel::FrameGrabberCallbackFn(std::vector<unsigned char>& bufDepth, int widthDepth, int heightDepth,
                                               std::vector<unsigned char>& bufColor, int widthColor, int heightColor,
                                               int serialNumber, void* pParam)
{
    CVideoDeviceModel *pModel = static_cast<CVideoDeviceModel *>(pParam);
    pModel->ProcessFrameGrabberCallback(bufDepth, widthDepth, heightDepth,
                                        bufColor, widthColor, heightColor,
                                        serialNumber);
}

int CVideoDeviceModel::ProcessFrameGrabberCallback(std::vector<unsigned char>& bufDepth, int widthDepth, int heightDepth,
                                                   std::vector<unsigned char>& bufColor, int widthColor, int heightColor,
                                                   int serialNumber)
{


    size_t nPointCloudSize = widthColor * heightColor * 3;
    if (m_pointCloudDepth.size() != nPointCloudSize) m_pointCloudDepth.resize(nPointCloudSize);
    else                                             std::fill(m_pointCloudDepth.begin(), m_pointCloudDepth.end(), 0.0f);

    if (m_pointCloudColor.size() != nPointCloudSize) m_pointCloudColor.resize(nPointCloudSize);
    else                                             std::fill(m_pointCloudColor.begin(), m_pointCloudColor.end(), 0);

    FrameGrabberDataTransform(bufDepth, widthDepth, heightDepth,
                              bufColor, widthColor, heightColor,
                              serialNumber);

    int nZNear, nZFar;
    m_pVideoDeviceController->GetPreviewOptions()->GetZRange(nZNear, nZFar);
    float fZNear = (nZNear * 1.0f) > 0 ? nZNear : 0.1f;
    float fZFar = (nZFar * 1.0f) > 0? nZFar : 1000.0f;
    if(ETronDI_OK == EtronDI_GetPointCloud(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                           m_deviceSelInfo[0],
                                           &bufColor[0], widthColor, heightColor,
                                           &bufDepth[0], widthDepth, heightDepth,
                                           &m_pointCloudInfo,
                                           &m_pointCloudColor[0],
                                           &m_pointCloudDepth[0],
                                           fZNear, fZFar)){
        m_pVideoDeviceController->GetControlView()->PointCloudCallback(m_pointCloudDepth, m_pointCloudColor);
    }
    return ETronDI_OK;
}

int CVideoDeviceModel::InitIMU()
{
    if (!IMUSupport()) return ETronDI_NotSupport;

    if (m_pIMUModel) delete m_pIMUModel;

    for (CIMUModel::INFO info : GetIMUInfo()){
        m_pIMUModel = new CIMUModel(info, this);

        if (m_pIMUModel->IsValid()) break;

        delete m_pIMUModel;
        m_pIMUModel = nullptr;
    }

    if (!m_pIMUModel) {
        m_pIMUModel = new CIMUModel({0, 0, CIMUModel::IMU_UNKNOWN},
                                    this);
    }

    return ETronDI_OK;
}

void CVideoDeviceModel::SetIMUSyncWithFrame(bool bSync)
{
    if (!m_pVideoDeviceController) return;
    if (bSync == m_pVideoDeviceController->GetPreviewOptions()->IsIMUSyncWithFrame()) return;

    if (bSync){
        m_pVideoDeviceController->StartIMUSyncWithFrame();
    }else{
        m_pVideoDeviceController->StopIMUSyncWithFrame();
    }

    m_pVideoDeviceController->GetPreviewOptions()->SetIMUSyncWithFrame(bSync);
}

bool CVideoDeviceModel::IsIMUSyncWithFrame()
{
    if (!IMUSupport()) return false;
    if (!GetIMUModel()) return false;
    if (!m_pVideoDeviceController) return false;

    return m_pVideoDeviceController->GetPreviewOptions()->IsIMUSyncWithFrame();
}

bool CVideoDeviceModel::IsInterleaveMode()
{
    if (!InterleaveModeSupport()) return false;

    bool bIsInterLeave = false;

    bool bColorStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_COLOR);
    bool bDepthStream = m_pVideoDeviceController->GetPreviewOptions()->IsStreamEnable(STREAM_DEPTH);

    if (!bColorStream || !bDepthStream) {
        return bIsInterLeave;
    }

    int nFPS = m_pVideoDeviceController->GetPreviewOptions()->GetStreamFPS(STREAM_COLOR);

    for (int nInterleaveFPS : GetInterleaveModeFPS()){
        if (nInterleaveFPS == nFPS){
            bIsInterLeave = true;
            break;
        }
    }

    return bIsInterLeave;
}

int CVideoDeviceModel::AdjustInterleaveModeState()
{
    if (!InterleaveModeSupport()) return ETronDI_NotSupport;
    if(STREAMING != GetState()) return ETronDI_OK;

    return SetInterleaveModeEnable(IsInterleaveMode());
}

std::vector<int> CVideoDeviceModel::GetInterleaveModeFPS()
{
    if (!InterleaveModeSupport()) return {};

    ModeConfig::MODE_CONFIG modeConfig = m_pVideoDeviceController->GetModeConfigOptions()->GetCurrentModeInfo();

    return { modeConfig.iInterLeaveModeFPS };
}

int CVideoDeviceModel::SetInterleaveModeEnable(bool bEnable)
{
    if (!InterleaveModeSupport()) return ETronDI_NotSupport;

    int ret;
    RETRY_ETRON_API(ret, EtronDI_EnableInterleave(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                                  m_deviceSelInfo[0],
                                                  bEnable));

    return ret;
}
