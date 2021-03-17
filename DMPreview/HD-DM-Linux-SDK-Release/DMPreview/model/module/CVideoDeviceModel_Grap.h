#ifndef CVIDEODEVICEMODEL_GRAP_H
#define CVIDEODEVICEMODEL_GRAP_H
#include "CVideoDeviceModel.h"

class CVideoDeviceModel_Grap : public CVideoDeviceModel
{
public:
    virtual int SetDepthDataType(int nDepthDataType){ return ETronDI_NotSupport;}
    virtual bool IsDepthDataTypeSupport(DEPTH_DATA_TYPE type){ return false; }
    virtual int UpdateDepthDataType(){ return ETronDI_NotSupport;}

    virtual int SetIRValue(unsigned short nValue){ return ETronDI_NotSupport;}
    virtual int UpdateIR(){ return ETronDI_NotSupport;}
    virtual int GetIRRange(unsigned short &nMin, unsigned short &nMax){ return ETronDI_NotSupport;}
    virtual unsigned short GetIRValue(){ return 0;}

    virtual bool IRExtendSupport(){ return false; }
    virtual bool IsIRExtended(){ return false; }
    virtual int ExtendIR(bool bEnable){ return ETronDI_NotSupport; }

    virtual bool HWPPSupprot(){ return false; }
    virtual bool IsHWPP(){ return false; }
    virtual int SetHWPP(bool bEnable){ return ETronDI_NotSupport; }

    virtual int InitDeviceSelInfo();
    virtual int InitDeviceInformation();

    virtual bool IsStreamSupport(STREAM_TYPE type);
    virtual int InitStreamInfoList();

    virtual bool IsStreamAvailable();

    virtual int PrepareOpenDevice();
    virtual int OpenDevice();
    virtual int StartStreamingTask();

    virtual int CloseDevice();
    virtual int ClosePreviewView();

    virtual int GetImage(STREAM_TYPE type);

    friend class CVideoDeviceModelFactory;
protected:
    CVideoDeviceModel_Grap(DEVSELINFO *pDeviceSelfInfo);

    virtual int GetColorImage(STREAM_TYPE type);
};

#endif // CVIDEODEVICEMODEL_GRAP_H
