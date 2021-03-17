#ifndef CVIDEODEVICEMODEL_8036_H
#define CVIDEODEVICEMODEL_8036_H
#include "CVideoDeviceModel_8036_8052.h"

class CVideoDeviceModel_8036 : public CVideoDeviceModel_8036_8052
{
public:

    virtual int Init();

    virtual bool InterleaveModeSupport(){ return m_bIsInterleaveSupport; }

    friend class CVideoDeviceModelFactory;

protected:
    CVideoDeviceModel_8036(DEVSELINFO *pDeviceSelfInfo);

private:
    bool m_bIsInterleaveSupport;
};

#endif // CVIDEODEVICEMODEL_8036_H
