#include "CEtronDeviceManager.h"
#include "CVideoDeviceModel.h"
#include "CVideoDeviceModelFactory.h"
#include "eSPDI.h"
#include <QDBusInterface>
#include "CIMUDeviceManager.h"

CEtronDeviceManager::CEtronDeviceManager():
m_pEtronDI(nullptr),
m_bEnableSDKLog(false)
{
    QDBusMessage m = QDBusMessage::createMethodCall("org.freedesktop.login1",
                                                    "/org/freedesktop/login1",
                                                    "org.freedesktop.login1.Manager",
                                                    "Inhibit");

    QList<QVariant> args;
    args.append("sleep:shutdown");
    args.append("DMPreview");
    args.append("Inhibit sleep:shutdown avoid nvidia driver issue.");
    args.append("block");
    m.setArguments(args);
    QDBusMessage response = QDBusConnection::systemBus().call(m);
    QList<QVariant> list = response.arguments();
    if (!list.empty()){
        static QVariant lock = list.first();
    }
}

CEtronDeviceManager::~CEtronDeviceManager()
{
    if (m_pEtronDI){
        EtronDI_Release(&m_pEtronDI);
        m_pEtronDI = nullptr;
    }

    CVideoDeviceModelFactory::ReleaseModels(m_videoDeviceModels);
}

void CEtronDeviceManager::EnableSDKLog(bool bEnable)
{
    if(m_bEnableSDKLog == bEnable) return;

    m_bEnableSDKLog = bEnable;
    Reconnect();
}

int CEtronDeviceManager::Reconnect()
{        
    for (CVideoDeviceModel *pModel : m_videoDeviceModels){
        pModel->ChangeState(CVideoDeviceModel::RECONNECTING);
    }

    std::vector<CVideoDeviceModel *> findDevices;
    InitEtronDI();

    CIMUDeviceManager::GetInstance()->Clear();
    CIMUDeviceManager::GetInstance()->Init();

    FindDevices(findDevices);

    if (findDevices.size() != m_videoDeviceModels.size()) return ETronDI_Init_Fail;

    for (CVideoDeviceModel *pTargetModel : m_videoDeviceModels){
        CVideoDeviceModel *pFindModel = nullptr;
        for (CVideoDeviceModel *pModel : findDevices){
            if (pTargetModel->EqualModel(pModel)){
                pFindModel = pModel;
                break;
            }
        }

        if (!pFindModel) {
            CVideoDeviceModelFactory::ReleaseModels(findDevices);
            return ETronDI_Init_Fail;
        }

        pTargetModel->AdjustDeviceSelfInfo(pFindModel->GetDeviceSelInfo()[0]);
    }

    for (CVideoDeviceModel *pModel : m_videoDeviceModels){
        pModel->ChangeState(CVideoDeviceModel::RECONNECTED);
    }

    CVideoDeviceModelFactory::ReleaseModels(findDevices);

    return ETronDI_OK;
}

int CEtronDeviceManager::UpdateDevice()
{
    return FindDevices(m_videoDeviceModels);
}

std::vector<CVideoDeviceModel *> CEtronDeviceManager::GetDeviceModels()
{
    return m_videoDeviceModels;
}

int CEtronDeviceManager::InitEtronDI()
{
    if (m_pEtronDI){
        EtronDI_Release(&m_pEtronDI);
        m_pEtronDI = nullptr;
    }

    return EtronDI_Init(&m_pEtronDI, m_bEnableSDKLog);
}

int CEtronDeviceManager::FindDevices(std::vector<CVideoDeviceModel *> &devices)
{
    if (!m_pEtronDI){
        if (ETronDI_OK != InitEtronDI()){
            return ETronDI_Init_Fail;
        }
    }

    devices.clear();
    int nDevCount = EtronDI_GetDeviceNumber(m_pEtronDI);
    for (int i = 0 ; i < nDevCount ; ++i){
        DEVSELINFO devSelInfo;
        devSelInfo.index = i;
        CVideoDeviceModel *pDeviceModel = CVideoDeviceModelFactory::CreateVideoDeviceModel(&devSelInfo);
        if (nullptr != pDeviceModel){
            devices.push_back(std::move(pDeviceModel));
        }
    }

    return ETronDI_OK;
}
