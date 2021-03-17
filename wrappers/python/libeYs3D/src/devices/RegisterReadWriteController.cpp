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
 
#include "devices/RegisterReadWriteController.h"
#include "base/threads/Async.h"
#include "EYS3DSystem.h"
#include "devices/CameraDevice.h"
#include "debug.h"
#include "macros.h"
#include "utils.h"

#define LOG_TAG "RegisterReadWriteController"

namespace libeYs3D    {
namespace devices    {

void RegisterReadWriteController::commandExecutor()    {
    COMMAND command;
    while(true)    {
        auto message = mJobSignal.receive();
        if(!message)    break;
        
        command = (COMMAND)(*message);
        switch(command)    {
            case COMMAND::FORCE_READ_REGISTER:
                readRegisters();
                mReadCommitSignal.send(1);
                break;
            case COMMAND::READ_REGISTER:
                readRegisters();
                break;
            case COMMAND::FORCE_WRITE_REGISTER:
                writeRegisters();
                mWriteCommitSignal.send(1);
                break;
            case COMMAND::WRITE_REGISTER:
                writeRegisters();
                break;
            case COMMAND::LOG_REGISTER:
                logRegisters();
                break;
            case COMMAND::EXIT:
                goto out;
        }
    }

out:
    LOG_INFO(LOG_TAG, "Exiting RegisterReadWriteController commandExecutor thread...");
    mJobSignal.stop();
    mSignal.send(1);
}

void RegisterReadWriteController::commitReadRegisters()    {
    mJobSignal.send(COMMAND::FORCE_READ_REGISTER);
    
    // Wait till the worker finishes the job 
    mReadCommitSignal.receive();
}

void RegisterReadWriteController::commitWriteRegisters()    {
    mJobSignal.send(COMMAND::FORCE_WRITE_REGISTER);
    
    // Wait till the worker finishes the job 
    mWriteCommitSignal.receive();
    
    // read update back to mCameraDevice.mRegisterReadWriteOptions
    mJobSignal.send(COMMAND::FORCE_READ_REGISTER);
    
    // Wait till the worker finishes the job 
    mReadCommitSignal.receive();
}

intptr_t RegisterReadWriteController::main()    {
    int64_t time;
    COMMAND command;
    
    mIsStopped = false;

    libeYs3D::base::async([this]() { commandExecutor(); });
    
    while(true)    {
        if(mIsStopped)    break;
        
        do    {
            auto message = mPauseSignal.timedReceive(now_in_microsecond_unix_time());
            if(message)    {
                command = (COMMAND)(*message);
                switch(command)    {
                    case COMMAND::PAUSE:
                        mPauseBackSignal.send(COMMAND::PAUSE);
                        mResumeSignal.receive();
                        break;
                    case COMMAND::EXIT:
                        mPauseBackSignal.send(COMMAND::EXIT);
                        goto out;
                }
            }
            
            //libeYs3D::base::Thread::sleepMs(mCameraDevice->mRegisterReadWriteOptions.getPeriodTimeMs());
            libeYs3D::base::Thread::sleepMs(100);
            time += 100000;
        } while(!mCameraDevice->mRegisterReadWriteOptions.isPeriodicRead());

        if((time / 1000) >= mCameraDevice->mRegisterReadWriteOptions.getPeriodTimeMs())    {
            mJobSignal.send(COMMAND::READ_REGISTER);
            if(mCameraDevice->mRegisterReadWriteOptions.isSaveLog())    mJobSignal.send(COMMAND::LOG_REGISTER);
            
            time = 0ll;
        }
    }

out:
    LOG_INFO(LOG_TAG, "Exiting RegisterReadWriteController main thread...");
    mSignal.send(1);
    
    return 0;
}

void RegisterReadWriteController::stop()    {
    LOG_INFO(LOG_TAG, "Stopping RegisterReadWriteController...");
    
    mIsStopped = true;
    mJobSignal.send(COMMAND::EXIT);
    
    { // stopping main thread
        mResumeSignal.stop();
        mPauseSignal.send(COMMAND::EXIT);
        mPauseBackSignal.receive(); // waiting for main thread really processing COMMAND::EXIT
    }
    
    mPauseSignal.stop();
    mPauseBackSignal.stop();
    mJobSignal.stop();

    // Making sure both thread are finished...
    // self, commandExecutor
    mSignal.receive();
    mSignal.receive();
    
    LOG_INFO(LOG_TAG, "RegisterReadWriteController Stopped...");
}

void RegisterReadWriteController::pause()    {
    mPauseSignal.send(COMMAND::PAUSE);
    mPauseBackSignal.receive();
}

void RegisterReadWriteController::resume()    {
    mResumeSignal.send(1);
}

void RegisterReadWriteController::readRegisters()    {
    mLastestReadTimeMs = now_in_microsecond_unix_time() / 1000;
    
    for(int i = 0 ; i < REGISTER_REQUEST_MAX_COUNT ; ++i)    {
        if (EOF == mCameraDevice->mRegisterReadWriteOptions.getRequestAddress(i)) continue;

        mCameraDevice->mRegisterReadWriteOptions.setRequestValue(i, EOF);

        int ret;
        unsigned short value;
        switch(mCameraDevice->mRegisterReadWriteOptions.getType())    {
            case RegisterReadWriteOptions::IC2:
                ret = RETRY_ETRON_API(EtronDI_GetSensorRegister(
                                          libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                          &mCameraDevice->mDevSelInfo,
                                          mCameraDevice->mRegisterReadWriteOptions.getSlaveID(),
                                          mCameraDevice->mRegisterReadWriteOptions.getRequestAddress(i),
                                          &value,
                                          mCameraDevice->mRegisterReadWriteOptions.getAddressSize() |
                                          mCameraDevice->mRegisterReadWriteOptions.getValueSize(),
                                          mCameraDevice->mRegisterReadWriteOptions.getSensorMode()));
                break;
            case RegisterReadWriteOptions::ASIC:
                ret = RETRY_ETRON_API(EtronDI_GetHWRegister(
                                          libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                          &mCameraDevice->mDevSelInfo,
                                          mCameraDevice->mRegisterReadWriteOptions.getRequestAddress(i),
                                          &value,
                                          mCameraDevice->mRegisterReadWriteOptions.getAddressSize() |
                                          mCameraDevice->mRegisterReadWriteOptions.getValueSize()));
                break;
            case RegisterReadWriteOptions::FW:
                ret = RETRY_ETRON_API(EtronDI_GetFWRegister(
                                          libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                          &mCameraDevice->mDevSelInfo,
                                          mCameraDevice->mRegisterReadWriteOptions.getRequestAddress(i),
                                          &value,
                                          mCameraDevice->mRegisterReadWriteOptions.getAddressSize() |
                                          mCameraDevice->mRegisterReadWriteOptions.getValueSize()));
                break;
            default:
                return;
        }
        
        if(ETronDI_OK == ret)    {
            mCameraDevice->mRegisterReadWriteOptions.setRequestValue(i, value);
        } else    {
            mCameraDevice->mRegisterReadWriteOptions.setRequestValue(i, 0xff);
        }
    } // end of for 
}

void RegisterReadWriteController::logRegisters()    {
    FILE *file;
    char tmpBuffer[PATH_MAX];
    char buffer[2048 + PATH_MAX];
    const char *logPath = libeYs3D::EYS3DSystem::getEYS3DSystem()->getLogPath();
    
    get_time_YYYY_MM_DD_HH_MM_SS(mLastestReadTimeMs, tmpBuffer, sizeof(tmpBuffer));
    snprintf(buffer, sizeof(buffer), "%s/RegisterReadWrite-%s", logPath, tmpBuffer);
    file = fopen(buffer, "wt");
    if(!file) {
        LOG_INFO(LOG_TAG, "Error opening %s", tmpBuffer);
        return;
    }

    mCameraDevice->mRegisterReadWriteOptions.toString(buffer, sizeof(buffer));
    fprintf(file, "%s", buffer);

    fflush(file);
    fclose(file);
}

void RegisterReadWriteController::writeRegisters()    {
    for(int i = 0 ; i < REGISTER_REQUEST_MAX_COUNT ; ++i)    {
        if(!mCameraDevice->mRegisterReadWriteOptions.getRequestAddress(i)) continue;
        if(EOF == mCameraDevice->mRegisterReadWriteOptions.getRequestValue(i)) continue;

        int ret;
        switch(mCameraDevice->mRegisterReadWriteOptions.getType())    {
            case RegisterReadWriteOptions::IC2:
                ret = RETRY_ETRON_API(EtronDI_SetSensorRegister(
                                          libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                          &mCameraDevice->mDevSelInfo,
                                          mCameraDevice->mRegisterReadWriteOptions.getSlaveID(),
                                          mCameraDevice->mRegisterReadWriteOptions.getRequestAddress(i),
                                          mCameraDevice->mRegisterReadWriteOptions.getRequestValue(i),
                                          mCameraDevice->mRegisterReadWriteOptions.getAddressSize() |
                                          mCameraDevice->mRegisterReadWriteOptions.getValueSize(),
                                          mCameraDevice->mRegisterReadWriteOptions.getSensorMode()));
                break;
            case RegisterReadWriteOptions::ASIC:
                ret = RETRY_ETRON_API(EtronDI_SetHWRegister( libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                          &mCameraDevice->mDevSelInfo,
                                             mCameraDevice->mRegisterReadWriteOptions.getRequestAddress(i),
                                             mCameraDevice->mRegisterReadWriteOptions.getRequestValue(i),
                                             mCameraDevice->mRegisterReadWriteOptions.getAddressSize() |
                                             mCameraDevice->mRegisterReadWriteOptions.getValueSize()));
                break;
            case RegisterReadWriteOptions::FW:
                ret = RETRY_ETRON_API(EtronDI_SetFWRegister(
                                          libeYs3D::EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                          &mCameraDevice->mDevSelInfo,
                                          mCameraDevice->mRegisterReadWriteOptions.getRequestAddress(i),
                                          mCameraDevice->mRegisterReadWriteOptions.getRequestValue(i),
                                          mCameraDevice->mRegisterReadWriteOptions.getAddressSize() |
                                          mCameraDevice->mRegisterReadWriteOptions.getValueSize()));
                break;
            default:
                return;
        }
        
        if(ret != ETronDI_OK)    {
            mCameraDevice->mRegisterReadWriteOptions.setRequestValue(i, 0xff);
        }
    }

    // reload camera device properties from h/w device
    mCameraDevice->reloadCameraDeviceProperties();
} 

RegisterReadWriteController::RegisterReadWriteController(CameraDevice *cameraDevice)
    : mCameraDevice(cameraDevice), mIsStopped(false)    {
}

} // end of namespace devices
} // end of namespace libeYs3D
