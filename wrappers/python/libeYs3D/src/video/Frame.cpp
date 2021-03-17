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

#include "video/Frame.h"
#include "video/FrameProducer.h"
#include "debug.h"
#include "utils.h"

#include <stdio.h>
#include <inttypes.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/limits.h>

#define LOG_TAG    "Frame"

#define SENSOR_DATA_VEC_CAP (16) // better align with SensorDataProducer.cpp

namespace libeYs3D    {
namespace video    {

#ifdef DEVICE_MEMORY_ALLOCATOR

static std::allocator<uint8_t> sDefaultAllocator;

// Allocates the space with capacity |cap| and sets each element to |val|
// This constructor is only use by  base/synchronization/MessageChannel.h
//     template <typename T, size_t CAPACITY>
//     class MessageChannel : public MessageChannelBase {   
//         private:
//            T mItems[CAPACITY];
Frame::Frame(uint64_t dataBufferSize, uint64_t rgbBufferSize, uint8_t val)
    : actualDataBufferSize(0), dataBufferSize(dataBufferSize),
      dataVec(dataBufferSize, val, sDefaultAllocator),
      actualRGBBufferSize(0), rgbBufferSize(rgbBufferSize),
      rgbVec(rgbBufferSize, val, sDefaultAllocator),
      sensorDataSet(libeYs3D::sensors::SensorData::SensorDataType::UNKNOWN, SENSOR_DATA_VEC_CAP)    {
}

Frame::Frame(uint64_t dataBufferSize, uint64_t rgbBufferSize, uint8_t val,
             libeYs3D::devices::MemoryAllocator<uint8_t> &allocator)
    : actualDataBufferSize(0), dataBufferSize(dataBufferSize),
      dataVec(dataBufferSize, val, allocator),
      actualRGBBufferSize(0), rgbBufferSize(rgbBufferSize),
      rgbVec(dataBufferSize, val, allocator),
      sensorDataSet(libeYs3D::sensors::SensorData::SensorDataType::UNKNOWN, SENSOR_DATA_VEC_CAP)    {
}

#else

Frame::Frame(uint64_t dataBufferSize, uint8_t initDataVal,
             uint64_t zdDepthBufferSize, uint16_t initZDDepthVal,
             uint64_t rgbBufferSize, uint8_t initRGBVal)
    : actualDataBufferSize(0), dataBufferSize(dataBufferSize), dataVec(dataBufferSize, initDataVal),
      actualZDDepthBufferSize(0), zdDepthBufferSize(zdDepthBufferSize), zdDepthVec(zdDepthBufferSize, initZDDepthVal),
      actualRGBBufferSize(0), rgbBufferSize(rgbBufferSize), rgbVec(rgbBufferSize, initRGBVal),
      sensorDataSet(libeYs3D::sensors::SensorData::SensorDataType::UNKNOWN, SENSOR_DATA_VEC_CAP)    {
}

#endif

int Frame::toString(char *buffer, int bufferLength) const    {
    return snprintf(buffer, (size_t)bufferLength,
                    "---- Frame Info. (%p) ----\n"
                    "    tsUs:         %" PRIu64 "\n"
                    "    serialNumber: %" PRIu32 "\n"
                    "    format:       %" PRIu32 "\n"
                    "    width:        %" PRIu32 "\n"
                    "    height:       %" PRIu32 "\n"
                    "    dataBufferSize:          %" PRIu64 "\n"
                    "    actualDataBufferSize:    %" PRIu64 "\n"
                    "    zdDepthBufferSize:       %" PRIu64 "\n"
                    "    actualZDDepthBufferSize: %" PRIu64 "\n"
                    "    rgbBufferSize:           %" PRIu64 "\n"
                    "    actualRGBBufferSize:     %" PRIu64 "\n"
                    "    rgbTranscodingTime:      %" PRIu64 " us\n"
                    "    filteringTime:           %" PRIu64 " us",
                    this, tsUs, serialNumber, dataFormat, width, height,
                    dataBufferSize, actualDataBufferSize,
                    zdDepthBufferSize, actualZDDepthBufferSize,
                    rgbBufferSize, actualRGBBufferSize,
                    rgbTranscodingTime, filteringTime);
}

int Frame::toStringSimple(char *buffer, int bufferLength) const    {
    return snprintf(buffer, (size_t)bufferLength,
                    "---- Frame Info. --------------------------------------\n"
                    "                    tsUs: %" PRIu64 "\n"
                    "            serialNumber: %" PRIu32 "\n"
                    "         sensor data set: sn(%" PRIu32 "), count(%d)",
                    tsUs, serialNumber,
                    sensorDataSet.serialNumber, sensorDataSet.actualDataCount);
}

int Frame::toStringFull(char *buffer, int bufferLength) const    {
    return snprintf(buffer, (size_t)bufferLength,
                    "---- Frame Info. (%p) ----\n"
                    "    tsUs:         %" PRIu64 "\n"
                    "    serialNumber: %" PRIu32 "\n"
                    "    format:       %" PRIu32 "\n"
                    "    width:        %" PRIu32 "\n"
                    "    height:       %" PRIu32 "\n"
                    "    dataBufferSize:          %" PRIu64 "\n"
                    "    actualDataBufferSize:    %" PRIu64 "\n"
                    "    zdDepthBufferSize:       %" PRIu64 "\n"
                    "    actualZDDepthBufferSize: %" PRIu64 "\n"
                    "    rgbBufferSize:           %" PRIu64 "\n"
                    "    actualRGBBufferSize:     %" PRIu64 "\n"
                    "    rgbTranscodingTime:      %" PRIu64 " us\n"
                    "    filteringTime:           %" PRIu64 " us\n"
                    "    ---- DepthAccuracyInfo ----\n"
                    "        fDistance:        %.4f\n"
                    "        fFillRate:        %.4f\n"
                    "        fZAccuracy:       %.4f\n"
                    "        fDistance:        %.4f\n"
                    "        fTemporalNoise:   %.4f\n"
                    "        fAngle:           %.4f\n"
                    "            fAngleX:      %.4f\n"
                    "            fAngleY:      %.4f\n",
                    this, tsUs, serialNumber, dataFormat, width, height,
                    dataBufferSize, actualDataBufferSize,
                    zdDepthBufferSize, actualZDDepthBufferSize,
                    rgbBufferSize, actualRGBBufferSize,
                    rgbTranscodingTime, filteringTime,
                    this->extra.depthAccuracyInfo.fDistance,
                    this->extra.depthAccuracyInfo.fFillRate,
                    this->extra.depthAccuracyInfo.fZAccuracy,
                    this->extra.depthAccuracyInfo.fTemporalNoise,
                    this->extra.depthAccuracyInfo.fSpatialNoise,
                    this->extra.depthAccuracyInfo.fAngle,
                    this->extra.depthAccuracyInfo.fAngleX,
                    this->extra.depthAccuracyInfo.fAngleY);
}

int Frame::toString(std::string &string) const    {
    int ret = 0;
    char buffer[512];
    
    ret = toString(buffer, 512);
    string.append(buffer);
    
    return ret;
}

int Frame::saveToFile(const char *dirPath) const   {
    DIR* dir = ::opendir(dirPath);
    if(dir) { // Directory exists.
        ::closedir(dir);
        if(::access(dirPath, W_OK) != 0) {
            LOG_ERR(LOG_TAG, "%s is not writable!", dirPath);
            return -errno;
        }
    } else if(ENOENT == errno) { //Directory does not exist.
        if(0 != mkdir(dirPath, 0775))    {
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

    { // create data & fgb files 
        int fd;
        int i;
        char filePath[PATH_MAX];
        
        if(actualDataBufferSize > 0)    {
            snprintf(filePath, PATH_MAX, "%s/frame_data.%" PRIu32 "", dirPath, serialNumber);
            fd = open(filePath, O_RDWR | O_CLOEXEC | O_CREAT, 0664);
            ::write_fully(fd, (const void *)(dataVec.data()), actualDataBufferSize, LOG_TAG"-data");
            ::close (fd);
        }
        
        if(actualRGBBufferSize > 0)    {
            snprintf(filePath, PATH_MAX, "%s/frame_rgb.%" PRIu32 "", dirPath, serialNumber);
            fd = open(filePath, O_RDWR | O_CLOEXEC | O_CREAT, 0664);
            ::write_fully(fd, (const void *)(rgbVec.data()), actualRGBBufferSize, LOG_TAG"-RGB");
            ::close (fd);
        }
    }
    
    return 0;
}

void Frame::clone(const Frame *frame)    {
    this->tsUs = frame->tsUs ;   
    this->serialNumber = frame->serialNumber; 
    this->width = frame->width;
    this->height = frame->height;
    
    this->actualDataBufferSize = frame->actualDataBufferSize;
    this->dataBufferSize = frame->dataBufferSize;
    if(this->dataVec.capacity() != frame->dataVec.capacity())
        this->dataVec.resize(frame->dataVec.capacity());
    memcpy((void *)this->dataVec.data(),
           (void *)frame->dataVec.data(),
           this->actualDataBufferSize);
    
    this->actualZDDepthBufferSize = frame->actualZDDepthBufferSize;
    this->zdDepthBufferSize = frame->zdDepthBufferSize;
    if(this->zdDepthVec.capacity() != frame->zdDepthVec.capacity())
        this->zdDepthVec.resize(frame->zdDepthVec.capacity());
    memcpy((void *)this->zdDepthVec.data(),
           (void *)frame->zdDepthVec.data(),
           this->actualZDDepthBufferSize < 1 /* uint16_t */);

    this->actualRGBBufferSize = frame->actualRGBBufferSize;
    this->rgbBufferSize = frame->rgbBufferSize;
    if(this->rgbVec.capacity() != frame->rgbVec.capacity())
        this->rgbVec.resize(frame->rgbVec.capacity());
    memcpy((void *)this->rgbVec.data(),
           (void *)frame->rgbVec.data(),
           this->actualRGBBufferSize);

    this->dataFormat = frame->dataFormat;
    this->rgbFormat = frame->rgbFormat;
    this->rgbTranscodingTime = frame->rgbTranscodingTime;
    this->filteringTime = frame->filteringTime;
        
    this->sensorDataSet.clone(&frame->sensorDataSet);
    
    memcpy((void *)&this->extra, (void *)&frame->extra, sizeof(extra));
    
    this->toCallback = frame->toCallback;
    this->toPCCallback = frame->toPCCallback;
    this->interleaveMode = frame->interleaveMode;
}

} // namespace video
} // namespace libeYs3D
