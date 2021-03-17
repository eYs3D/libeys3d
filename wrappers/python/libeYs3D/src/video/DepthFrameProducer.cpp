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

#include "video/DepthFrameProducer.h"
#include "EYS3DSystem.h"
#include "eSPDI.h"
#include "base/threads/Async.h"
#include "video/coders.h"
#include "video/pc_coders.h"
#include "utils.h"
#include "debug.h"
#include "Constants.h"

#include <math.h>
#include <algorithm>

#define LOG_TAG "DepthFrameProducer"

#define TEMPORAL_NOISE_COUNT (9)

namespace libeYs3D    {
namespace video    {

DepthFrameProducer::DepthFrameProducer(CameraDevice *cameraDevice)
    : FrameProducer(cameraDevice,
                    cameraDevice->mDepthWidth, cameraDevice->mDepthHeight,
                    cameraDevice->mDepthFormat, cameraDevice->mActualFps),
      mDepthList(TEMPORAL_NOISE_COUNT),
      mDACalculateThreadPool(3, [this](DACalculateWorkItem&& item)    {
          item.callback(item.frame);
      })    {
    for(auto& i : mDepthList)
        i.resize(cameraDevice->mDepthWidth * cameraDevice->mDepthHeight, 0);

    mCalculateDepthAccuracyInfo = std::bind(&DepthFrameProducer::calculateDepthAccuracyInfo,
                                            this,
                                            std::placeholders::_1);
    mCalculateDepthSpatialNoise = std::bind(&DepthFrameProducer::calculateDepthSpatialNoise,
                                            this,
                                            std::placeholders::_1);
    mCalculateDepthTemporalNoise = std::bind(&DepthFrameProducer::calculateDepthTemporalNoise,
                                             this,
                                             std::placeholders::_1);                                            

    mDACalculateThreadPool.start();
}

void DepthFrameProducer::checkIMUDeviceCBEnablement()    {
    if(!mCameraDevice->isIMUDevicePresent())    return;
    
    if((mFrameProducerState & FB_APP_STREAM_ACTIVATED) ||
       (mFrameProducerState & FB_PC_STREAM_ACTIVATED))
        mCameraDevice->mIMUDevice->enableDepthStream();
    else
        mCameraDevice->mIMUDevice->pauseDepthStream();
}

int DepthFrameProducer::getRawFormatBytesPerPixel(uint32_t format)    {
    return get_depth_image_format_byte_length_per_pixel(depth_raw_type_to_depth_image_type(format));
}

int DepthFrameProducer::readFrame(Frame *frame)    {
    int ret = 0;
    ret = mCameraDevice->readDepthFrame(frame->dataVec.data(), frame->dataBufferSize,
                                        &(frame->actualDataBufferSize), &(frame->serialNumber));
    
    return ret;
}

int DepthFrameProducer::produceRGBFrame(Frame *frame)    {
    int ret = 0;
    
    // producing rgb image
    int64_t currTime = now_in_microsecond_high_res_time();
    {
        ret = depth_image_produce_rgb_frame((const CameraDevice *)(this->mCameraDevice), frame);
    }
    int64_t newTime = now_in_microsecond_high_res_time();
    frame->rgbTranscodingTime = newTime - currTime;
    
    return ret;
}

int DepthFrameProducer::performFiltering(Frame *frame)    {
    if(false == mCameraDevice->mDepthFilterOptions.isEnabled())    return 0;
    
    uint8_t *subDisparity = frame->dataVec.data();
    int new_width = mFrameWidth;
    int new_height = mFrameHeight;

    EtronDI_ResetFilters(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                         &(mCameraDevice->mDevSelInfo));

    bool bIsSubSample = mCameraDevice->mDepthFilterOptions.isSubSampleEnabled();
    if(bIsSubSample)    {   
        new_width = 0;
        new_height = 0;
        subDisparity = nullptr;   // sub_disparity Initialize;
        EtronDI_SubSample(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                          &(mCameraDevice->mDevSelInfo),
                          &subDisparity,
                          frame->dataVec.data(),
                          mCameraDevice->mDepthFilterOptions.getBytesPerPixel(),
                          mFrameWidth, mFrameHeight,
                          new_width, new_height,
                          mCameraDevice->mDepthFilterOptions.getSubSampleMode(),
                          mCameraDevice->mDepthFilterOptions.getSubSampleFactor());
    }
    
    if(mCameraDevice->mDepthFilterOptions.isEdgePreServingFilterEnabled())
        EtronDI_EdgePreServingFilter(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                     &(mCameraDevice->mDevSelInfo),
                                     subDisparity,
                                     mCameraDevice->mDepthFilterOptions.getType(),
                                     new_width, new_height,
                                     mCameraDevice->mDepthFilterOptions.getEdgeLevel(),
                                     mCameraDevice->mDepthFilterOptions.getSigma(),
                                     mCameraDevice->mDepthFilterOptions.getLumda());

    if(mCameraDevice->mDepthFilterOptions.isHoleFillEnabled())
        EtronDI_HoleFill(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                         &(mCameraDevice->mDevSelInfo),
                         subDisparity,
                         mCameraDevice->mDepthFilterOptions.getBytesPerPixel(),
                         mCameraDevice->mDepthFilterOptions.getKernelSize(),
                         new_width, new_height,
                         mCameraDevice->mDepthFilterOptions.getLevel(),
                         mCameraDevice->mDepthFilterOptions.isHorizontal());

    if(mCameraDevice->mDepthFilterOptions.isTemporalFilterEnabled())
        EtronDI_TemporalFilter(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                               &(mCameraDevice->mDevSelInfo),
                               subDisparity,
                               mCameraDevice->mDepthFilterOptions.getBytesPerPixel(),
                               new_width, new_height,
                               mCameraDevice->mDepthFilterOptions.getAlpha(),
                               mCameraDevice->mDepthFilterOptions.getHistory());
                               
    if(bIsSubSample)
        EtronDI_ApplyFilters(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                             &(mCameraDevice->mDevSelInfo),
                             frame->dataVec.data(),
                             subDisparity,
                             mCameraDevice->mDepthFilterOptions.getBytesPerPixel(),
                             mFrameWidth, mFrameHeight,
                             new_width, new_height);

    if(mCameraDevice->mDepthFilterOptions.isFlyingDepthCancellationEnabled())    {
        if(depth_raw_type_to_depth_image_type(mPixelRawFormat) == EtronDIImageType::DEPTH_8BITS)
            EtronDI_FlyingDepthCancellation_D8(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                               &(mCameraDevice->mDevSelInfo),
                                               frame->dataVec.data(),
                                               mFrameWidth, mFrameHeight);
        else if(depth_raw_type_to_depth_image_type(mPixelRawFormat) == EtronDIImageType::DEPTH_11BITS)
            EtronDI_FlyingDepthCancellation_D11(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                &(mCameraDevice->mDevSelInfo),
                                                frame->dataVec.data(),
                                                mFrameWidth, mFrameHeight);
        else if (depth_raw_type_to_depth_image_type(mPixelRawFormat) == EtronDIImageType::DEPTH_14BITS)    {
            const size_t depthSize = mFrameWidth * mFrameHeight;
            const uint16_t *zdTable = (uint16_t *)(mCameraDevice->mZDTableInfo.nZDTable);

            if(depthSize != mZ14ToD11.size())    mZ14ToD11.resize(depthSize);
            if(mTableZ14ToD11.empty())    {
                int desparity = 0;
                mTableZ14ToD11.resize(16385, 0);
                for(int i = 0; i < 2048; i++)    {   
                    mTableZ14ToD11[((zdTable[i] & 0xff) << 8) | ((zdTable[i] & 0xff00) >> 8)] = ((i & 0xff) << 8) | ((i & 0xff00) >> 8);
                }
                
                for(int i = 16384; i >= 0; i--)    {
                    if(mTableZ14ToD11[i])    desparity = mTableZ14ToD11[i];
                    else mTableZ14ToD11[i] = desparity;
                }
            }
            
            uint16_t *pZ14Depth = (uint16_t *)frame->dataVec.data();
            EtronDI_TableToData(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                &(mCameraDevice->mDevSelInfo),
                                mFrameWidth, mFrameHeight,
                                mTableZ14ToD11.size() * sizeof(uint16_t),
                                mTableZ14ToD11.data(),
                                pZ14Depth,
                                mZ14ToD11.data());
            EtronDI_FlyingDepthCancellation_D11(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                                &(mCameraDevice->mDevSelInfo),
                                                (uint8_t *)mZ14ToD11.data(),
                                                mFrameWidth, mFrameHeight);
            EtronDI_TableToData(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                &(mCameraDevice->mDevSelInfo),
                                mFrameWidth, mFrameHeight,
                                2048 * sizeof(uint16_t),
                                (uint16_t *)zdTable,
                                mZ14ToD11.data(),
                                pZ14Depth);
        }
    }
    
    return 0;
}

int DepthFrameProducer::performInterleave(Frame *frame)    {
     if(mCameraDevice->isInterleaveModeEnabled())    {
        frame->interleaveMode = true;
        if((frame->serialNumber % 2) == 1)
            return 0;
        else // drop it...
            return -1;
    } else    {
        frame->interleaveMode = false;
    }
    
    return 0;
}

int DepthFrameProducer::performAccuracyComputation(Frame *frame)    {
    memset(&(frame->extra.depthAccuracyInfo), 0, sizeof(frame->extra.depthAccuracyInfo));
    
    if(false == mCameraDevice->mDepthAccuracyOptions.isEnabled())    return 0;
    
    DACalculateWorkItem workItem1(mCalculateDepthAccuracyInfo, frame);
    DACalculateWorkItem workItem2(mCalculateDepthSpatialNoise, frame);
    DACalculateWorkItem workItem3(mCalculateDepthTemporalNoise, frame);
    mDACalculateThreadPool.enqueue(std::move(workItem1));
    mDACalculateThreadPool.enqueue(std::move(workItem2));
    mDACalculateThreadPool.enqueue(std::move(workItem3));
    
    // waiting for accomplishment of jobs
    mFinishSignal.receive();
    mFinishSignal.receive();
    mFinishSignal.receive();
    
    return 0;
}

void DepthFrameProducer::performSnapshotWork(Frame *frame)    {
    // clone data
    std::vector<uint8_t> dataVec(frame->dataVec); // using copy constructor
    std::vector<uint8_t> rgbVec(frame->rgbVec); // using copy constructor
    int64_t tsMs = frame->tsUs / 1000;
    uint32_t serialNumber = frame->serialNumber;
    int nWidth = frame->width;
    int nHeight = frame->height;
    int depthRawBytePerPixel = get_depth_image_format_byte_length_per_pixel(depth_raw_type_to_depth_image_type(frame->dataFormat));
    const char *snapShotPath = libeYs3D::EYS3DSystem::getEYS3DSystem()->getSnapshotPath();
    
    // notify as soon as data cloning is finished
    mSnapshotFinishedSignal.send(1);
    
    {
        char depthPathRGB[PATH_MAX];
        char depthPathYUV[PATH_MAX];
        char tmpBuffer[128];
        
        get_time_YYYY_MM_DD_HH_MM_SS(tsMs, tmpBuffer, sizeof(tmpBuffer));
        snprintf(depthPathRGB, sizeof(depthPathRGB), "%s/snapshot-depth-%" PRIu32 "-%s.bmp",
                 snapShotPath, serialNumber, tmpBuffer);
        snprintf(depthPathYUV, sizeof(depthPathYUV), "%s/snapshot-depth-%" PRIu32 "-%s.yuv",
                 snapShotPath, serialNumber, tmpBuffer);
        
        save_bitmap(depthPathRGB, rgbVec.data(), nWidth, nHeight);
        save_yuv(depthPathYUV, dataVec.data(), nWidth, nHeight, depthRawBytePerPixel);
    }
}

void DepthFrameProducer::calculateDepthAccuracyInfo(Frame *frame)    {
    const Rect region = mCameraDevice->getDepthAccuracyRegion();
    const int nLeft   = region.x;
    const int nTop    = region.y;
    const int nRight  = region.x + region.width - 1;;
    const int nBottom = region.y + region.height - 1;

    unsigned long nFillCount = 0;
    unsigned long nTotalCount = 0;
    unsigned long nTotalDepth = 0;
    unsigned short wDepth;
    
    for(int y = nTop; y < nBottom; ++y)    {
        for(int x = nLeft; x < nRight; ++x)    {
            wDepth = getZValue(frame, getDepth(frame, x, y));

            if(wDepth)    nFillCount++;

            nTotalDepth += wDepth;

            ++nTotalCount;
        }
    }
    
    struct libeYs3D::devices::DepthAccuracyInfo *depthAccuracyInfo = &(frame->extra.depthAccuracyInfo);
    if(nFillCount)    {
        depthAccuracyInfo->fDistance = nTotalDepth / (float)nFillCount;
                
        if(nTotalCount)    {
            depthAccuracyInfo->fFillRate = nFillCount / (float)nTotalCount;
        }

        float groundTruthDistanceMM = mCameraDevice->mDepthAccuracyOptions.getGroundTruthDistanceMM();
        if(groundTruthDistanceMM > 0.0f)    {
            depthAccuracyInfo->fZAccuracy = (depthAccuracyInfo->fDistance - groundTruthDistanceMM) /
                                            groundTruthDistanceMM;
        } else    {
            depthAccuracyInfo->fZAccuracy = 0.0f;
        }        
    }
    
    mFinishSignal.send(1);
}

void DepthFrameProducer::calculateDepthSpatialNoise(Frame *frame)    {
    const int nDepthSize = frame->width * frame->height;
    std::vector<uint16_t> vecDepthZ(nDepthSize);

    double MatrixXX = 0.0;
    double MatrixYY = 0.0;
    double MatrixXY = 0.0;
    double MatrixX  = 0.0;
    double MatrixY  = 0.0;
    double MatrixN  = 0.0;
    double MatrixXZ = 0.0;
    double MatrixYZ = 0.0;
    double MatrixZ  = 0.0;

    double MatrixBase = 0.0;
    double a          = 0.0;
    double b          = 0.0;
    double d          = 0.0;
    double DepthZSum  = 0.0;
    int    Count      = 0;
    int    idx        = 0;

    memset(vecDepthZ.data(), 0, nDepthSize * sizeof(uint16_t));

    const Rect region = mCameraDevice->getDepthAccuracyRegion();
    const int nLeft   = region.x;
    const int nTop    = region.y;
    const int nRight  = region.x + region.width - 1;;
    const int nBottom = region.y + region.height - 1;
    
    for(int y = nTop; y < nBottom; y++)    {
        for(int x = nLeft; x < nRight; x++)    {
            idx = y * frame->width + x;

            vecDepthZ[idx] = getZValue(frame, getDepth(frame, x, y));

            if (vecDepthZ[idx])    {
                MatrixXX += (x * x);
                MatrixYY += (y * y);
                MatrixXY += (x * y);
                MatrixX  += x;
                MatrixY  += y;
                MatrixN++;
                MatrixXZ += (x * vecDepthZ[idx]);
                MatrixYZ += (y * vecDepthZ[idx]);
                MatrixZ  += vecDepthZ[idx];
            }
        }
    }
    MatrixBase = MatrixXX * MatrixYY * MatrixN + 2 * MatrixXY * MatrixX * MatrixY -
                 MatrixX  * MatrixX  * MatrixYY - MatrixY  * MatrixY  * MatrixXX -
                 MatrixXY * MatrixXY * MatrixN;
    a = ( MatrixXZ * MatrixYY * MatrixN + MatrixZ  * MatrixXY * MatrixY + MatrixYZ * MatrixX  * MatrixY -
          MatrixZ  * MatrixYY * MatrixX - MatrixXZ * MatrixY  * MatrixY - MatrixYZ * MatrixXY * MatrixN ) / MatrixBase;
    b = ( MatrixYZ * MatrixXX * MatrixN + MatrixXZ * MatrixX  * MatrixY + MatrixZ  * MatrixXY * MatrixX -
          MatrixYZ * MatrixX  * MatrixX - MatrixZ  * MatrixXX * MatrixY - MatrixXZ * MatrixXY * MatrixN ) / MatrixBase;
    d = ( MatrixZ  * MatrixXX * MatrixYY + MatrixYZ * MatrixXY * MatrixX + MatrixXZ * MatrixXY * MatrixY -
          MatrixXZ * MatrixYY * MatrixX  - MatrixYZ * MatrixXX * MatrixY - MatrixZ  * MatrixXY * MatrixXY ) / MatrixBase;
          
    for(int y = nTop; y < nBottom; y++)    {
        for(int x = nLeft; x < nRight; x++)    {
            idx = y * frame->width + x;

            if(vecDepthZ[idx])    {
                Count++;
                DepthZSum += pow(vecDepthZ[idx] - (a * x + b * y + d ), 2);
            }
        }
    }
    
    struct libeYs3D::devices::DepthAccuracyInfo *depthAccuracyInfo = &(frame->extra.depthAccuracyInfo);

    depthAccuracyInfo->fSpatialNoise = Count ? (sqrt(DepthZSum / Count)) : 0.0f;
    float groundTruthDistanceMM = mCameraDevice->mDepthAccuracyOptions.getGroundTruthDistanceMM();
    if(groundTruthDistanceMM > 0.0f)    {
        depthAccuracyInfo->fSpatialNoise = (depthAccuracyInfo->fSpatialNoise - groundTruthDistanceMM ) /
                                           groundTruthDistanceMM;
    } else    {
        depthAccuracyInfo->fSpatialNoise = 0.0f;
    }

    depthAccuracyInfo->fAngle = acos(1.0f / sqrt(a * a + b * b + 1)) * 180.0f / M_PI;
    depthAccuracyInfo->fAngleX = acos(1.0f / sqrt(a * a + 1)) * 180.0f / M_PI;
    if(a < 0)    depthAccuracyInfo->fAngleX *= -1.0f;
    depthAccuracyInfo->fAngleY = acos(1.0f / sqrt(b * b + 1)) * 180.0f / M_PI;
    if(b < 0)    depthAccuracyInfo->fAngleY *= -1.0f;
    
    mFinishSignal.send(1);
}

void DepthFrameProducer::calculateDepthTemporalNoise(Frame *frame)    {
    const int nDepthSize = frame->width * frame->height;

    int nDepthZSum = 0;
    int nCount = 0;
    short nAvgDepth = 0;

    std::vector<float> vecSTD(nDepthSize, 0);

    int std_cnt = 0;
    int idx = 0;

    const Rect region = mCameraDevice->getDepthAccuracyRegion();
    const int nLeft   = region.x;
    const int nTop    = region.y;
    const int nRight  = region.x + region.width - 1;;
    const int nBottom = region.y + region.height - 1;

    std::vector<int16_t>& vecDepthZ = *mDepthList.rbegin();
    std_cnt = 0;
    for(int y = nTop; y < nBottom; y++)    {
        for( int x = nLeft; x < nRight; x++)    {
            vecDepthZ[y * frame->width + x] = getZValue(frame, getDepth(frame, x, y));
        }
    }
    
    for(int y = nTop; y < nBottom; y++)    {
        for(int x = nLeft; x < nRight; x++)    {
            idx = y * frame->width + x;

            nDepthZSum = 0;
            nCount     = 0;

            for(auto& vecDepth : mDepthList)    {
                if(vecDepth[idx])    nCount++;

                nDepthZSum += vecDepth[idx];
            }
            if(nCount)    {
                nAvgDepth = nDepthZSum / nCount;

                nDepthZSum = 0;

                for(auto& vecDepth : mDepthList)    {
                    if(vecDepth[idx])    {
                        nDepthZSum += pow(vecDepth[idx] - nAvgDepth, 2);
                    }
                }
                
                vecSTD[std_cnt++] = sqrt(nDepthZSum / (float)nCount);
            }
        }
    }

    if(std_cnt)    {
        struct libeYs3D::devices::DepthAccuracyInfo *depthAccuracyInfo = &(frame->extra.depthAccuracyInfo);
        float groundTruthDistanceMM = mCameraDevice->mDepthAccuracyOptions.getGroundTruthDistanceMM();
        
        std::sort(vecSTD.begin(), vecSTD.begin() + std_cnt);
        depthAccuracyInfo->fTemporalNoise = vecSTD[std_cnt / 2];
        if(groundTruthDistanceMM > 0.0f)    {
            depthAccuracyInfo->fTemporalNoise  =
                (depthAccuracyInfo->fTemporalNoise - groundTruthDistanceMM ) / groundTruthDistanceMM;
        } else    {
            depthAccuracyInfo->fTemporalNoise = 0.0f;
        }
    }
    
    mDepthList.splice( mDepthList.end(), mDepthList, mDepthList.begin() );
    
    mFinishSignal.send(1);
}

uint16_t DepthFrameProducer::getDepth(const Frame *frame, int x, int y)    {
    if(x < 0 || y < 0)    return 0;
    if(x >= frame->width || y >= frame->height)    return 0;

    uint32_t nBytePerPixel = getRawFormatBytesPerPixel(frame->dataFormat);
    uint32_t nPixelPosition = (y * frame->width + x) * nBytePerPixel;
    uint16_t nDepth = 0;
    switch (nBytePerPixel){
        case 1:
            nDepth = frame->dataVec.data()[nPixelPosition];
            break;
        case 2:
            nDepth = *(uint16_t *)(&(frame->dataVec.data()[nPixelPosition]));
            break;
        default:
            break;
    }

    return nDepth;
}

uint16_t DepthFrameProducer::getZValue(const Frame *frame, uint16_t depth)    {
    struct libeYs3D::devices::ZDTableInfo *pZDTableInfo = &(mCameraDevice->mZDTableInfo);
    EtronDIImageType::Value imageType = depth_raw_type_to_depth_image_type(frame->dataFormat);

    if(EtronDIImageType::DEPTH_14BITS == imageType)    {
        return depth;
    }

    uint16_t zdIndex = (EtronDIImageType::DEPTH_8BITS == imageType) ? depth << 3 : depth;
    uint16_t nDevType = mCameraDevice->mCameraDeviceInfo.devInfo.nDevType;
    if(PUMA != nDevType)    {
        zdIndex = depth;
    }

    if(zdIndex >= pZDTableInfo->nZDTableSize >> 1)    {    // / 2
        zdIndex = (pZDTableInfo->nZDTableSize >> 1) -1;
    }

    int nZValue = pZDTableInfo->nZDTable[zdIndex << 1] << 8 | pZDTableInfo->nZDTable[zdIndex << 1 + 1];

    return nZValue;
}

std::unique_ptr<FrameProducer> createDepthFrameProducer(CameraDevice *cameraDevice)    {
    std::unique_ptr<FrameProducer> producer(new DepthFrameProducer(cameraDevice));
    return std::move(producer);
}

}  // namespace video
}  // namespace libeYs3D
