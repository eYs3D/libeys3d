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

#include "video/FrameProducer.h"
#include "EYS3DSystem.h"
#include "base/Optional.h"
#include "base/threads/Async.h"
#include "base/threads/ThreadPool.h"
#include "devices/CameraDevice.h"
#include "devices/MemoryAllocator.h"
#include "video/Frame.h"
#include "utils.h"

#define DEBUGGING false
#define LOG_TAG "FrameProducer"

#define PAUSE_CALLBACK      1
#define PAUSE_PC_CALLBACK   2
#define STOP               -1
#define RESUME_CALLBACK     4
#define RESUME_PC_CALLBACK  8

#define SENSOR_DATA_VEC_CAP (16) // better align with SensorDataProducer.cpp

namespace libeYs3D    {
namespace video    {

using base::MessageChannel;

FrameProducer::FrameProducer(libeYs3D::devices::CameraDevice *cameraDevice,
                             int32_t fWidth, int32_t fHeight, int32_t fFormat, uint8_t fps)
    : mCameraDevice(cameraDevice),
      mFrameWidth(fWidth), mFrameHeight(fHeight), mFrameProducerState(0), mFps(fps),
      mCBThreadPool(1, [this](CallbackWorkItem&& item)    {
          item.callback((const Frame*)item.frame);
      }),
      mPCCBThreadPool(1, [this](CallbackWorkItem&& item)    {
          item.callback((const Frame*)item.frame);
      })    {
    mPixelRawFormat = fFormat;
    mTimeDeltaMs = (1000 / ((int)mFps)) > 1;
    
    mCallbackWrapper = std::bind(&FrameProducer::callbackWrapper,
                                 this,
                                 std::placeholders::_1);
    mPCCallbackWrapper = std::bind(&FrameProducer::pcCallbackWrapper,
                                   this,
                                   std::placeholders::_1);
                                   
    mIMUDataCallback = std::bind(&FrameProducer::imuDataSetCallback,
                                 this,
                                 std::placeholders::_1);
}

#if 0

void FrameProducer::initialize()    {
    int i;
    // Prefill the free queue with empty frames
    //auto sz = mFrameWidth * mFrameHeight * getImageFormatByteLength(mPixelRawFormat);
    uint64_t sz = (mFrameWidth * mFrameHeight) << 2;
    for(i = 0; i < kMaxFrames; ++i) {

#ifdef DEVICE_MEMORY_ALLOCATOR
        Frame f(sz, sz, 0, mCameraDevice->mPixelByteMemoryAllocator);
#else
        Frame f(sz, 0, (mFrameWidth * mFrameHeight), 0, sz, 0);
#endif

        f.dataFormat = mPixelRawFormat;
        f.rgbFormat = 0;
        f.width = mFrameWidth;
        f.height = mFrameHeight;
        mFreeQueue.send(std::move(f));
    }
    
    for(i = 0; i < kMaxFrames; ++i)    {
        libeYs3D::sensors::SensorDataSet sensorDataSet(
            libeYs3D::sensors::SensorData::SensorDataType::UNKNOWN,
            SENSOR_DATA_VEC_CAP);
        mFreeSensorDataSetQueue.send(std::move(sensorDataSet));
    }
       
    mCBThreadPool.start();
    mPCCBThreadPool.start();
    
    mFrameProducerState |= FP_INITIALIZED;
}

#else

void FrameProducer::initialize()    {
    int i;
    // Prefill the free queue with empty frames
    //auto sz = mFrameWidth * mFrameHeight * getImageFormatByteLength(mPixelRawFormat);
    uint64_t sz = (mFrameWidth * mFrameHeight) << 2;
    for(i = 0; i < kMaxFrames; ++i) {

#ifdef DEVICE_MEMORY_ALLOCATOR
        Frame f((mFrameWidth * mFrameHeight) << 2,
                (mFrameWidth * mFrameHeight) << 2,
                0, mCameraDevice->mPixelByteMemoryAllocator);
#else
        Frame f((mFrameWidth * mFrameHeight * getRawFormatBytesPerPixel(mPixelRawFormat)), 0,
                (mFrameWidth * mFrameHeight), 0,
                (mFrameWidth * mFrameHeight * 3), 0);
#endif

        f.dataFormat = mPixelRawFormat;
        f.rgbFormat = 0;
        f.width = mFrameWidth;
        f.height = mFrameHeight;
        mFreeQueue.send(std::move(f));
    }
    
    for(i = 0; i < kMaxFrames; ++i)    {
        libeYs3D::sensors::SensorDataSet sensorDataSet(
            libeYs3D::sensors::SensorData::SensorDataType::UNKNOWN,
            SENSOR_DATA_VEC_CAP);
        mFreeSensorDataSetQueue.send(std::move(sensorDataSet));
    }
       
    mCBThreadPool.start();
    mPCCBThreadPool.start();
    
    mFrameProducerState |= FP_INITIALIZED;
}

#endif

bool FrameProducer::imuDataSetCallback(const libeYs3D::sensors::SensorDataSet *sensorDataSet)    {
    auto localSensorDataSet = mFreeSensorDataSetQueue.receive();
    if(!localSensorDataSet)    return true;
    
    libeYs3D::sensors::SensorDataSet *sds = &*localSensorDataSet;
    sds->clone(sensorDataSet);
    mSensorDataSetQueue.send(std::move(*sds));

    return true;
}

bool FrameProducer::callbackWrapper(const Frame *frame)    {
    if(mCallback)    {
        mCallback(frame);
    }
    
    mCBFinishSignal.send(1);
    
    return true;
}

bool FrameProducer::pcCallbackWrapper(const Frame *frame)    {
    if(mPCCallback)    {
        mPCCallback(frame);
    }

    mCBFinishSignal.send(1);

    return true;
}

intptr_t FrameProducer::main()    {
    int loopCount = 0;
    bool cbPaused = true;
    bool pcCBPaused = true;
    uint32_t serialNumber = 0;
    base::Optional<Frame> frame;
    base::Optional<libeYs3D::sensors::SensorDataSet> sensorDataSet;
    bool imuFound = false;
    bool frameFound = false;
    
    mIsStopped = false;

    this->initialize();

    base::async([this]() { sendFramesWorker(); });
    base::async([this]() { rgbFramesWorker(); });
    base::async([this]() { frameFilteringWorker(); });
    while(true) {
        { // mapping Frame & IMUDataSet by serial number
            if(mCameraDevice->isIMUDevicePresent())    {
                frameFound = false;
                imuFound = false;
                while((!imuFound) || (!frameFound))    {
                    while(!imuFound)    {
                        sensorDataSet = mSensorDataSetQueue.receive();
                        if(!sensorDataSet)    goto stop;
                        
                        if(sensorDataSet->serialNumber == serialNumber)    {
                            imuFound = true;
                        } else if(sensorDataSet->serialNumber < serialNumber)    {
                            // force consuming next IMU data
                            imuFound = false;
                            if(!mFreeSensorDataSetQueue.send(std::move(*sensorDataSet)))    goto stop;
                        } else    {
                            if(frameFound)    {
                                frameFound = false;
                                if(!mFreeQueue.send(std::move(*frame)))    goto stop;
                            }
                            
                            imuFound = true;
                            serialNumber = sensorDataSet->serialNumber;
                        }
                    }
                
                    while(!frameFound)    {
                        frame = mDataQueue.receive();
                        if(!frame)    goto stop;

                        if(frame->serialNumber == serialNumber)    {
                            frameFound = true;
                        } else if(frame->serialNumber > serialNumber)    {
                            frameFound = true;
                            serialNumber = frame->serialNumber;
                            
                            { // force consuming next IMU data
                                imuFound = false;
                                if(!mFreeSensorDataSetQueue.send(std::move(*sensorDataSet))) goto stop;
                            }
                        } else { // force consuming next frame data
                            frameFound = false;
                            if(!mFreeQueue.send(std::move(*frame)))    goto stop;
                        }
                    }
                } // end of while((!frameFound) || (!imuFound))
                
                { // clone IMU data set
                    frame->sensorDataSet.clone(&*sensorDataSet);
                    if(!mFreeSensorDataSetQueue.send(std::move(*sensorDataSet)))    goto stop;
                }
            } else    {
                // Consumer
                // Blocks until either we get a frame or the queue is closed.
                frame = mDataQueue.receive();
                if(!frame)    goto stop;
            }
        } // end of mapping Frame & IMUDataSet by serial number

        {
            int count = 0;
            if(frame->toCallback)    {
                count++;
                CallbackWorkItem cbwi1(mCallbackWrapper, &*frame);
                //CallbackWorkItem cbwi1([this](const Frame *frame)    {
                //    callbackWrapper(frame);
                //}, &*frame);
    
                mCBThreadPool.enqueue(std::move(cbwi1));
            }

            if(frame->toPCCallback)    {
                count++;
                CallbackWorkItem cbwi2(mPCCallbackWrapper, &*frame);
                //CallbackWorkItem cbwi2([this](const Frame *frame)    {
                //    pcCallbackWrapper(frame);
                //}, &*frame);
    
                mPCCBThreadPool.enqueue(std::move(cbwi2));
            }
            
            if(mFrameDumpCount > 0)    { // dump frame info if necesary
                mFrameDumpCount -= 1;
                if(mFrameLogFile)    {
                    char buffer[256];
                    frame->toStringSimple(buffer, sizeof(buffer));
                    fprintf(mFrameLogFile, "%s\n", buffer);
                            
                    if(mFrameDumpCount == 0)    {
                        fflush(mFrameLogFile);
                        fclose(mFrameLogFile);
                        mFrameLogFile = nullptr;
                    }
                }
            }

            // waiting for callback workers to finish
            for(int i = 0; i < count; i++)    mCBFinishSignal.receive();
        }

        // Blocks until there is space in the queue or is closed.
        if(!mFreeQueue.send(std::move(*frame)))    {
            break;
        }
    }
    
stop:
    LOG_INFO(LOG_TAG, "%s: Exiting callback thread...", getName());
    // just in case sendFramesWorker might exits from stopped mDataQueue 
    mCBFinishSignal.stop();
    pokeReceiver();

    return 0;
}

void FrameProducer::pokeReceiver() {
    mSignal.send(1);
}

void FrameProducer::waitForPoke() {
    mSignal.receive();
}

void FrameProducer::stop()    {
    LOG_INFO(LOG_TAG, "Stopping %s...", getName());

    mPauseSignal.send(STOP);
    
    mIsStopped = true;

    mDataQueue.stop();
    mFreeQueue.stop();
    mStageQueue.stop();
    mStage2Queue.stop();
    mSensorDataSetQueue.stop();
    mFreeSensorDataSetQueue.stop();
    
    Thread::sleepMs(32); // wait for 2 frames / 60 FPS
    
    // Stopping mCBFinishSignal, mPauseSignal again in case
    // some other threads still stocked in sending/receiving signals
    mCBFinishSignal.stop();
    mPauseSignal.stop();
    mSnapshotSignal.stop();

    // To prevent thread object deletion race condition,
    // Making sure both thread are finished...
    // self, sendFramesWorker, rgbFramesWorker and frameFilteringWorker
    waitForPoke();
    waitForPoke();
    waitForPoke();
    waitForPoke();
    
    LOG_INFO(LOG_TAG, "%s Stopped...", getName());
}

// Helper to send frames at the user-specified FPS
void FrameProducer::sendFramesWorker()    {
    int64_t currTimeMs, newTimeMs;
    
    while (true) {
        if(mIsStopped)    break;
        
        { // check for pause request
            base::Optional<int> message;
            while(true)    {
                if(!(mFrameProducerState & FB_APP_STREAM_ACTIVATED) &&
                   !(mFrameProducerState & FB_PC_STREAM_ACTIVATED))
                    message = mPauseSignal.receive();
                else
                    message = mPauseSignal.timedReceive(now_in_microsecond_unix_time());

                if(message)   {
                    switch(*message)    {
                        case RESUME_CALLBACK:
                            mFrameProducerState |= FB_APP_STREAM_ACTIVATED;
                            checkIMUDeviceCBEnablement();
                            break;
                        case RESUME_PC_CALLBACK:
                            mFrameProducerState |= FB_PC_STREAM_ACTIVATED;
                            checkIMUDeviceCBEnablement();
                            break;
                        case PAUSE_CALLBACK:
                            mFrameProducerState &= ~FB_APP_STREAM_ACTIVATED;
                            checkIMUDeviceCBEnablement();
                            break;
                        case PAUSE_PC_CALLBACK:
                            mFrameProducerState &= ~FB_PC_STREAM_ACTIVATED;
                            checkIMUDeviceCBEnablement();
                            break;
                        case STOP:                goto stop;
                    }
                } else if(mPauseSignal.isStopped())    {
                    goto stop;
                }

                if((mFrameProducerState & FB_APP_STREAM_ACTIVATED) ||
                   (mFrameProducerState & FB_PC_STREAM_ACTIVATED))    break;
            }
        }

        currTimeMs = now_in_microsecond_high_res_time() / 1000;
        auto frame = mFreeQueue.receive();
        if(!frame) {
            if(mFreeQueue.isStopped()) {
                break;
            }
            
            continue;
        }
        
        frame->toCallback = (mFrameProducerState & FB_APP_STREAM_ACTIVATED);
        frame->toPCCallback = (mFrameProducerState & FB_PC_STREAM_ACTIVATED);
        frame->tsUs = now_in_microsecond_high_res_time();
        bool gotFrame = false;
        if(0 == readFrame(&*frame))    gotFrame = true;
        
        // interleave mode detection
        if(0 != performInterleave(&*frame))    gotFrame = false;

        if(gotFrame)    {
            if(DEBUGGING)
                LOG_DEBUG_S(LOG_TAG, "%s: Sending video frame, sn=%" PRIu32 "", 
                            getName(), frame->serialNumber);
            if(!mStageQueue.send(std::move(*frame)))    break;
        } else {
            if(!mFreeQueue.send(std::move(*frame)))    break;
        }

        // TODO: Replace this with high precision timers.
        newTimeMs = now_in_microsecond_high_res_time() / 1000;

        // Stop recording if time limit is reached
        //            if (newTimeMs - startMs >= mTimeLimitSecs * 1000) {
        //                D("Time limit reached (%lld ms). Stopping the
        //                recording",
        //                  newTimeMs - startMs);
        //                stop();
        //                break;
        //            }

#if 0
        if(gotFrame) {
            if(DEBUGGING)
                LOG_DEBUG_S(LOG_TAG, "%s: Video frame sent in [%" PRId64 "] ms",
                            getName(), newTimeMs - currTimeMs);
        }

        // since the read is in blocking mode, read as more frequently as we can....
        if(newTimeMs - currTimeMs < mTimeDeltaMs) {
            if(DEBUGGING)
                LOG_DEBUG_S(LOG_TAG, "%s: Video sending thread sleeping for [%" PRId64 "] ms",
                            getName(), mTimeDeltaMs - newTimeMs + currTimeMs);
            base::Thread::sleepMs(mTimeDeltaMs - newTimeMs + currTimeMs);
        }
#endif
    }

stop:
    LOG_INFO(LOG_TAG, "%s: Exiting sending video frame thread...", getName());
    // mPauseSignal is stopped here cause sendFramesWorker might exits from stopped mFreeQueue 
    mPauseSignal.stop();
    pokeReceiver();
}

// Helper to produce RGB frames
void FrameProducer::rgbFramesWorker()    {
    int ret = 0;
    
    while (true) {
        if(mIsStopped) {
              break;
        }

        auto frame = mStage2Queue.receive();
        if(!frame) {
            if(mStage2Queue.isStopped()) {
                break;
            }
            
            continue;
        }

        int64_t currTime = now_in_microsecond_high_res_time();
        {
            ret = produceRGBFrame(&*frame);
        }
        int64_t newTime = now_in_microsecond_high_res_time();
        frame->rgbTranscodingTime = newTime - currTime;
        
        if(ETronDI_OK == ret)    { 
            { // Do sanpshot if necessary 
                auto snapMessage = mSnapshotSignal.timedReceive(now_in_microsecond_unix_time());
                if(snapMessage)    {
                   performSnapshotWork(&*frame);
                   mSnapshotFinishedSignal.receive();
                }
            }
            
            if(!mDataQueue.send(std::move(*frame)))    break;
        } else    { // drop the frame if produceRGBFrame failed
            LOG_INFO(LOG_TAG, "%s: Dropping RGB VERIFY_DATA_FAIL frame, sn=%" PRIu32 "",
                     getName(), frame->serialNumber);

            if(!mFreeQueue.send(std::move(*frame)))    break;
        }
    }

    LOG_INFO(LOG_TAG, "%s: Exiting RGB frame producing thread...", getName());
    pokeReceiver();
}

// Helper to produce RGB frames
void FrameProducer::frameFilteringWorker()    {
    while (true) {
        if(mIsStopped) {
          break;
        }

        auto frame = mStageQueue.receive();
        if(!frame) {
            if(mStageQueue.isStopped()) {
                break;
            }
            
            continue;
        }
        
        int64_t currTime = now_in_microsecond_high_res_time();
        {
            // It seems that these 2 operations simultaneously can be done in < 15 MS
            performFiltering(&*frame);
            performAccuracyComputation(&*frame);
        }
        int64_t newTime = now_in_microsecond_high_res_time();
        frame->filteringTime = newTime - currTime;
        
        if(!mStage2Queue.send(std::move(*frame)))    break;
    }

    LOG_INFO(LOG_TAG, "%s: Exiting frame filter work thread...", getName());
    pokeReceiver();
}

void FrameProducer::enableCallback()    {
    if(mCallback)    {
        mPauseSignal.send(RESUME_CALLBACK);
    }
}

void FrameProducer::pauseCallback()    {
    if(mCallback)    {
        mPauseSignal.send(PAUSE_CALLBACK);
    }
}

void FrameProducer::enablePCCallback()    {
    if(mPCCallback)    {
        mPauseSignal.send(RESUME_PC_CALLBACK);
    }
}

void FrameProducer::pausePCCallback()    {
    if(mPCCallback)    {
        mPauseSignal.send(PAUSE_PC_CALLBACK);
    }
}

void FrameProducer::dumpFrameInfo(int frameCount)    {
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

void FrameProducer::doSnapshot()    {
    mSnapshotSignal.send(1);
}

}  // namespace video
}  // namespace libeYs3D
