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

#include "video/pc_coders.h"
#include "video/coders.h"
#include "debug.h"
#include "eSPDI.h"
#include "EYS3DSystem.h"
#include "devices/CameraDevice.h"
#include "DMPreview_utility/PlyFilter.h"

#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>

#define LOG_TAG "PC_CODERS"

namespace libeYs3D    {
namespace video    {

static int plyfilter_transform(std::vector<uint8_t> &depthData,
                               std::vector<uint8_t> &colorData,
                               int &nDepthWidth, int &nDepthHeight,
                               int &nColorWidth, int &nColorHeight,
                               std::vector<float> &imgFloatBufOut,
                               eSPCtrl_RectLogData &rectifyLogData,
                               EtronDIImageType::Value depthImageType)    {
    float ratio = (float)rectifyLogData.OutImgHeight / nDepthHeight;
    if (ratio != 1.0f)    {
        int resampleWidthDepth = nDepthWidth * ratio;
        int resampleHeightDepth = nDepthHeight * ratio;

        int bufSize = resampleWidthDepth * resampleHeightDepth * 2;
        std::vector<uint8_t> dArrayResized(bufSize);
        if(depthImageType == EtronDIImageType::DEPTH_8BITS)
            PlyWriter::MonoBilinearFineScaler(&depthData[0], &dArrayResized[0],
                                              nDepthWidth, nDepthHeight,
                                              resampleWidthDepth, resampleHeightDepth,
                                              1);
        else
            PlyWriter::MonoBilinearFineScaler_short((unsigned short*)&depthData[0],
                                                    (unsigned short*)&dArrayResized[0],
                                                    nDepthWidth, nDepthHeight,
                                                    resampleWidthDepth, resampleHeightDepth,
                                                    1);

        depthData.resize(bufSize);
        depthData.assign(dArrayResized.begin(), dArrayResized.end());

        nDepthWidth = resampleWidthDepth;
        nDepthHeight = resampleHeightDepth;
    }

    switch(depthImageType)    {
        case EtronDIImageType::DEPTH_8BITS:    {
            //D8 TO D11 IMAGE +
            std::vector<uint8_t> bufDepthTmpout;
            bufDepthTmpout.resize(depthData.size() * 2);

            uint16_t *pDepthOut = (uint16_t *)bufDepthTmpout.data();

            for( size_t i = 0; i != depthData.size(); i++ )    {
                pDepthOut[i] = ((uint16_t)depthData[i] ) << 3;
            }

            //D8 TO D11 IMAGE -
            PlyFilter::CF_FILTER(bufDepthTmpout, colorData,
                                 nDepthWidth, nDepthHeight,
                                 nColorWidth, nColorHeight,
                                 imgFloatBufOut,
                                 &rectifyLogData);
            break;
        }
        case EtronDIImageType::DEPTH_11BITS:
        {
            PlyFilter::UnavailableDisparityCancellation(depthData, nDepthWidth, nDepthHeight, 16383);
            PlyFilter::CF_FILTER(depthData, colorData,
                                 nDepthWidth, nDepthHeight,
                                 nColorWidth, nColorHeight,
                                 imgFloatBufOut,
                                 &rectifyLogData);
            break;
        }
        case EtronDIImageType::DEPTH_14BITS:    {
            PlyFilter::CF_FILTER_Z14(depthData, colorData,
                                     nDepthWidth, nDepthHeight,
                                     nColorWidth, nColorHeight,
                                     imgFloatBufOut,
                                     &rectifyLogData);
            break;
        }
        default:
            break;
    }

    if(imgFloatBufOut.empty())    {
        return ETronDI_NullPtr;
    }

    return ETronDI_OK;
}

int generate_point_cloud(CameraDevice *cameraDevice, const char *plyFilePath,
                         std::vector<uint8_t> &depthData,
                         int nDepthWidth, int nDepthHeight,
                         std::vector<uint8_t> &colorData,
                         int nColorWidth, int nColorHeight,
                         bool usePlyFilter)    {
    int ret = 0;
    EtronDIImageType::Value depthImageType = depth_raw_type_to_depth_image_type(cameraDevice->mDepthFormat);
    bool bUsePlyFilter = usePlyFilter &&
                         cameraDevice->isPlyFilterSupported() &&
                         cameraDevice->isPlyFilterEnabled();
    uint16_t nZNear, nZFar;

    cameraDevice->getDepthOfField(&nZNear, &nZFar);

    std::vector<float> imgFloatBufOut;
    if(bUsePlyFilter)    {
        ret = plyfilter_transform(depthData, colorData,
                                  nDepthWidth, nDepthHeight,
                                  nColorWidth, nColorHeight,
                                  imgFloatBufOut,
                                  cameraDevice->mRectifyLogData,
                                  depthImageType);
        if(ret != ETronDI_OK)    {
            LOG_ERR(LOG_TAG, "Error plyfilter_transform, ret = %d", ret);
            return ret;
        }
    }

    { // eneratePointCloud(depthData, colorData, ...)
        std::vector<CloudPoint> cloudPoints;
        if(bUsePlyFilter && !imgFloatBufOut.empty())    {
            ret = PlyWriter::etronFrameTo3D_PlyFilterFloat(nDepthWidth, nDepthHeight,
                                                           imgFloatBufOut,
                                                           nColorWidth, nColorHeight,
                                                           colorData,
                                                           &cameraDevice->mRectifyLogData,
                                                           depthImageType,
                                                           cloudPoints,
                                                           true, nZNear, nZFar,
                                                           true, false, 1.0f);
        } else    {
            ret = PlyWriter::etronFrameTo3D(nDepthWidth, nDepthHeight, depthData,
                                            nColorWidth, nColorHeight, colorData,
                                            &cameraDevice->mRectifyLogData,
                                            depthImageType,
                                            cloudPoints,
                                            true, nZNear, nZFar,
                                            true, false, 1.0f);
        }

        if(ret == 0)    {
            LOG_INFO(LOG_TAG, "Successfully generating point cloud: %s", plyFilePath);
            save_ply(plyFilePath, cloudPoints);
        } else    {
            LOG_ERR(LOG_TAG, "Error generating point cloud (ret = %d): %s", ret, plyFilePath);
        }
    }
    
    return ret;
}

#if 0
int save_bitmap(const char *filePath, uint8_t *buffer, int width, int height, int bytePerPixel)    {
    if (!filePath || !buffer) return ETronDI_NullPtr;

    //uint8_t *pMirroBuffer = (uint8_t *)calloc(width * height * bytePerPixel, sizeof(uint8_t));
    //EtronDI_ImgMirro(EtronDIImageType::COLOR_RGB24, width, height, buffer, pMirroBuffer);
    //EtronDI_RGB2BMP((char *)filePath, width, height, pMirroBuffer);
    //free(pMirroBuffer);
    EtronDI_RGB2BMP((char *)filePath, width, height, buffer);

    return ETronDI_OK;
}
#endif

typedef struct tagBITMAPFILEHEADER {
    unsigned short    bfType; // 2  /* Magic identifier */
    unsigned int   bfSize; // 4  /* File size in bytes */
    unsigned short    bfReserved1; // 2
    unsigned short    bfReserved2; // 2
    unsigned int   bfOffBits; // 4 /* Offset to image data, bytes */
} __attribute__((packed)) BITMAPFILEHEADER;

typedef struct tagBITMAPINFOHEADER {
    unsigned int    biSize;     // 4 /* Header size in bytes */
    int             biWidth;    // 4 /* Width of image */
    int             biHeight;   // 4 /* Height of image */
    unsigned short  biPlanes;   // 2 /* Number of colour planes */
    unsigned short  biBitCount; // 2 /* Bits per pixel */
    unsigned int    biCompress; // 4 /* Compression type */
    unsigned int    biSizeImage; // 4 /* Image size in bytes */
    int             biXPelsPerMeter; // 4
    int             biYPelsPerMeter; // 4 /* Pixels per meter */
    unsigned int    biClrUsed; // 4 /* Number of colours */
    unsigned int    biClrImportant; // 4 /* Important colours */
} __attribute__((packed)) BITMAPINFOHEADER;

int save_bitmap(const char *filePath, uint8_t *data, int width, int height)    {
    BITMAPFILEHEADER bmp_head;
    BITMAPINFOHEADER bmp_info;
    int size = width * height * 3;

    bmp_head.bfType = 0x4D42; // 'BM'
    bmp_head.bfSize= size + sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER); // 24 + head + info no quad
    bmp_head.bfReserved1 = bmp_head.bfReserved2 = 0;
    bmp_head.bfOffBits = bmp_head.bfSize - size;
    // finish the initial of head

    bmp_info.biSize = sizeof(BITMAPINFOHEADER);
    bmp_info.biWidth = width;
    bmp_info.biHeight = height;
    bmp_info.biPlanes = 1;
    bmp_info.biBitCount = 24; // bit(s) per pixel, 24 is true color
    bmp_info.biCompress = 0;
    bmp_info.biSizeImage = size;
    bmp_info.biXPelsPerMeter = 0;
    bmp_info.biYPelsPerMeter = 0;
    bmp_info.biClrUsed = 0 ;
    bmp_info.biClrImportant = 0;
    // finish the initial of infohead;

    // copy the data
    FILE *fp;
    if (!(fp = fopen(filePath, "wb"))) return -errno;

    fwrite(&bmp_head, 1, sizeof(BITMAPFILEHEADER), fp);
    fwrite(&bmp_info, 1, sizeof(BITMAPINFOHEADER), fp);
    for (int h = 0 ; h < height ; h++)    {
        for (int w = 0 ; w < width ; ++w)    {
            fwrite(data + ((width * 3) * (height - (h + 1))) + (w * 3) + 2, 1, 1, fp);
            fwrite(data + ((width * 3) * (height - (h + 1))) + (w * 3) + 1, 1, 1, fp);
            fwrite(data + ((width * 3) * (height - (h + 1))) + (w * 3), 1, 1, fp);
        }
    }
    
    fflush(fp);
    fclose(fp);
    
    return 0;
}

int save_yuv(const char *filePath, uint8_t *buffer, int width, int height, int bytePerPixel)    {
    if (!filePath || !buffer) return ETronDI_NullPtr;

    FILE *pFile = fopen(filePath, "wb");
    if (!pFile) return ETronDI_NullPtr;

    fseek(pFile, 0, SEEK_SET);
    fwrite(&buffer[0], sizeof(uint8_t), width * height * bytePerPixel, pFile);
    fclose(pFile);
    
    return ETronDI_OK;
}

int save_ply(const char *filePath, std::vector<CloudPoint> &cloudPoints, bool launchMeshlab)    {
    if (!filePath) return ETronDI_NullPtr;
    if (cloudPoints.empty()) return ETronDI_NullPtr;

    if(0 != PlyWriter::writePly(cloudPoints, filePath))    {
        LOG_ERR(LOG_TAG, "Error writing PLY: %s", filePath);
        return ETronDI_NullPtr;
    }

    if(launchMeshlab)    {
        char buffer[PATH_MAX];
        sprintf(buffer, "meshlab \"%s\" &", filePath);
        system(buffer);
    }
    
    return ETronDI_OK;
}

} // endo of namespace video
} // endo of namespace libeYs3D
