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

#include "EYS3DSystem.h"
#include "devices/CameraDevice.h"
#include "video/Frame.h"
#include "sensors/SensorData.h"
#include "debug.h"

#include <unistd.h>

#define LOG_TAG "EYS3DSystem Test"
#define DURATION 30

using namespace libeYs3D;

// using Callback = std::function<bool(const Frame* frame)>;
static bool color_image_callback(const libeYs3D::video::Frame* frame)    {
    char buffer[512];
    static int64_t count = 0;
    static int64_t time = 0;
    static int64_t transcodingTime = 0;

#if 1
    LOG_INFO(LOG_TAG, "[# COLOR #] color_image_callback: S/N=%" PRIu32 "", frame->serialNumber);
#endif
#if 0
    frame->toStringSimple(buffer, sizeof(buffer));
    LOG_INFO(LOG_TAG": color_image_callback", "Color image: %s", buffer); 
#endif
#if 0
    if((count++ % DURATION) == 0)    {
        if(count != 1)    {
            int64_t temp = 0ll;
            LOG_INFO(LOG_TAG, "Color image trancoding cost average: %" PRId64 " ms...",
                     (transcodingTime / 1000 / DURATION));

            temp = (frame->tsUs - time) / 1000 / DURATION;
            LOG_INFO(LOG_TAG, "Color image cost average: %" PRId64 " ms, %" PRId64 " fps...",
                     temp, ((int64_t)1000) / temp);    
        }

        time = frame->tsUs;
        transcodingTime = frame->rgbTranscodingTime;
    } else    {
        transcodingTime += frame->rgbTranscodingTime;
    }
#endif
    return true;
}

static bool depth_image_callback(const libeYs3D::video::Frame* frame)    {
    char buffer[1024];
    static int64_t count = 0ll;
    static int64_t time = 0ll;
    static int64_t transcodingTime = 0ll;
    static int64_t filteringTime = 0ll;
    
#if 1
    LOG_INFO(LOG_TAG, "[# DEPTH #] depth_image_callback: S/N=%" PRIu32 "", frame->serialNumber);
#endif
#if 0
    frame->toStringSimple(buffer, sizeof(buffer));
    LOG_INFO(LOG_TAG": depth_image_callback", "%s", buffer);
#endif
#if 0 
    if((count++ % DURATION) == 0)    {
        if(count != 1)    {
            int64_t temp = 0ll;
            LOG_INFO(LOG_TAG, "Depth image trancoding cost average: %" PRId64 " ms...",
                     (transcodingTime / 1000 / DURATION));
            LOG_INFO(LOG_TAG, "Depth image filtering cost average: %" PRId64 " ms...",
                     (filteringTime / 1000 / DURATION));
            temp = (frame->tsUs - time) / 1000 / DURATION;
            LOG_INFO(LOG_TAG, "Depth image cost average: %" PRId64 " ms, %" PRId64 " fps...",
                     temp, ((int64_t)1000) / temp);    
        }
        
        time = frame->tsUs;
        transcodingTime = frame->rgbTranscodingTime;
        filteringTime = frame->filteringTime;
    } else    {
        transcodingTime += frame->rgbTranscodingTime;
        filteringTime += frame->filteringTime;
    }
#endif
    return true;
}

static bool pc_frame_callback(const libeYs3D::video::PCFrame *pcFrame)    {
    char buffer[2048];
    static int64_t count = 0ll;
    static int64_t time = 0ll;
    static int64_t transcodingTime = 0ll;

#if 1
    pcFrame->toStringSimple(buffer, sizeof(buffer));
    LOG_INFO(LOG_TAG": pc_frame_callback", "%s", buffer);
#endif

#if 0
    if((count++ % DURATION) == 0)    {
        if(count != 1)    {
            int64_t temp = 0ll;
            LOG_INFO(LOG_TAG, "PC image trancoding cost average: %" PRId64 " ms...",
                     (transcodingTime / 1000 / DURATION));
                     
            temp = (pcFrame->tsUs - time) / 1000 / DURATION;
            LOG_INFO(LOG_TAG, "PC image cost average: %" PRId64 " ms, %" PRId64 " fps...",
                     temp, ((int64_t)1000) / temp);
        }
        
        time = pcFrame->tsUs;
        transcodingTime = pcFrame->transcodingTime;
    } else    {
        transcodingTime += pcFrame->transcodingTime;
    }
#endif

    return true;
}

static bool imu_data_callback(const libeYs3D::sensors::SensorData *sensorData)    {
    char buffer[2048];
#if 1
    LOG_INFO(LOG_TAG, "[# IMU #] imu_data_callback: S/N=%" PRIu32 "", sensorData->serialNumber);
#endif
#if 0
    sensorData->toString(buffer, sizeof(buffer));
    LOG_INFO(LOG_TAG, "%s", buffer);
#endif

    return true;
}

int main(int argc, char** argv)    {
    LOG_INFO(LOG_TAG, "Starting EYS3DSystem...");

    std::shared_ptr<EYS3DSystem> eYs3DSystem = EYS3DSystem::getEYS3DSystem();
    std::shared_ptr<libeYs3D::devices::CameraDevice> device = eYs3DSystem->getCameraDevice(0);
    
    if(!device)    {
        LOG_INFO(LOG_TAG, "Unable to find any camera devices...");
        exit(-1);
    }
    
    int retry = 1;
    while(retry-- > 0)    {
        int ret = 0;
        LOG_INFO(LOG_TAG, "\n\nEnabling device stream...\n");
#if 1 // USB3
        ret = device->initStream(libeYs3D::video::COLOR_RAW_DATA_TYPE::COLOR_RAW_DATA_YUY2,
                                 1280, 720, 60, /* or 24 */
                                 libeYs3D::video::DEPTH_RAW_DATA_TYPE::DEPTH_RAW_DATA_11_BITS,
                                 1280, 720,
//                                 DEPTH_IMG_NON_TRANSFER,
                                 DEPTH_IMG_COLORFUL_TRANSFER,
                                 IMAGE_SN_SYNC,
                                 0, // rectifyLogIndex
                                 color_image_callback,
                                 depth_image_callback,
                                 pc_frame_callback,
                                 imu_data_callback);
#endif
#if 0 // USB2
        ret = device->initStream(libeYs3D::video::COLOR_RAW_DATA_TYPE::COLOR_RAW_DATA_MJPG,
                                 1280, 720, 30, /* or 24 */
                                 libeYs3D::video::DEPTH_RAW_DATA_TYPE::DEPTH_RAW_DATA_11_BITS,
                                 640, 360,
//                                 DEPTH_IMG_NON_TRANSFER,
                                 DEPTH_IMG_COLORFUL_TRANSFER,
                                 IMAGE_SN_SYNC,
                                 0, // rectifyLogIndex
                                 color_image_callback,
                                 nullptr, //depth_image_callback,
                                 pc_frame_callback,
                                 imu_data_callback);
#endif
#if 0 // depth only
        ret = device->initStream(libeYs3D::video::COLOR_RAW_DATA_TYPE::COLOR_RAW_DATA_YUY2,
                                 0, 0, 60, /* or 24 */
                                 libeYs3D::video::DEPTH_RAW_DATA_TYPE::DEPTH_RAW_DATA_11_BITS,
                                 1280, 720,
//                                 DEPTH_IMG_NON_TRANSFER,
                                 DEPTH_IMG_COLORFUL_TRANSFER,
                                 IMAGE_SN_SYNC,
                                 0, // rectifyLogIndex
                                 color_image_callback,
                                 depth_image_callback,
                                 pc_frame_callback,
                                 imu_data_callback);
#endif
        if(ret != 0)    break;
                         
        device->enableStream();
        sleep(4);

#if 0
        { // Stream enablement testing
            device->pauseColorStream();
            sleep(2);
            device->pauseDepthStream();
            sleep(2);
            device->pauseIMUStream();
            sleep(4);
            
            device->enableInterleaveMode(true);
            sleep(4);
            
            device->pausePCStream();
            sleep(4);
            
            device->enableInterleaveMode(false);
            device->enablePCStream();
            sleep(4);
            device->enableColorStream();
            sleep(4);
            device->enableDepthStream();
            sleep(4);
            device->enableIMUStream();
            sleep(4);
        }
#endif
#if 0
        { // test IMU data dump
            device->dumpIMUData();
            sleep(4);
        }
#endif       

#if 0       
        { // verify snapshut
            if(device->isPlyFilterSupported())    {
                LOG_INFO(LOG_TAG, "Ply filter is supported, enable it...");
                device->enablePlyFilter(true);
            }
            
            device->doSnapshot();
            
            //device->enablePlyFilter(false);
            //device->doSnapshot();
            
            sleep(3);
        }
#endif
#if 0
        { // verify register read/write features
            libeYs3D::devices::RegisterReadWriteOptions options = device->getRegisterReadWriteOptions();
            options.enablePerodicRead(true);
            options.enableSaveLog(true);
            device->setRegisterReadWriteOptionsForRead(options);
            sleep(2);
            
            options.enablePerodicRead(false);
            options.enableSaveLog(false);
            device->setRegisterReadWriteOptionsForWrite(options);
            sleep(2);
        }
        
        { // verify depth acuracy 
            libeYs3D::devices::DepthAccuracyOptions depthAccuracyOptions = device->getDepthAccuracyOptions();
            depthAccuracyOptions.enable(true);
            depthAccuracyOptions.setRegionRatio(0.8f);
            depthAccuracyOptions.setGroundTruthDistanceMM(300.0f);
            device->setDepthAccuracyOptions(depthAccuracyOptions);
            sleep(4);
        
            depthAccuracyOptions = device->getDepthAccuracyOptions();
            depthAccuracyOptions.enable(false);
            device->setDepthAccuracyOptions(depthAccuracyOptions);
            sleep(4);
        }
        
        device->enableInterleaveMode(true);
        sleep(4);
        device->enableInterleaveMode(false);
        sleep(4);
        device->enableInterleaveMode(true);
        sleep(4);
        device->enableInterleaveMode(false);
        sleep(4);
#endif

        LOG_INFO(LOG_TAG, "\n\nClosing device stream...\n");
        device->closeStream();
        sleep(2);
    }
    
    return 0;
}
