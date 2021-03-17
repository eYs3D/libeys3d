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

#include "devices/model/DepthAccuracyOptions.h"
#include "debug.h"

#include <stdio.h>
#include <inttypes.h>

#define LOG_TAG "DepthAccuracyOptions"

namespace libeYs3D    {
namespace devices    {

DepthAccuracyOptions::DepthAccuracyOptions()
    : mEnabled(false), mRegionRatio(0.0f), mGroundTruthDistanceMM(0.0f)    {
}

void DepthAccuracyOptions::resetDefault()    {
    mEnabled = false;
    mRegionRatio = 0.0f;
    mGroundTruthDistanceMM = 0.0f;
}

void DepthAccuracyOptions::enable(bool enable)    {
    mEnabled = enable;
}

void DepthAccuracyOptions::setRegionRatio(float regionRatio)    {
    mRegionRatio = regionRatio;
}

float DepthAccuracyOptions::getRegionRatio()    {
    return mRegionRatio;
}
    
void DepthAccuracyOptions::setGroundTruthDistanceMM(float groundTruthDistanceMM)    {
    mGroundTruthDistanceMM = groundTruthDistanceMM;
}

float DepthAccuracyOptions::getGroundTruthDistanceMM()    {
    return mGroundTruthDistanceMM;
}

int DepthAccuracyOptions::toString(char *buffer, int bufferLength) const    {
    int length = 0;
    
    length = snprintf(buffer, (size_t)bufferLength,
                      "---- DepthAccuracyOptions: enabled(%s) ----------------\n"
                      "        mRegionRatio:     %.3f\n"
                      "        mGroundTruthDistanceMM:     %.3f\n",
                      mEnabled ? "true" : "false",
                      mRegionRatio,
                      mGroundTruthDistanceMM);
    
    return (length > bufferLength) ? bufferLength : length;
}

int DepthAccuracyOptions::toString(std::string &string) const    {
    int ret = 0;
    char buffer[2048];
    
    ret = toString(buffer, sizeof(buffer));
    string.append(buffer);
    
    return ret;
}

} // end of namespace devices
} // end of namespace libeYs3D
