/*
 * Copyright (C) 2015-2019 ICL/ITRI
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

#include "sensors/SensorDataProducer.h"
#include "base/threads/Async.h"

#define LOG_TAG "SensorDataProducer"

#define PAUSE_APP_CALLBACK     1
#define PAUSE_COLOR_CALLBACK   2
#define PAUSE_DEPTH_CALLBACK   3
#define STOP                  -1
#define RESUME_APP_CALLBACK      4
#define RESUME_COLOR_CALLBACK    5
#define RESUME_DEPTH_CALLBACK    6

#define SENSOR_DATA_VEC_CAP (16)

namespace libeYs3D    {
namespace sensors    {

SensorDataProducer::SensorDataProducer(SensorData::SensorDataType type)
    : mSensorDataType(type),
      mCBThreadPool(2, [this](CallbackWorkItem&& item)    {
          item.mCallback((const SensorDataSet*)item.mSensorDataSet);
      })    {
    mColorCallbackWrapper = std::bind(&SensorDataProducer::colorCallbackWrapper,
                                      this,
                                      std::placeholders::_1);
    mDepthCallbackWrapper = std::bind(&SensorDataProducer::depthCallbackWrapper,
                                      this,
                                      std::placeholders::_1);
}

void SensorDataProducer::initialize()    {
    // Prefill the free queue with empty sensor data sets
    for(int i = 0; i < kMaxDataItems; ++i)    {
        SensorDataSet sensorDataSet(mSensorDataType, SENSOR_DATA_VEC_CAP);
        mFreeQueue.send(std::move(sensorDataSet));
    }
    
    // Prefill the free queue with empty sensor data items
    for(int i = 0; i < kMaxDataItems << 2; ++i)    {
        SensorData sensorData(SensorData::SensorDataType::UNKNOWN);
        mAppFreeDataQueue.send(std::move(sensorData));
    }
    
    mCBThreadPool.start();
}

void SensorDataProducer::sendSensorDataWorker()    {
    base::Optional<SensorData> sensorData;
    base::Optional<SensorDataSet> sensorDataSet;
    uint32_t currentSN = 0;
    bool appCBPaused = true;
    bool colorCBPaused = true;
    bool depthCBPaused = true;
    
    while(true) {
        if(mIsStopped)    break;
        
        sensorDataSet = mFreeQueue.receive();
        if(!sensorDataSet)    {
            if(mFreeQueue.isStopped())    goto stop;
            
            continue;
        }

        sensorDataSet->reset();
        sensorDataSet->toColorCallback = !colorCBPaused;
        sensorDataSet->toDepthCallback = !depthCBPaused;
        sensorDataSet->tsUs = now_in_microsecond_high_res_time();
        sensorDataSet->serialNumber = currentSN;
        if(sensorData)    {
            sensorData->toAppCallback = !appCBPaused;
            sensorDataSet->addClone(&*sensorData);
            if(!mAppDataQueue.send(std::move(*sensorData)))    goto stop;
        }
        while(true)    {
            { // check for pause request
                base::Optional<int> message;
                while(true)    {
                    if(appCBPaused & colorCBPaused & depthCBPaused)    {
                        message = mPauseSignal.receive();
                    } else    {
                        message = mPauseSignal.timedReceive(now_in_microsecond_unix_time());
                    }

                    if(message)    {
                        switch(*message)    {
                            case RESUME_APP_CALLBACK:    appCBPaused = false; break;
                            case RESUME_COLOR_CALLBACK:  colorCBPaused = false; break;
                            case RESUME_DEPTH_CALLBACK:  depthCBPaused = false; break;
                            case PAUSE_APP_CALLBACK:     appCBPaused = true; break;
                            case PAUSE_COLOR_CALLBACK:   colorCBPaused = true; break;
                            case PAUSE_DEPTH_CALLBACK:   depthCBPaused = true; break;
                            case STOP:                   goto stop;
                        }
                    } else if(mPauseSignal.isStopped())    {
                        goto stop;
                    }

                    if(!appCBPaused || !colorCBPaused || !depthCBPaused)    break;
                }
            }    
        
            { // read sensor data
                sensorData = mAppFreeDataQueue.receive();
                if(!sensorData)    {
                    if(mAppFreeDataQueue.isStopped())    goto stop;
            
                    continue;
                }

                if(readSensorData(&*sensorData) <= 0)    {
                    if(!mAppFreeDataQueue.send(std::move(*sensorData)))    goto stop;

                    continue;
                }

                if(sensorData->serialNumber == currentSN)    {
                    sensorDataSet->addClone(&*sensorData);
                    sensorDataSet->serialNumber = currentSN;
                    sensorData->toAppCallback = !appCBPaused;
                    if(!mAppDataQueue.send(std::move(*sensorData)))    goto stop;
                } else if(sensorData->serialNumber > currentSN)    {
                    currentSN = sensorData->serialNumber;
                    if(!mDataQueue.send(std::move(*sensorDataSet)))    goto stop;
                
                    break;
                } else    { // this should never happen, for debugging purpose
                    LOG_WARN(LOG_TAG, "%s: IMU data S/N (%" PRIu32 ") is less than previous one (%" PRIu32 ")",
                             getName(), sensorData->serialNumber, currentSN);
                    if(!mAppFreeDataQueue.send(std::move(*sensorData)))    continue;
                }
            }
        } // end of while
    }

stop:
    LOG_INFO(LOG_TAG, "%s: Exiting sending sensor data thread...", getName());
    // just in case sendSensorDataWorker might exits from stopped mFreeQueue 
    mPauseSignal.stop();
    mSignal.send(1);
}

void SensorDataProducer::appCallbackWorker()    {
    while(true)    {
        auto sensorData = mAppDataQueue.receive();
        if(!sensorData)    break;
        
        if(sensorData->toAppCallback)    {
            if(mAppCallback)    {
                mAppCallback(&*sensorData);
            }
        }
        
        if(!mAppFreeDataQueue.send(std::move(*sensorData)))    break;
    }
    
    LOG_INFO(LOG_TAG, "%s: Exiting App sensor data callback thread...", getName());
    mSignal.send(1);
}

bool SensorDataProducer::colorCallbackWrapper(const SensorDataSet *sensorDataSet)    {
    if(mColorCallback)    {
        mColorCallback(sensorDataSet);
    }
    
    mCBFinishSignal.send(1);
    
    return true;
}

bool SensorDataProducer::depthCallbackWrapper(const SensorDataSet *sensorDataSet)    {
    if(mDepthCallback)    {
        mDepthCallback(sensorDataSet);
    }
    
    mCBFinishSignal.send(1);
    
    return true;
}


intptr_t SensorDataProducer::main()    {
    bool appCBPaused = true;
    bool colorCBPaused = true;
    bool depthCBPaused = true;
    int count = 0;
    
    mIsStopped = false;

    this->initialize();

    base::async([this]() { sendSensorDataWorker(); });
    base::async([this]() { appCallbackWorker(); });
    while(true)    {
        auto sensorDataSet = mDataQueue.receive();
        if(!sensorDataSet)    break;

#if 0
        { // calling callbacks
            count = 0;
            if(sensorDataSet->toColorCallback)    {
                count++;
                CallbackWorkItem cbwi1(mColorCallbackWrapper, &*sensorDataSet);
    
                mCBThreadPool.enqueue(std::move(cbwi1));
            }
    
            if(sensorDataSet->toDepthCallback)    {
                count++;
                CallbackWorkItem cbwi2(mDepthCallbackWrapper, &*sensorDataSet);
    
                mCBThreadPool.enqueue(std::move(cbwi2));
            }

            // waiting for callback workers to finish
            for(int i = 0; i < count; i++)    mCBFinishSignal.receive();
        }
        
        // Blocks until there is space in the queue or is closed.
        if(!mFreeQueue.send(std::move(*sensorDataSet)))    {
            break;
        }
#endif
        { // calling callbacks
            count = 0;
            if(sensorDataSet->toColorCallback)    {
                if(mColorCallback)    mColorCallback(&*sensorDataSet);
            }
    
            if(sensorDataSet->toDepthCallback)    {
                if(mDepthCallback)    mDepthCallback(&*sensorDataSet);
            }
        }
        
        // Blocks until there is space in the queue or is closed.
        if(!mFreeQueue.send(std::move(*sensorDataSet)))    {
            break;
        }
    }

    LOG_INFO(LOG_TAG, "%s: Exiting callback thread...", getName());
    // just in case appCallbackWorker might exits from stopped mDataQueue 
    mCBFinishSignal.stop();
    mSignal.send(1);

    return 0;
}

void SensorDataProducer::stop()    {
    LOG_INFO(LOG_TAG, "Stopping %s...", getName());
    
    mIsStopped = true;
    
    mPauseSignal.send(STOP);
    
    mDataQueue.stop();
    mFreeQueue.stop();
    mAppDataQueue.stop();
    mAppFreeDataQueue.stop();
        
    Thread::sleepMs(32); // wait for 2 frames / 60 FPS
    
    // Stopping mCBFinishSignal, mPauseSignal again in case
    // some other threads still stocked in sending/receiving signals 
    mCBFinishSignal.stop();
    mPauseSignal.stop();
    
    // To prevent thread object deletion race condition,
    // Making sure threads are finished...
    // self, sendSensorDataWorker and appCallbackWorker
    mSignal.receive();
    mSignal.receive();
    mSignal.receive();
    
    LOG_INFO(LOG_TAG, "%s Stopped...", getName());
}

void SensorDataProducer::enableAppCallback()    {
    if(mAppCallback)    {
        mPauseSignal.send(RESUME_APP_CALLBACK);
    }
}

void SensorDataProducer::pauseAppCallback()    {
    if(mAppCallback)    {
        mPauseSignal.send(PAUSE_APP_CALLBACK);
    }
}

void SensorDataProducer::enableColorCallback()    {
    if(mColorCallback)    {
        mPauseSignal.send(RESUME_COLOR_CALLBACK);
    }
}

void SensorDataProducer::pauseColorCallback()    {
    if(mColorCallback)    {
        mPauseSignal.send(PAUSE_COLOR_CALLBACK);
    }
}

void SensorDataProducer::enableDepthCallback()    {
    if(mDepthCallback)    {
        mPauseSignal.send(RESUME_DEPTH_CALLBACK);
    }
}

void SensorDataProducer::pauseDepthCallback()    {
    if(mDepthCallback)    {
        mPauseSignal.send(PAUSE_DEPTH_CALLBACK);
    }
}
    
}  // namespace sensors
}  // namespace libeYs3D

