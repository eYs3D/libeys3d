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

#include "devices/model/DepthFilterOptions.h"
#include "debug.h"

#include <stdio.h>
#include <inttypes.h>

#define LOG_TAG "DepthFilterOptions"

namespace libeYs3D    {
namespace devices    {

DepthFilterOptions::DepthFilterOptions() : mEnabled(false), mType(-1)    {
}

void DepthFilterOptions::resetDefault()    {
    setSubSampleMode(0);
    setSubSampleFactor(3);

    setLumda(0.1f);
    setSigma(0.015f);
    setEdgeLevel(1);

    setHorizontal(false);
    setLevel(1);

    setHistory(3);
    setAlpha(0.4f);
}

void DepthFilterOptions::enable(bool enable)    {
    // According to DMPreview/model/CImageDataModel.cpp::DepthFilter(BYTE *pData),
    // it seems EtronDIImageType::DEPTH_8BITS_0x80 does NOT support depth filtering
    if(mType < 0)    {
        LOG_WARN(LOG_TAG, "Depth filtering is not supported fro unknown mType...");
        return;
    } 

    mEnabled = enable;
}

void DepthFilterOptions::setBytesPerPixel(int32_t bytesPerPixel)    {
     mBytesPerPixel = bytesPerPixel;
}

void DepthFilterOptions::enableSubSample(bool enable)    {
    mSubSampleEnabled = enable;
}

void DepthFilterOptions::setSubSampleMode(int mode)    {
    mSubSampleMode = mode;
    switch(mSubSampleMode)    {
        case 0: setSubSampleFactor(3); break;
        case 1: setSubSampleFactor(4); break;
        default: break;
    }
}

void DepthFilterOptions::setSubSampleFactor(int32_t subSampleFactor)    {
    mSubSampleFactor = subSampleFactor;
}

void DepthFilterOptions::enableEdgePreServingFilter(bool enable)    {
    mEdgePreServingFilterEnabled = enable;
}

void DepthFilterOptions::setType(int type)    { 
    mType = type;
}

void DepthFilterOptions::setEdgeLevel(int edgeLevel)    {
    mEdgeLevel = edgeLevel;
}

void DepthFilterOptions::setSigma(float sigma)    {
    mSigma = sigma;
}

void DepthFilterOptions::setLumda(float lumda)    {
    mLumda = lumda;
}

void DepthFilterOptions::enableHoleFill(bool enable)    {
    mHoleFillEnabled = enable;
}

void DepthFilterOptions::setKernelSize(int32_t kernelSize)    {
    mKernelSize = kernelSize;
}

void DepthFilterOptions::setLevel(int32_t level)    {
    mLevel = level;
}

void DepthFilterOptions::setHorizontal(bool horizontal)    {
    mHorizontal = horizontal;
}

void DepthFilterOptions::enableTemporalFilter(bool enable)    {
    mTemporalFilterEnabled = enable;
}

void DepthFilterOptions::setAlpha(float alpha)    {
    mAlpha = alpha;
}

void DepthFilterOptions::setHistory(int32_t history)    {
    mHistory = history;
}

void DepthFilterOptions::enableFlyingDepthCancellation(bool enable)    {
    mFlyingDepthCancellationEnabled = enable;
}

void DepthFilterOptions::setFlyingDepthCancellationLocked(bool lock)    {
    mFlyingDepthCancellationLocked = lock;
}

int DepthFilterOptions::toString(char *buffer, int bufferLength) const    {
    int length = 0;
    
    length = snprintf(buffer, (size_t)bufferLength,
                      "---- DepthFilterOptions: enabled(%s) ----------------\n"
                      "    mBytesPerPixel: %" PRId32 "\n"
                      "    mSubSampleEnabled(%s):\n"
                      "        mSubSampleMode:   %" PRId32 "\n"
                      "        mSubSampleFactor: %" PRId32 "\n"
                      "    mEdgePreServingFilterEnabled(%s):\n"
                      "        mType:      %" PRId32 "\n"
                      "        mEdgeLevel: %" PRId32 "\n"
                      "        mSigma:     %.6f\n"
                      "        mLumda:     %.6f\n"
                      "    mHoleFillEnabled(%s):\n"
                      "        mKernelSize: %" PRId32 "\n"
                      "        mLevel:      %" PRId32 "\n"
                      "        mHorizontal: %s\n"
                      "    mTemporalFilterEnabled(%s):\n"
                      "        mAlpha:    %.6f\n"
                      "        mHistory:  %" PRId32 "\n"
                      "    mFlyingDepthCancellationEnabled(%s):\n"
                      "        mFlyingDepthCancellationLocked: %s",
                      mEnabled ? "true" : "false",
                      mBytesPerPixel,
                      mSubSampleEnabled ? "true" : "false",
                      mSubSampleMode, mSubSampleFactor,
                      mEdgePreServingFilterEnabled ? "true" : "false",
                      mType, mEdgeLevel, mSigma, mLumda,
                      mHoleFillEnabled ? "true" : "false",
                      mKernelSize, mLevel, mHorizontal ? "true" : "false",
                      mTemporalFilterEnabled ? "true" : "false",
                      mAlpha, mHistory,
                      mFlyingDepthCancellationEnabled ? "true" : "false",
                      mFlyingDepthCancellationLocked ? "true" : "false");
    
    return (length > bufferLength) ? bufferLength : length;
}

int DepthFilterOptions::toString(std::string &string) const    {
    int ret = 0;
    char buffer[2048];
    
    ret = toString(buffer, sizeof(buffer));
    string.append(buffer);
    
    return ret;
}

} // end of namespace devices
} // end of namespace libeYs3D
