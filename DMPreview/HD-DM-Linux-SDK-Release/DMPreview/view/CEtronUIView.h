﻿#ifndef CETRONUIVIEW_H
#define CETRONUIVIEW_H

#include "CVideoDeviceModel.h"
#include <vector>

#ifndef RUN_ON_UI_THREAD
#include <QThread>
#include <QApplication>
#include <QTimer>
#define RUN_ON_UI_THREAD(fullFunction) \
    do{ \
        if (QApplication::instance()->thread() != QThread::currentThread()) \
        { \
            QTimer timer; \
            timer.moveToThread(QApplication::instance()->thread()); \
            timer.setSingleShot(true); \
            bool bDone = false; \
            QObject::connect(&timer, &QTimer::timeout, [&]() \
            { \
                fullFunction \
                bDone = true; \
            }); \
            QMetaObject::invokeMethod(&timer, "start", Qt::QueuedConnection, Q_ARG(int, 0)); \
            while (!bDone) { \
                QThread::msleep(100); \
            } \
         }else{ \
            fullFunction \
         } \
     }while (false) \

#endif

class CImageDataModel;
class QWidget;
class CEtronUIView
{
public:
    CEtronUIView();
    void UpdateUI();

    virtual void BeginFrameSync(){}
    virtual void EndFrameSync(){}

    virtual void OpenPreviewView(CVideoDeviceModel::STREAM_TYPE type){ return; }
    virtual void ClosePreviewView(CVideoDeviceModel::STREAM_TYPE type){ return; }
    virtual CImageDataModel *GetPreviewImageData(CVideoDeviceModel::STREAM_TYPE type){ return nullptr; }
    virtual int ImageCallback(EtronDIImageType::Value imageType, CVideoDeviceModel::STREAM_TYPE streamType,
                              BYTE *pImageBuffer, int nImageSize,
                              int nWidth, int nHeight, int nSerialNumber,
                              void *pUserData){ return ETronDI_OK; }

    virtual void ClosePointCloud(){ return; }
    virtual int PointCloudCallback(std::vector<float> &cloudPoints, std::vector<BYTE> &colors){ return ETronDI_OK; }
    virtual void UpdateColorPalette(){ return; }

protected:
    virtual void UpdateSelf(){ return; }
    virtual void UpdateChildern();
    void UpdateChildernEtronUIView(QWidget *pWidget);

    void AddUpdateTimer(int mesc = 10);
private:
    QTimer m_updateTimer;
};

#endif // CETRONUIVIEW_H
