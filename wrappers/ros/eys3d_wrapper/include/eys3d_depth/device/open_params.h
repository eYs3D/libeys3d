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
#ifndef EYS3D_DEVICE_OPEN_PARAMS_H_
#define EYS3D_DEVICE_OPEN_PARAMS_H_
#pragma once

#include <string>

#include "eys3d_depth/device/types.h"
#include "eys3d_depth/stubs/global.h"

EYS3D_DEPTH_BEGIN_NAMESPACE

/**
 * Device open parameters.
 */
struct EYS3D_API OpenParams {
  /**
   * Device index.
   */
  std::int32_t dev_index;

  /**
   * Framerate, range [0,60], [0,30](STREAM_2560x720), default 10.
   */
  std::int32_t framerate;

  /**
   * low framerate
   */
  std::int32_t low_framerate;

  /**
   * Device mode, default DEVICE_ALL
   *
   * <ul>
   * <li>DEVICE_COLOR: IMAGE_LEFT_COLOR y IMAGE_RIGHT_COLOR - IMAGE_DEPTH n
   * <li>DEVICE_DEPTH: IMAGE_LEFT_COLOR n IMAGE_RIGHT_COLOR n IMAGE_DEPTH y
   * <li>DEVICE_ALL:   IMAGE_LEFT_COLOR y IMAGE_RIGHT_COLOR - IMAGE_DEPTH y
   * </ul>
   *
   * Could detect image type is enabled after opened through Camera::IsStreamDataEnabled().
   *
   * Note: y: available, n: unavailable, -: depends on #stream_mode
   */
  DeviceMode dev_mode;

  /**
   * Color mode, default COLOR_RAW.
   */
  ColorMode color_mode;

  /**
   * Depth mode, default DEPTH_RAW.
   */
  DepthMode depth_mode;

  /**
   * Stream mode of color & depth, default STREAM_1280x720.
   */
  StreamMode stream_mode;

  /**
   * Color resolution when StreamMode is STREAM_CUSTOM;
   */
  std::int32_t color_width;
  std::int32_t color_height;

  /**
   * Depth resolution when StreamMode is STREAM_CUSTOM;
   */
  std::int32_t depth_width;
  std::int32_t depth_height;

  /**
   * Stream format of color, default STREAM_YUYV.
   */
  StreamFormat color_stream_format;

  /**
   * Stream format of depth, default STREAM_YUYV.
   */
  StreamFormat depth_stream_format;

  /**
    * Depth data type, default DEPTH_DATA_11_BITS.
    */

  DepthDataType depth_data_type;

  /**
   * Auto-exposure, default true.
   */
  bool state_ae;

  /**
   * Auto-white balance, default true.
   */
  bool state_awb;

  /**
   * IR (Infrared), range [0,6], default 0.
   */
  std::uint8_t ir_intensity;

  /**
   * IR Depth Only mode, default false.
   * Note: When frame rate less than 30fps, IR Depth Only will be not available.
   */
  bool ir_depth_only;

  /**
   * Colour depth image, default 5000. [0, 16384]
   */
  float colour_depth_value;

  /** Constructor. */
  OpenParams();
  explicit OpenParams(const std::int32_t& dev_index);

  /** Destructor. */
  ~OpenParams();
};

EYS3D_DEPTH_END_NAMESPACE

#endif  // EYS3D_DEVICE_OPEN_PARAMS_H_
