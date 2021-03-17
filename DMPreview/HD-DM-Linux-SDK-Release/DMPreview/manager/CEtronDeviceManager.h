#ifndef CETRONDEVICEMANAGER_H
#define CETRONDEVICEMANAGER_H

#include <vector>

class CVideoDeviceModel;
class CEtronDeviceManager
{
public:
    static CEtronDeviceManager *GetInstance(){
        static CEtronDeviceManager *pInstance = nullptr;
        if (!pInstance){
            pInstance = new CEtronDeviceManager();
        }

        return pInstance;
    }

    bool IsSDKLog(){ return m_bEnableSDKLog; }
    void EnableSDKLog(bool bEnable);

    int Reconnect();
    int UpdateDevice();
    std::vector<CVideoDeviceModel *> GetDeviceModels();

    void *GetEtronDI(){ return m_pEtronDI; }
private:
    CEtronDeviceManager();
    ~CEtronDeviceManager();

    int InitEtronDI();
    int FindDevices(std::vector<CVideoDeviceModel *> &devices);

private:
    void *m_pEtronDI;
    bool m_bEnableSDKLog;
    std::vector<CVideoDeviceModel *> m_videoDeviceModels;
};

#endif // CETRONDEVICEMANAGER_H
