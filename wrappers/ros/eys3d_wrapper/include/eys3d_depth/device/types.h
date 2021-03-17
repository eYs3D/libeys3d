// Copyright 2020 eYs3D Co., Ltd. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
#ifndef EYS3D_DEVICE_TYPES_H_
#define EYS3D_DEVICE_TYPES_H_
#pragma once

#include <cstdint>
#include <ostream>

#include "eys3d_depth/stubs/global.h"

EYS3D_DEPTH_BEGIN_NAMESPACE

/**
 * @ingroup enumerations
 * @brief List device modes.
 *
 * Control the color & depth streams enabled or not.
 *
 * Note: y: available, n: unavailable, -: depends on StreamMode
 */
enum class DeviceMode : std::int32_t {
  DEVICE_COLOR = 0,  /**< IMAGE_LEFT_COLOR y IMAGE_RIGHT_COLOR - IMAGE_DEPTH n */
  DEVICE_DEPTH = 1,  /**< IMAGE_LEFT_COLOR n IMAGE_RIGHT_COLOR n IMAGE_DEPTH y */
  DEVICE_ALL = 2,    /**< IMAGE_LEFT_COLOR y IMAGE_RIGHT_COLOR - IMAGE_DEPTH y */
};

/**
 * @ingroup enumerations
 * @brief List color modes.
 */
enum class ColorMode : std::int32_t {
  COLOR_RAW       = 0,  /**< color raw */
  COLOR_RECTIFIED = 1,  /**< color rectified */
  COLOR_MODE_LAST
};

/**
 * @ingroup enumerations
 * @brief List depth modes.
 */
enum class DepthMode : std::int32_t {
  DEPTH_RAW      = 0,  /**< ImageFormat::DEPTH_RAW */
  DEPTH_GRAY     = 1,  /**< ImageFormat::DEPTH_GRAY_24 */
  DEPTH_COLORFUL = 2,  /**< ImageFormat::DEPTH_RGB */
  DEPTH_MODE_LAST
};

/**
 * @ingroup enumerations
 * @brief List depth data type.
 */
enum class DepthDataType : std::int32_t {
    DEPTH_DATA_DEFAULT			                                                            = 0, /* raw (depth off, only raw color) */
    DEPTH_DATA_8_BITS				                                    =1, /* rectify, 1 byte per pixel */
    DEPTH_DATA_14_BITS				                            =2, /* rectify, 2 byte per pixel */
    DEPTH_DATA_8_BITS_x80		                                    =3, /* rectify, 2 byte per pixel but using 1 byte only */
    DEPTH_DATA_11_BITS				                            =4, /* rectify, 2 byte per pixel but using 11 bit only */
    DEPTH_DATA_OFF_RECTIFY		                                    =5, /* rectify (depth off, only rectify color) */
    DEPTH_DATA_8_BITS_RAW		                                    =6, /* raw */
    DEPTH_DATA_14_BITS_RAW		                                    =7, /* raw */
    DEPTH_DATA_8_BITS_x80_RAW                               	    =8, /* raw */
    DEPTH_DATA_11_BITS_RAW		                                    =9, /* raw */
    DEPTH_DATA_14_BITS_COMBINED_RECTIFY    = 11, /* multi-baseline */ 
    DEPTH_DATA_11_BITS_COMBINED_RECTIFY    =13, /* multi-baseline */ 
    DEPTH_DATA_LAST
};


/**
 * @ingroup enumerations
 * @brief List stream modes.
 */
enum class StreamMode : std::int32_t
{
  STREAM_640x480 = 0,  /**< 480p, vga, left */
  STREAM_1280x480 = 1, /**< 480p, vga, left+right */
  STREAM_1280x720 = 2, /**< 720p, hd, left */
  STREAM_2560x720 = 3, /**< 720p, hd, left+right */
  STREAM_640x400 = 4,  /**< 8059, 480p, hd, left+right */
  STREAM_CUSTOM = 5,  /**< 8059, 480p, hd, left+right */
  STREAM_MODE_LAST
};

/**
 * @ingroup enumerations
 * @brief List stream formats.
 */
enum class StreamFormat : std::int32_t {
  STREAM_MJPG = 0,
  STREAM_YUYV = 1,
  STREAM_FORMAT_LAST
};

EYS3D_API
std::ostream& operator<<(std::ostream& os, const StreamFormat& code);

/**
 * @ingroup enumerations
 * @brief List image types.
 */
enum class ImageType : std::int32_t {
  /** LEFT Color. */
  IMAGE_LEFT_COLOR,
  /** RIGHT Color. */
  IMAGE_RIGHT_COLOR,
  /** Depth. */
  IMAGE_DEPTH,
  /** All. */
  IMAGE_ALL,
};

EYS3D_API
std::ostream& operator<<(std::ostream& os, const ImageType& code);

/**
 * @ingroup enumerations
 * @brief List image formats.
 */
enum class ImageFormat : std::int32_t {
  IMAGE_BGR_24,   /**< 8UC3 */
  IMAGE_RGB_24,   /**< 8UC3 */
  IMAGE_GRAY_8,   /**< 8UC1 */
  IMAGE_GRAY_16,  /**< 16UC1 */
  IMAGE_GRAY_24,  /**< 8UC3 */
  IMAGE_YUYV,     /**< 8UC2 */
  IMAGE_MJPG,
  // color
  COLOR_BGR   = IMAGE_BGR_24,  // > COLOR_RGB
  COLOR_RGB   = IMAGE_RGB_24,  // > COLOR_BGR
  COLOR_YUYV  = IMAGE_YUYV,    // > COLOR_BGR, COLOR_RGB
  COLOR_MJPG  = IMAGE_MJPG,    // > COLOR_BGR, COLOR_RGB
  // depth
  DEPTH_RAW     = IMAGE_GRAY_16,  // > DEPTH_GRAY
  DEPTH_GRAY    = IMAGE_GRAY_8,
  DEPTH_GRAY_24 = IMAGE_GRAY_24,
  DEPTH_BGR     = IMAGE_BGR_24,   // > DEPTH_RGB
  DEPTH_RGB     = IMAGE_RGB_24,   // > DEPTH_BGR
  /** Last guard. */
  IMAGE_FORMAT_LAST
};

/**
 * @ingroup enumerations
 * @brief SensorType types.
 */
enum class SensorType : std::int32_t {
  SENSOR_TYPE_H22 = 0,
  SENSOR_TYPE_OV7740,
  SENSOR_TYPE_AR0134,
  SENSOR_TYPE_AR0135,
  SENSOR_TYPE_OV9714
};

/**
 * @ingroup enumerations
 * @brief SensorMode modes.
 */
enum class SensorMode : std::int32_t {
  LEFT = 0,
  RIGHT,
  ALL
};

/** Camera calibration. */
struct CameraCalibration {
  union {
    unsigned char uByteArray[1024];/**< union data defined as below struct { }*/
    struct {
      unsigned short  InImgWidth;/**< Input image width(SideBySide image) */
      unsigned short  InImgHeight;/**< Input image height */
      unsigned short  OutImgWidth;/**< Output image width(SideBySide image) */
      unsigned short  OutImgHeight;/**< Output image height */
      int             RECT_ScaleEnable;/**< Rectified image scale */
      int             RECT_CropEnable;/**< Rectified image crop */
      unsigned short  RECT_ScaleWidth;/**< Input image width(Single image) *RECT_Scale_Col_N /RECT_Scale_Col_M */
      unsigned short  RECT_ScaleHeight;/**< Input image height(Single image) *RECT_Scale_Row_N /RECT_Scale_Row_M */
      float      CamMat1[9];/**< Left Camera Matrix
                fx, 0, cx, 0, fy, cy, 0, 0, 1
                fx,fy : focus  ; cx,cy : principle point */
      float      CamDist1[8];/**< Left Camera Distortion Matrix
                k1, k2, p1, p2, k3, k4, k5, k6
                k1~k6 : radial distort ; p1,p2 : tangential distort */
      float      CamMat2[9];/**< Right Camera Matrix
                fx, 0, cx, 0, fy, cy, 0, 0, 1
                fx,fy : focus  ; cx,cy : principle point */
      float      CamDist2[8];/**< Right Camera Distortion Matrix
                k1, k2, p1, p2, k3, k4, k5, k6
                k1~k6 : radial distort ; p1,p2 : tangential distort */
      float      RotaMat[9];/**< Rotation matrix between the left and right camera coordinate systems.
                | [0] [1] [2] |       |Xcr|
                | [3] [4] [5] |   *   |Ycr|            => cr = right camera coordinate
                | [6] [7] [8] |       |Zcr| */
      float      TranMat[3];/**< Translation vector between the coordinate systems of the cameras.
                |[0]|      |Xcr|
                |[1]|   +  |Ycr|               => cr = right camera coordinate
                |[2]|      |Zcr| */
      float      LRotaMat[9];/**< 3x3 rectification transform (rotation matrix) for the left camera.
                | [0] [1] [2] |       |Xcl|
                | [3] [4] [5] |   *   |Ycl|            => cl = left camera coordinate
                | [6] [7] [8] |       |Zcl| */
      float      RRotaMat[9];/**< 3x3 rectification transform (rotation matrix) for the left camera.
                | [0] [1] [2] |       |Xcr|
                | [3] [4] [5] |   *   |Ycr|            => cr = right camera coordinate
                | [6] [7] [8] |       |Zcr| */
      float      NewCamMat1[12];/**< 3x4 projection matrix in the (rectified) coordinate systems for the left camera.
                fx' 0 cx' 0 0 fy' cy' 0 0 0 1 0
                fx',fy' : rectified focus ; cx', cy; : rectified principle point */
      float      NewCamMat2[12];/**< 3x4 projection matrix in the (rectified) coordinate systems for the rightt camera.
                fx' 0 cx' TranMat[0]* 0 fy' cy' 0 0 0 1 0
                fx',fy' : rectified focus ; cx', cy; : rectified principle point */
      unsigned short  RECT_Crop_Row_BG;/**< Rectidied image crop row begin */
      unsigned short  RECT_Crop_Row_ED;/**< Rectidied image crop row end */
      unsigned short  RECT_Crop_Col_BG_L;/**< Rectidied image crop column begin */
      unsigned short  RECT_Crop_Col_ED_L;/**< Rectidied image crop column end */
      unsigned char  RECT_Scale_Col_M;/**< Rectified image scale column factor M */
      unsigned char  RECT_Scale_Col_N;/**< Rectified image scale column factor N
                Rectified image scale column ratio =  Scale_Col_N/ Scale_Col_M */
      unsigned char  RECT_Scale_Row_M;/**< Rectified image scale row factor M */
      unsigned char  RECT_Scale_Row_N;/**< Rectified image scale row factor N */
      float      RECT_AvgErr;/**< Reprojection error */
      unsigned short  nLineBuffers;/**< Linebuffer for Hardware limitation < 60 */
            float ReProjectMat[16];
    };
  };
};

EYS3D_DEPTH_END_NAMESPACE

#endif  // EYS3D_DEVICE_TYPES_H_
