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

#include "video/PCFrame.h"

#include <stdio.h>
#include <inttypes.h>

#define LOG_TAG "PCFrame"

// better align with SensorDataProducer.cpp * 2,
// cause in interleave mode: [color S/N] + [Depth (S/N + 1)] + [IMU S/N] + [IMU S/N + 1]
#define SENSOR_DATA_VEC_CAP (16 + 16) // better align with SensorDataProducer.cpp * 2

namespace libeYs3D    {
namespace video    {

#ifdef DEVICE_MEMORY_ALLOCATOR

static std::allocator<uint8_t> sDefaultByteAllocator;
static std::allocator<float> sDefaultFloatAllocator;

// Allocates the space with capacity |cap| and sets each element to |val|
// This constructor is only use by  base/synchronization/MessageChannel.h
//     template <typename T, size_t CAPACITY>
//     class MessageChannel : public MessageChannelBase {   
//         private:
//            T mItems[CAPACITY];
PCFrame::PCFrame(uint32_t pixelCount, uint8_t val, float fVal)
    : rgbDataVec(pixelCount << 2, val, sDefaultByteAllocator),
      xyzDataVec(pixelCount * 3, fVal, sDefaultFloatAllocator),
      sensorDataSet(libeYs3D::sensors::SensorData::SensorDataType::UNKNOWN, SENSOR_DATA_VEC_CAP)    {
}

PCFrame::PCFrame(uint32_t pixelCount, uint8_t val, float fVal,
                 libeYs3D::devices::MemoryAllocator<uint8_t> &byteAllocator,
                 libeYs3D::devices::MemoryAllocator<float> &floatAllocator)
    : rgbDataVec(pixelCount << 2, val, byteAllocator),
      xyzDataVec(pixelCount * 3, fVal, floatAllocator),
      sensorDataSet(libeYs3D::sensors::SensorData::SensorDataType::UNKNOWN, SENSOR_DATA_VEC_CAP)    {
}

#else

PCFrame::PCFrame(uint32_t pixelCount, uint8_t val, float fVal)
    : rgbDataVec(pixelCount << 2), xyzDataVec(pixelCount * 3, fVal),
      sensorDataSet(libeYs3D::sensors::SensorData::SensorDataType::UNKNOWN, SENSOR_DATA_VEC_CAP)    {
}

#endif

int PCFrame::toString(char *buffer, int bufferLength) const    {
    return snprintf(buffer, (size_t)bufferLength,
                    "---- PCFrame Info. (%p) ----\n"
                    "        tsUs:         %" PRId64 "\n"
                    "        serialNumber: %" PRIu32 "\n"
                    "        width:        %" PRIu32 "\n"
                    "        height:       %" PRIu32 "",
                    this, tsUs, serialNumber, width, height);
}

int PCFrame::toStringSimple(char *buffer, int bufferLength) const    {
    return snprintf(buffer, (size_t)bufferLength,
                    "---- PCFrame Info. ----------------------------\n"
                    "                    tsUs: %" PRId64 "\n"
                    "            serialNumber: %" PRIu32 "\n"
                    "         sensor data set: sn(%" PRIu32 "), count(%d)",
                    tsUs, serialNumber,
                    sensorDataSet.serialNumber, sensorDataSet.actualDataCount);
}

int PCFrame::toString(std::string &string) const   {
    int ret = 0;
    char buffer[512];
    
    ret = toString(buffer, 512);
    string.append(buffer);
    
    return ret;
}

} // namespace video
} // namespace libeYs3D
