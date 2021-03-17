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

#include <assert.h>
#include <cstdint>
#include <functional>
#include <memory>
#include <new>
#include <utility>
#include <vector>

#include "video/PCFrameProducer.h"
#include "video/coders.h"
#include "video/pc_coders.h"
#include "base/Optional.h"
#include "base/threads/Async.h"
#include "video/Frame.h"
#include "utils.h"

#define DEBUGGING false
#define LOG_TAG "PCFrameProducer"

namespace libeYs3D    {
namespace video    {

using base::MessageChannel;

PCFrameProducer::PCFrameProducer(CameraDevice *cameraDevice)
    : mCameraDevice(cameraDevice), mFrameIndex(0)    {
    mColorProducerCallback = std::bind(&PCFrameProducer::colorProducerCallback,
                                       this, 
                                       std::placeholders::_1);
    mDepthProducerCallback = std::bind(&PCFrameProducer::depthProducerCallback,
                                       this, 
                                       std::placeholders::_1);
                                       
    cameraDevice->mColorFrameProducer->attachPCCallback(mColorProducerCallback);
    cameraDevice->mDepthFrameProducer->attachPCCallback(mDepthProducerCallback);
}

void PCFrameProducer::initialize()    {
    int i = 0;
    
    // Prefill the free queue with empty frames
    auto pixelCount = mCameraDevice->mColorWidth * mCameraDevice->mColorHeight;
    for(i = 0; i < kMaxFrames; ++i) {
    
#ifdef DEVICE_MEMORY_ALLOCATOR
        PCFrame f(pixelCount, 0, 0.0,
                  mCameraDevice->mPixelByteMemoryAllocator,
                  mCameraDevice->mPixelFloatMemoryAllocator);
#else
        PCFrame f(pixelCount, 0, 0.0);
#endif

        f.width = mCameraDevice->mColorWidth;
        f.height = mCameraDevice->mColorHeight;
        mPCFreeQueue.send(std::move(f));
    }
    
    uint64_t sz = (mCameraDevice->mColorWidth * mCameraDevice->mColorHeight) << 2;
    for(i = 0; i < kMaxFrames; ++i) {
        Frame f(sz, 0,
                (mCameraDevice->mColorWidth * mCameraDevice->mColorHeight), 0,
                sz, 0);

        mFreeColorFrameQueue.send(std::move(f));
    }
    
    for(i = 0; i < kMaxFrames; ++i) {
        Frame f(sz, 0,
                (mCameraDevice->mColorWidth * mCameraDevice->mColorHeight), 0,
                sz, 0);

        mFreeDepthFrameQueue.send(std::move(f));
    }
}

intptr_t PCFrameProducer::main()    {
    assert(mPCCallback);
    mIsStopped = false;
    
    this->initialize();
    
    base::async([this]() { sendFramesWorker(); });

    PCFrame *localPCFrame;
    while(true) {
        // Consumer
        // Blocks until either we get a frame or the queue is closed.
        auto pcFrame = mPCDataQueue.receive();
        if(!pcFrame)    break;
        
        localPCFrame = &*pcFrame;
        
        if(mFrameDumpCount > 0)    { // dump frame info if necesary
            mFrameDumpCount -= 1;
            if(mFrameLogFile)    {
                char buffer[256];
                localPCFrame->toStringSimple(buffer, sizeof(buffer));
                fprintf(mFrameLogFile, "%s\n", buffer);
                            
                if(mFrameDumpCount == 0)    {
                    fflush(mFrameLogFile);
                    fclose(mFrameLogFile);
                    mFrameLogFile = nullptr;
                }
            }
        }
        
        mPCCallback(localPCFrame);
        
        // Blocks until there is space in the queue or is closed.
        if(!mPCFreeQueue.send(std::move(*localPCFrame))) {
            break;
        }
    }
    
    LOG_INFO(LOG_TAG, "%s Exiting callback thread...", getName());
    pokeReceiver();

    return 0;
}

bool PCFrameProducer::colorProducerCallback(const Frame *frame)    {
    //LOG_INFO(LOG_TAG, "[+ COLOR +] colorProducerCallback S/N=%u, interleave=%s",
    //         frame->serialNumber, (frame->interleaveMode ? "yes" : "no"));
    auto localFrame = mFreeColorFrameQueue.receive();
    if(!localFrame)    return true;
    
    localFrame->clone(frame);
    mColorFrameQueue.send(std::move(*localFrame));

    return true;
}

bool PCFrameProducer::depthProducerCallback(const Frame *frame)    {
    //LOG_INFO(LOG_TAG, "[+ DEPTH +] depthProducerCallback S/N=%u, interleave=%s",
    //         frame->serialNumber, (frame->interleaveMode ? "yes" : "no"));
    auto localFrame = mFreeDepthFrameQueue.receive();
    if(!localFrame)    return true;
    
    localFrame->clone(frame);
    mDepthFrameQueue.send(std::move(*localFrame));

    return true;
}

void PCFrameProducer::pokeReceiver() {
    mSignal.send(1);
}

void PCFrameProducer::waitForPoke()    {
    mSignal.receive();
}

void PCFrameProducer::stop()    {
    LOG_INFO(LOG_TAG, "Stopping %s...", getName());

    mIsStopped = true;

    // mColorFrameProducer & mDepthFrameProducer might have been stopped.
    //mCameraDevice->mColorFrameProducer->pausePCCallback();
    //mCameraDevice->mDepthFrameProducer->pausePCCallback();
    
    mPCDataQueue.stop();
    mPCFreeQueue.stop();
    mColorFrameQueue.stop();
    mFreeColorFrameQueue.stop();
    mDepthFrameQueue.stop();
    mFreeDepthFrameQueue.stop();
    
    mSnapshotSignal.stop();
    
    // To prevent thread object deletion race condition,
    // Making sure both thread are finished...
    waitForPoke();
    waitForPoke();
    
    LOG_INFO(LOG_TAG, "%s Stopped...", getName());
}

void PCFrameProducer::performSnapshotWork(const Frame *colorFrame, const Frame *depthFrame)    {
    // clone data
    std::vector<uint8_t> depthData(depthFrame->dataVec); // using copy constructor
    std::vector<uint8_t> depthDataRGB(depthFrame->rgbVec); // using copy constructor
    int64_t tsMs = depthFrame->tsUs / 1000;
    uint32_t serialNumber = depthFrame->serialNumber;
    int nDepthWidth = depthFrame->width;
    int nDepthHeight = depthFrame->height;
    int depthRawBytePerPixel = get_depth_image_format_byte_length_per_pixel(
                                   depth_raw_type_to_depth_image_type(depthFrame->dataFormat));
    std::vector<uint8_t> colorData(colorFrame->dataVec); // using copy constructor
    std::vector<uint8_t> colorDataRGB(colorFrame->rgbVec); // using copy constructor
    int nColorWidth = colorFrame->width;
    int nColorHeight = colorFrame->height;
    int colorRawBytePerPixel = get_color_image_format_byte_length_per_pixel((EtronDIImageType::Value)colorFrame->dataFormat);
    const char *snapShotPath = libeYs3D::EYS3DSystem::getEYS3DSystem()->getSnapshotPath();

    // notify as soon as data cloning is finished
    mSnapshotFinishedSignal.send(1);
    
    {
        char colorPath[PATH_MAX];
        char depthPathRGB[PATH_MAX];
        char colorPathYUV[PATH_MAX];
        char depthPathYUV[PATH_MAX];
        char pcPath[PATH_MAX];
        char tmpBuffer[128];

        get_time_YYYY_MM_DD_HH_MM_SS(tsMs, tmpBuffer, sizeof(tmpBuffer));
        snprintf(colorPath, sizeof(colorPath), "%s/snapshot-color-%" PRIu32 "-%s.bmp",
                 snapShotPath, serialNumber, tmpBuffer);
        snprintf(depthPathRGB, sizeof(depthPathRGB), "%s/snapshot-depth-%" PRIu32 "-%s.bmp",
                 snapShotPath, serialNumber, tmpBuffer);
        snprintf(colorPathYUV, sizeof(colorPathYUV), "%s/snapshot-color-%" PRIu32 "-%s.yuv",
                 snapShotPath, serialNumber, tmpBuffer);
        snprintf(depthPathYUV, sizeof(depthPathYUV), "%s/snapshot-depth-%" PRIu32 "-%s.yuv",
                 snapShotPath, serialNumber, tmpBuffer);
        snprintf(pcPath, sizeof(pcPath), "%s/snapshot-pc-%" PRIu32 "-%s.ply",
                 snapShotPath, serialNumber, tmpBuffer);
        
        save_bitmap(colorPath, colorDataRGB.data(), nColorWidth, nColorHeight);
        save_bitmap(depthPathRGB, depthDataRGB.data(), nDepthWidth, nDepthHeight);
        save_yuv(colorPathYUV, colorData.data(), nColorWidth, nColorHeight, colorRawBytePerPixel);
        save_yuv(depthPathYUV, depthData.data(), nDepthWidth, nDepthHeight, depthRawBytePerPixel);
        // save_ply(pcPath, cloudPoints) in generate_point_cloud
        generate_point_cloud(mCameraDevice, pcPath,
                             depthData, nDepthWidth, nDepthHeight,
                             colorDataRGB, nColorWidth, nColorHeight,
                             mCameraDevice->isPlyFilterEnabled());
    }
}

int PCFrameProducer::producePCFrame(PCFrame *pcFrame)    {
    int ret = 0;
    int64_t currTime, newTime;
    uint32_t expectedFrameIndex;

    // Matching serial numbers...
    bool colorFound = false;
    bool depthFound = false;
    base::Optional<Frame> colorFrame;
    base::Optional<Frame> depthFrame;
    mColorFrame = nullptr;
    mDepthFrame = nullptr;
    while((!colorFound) || (!depthFound))    {
        while(!colorFound)    {
            colorFrame = mColorFrameQueue.receive();
            if(!colorFrame)    goto end;
            
            if(mFrameIndex == colorFrame->serialNumber)    {
                colorFound = true;
            } else if(mFrameIndex > colorFrame->serialNumber)    {
                // force consuming next color frame
                colorFound = false;
                if(!mFreeColorFrameQueue.send(std::move(*colorFrame)))    goto end;
            } else    { // (mFrameIndex < colorFrame->serialNumber)
                if(depthFound)    { // force consuming next depth frame
                    depthFound = false;
                    if(!mFreeDepthFrameQueue.send(std::move(*depthFrame)))    goto end;
                }

                colorFound = true;
                mFrameIndex = colorFrame->serialNumber + ((colorFrame->interleaveMode) ? 1 : 0);
            }
        }

        while(!depthFound)    {
            expectedFrameIndex = mFrameIndex + ((colorFrame->interleaveMode) ? 1 : 0);
            
            depthFrame = mDepthFrameQueue.receive();
            if(!depthFrame)    goto end;

            if(expectedFrameIndex == depthFrame->serialNumber)    {
                depthFound = true;
            } else if(expectedFrameIndex > depthFrame->serialNumber)    {
                // force consuming next depth frame
                depthFound = false;
                if(!mFreeDepthFrameQueue.send(std::move(*depthFrame)))    goto end;
            } else    { // (expectedFrameIndex < depthFrame->serialNumber)
                // forceing consuming next color frame
                if(colorFrame->interleaveMode)    {
                    // for interleave mode, forcing depth frame to be next to color frame
                    depthFound = false;
                    mFrameIndex = depthFrame->serialNumber + 1;
                    if(!mFreeDepthFrameQueue.send(std::move(*depthFrame)))    goto end;
                } else    {
                    depthFound = true;
                    mFrameIndex = depthFrame->serialNumber;
                }
                
                colorFound = false;
                if(!mFreeColorFrameQueue.send(std::move(*colorFrame)))    goto end;
            }
        }
    }
    
    {
        mFrameIndex += colorFrame->interleaveMode ? 2 : 1;
        mColorFrame = &*colorFrame;
        mDepthFrame = &*depthFrame;
    }
    
    // Consumming
    currTime = now_in_microsecond_high_res_time();        
    {   
        { // cloning sensor data set
            if(mCameraDevice->isIMUDevicePresent())    {
                pcFrame->sensorDataSet.clone(&colorFrame->sensorDataSet);
                
                // [color S/N] + [Depth (S/N + 1)] + [IMU S/N] + [IMU S/N + 1].
                if(colorFrame->interleaveMode)    {
                    pcFrame->sensorDataSet.addClone(&depthFrame->sensorDataSet);
                }
            }
        }
        
        // produce point cloud frame here
        if((colorFrame->interleaveMode && (colorFrame->serialNumber == (depthFrame->serialNumber - 1))) ||
           (!colorFrame->interleaveMode && (colorFrame->serialNumber == (depthFrame->serialNumber))))    {
            pcFrame->interleaveMode = colorFrame->interleaveMode;
            pcFrame->tsUs = now_in_microsecond_high_res_time();
            pcFrame->serialNumber = colorFrame->serialNumber;
            pcFrame->width = mCameraDevice->mColorWidth;
            pcFrame->height = mCameraDevice->mColorHeight;
            ret = mCameraDevice->readPCFrame(colorFrame->rgbVec.data(),
                                             depthFrame->dataVec.data(),
                                             pcFrame->rgbDataVec.data(),
                                             pcFrame->xyzDataVec.data());
        } else    { // this should never happen, for dubugging purpose only...
            LOG_ERR(LOG_TAG,
                    "Color(%" PRIu32 ") & Depth(%" PRIu32 ") image serial number mismatch, "
                    "interleave mode: %s",
                    colorFrame->serialNumber, depthFrame->serialNumber,
                    colorFrame->interleaveMode ? "true" : "false");
        }
    }
    newTime = now_in_microsecond_high_res_time();
    pcFrame->transcodingTime = newTime - currTime;

    { // Do sanpshot if necessary 
        auto snapMessage = mSnapshotSignal.timedReceive(now_in_microsecond_unix_time());
        if(snapMessage)    {
           base::async([this]() { performSnapshotWork(mColorFrame, mDepthFrame); });
           mSnapshotFinishedSignal.receive();
        }
    }

end:
    if(mColorFrame)    mFreeColorFrameQueue.send(std::move(*colorFrame));
    if(mDepthFrame)    mFreeDepthFrameQueue.send(std::move(*depthFrame));

    return ret;
}

// Helper to send frames at the user-specified FPS
void PCFrameProducer::sendFramesWorker() {
    while (true) {
        if(mIsStopped) {
          break;
        }

        int64_t currTimeMs = now_in_microsecond_high_res_time() / 1000;
        auto frame = mPCFreeQueue.receive();
        if(!frame) {
            if(mPCFreeQueue.isStopped()) {
                break;
            }
            
            continue;
        }
        
        frame->tsUs = now_in_microsecond_high_res_time();
        bool gotFrame = false;
        if(0 == producePCFrame(&*frame))
            gotFrame = true;

        if(gotFrame)    {
            if(DEBUGGING)
                LOG_DEBUG_S(LOG_TAG, "%s: Sending video frame %u", 
                            getName(), frame->serialNumber);
            if(!mPCDataQueue.send(std::move(*frame)))    break;
        } else {
            if(!mPCFreeQueue.send(std::move(*frame)))    break;
        }

        // TODO: Replace this with high precision timers.
        int64_t newTimeMs = now_in_microsecond_high_res_time() / 1000;

        if(gotFrame) {
            if(DEBUGGING)
                LOG_DEBUG_S(LOG_TAG, "%s: Video frame sent in [%" PRId64 "] ms",
                            getName(), newTimeMs - currTimeMs);
        }
    }

    LOG_INFO(LOG_TAG, "%s: Exiting sending video frame thread...", getName());
    pokeReceiver();
}

void PCFrameProducer::enableCallback()    {
    mCameraDevice->mColorFrameProducer->enablePCCallback();
    mCameraDevice->mDepthFrameProducer->enablePCCallback();
}

void PCFrameProducer::pauseCallback()    {
    mCameraDevice->mColorFrameProducer->pausePCCallback();
    mCameraDevice->mDepthFrameProducer->pausePCCallback();
}

void PCFrameProducer::dumpFrameInfo(int frameCount)    {
    if(frameCount <= 0)    return;
    
    if(mFrameDumpCount > 0)    {
        LOG_INFO(LOG_TAG, "%s is dumping frame info, ignore it...", getName());
        return;
    }
    
    { // prepare data log file
        char logPath[PATH_MAX], tmpBuffer[128];
        
        get_time_YYYY_MM_DD_HH_MM_SS(now_in_microsecond_unix_time() / 1000,
                                     tmpBuffer, sizeof(tmpBuffer));
        snprintf(logPath, PATH_MAX, "%s/%s-%s.log",
                 libeYs3D::EYS3DSystem::getEYS3DSystem()->getFramePath(),
                 getName(), tmpBuffer);
        mFrameLogFile = fopen(logPath, "wt");
        if(!mFrameLogFile) {
            LOG_ERR(LOG_TAG, "Error opening %s", logPath);
            return;
        }
    }
    
    // @ most 1 min, ( 60FPS * 60)
    if(frameCount > 3600)
        mFrameDumpCount = 3600;
    else
        mFrameDumpCount = frameCount;
}

void PCFrameProducer::doSnapshot()    {
    mSnapshotSignal.send(1);
}

std::unique_ptr<PCProducer> createPCFrameProducer(CameraDevice *cameraDevice)    {
    std::unique_ptr<PCProducer> producer(new PCFrameProducer(cameraDevice));
    return std::move(producer);
}

}  // namespace video
}  // namespace libeYs3D
