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

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <dirent.h>
#include <unistd.h>
#include <sys/stat.h>

#include "EYS3DSystem.h"
#include "devices/CameraDevice.h"
#include "macros.h"
#include "utils.h"
#include "debug.h"

#define DEBUGGING true
#define LOG_TAG "EYS3DSystem"

using ::libeYs3D::devices::CameraDeviceFactory;

namespace libeYs3D    {

std::shared_ptr<EYS3DSystem> EYS3DSystem::sEYS3DSystem;

std::shared_ptr<EYS3DSystem> EYS3DSystem::getEYS3DSystem()    {
    if(sEYS3DSystem.get())    {
        return sEYS3DSystem;
    }

    EYS3DSystem::sEYS3DSystem = std::shared_ptr<EYS3DSystem>(new EYS3DSystem());
    if(0 == sEYS3DSystem->initialize())    {
        return sEYS3DSystem;
    } else    {
        sEYS3DSystem = nullptr;
        return nullptr;
    }
}

std::shared_ptr<CameraDevice> EYS3DSystem::getCameraDevice(int index)    {
    DeviceSellectInfo devSelInfo(index);
    std::map<DeviceSellectInfo, std::shared_ptr<CameraDevice>>::iterator iter;
    iter = mDeviceMap.find(devSelInfo);
    if(iter == mDeviceMap.end())    {
        LOG_DEBUG_S(LOG_TAG, "Not able to find camera device, index(%d)...", index);
        return nullptr;
    }

    return iter->second;
}

void* EYS3DSystem::getEYS3DDIHandle()    {
    return mEYS3DDIHandle;
}

std::vector<std::string> EYS3DSystem::getHIDDeviceList(uint16_t nVID, uint16_t nPID)    {
    std::pair<uint16_t, uint16_t> info(nVID, nPID);

    if(0 == mHidDeviceMap.count(info))    {
        hid_device_info *deviceInfo = hid_enumerate(nVID, nPID);
        hid_device_info *headInfo = deviceInfo;
        while(deviceInfo)    {
            LOG_INFO(LOG_TAG, "HID device found, VID(0X%04X), PID(0X%04X), path(%s)",
                     (int)nVID, (int)nPID, deviceInfo->path);
            std::string devicePath = (deviceInfo->path);
            mHidDeviceMap[info].push_back(devicePath);
            
            deviceInfo = deviceInfo->next;
        }
        
        if (headInfo)    hid_free_enumeration(headInfo);
    }

    return mHidDeviceMap[info];
}

EYS3DSystem::EYS3DSystem() : mEYS3DDIHandle(nullptr), mDeviceCount(0)    {
}

int EYS3DSystem::initialize()    {
    int i = 0, ret, length;
    DeviceSellectInfo devSelInfo;
    DEVINFORMATION deviceInfo;
    CameraDevice *cameraDevice;

    LOG_INFO(LOG_TAG, "Initializing Etron System...");
    
    ret = createEYS3DHome();
    if(ret != 0)    {
        LOG_ERR(LOG_TAG, "Error creating EYS3D home directory, error(%d)", ret);
        return ret;
    }
    
    ret = RETRY_ETRON_API(EtronDI_Init(&mEYS3DDIHandle, DEBUGGING));
    if(ret != ETronDI_OK)    { // ETronDI_OK == 0
        LOG_ERR(LOG_TAG, "Failed initializing Etron System, error(%d)", ret);
        return ret;
    }

    mDeviceCount = RETRY_ETRON_API(EtronDI_GetDeviceNumber(mEYS3DDIHandle));
    if(mDeviceCount == 0)    {
        LOG_WARN(LOG_TAG, "NONE devices found ...");
    }

    // std containers copies all data into the container.
    for(i = 0; i < mDeviceCount; i++)    {
        devSelInfo.devSelInfo.index = i;
        RETRY_ETRON_API(EtronDI_GetDeviceInfo(mEYS3DDIHandle,
                                              (DEVSELINFO *)&devSelInfo,
                                              &deviceInfo));

        cameraDevice = CameraDeviceFactory::createCameradevice((DEVSELINFO *)&devSelInfo, 
                                                               &deviceInfo);

        { // Print device information...
            char buffer[512];
            devSelInfo.toString(buffer, sizeof(buffer));
            LOG_INFO(LOG_TAG, "%s", buffer);
            cameraDevice->mCameraDeviceInfo.toString(buffer, sizeof(buffer));
            LOG_INFO(LOG_TAG, "%s", buffer);
        }

        { // Print device property information...
            char buffer[2048] = {0};
            cameraDevice->mCameraDeviceProperties.toString(buffer, sizeof(buffer));
            LOG_INFO(LOG_TAG, "%s", buffer);
        }

        // Inserting deviceInfo to map...
        mDeviceMap[devSelInfo] = std::shared_ptr<CameraDevice>(cameraDevice);
    }

    return 0;
}

const char* EYS3DSystem::getSDKHomePath()    {
    char tmpPath[PATH_MAX];
    int ret = 0;
    const char *sdkHomeDir = ::getenv("EYS3D_SDK_HOME");
    if(sdkHomeDir)    {
        snprintf(mSDKHomePath, sizeof(mSDKHomePath), "%s", sdkHomeDir);
        return mSDKHomePath;
    }
    
    ret = get_executable_dir(tmpPath, sizeof(tmpPath), LOG_TAG);
    if(ret <= 0)    {
        mSDKHomePath[0] = '\0';
        return mSDKHomePath;
    }
    
    // python_wrapper_itri/libeYs3D/out/../..
    snprintf(mSDKHomePath, sizeof(mSDKHomePath), "%s/../..", tmpPath);
    
    return mSDKHomePath;
}

int EYS3DSystem::createEYS3DHome()    {
    char buffer[PATH_MAX];
    char *dirPath;
    DIR *dir;
    const char *homeDir = ::getenv("EYS3D_HOME");
    if(!homeDir)    {
        snprintf(buffer, sizeof(buffer), "%s/.eYs3D", ::getenv("HOME"));
        homeDir = buffer;
    }
    
    dirPath = (char *)homeDir;
    dir = ::opendir(dirPath);
    if(dir) { // Directory exists.
        ::closedir(dir);
        if(::access(dirPath, W_OK) != 0) {
            LOG_ERR(LOG_TAG, "%s is not writable!", dirPath);
            return -errno;
        }
    } else if(ENOENT == errno) { //Directory does not exist.
        if(0 != ::mkdir(dirPath, 0775))    {   
            LOG_ERR(LOG_TAG, "Not able to create %s", dirPath);
            return -errno;
        }
    } else if(ENOTDIR == errno) { //Not a directory.
        LOG_ERR(LOG_TAG, "%s is not a directory!", dirPath);
        return -errno;
    } else    { // failed for some other reason.
        LOG_ERR(LOG_TAG, "Error accessing %s!", dirPath);
        return -errno;
    }
    
    snprintf(mHomePath, PATH_MAX, "%s", homeDir);
    snprintf(mLogPath, PATH_MAX, "%s/logs", homeDir);
    snprintf(mFramePath, PATH_MAX, "%s/frames", homeDir);
    snprintf(mSnapshotPath, PATH_MAX, "%s/snapshots", homeDir);
    snprintf(mIMULogPath, PATH_MAX, "%s/imu_log", homeDir);
 
    dirPath = mLogPath;
    dir = ::opendir(dirPath);
    if(dir) { // Directory exists.
        ::closedir(dir);
        if(::access(dirPath, W_OK) != 0) {
            LOG_ERR(LOG_TAG, "%s is not writable!", dirPath);
            return -errno;
        }
    } else if(ENOENT == errno) { //Directory does not exist.
        if(0 != ::mkdir(dirPath, 0775))    {   
            LOG_ERR(LOG_TAG, "Not able to create %s!", dirPath);
            return -errno;
        }
    } else if(ENOTDIR == errno) { //Not a directory.
        LOG_ERR(LOG_TAG, "%s is not a directory!", dirPath);
        return -errno;
    } else    { // failed for some other reason.
        LOG_ERR(LOG_TAG, "Error accessing %s!", dirPath);
        return -errno;
    }
    
    dirPath = mFramePath;
    dir = ::opendir(dirPath);
    if(dir) { // Directory exists.
        ::closedir(dir);
        if(::access(dirPath, W_OK) != 0) {
            LOG_ERR(LOG_TAG, "%s is not writable!", dirPath);
            return -errno;
        }
    } else if(ENOENT == errno) { //Directory does not exist.
        if(0 != ::mkdir(dirPath, 0775))    {   
            LOG_ERR(LOG_TAG, "Not able to create %s!", dirPath);
            return -errno;
        }
    } else if(ENOTDIR == errno) { //Not a directory.
        LOG_ERR(LOG_TAG, "%s is not a directory!", dirPath);
        return -errno;
    } else    { // failed for some other reason.
        LOG_ERR(LOG_TAG, "Error accessing %s!", dirPath);
        return -errno;
    }
    
    dirPath = mSnapshotPath;
    dir = ::opendir(dirPath);
    if(dir) { // Directory exists.
        ::closedir(dir);
        if(::access(dirPath, W_OK) != 0) {
            LOG_ERR(LOG_TAG, "%s is not writable!", dirPath);
            return -errno;
        }
    } else if(ENOENT == errno) { //Directory does not exist.
        if(0 != ::mkdir(dirPath, 0775))    {   
            LOG_ERR(LOG_TAG, "Not able to create %s!", dirPath);
            return -errno;
        }
    } else if(ENOTDIR == errno) { //Not a directory.
        LOG_ERR(LOG_TAG, "%s is not a directory!", dirPath);
        return -errno;
    } else    { // failed for some other reason.
        LOG_ERR(LOG_TAG, "Error accessing %s!", dirPath);
        return -errno;
    }
    
    dirPath = mIMULogPath;
    dir = ::opendir(dirPath);
    if(dir) { // Directory exists.
        ::closedir(dir);
        if(::access(dirPath, W_OK) != 0) {
            LOG_ERR(LOG_TAG, "%s is not writable!", dirPath);
            return -errno;
        }
    } else if(ENOENT == errno) { //Directory does not exist.
        if(0 != ::mkdir(dirPath, 0775))    {   
            LOG_ERR(LOG_TAG, "Not able to create %s!", dirPath);
            return -errno;
        }
    } else if(ENOTDIR == errno) { //Not a directory.
        LOG_ERR(LOG_TAG, "%s is not a directory!", dirPath);
        return -errno;
    } else    { // failed for some other reason.
        LOG_ERR(LOG_TAG, "Error accessing %s!", dirPath);
        return -errno;
    }
    
    return 0;
}

void EYS3DSystem::initHID()    {
    hid_init();
}

void EYS3DSystem::clearHID()    {
    mHidDeviceMap.clear();

    hid_exit();
}

uint8_t EYS3DSystem::generateModelID()    {
    static bool initialized = false;
    
    if(!initialized)    {
        /* initialize random seed: */
        srand(time(NULL));
        initialized = true;
    }

    return (uint8_t)(rand() % UCHAR_MAX);
}

EYS3DSystem::~EYS3DSystem()    {
    LOG_INFO(LOG_TAG, "\n\nDeleating global EYS3DSystem instance...\n");
    
    clearHID();
    
    LOG_INFO(LOG_TAG, "Releasing plugged camera devices...");
    for(auto iter = mDeviceMap.begin(); iter != mDeviceMap.end(); ++iter)    {
        iter->second->closeStream();
        
        mDeviceMap.erase(iter);
    }
    
    LOG_INFO(LOG_TAG, "Releasing resource that EtronDI_Init had allocated...");
    EtronDI_Release(&mEYS3DDIHandle);
}

int DeviceSellectInfo::toString(char *buffer, int bufferLength) const    {
    return snprintf(buffer, (size_t)bufferLength, 
                    "---- Device Select Info. (%p), index(%d) ----",
                    this, devSelInfo.index);
}

int DeviceSellectInfo::toString(std::string string) const    {
    int ret = 0;
    char buffer[512];
    
    ret = toString(buffer, 512);
    string.append(buffer);
    
    return ret;
}

} // end of namespace libeYs3D
