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

#include "debug.h"
#include "EYS3DSystem.h"
#include "video/coders.h"
#include "video/Frame.h"
#include "devices/CameraDevice.h"

//#include "jpeglib.h"
#include <turbojpeg.h>
#include <inttypes.h>


#define LOG_TAG "CODERS"

namespace libeYs3D    {
namespace video    {

static inline void convert_yuv_to_rgb_pixel(int32_t y, int32_t u, int32_t v, 
                                            uint8_t *rgb_pixel)    {
    int32_t r, g, b;

    r = y + (1.370705 * (v-128));
    g = y - (0.698001 * (v-128)) - (0.337633 * (u-128));
    b = y + (1.732446 * (u-128));

    if(r > 255) r = 255;
    if(g > 255) g = 255;
    if(b > 255) b = 255;

    if(r < 0) r = 0;
    if(g < 0) g = 0;
    if(b < 0) b = 0;

    rgb_pixel[0] = r * 220 / 256;
    rgb_pixel[1] = g * 220 / 256;
    rgb_pixel[2] = b * 220 / 256;
}

int convert_yuv_to_rgb_buffer(uint8_t *yuv, uint8_t *rgb,
                              int32_t width, int32_t height,
                              uint64_t *rgb_actual_size)    {
    uint8_t *output = rgb;
    for(int in = 0; in < width * height * 2; in += 4)    {
        convert_yuv_to_rgb_pixel(yuv[in + 0], yuv[in + 1], yuv[in + 3], output);
        output += 3;

        convert_yuv_to_rgb_pixel(yuv[in + 2], yuv[in + 1], yuv[in + 3], output);
        output += 3;
    }
    
    *rgb_actual_size = width * height * 3;

    return 0;
}



static tjhandle tj_handle = NULL;
static inline tjhandle get_tjhandle()    {
    if(tj_handle)    return tj_handle;
    
    tj_handle = tj_handle;

    return tjInitDecompress();
}

int turbo_jpeg_jpeg2yuv(uint8_t* jpeg_buffer, uint64_t jpeg_size, uint8_t *yuv_buffer,
              uint64_t* yuv_actual_size, int32_t* yuv_type)    {
    tjhandle handle = get_tjhandle();
    int width, height, subsample, colorspace;
    int flags = 0;
    int padding = 1;
    int ret = 0;

    tjDecompressHeader3(handle, jpeg_buffer, jpeg_size, &width, &height, &subsample, &colorspace);

    flags |= 0;
    *yuv_type = subsample;
    *yuv_actual_size = tjBufSizeYUV2(width, padding, height, subsample);
    ret = tjDecompressToYUV2(handle, jpeg_buffer, jpeg_size, yuv_buffer, width,
                             padding, height, flags);
    if (ret < 0)    {
        LOG_ERR(LOG_TAG, "tjpeg2yuv failed: %s", tjGetErrorStr());
    }

    return ret;
}

int turbo_jpeg_yuv2rgb(uint8_t *yuv_buffer, uint64_t yuv_size, int width, int height,
                       int sub_sample, uint8_t *rgb_buffer, uint64_t *rgb_actual_size)    {
    tjhandle handle = get_tjhandle();
    int flags = 0;
    int padding = 1;
    int pixelfmt = TJPF_RGB;
    uint64_t need_size;
    int ret = 0;

    flags |= 0;

    need_size = tjBufSizeYUV2(width, padding, height, sub_sample);
    if(need_size != yuv_size)    {
        LOG_ERR(LOG_TAG, "Expecting yuv size: %" PRIu64 ", but input size: %" PRIu64 "",
                need_size, yuv_size);
        return -1;
    }

    *rgb_actual_size = width * height * tjPixelSize[pixelfmt];
    ret = tjDecodeYUV(handle, yuv_buffer, padding, sub_sample, rgb_buffer, width, 0, height, pixelfmt, flags);
    if(ret < 0)    {
        LOG_ERR(LOG_TAG, "tyuv2rgb failed: %s", tjGetErrorStr());
    }

    return ret;
}

static void UpdateD8bitsDisplayImage_DIB24(const RGBQUAD* pClr,
                                           uint16_t deviceType,
                                           const uint8_t *zdTable,
                                           const uint8_t *pDepth,
                                           int32_t width, int32_t height,
                                           uint16_t *pZDDepth, uint64_t *actualZDDepthSize,
                                           uint8_t *pRGB, uint64_t *actualRGBSize)    {
    int32_t nBPS = width * 3;;
    uint8_t *pDL    = pRGB;
    uint8_t *pD     = NULL;
    unsigned short z = 0;
    unsigned short zdIndex;

    if((width <= 0) || (height <= 0)) return;

    for(int y = 0; y < height; y++) {
        pD = pDL;
        for(int x = 0; x < width; x++) {
            int pixelIndex = y * width + x;
            unsigned short depth = (unsigned short)pDepth[pixelIndex];

            if(deviceType == PUMA)
                zdIndex = (depth << 3) * sizeof(unsigned short);
            else
                zdIndex = depth * sizeof(unsigned short);

            z = (((unsigned short)zdTable[zdIndex]) << 8) + zdTable[zdIndex + 1];
            
            *pZDDepth++ = z; // store ZD depth
            
            if(z >= COLOR_PALETTE_MAX_COUNT) {
                pD += 3;
                continue;
            }

            pD[0] = pClr[z].rgbRed;
            pD[1] = pClr[z].rgbGreen;
            pD[2] = pClr[z].rgbBlue;

            pD += 3;
        }
        
        pDL += nBPS;
    }

    *actualZDDepthSize = width * height << 1;
    *actualRGBSize = nBPS * height;
}

static void UpdateD11DisplayImage_DIB24(const RGBQUAD* colorPalette,
                                        uint16_t deviceType,
                                        const uint8_t *zdTable,
                                        const uint8_t *pDepth,
                                        int width, int height,
                                        uint16_t *pZDDepth, uint64_t *actualZDDepthSize,
                                        uint8_t *pRGB, uint64_t *actualRGBSize)    {
    if (width <=0 || height <= 0 ) return;

    int nBPS = ((width * 3 + 3) / 4) * 4;
    uint8_t *pDL    = pRGB;
    uint8_t *pD     = NULL;
    const RGBQUAD* pClr = NULL;
    uint16_t z = 0;

    uint16_t depthValueLimit11Bit = ((1 << 12) - 1);
    for(int y = 0; y < height; y++)    {
        pD = pDL;
        for(int x = 0; x < width; x++)    {
            int pixelIndex = (y * width + x) * sizeof(uint16_t);
            uint16_t depth = (((uint16_t)pDepth[pixelIndex + 1]) << 8) |
                             ((uint16_t)pDepth[pixelIndex]);
            if(depth > depthValueLimit11Bit)    {
                pD += 3;
                continue;
            }

            unsigned short zdIndex = depth * sizeof(uint16_t);
            z = (((uint16_t)zdTable[zdIndex]) << 8) + ((uint16_t)zdTable[zdIndex + 1]);
            
            *pZDDepth++ = z; // store ZD depth
            
            if(z >= COLOR_PALETTE_MAX_COUNT) {
                pD += 3;
                continue;
            }

            pClr = &(colorPalette[z]);
            pD[0] = pClr->rgbRed;
            pD[1] = pClr->rgbGreen;
            pD[2] = pClr->rgbBlue;
            pD += 3;
        }
        
        pDL += nBPS;
    }
    
    *actualZDDepthSize = width * height << 1;
    *actualRGBSize = nBPS * height;
}

static void UpdateZ14DisplayImage_DIB24(const RGBQUAD* pClr,
                                        uint16_t deviceType,
                                        const uint8_t *zdTable,
                                        const uint8_t *pDepthZ14,
                                        int width, int height,
                                        uint16_t *pZDDepth, uint64_t *actualZDDepthSize,
                                        uint8_t *pDepthDIB24, uint64_t *actualRGBSize)    {
    int x, y, nBPS;
    unsigned short *pWSL, *pWS;
    unsigned char *pDL,*pD;
    
    //
    if((width <= 0) || (height <= 0))    return;
    //
    
    nBPS = width * 3;
    pWSL = (unsigned short *)pDepthZ14;
    pDL = pDepthDIB24;
    for(y = 0; y < height; y++)    {
        pWS = pWSL;
        pD = pDL;
        for(x = 0; x < width; x++)     {
        
            *pZDDepth++ = pWS[x]; // store ZD depth
        
            if(pWS[x] >= COLOR_PALETTE_MAX_COUNT) {
                pD += 3;
                continue;
            }

            pD[0] = pClr[pWS[x]].rgbRed;
            pD[1] = pClr[pWS[x]].rgbGreen;
            pD[2] = pClr[pWS[x]].rgbBlue;
            pD += 3;
        }
        
        pWSL += width;
        pDL += nBPS;
    }
    
    *actualZDDepthSize = width * height << 1;
    *actualRGBSize = nBPS * height;
}

int color_image_produce_rgb_frame(const CameraDevice *cameraDevice, Frame *frame)    {
    int ret = 0;
    
    if(frame->dataFormat == EtronDIImageType::COLOR_YUY2)   { //YUV to RGB
#ifdef OPENCL_SUPPORTED
        frame->actualRGBBufferSize = frame->width * frame->height * 3;
        ret = EtronDI_ColorFormat_to_RGB24(EYS3DSystem::getEYS3DSystem()->getEYS3DDIHandle(),
                                           (DEVSELINFO *)(&(cameraDevice->mDevSelInfo)),
                                           frame->rgbVec.data(), frame->dataVec.data(),
                                           frame->actualDataBufferSize,
                                           frame->width, frame->height,
                                           EtronDIImageType::COLOR_YUY2);
        if(ret != 0)
            LOG_DEBUG(LOG_TAG, "EtronDI_ColorFormat_to_RGB24 failed(%d)...", ret);
#else
        convert_yuv_to_rgb_buffer(frame->dataVec.data(),
                                  frame->rgbVec.data(),
                                  frame->width, frame->height,
                                  &(frame->actualRGBBufferSize)); 
#endif
    } else    { // MJPEG to RGB, mColorFormat == EtronDIImageType::COLOR_MJPG
        frame->actualRGBBufferSize = frame->width * frame->height * 3;
        ret = tjDecompress2(get_tjhandle(),
                            frame->dataVec.data(), frame->actualDataBufferSize,
                            frame->rgbVec.data(), frame->width, 0, frame->height,
                            TJPF_RGB,
                            TJFLAG_FASTDCT | TJFLAG_FASTUPSAMPLE);
        if (ret != 0)    {
            LOG_ERR(LOG_TAG, "tjDecompress2 failed: %s", tjGetErrorStr());
            ret = ETronDI_VERIFY_DATA_FAIL;
        }
    }
    
    return ret;
}


// source: DMPreview/model/CImageDataModel.cpp:: int CImageDataModel_Depth::TransformRawToRGB()
int depth_image_produce_rgb_frame(const CameraDevice *cameraDevice, Frame *frame)    {
    const RGBQUAD *pColorPalette = nullptr;

    switch (cameraDevice->mDepthDataTransferCtrl){
        case DEPTH_IMG_COLORFUL_TRANSFER:
            pColorPalette = cameraDevice->mColorPaletteZ14;
            break;
        case DEPTH_IMG_GRAY_TRANSFER:
            pColorPalette = cameraDevice->mGrayPaletteZ14;
            break;
        default: // DEPTH_IMG_NON_TRANSFER
            convert_yuv_to_rgb_buffer(frame->dataVec.data(), frame->rgbVec.data(),
                                      frame->width, frame->height,
                                      &(frame->actualRGBBufferSize));

            return ETronDI_OK;
    }

    if(depth_raw_type_to_depth_image_type(frame->dataFormat) ==
           EtronDIImageType::DEPTH_8BITS)    {
        UpdateD8bitsDisplayImage_DIB24(pColorPalette,
                                       cameraDevice->mCameraDeviceInfo.devInfo.nDevType,
                                       cameraDevice->mZDTableInfo.nZDTable,
                                       frame->dataVec.data(),
                                       frame->width, frame->height,
                                       frame->zdDepthVec.data(), &(frame->actualZDDepthBufferSize),
                                       frame->rgbVec.data(), &(frame->actualRGBBufferSize));
    } else if(depth_raw_type_to_depth_image_type(frame->dataFormat) ==
                  EtronDIImageType::DEPTH_11BITS)    {
        UpdateD11DisplayImage_DIB24(pColorPalette,
                                    cameraDevice->mCameraDeviceInfo.devInfo.nDevType,
                                    cameraDevice->mZDTableInfo.nZDTable,
                                    frame->dataVec.data(),
                                    frame->width, frame->height,
                                    frame->zdDepthVec.data(), &(frame->actualZDDepthBufferSize),
                                    frame->rgbVec.data(), &(frame->actualRGBBufferSize));
    } else if(depth_raw_type_to_depth_image_type(frame->dataFormat) ==
                  EtronDIImageType::DEPTH_14BITS)    {
        UpdateZ14DisplayImage_DIB24(pColorPalette,
                                    cameraDevice->mCameraDeviceInfo.devInfo.nDevType,
                                    cameraDevice->mZDTableInfo.nZDTable,
                                    frame->dataVec.data(),
                                    frame->width, frame->height,
                                    frame->zdDepthVec.data(), &(frame->actualZDDepthBufferSize),
                                    frame->rgbVec.data(), &(frame->actualRGBBufferSize));
    } else    {
        return ETronDI_NotSupport;
    }
    
    return ETronDI_OK;
}

} // endo of namespace video
} // endo of namespace libeYs3D
