/*
 * Copyright (C) 2015-2017 ICL/ITRI
 * All rights reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of ICL/ITRI and its suppliers, if any.
 * The intellectual and technical concepts contained
 * herein are proprietary to ICL/ITRI and its suppliers and
 * may be covered by Taiwan and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from ICL/ITRI.
 */

#include "video/ColorFrameProducer.h"
#include "video/factory.h"
#include "video/coders.h"
#include "video/pc_coders.h"
#include "EYS3DSystem.h"
#include "base/threads/Async.h"
#include "eSPDI.h"
#include "utils.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#define LOG_TAG "ColorFrameProducer"

namespace libeYs3D    {
namespace video    {

ColorFrameProducer::ColorFrameProducer(CameraDevice *cameraDevice)
    : FrameProducer(cameraDevice,
                    cameraDevice->mColorWidth, cameraDevice->mColorHeight,
                    cameraDevice->mColorFormat, cameraDevice->mActualFps)    {
}

void ColorFrameProducer::checkIMUDeviceCBEnablement()    {
    if(!mCameraDevice->isIMUDevicePresent())    return;
    
    if((mFrameProducerState & FB_APP_STREAM_ACTIVATED) ||
       (mFrameProducerState & FB_PC_STREAM_ACTIVATED))
        mCameraDevice->mIMUDevice->enableColorStream();
    else
        mCameraDevice->mIMUDevice->pauseColorStream();
}

int ColorFrameProducer::getRawFormatBytesPerPixel(uint32_t format)    {
    return get_color_image_format_byte_length_per_pixel((EtronDIImageType::Value)format);
}

int ColorFrameProducer::readFrame(Frame *frame)    {
    int ret = 0;
    ret = mCameraDevice->readColorFrame(frame->dataVec.data(), frame->dataBufferSize,
                                        &(frame->actualDataBufferSize), &(frame->serialNumber));
                                        
    return ret;
}

int ColorFrameProducer::produceRGBFrame(Frame *frame)    {
    int ret = 0;
    
    int64_t currTime = now_in_microsecond_high_res_time();
    {
        ret = color_image_produce_rgb_frame((const CameraDevice *)(this->mCameraDevice), frame);
    }
    int64_t newTime = now_in_microsecond_high_res_time();
    frame->rgbTranscodingTime = newTime - currTime;
    
    return ret;
}

int ColorFrameProducer::performFiltering(Frame *frame)    {
    return 0;
}

int ColorFrameProducer::performInterleave(Frame *frame)    {
    if(mCameraDevice->isInterleaveModeEnabled())    {
        frame->interleaveMode = true;
        if((frame->serialNumber % 2) == 0)
            return 0;
        else // drop it...
            return -1;
    } else    {
        frame->interleaveMode = false;
    }
    
    return 0;
}

int ColorFrameProducer::performAccuracyComputation(Frame *frame)    {
    return 0;
}

void ColorFrameProducer::performSnapshotWork(Frame *frame)    {
    // clone data
    std::vector<uint8_t> dataVec(frame->dataVec); // using copy constructor
    std::vector<uint8_t> rgbVec(frame->rgbVec); // using copy constructor
    int64_t tsMs = frame->tsUs / 1000;
    uint32_t serialNumber = frame->serialNumber;
    int nWidth = frame->width;
    int nHeight = frame->height;
    const char *snapShotPath = libeYs3D::EYS3DSystem::getEYS3DSystem()->getSnapshotPath();
    int colorRawBytePerPixel = getRawFormatBytesPerPixel(frame->dataFormat);
    uint64_t actualRGBBufferSize = frame->actualRGBBufferSize;

    // notify as soon as data cloning is finished
    mSnapshotFinishedSignal.send(1);
    
    {
        char colorPathRGB[PATH_MAX];
        char colorPathYUV[PATH_MAX];
        char tmpBuffer[128];
        
        get_time_YYYY_MM_DD_HH_MM_SS(tsMs, tmpBuffer, sizeof(tmpBuffer));
        snprintf(colorPathRGB, sizeof(colorPathRGB), "%s/snapshot-color-%" PRIu32 "-%s.bmp",
                 snapShotPath, serialNumber, tmpBuffer);
        snprintf(colorPathYUV, sizeof(colorPathYUV), "%s/snapshot-color-%" PRIu32 "-%s.yuv",
                 snapShotPath, serialNumber, tmpBuffer);
        
        save_bitmap(colorPathRGB, rgbVec.data(), nWidth, nHeight);
        save_yuv(colorPathYUV, dataVec.data(), nWidth, nHeight, colorRawBytePerPixel);
    }
    
#if 0
    { // create rgb raw file 
        int fd;
        int i;
        char path[PATH_MAX];
        char tmpBuffer[128];
        
        get_time_YYYY_MM_DD_HH_MM_SS(tsMs, tmpBuffer, sizeof(tmpBuffer));
        snprintf(path, sizeof(path), "%s/snapshot-color-%" PRIu32 "-%s.raw",
                 snapShotPath, serialNumber, tmpBuffer);
        
        if(actualRGBBufferSize > 0)    {
            fd = open(path, O_RDWR | O_CLOEXEC | O_CREAT, 0664);
            ::write_fully(fd, (const void *)(rgbVec.data()), actualRGBBufferSize, LOG_TAG"-RGB");
            ::close (fd);
        }
    }
#endif
}

std::unique_ptr<FrameProducer> createColorFrameProducer(CameraDevice *cameraDevice)    {
    std::unique_ptr<FrameProducer> producer(new ColorFrameProducer(cameraDevice));
    return std::move(producer);
}

}  // namespace video
}  // namespace libeYs3D
