#include "CVideoDeviceModel_8036.h"
#include "CEtronDeviceManager.h"

CVideoDeviceModel_8036::CVideoDeviceModel_8036(DEVSELINFO *pDeviceSelfInfo):
CVideoDeviceModel_8036_8052(pDeviceSelfInfo),
m_bIsInterleaveSupport(false)
{

}

int CVideoDeviceModel_8036::Init()
{
    int ret = CVideoDeviceModel::Init();

    if (ETronDI_OK != ret) return ret;

    unsigned short value;
    RETRY_ETRON_API(ret, EtronDI_GetFWRegister(CEtronDeviceManager::GetInstance()->GetEtronDI(),
                                               m_deviceSelInfo[0],
                                               0xe5, &value,
                                               FG_Address_1Byte | FG_Value_1Byte));

    m_bIsInterleaveSupport = (ETronDI_OK == ret) && (1 == value);

    return ret;
}
