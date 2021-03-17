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
#ifndef EYS3D_DEVICE_COLORIZER_P_H_
#define EYS3D_DEVICE_COLORIZER_P_H_
#pragma once

#include "eys3d_depth/device/colorizer.h"

EYS3D_DEPTH_BEGIN_NAMESPACE

struct CameraCalibration;

class ColorizerPrivate : public Colorizer {
 public:
  ColorizerPrivate() = default;
  virtual ~ColorizerPrivate() = default;

  virtual void Init(float z14_far,
      bool is_8bits,
      std::shared_ptr<CameraCalibration> calib_params);

  void CachedDepthBuffer(const Image::pointer& depth_buf);

  // process depth raw to other format by user
  Image::pointer Process(const Image::pointer& depth_raw,
        const ImageFormat& out_format) override;

  // process depth buf to expected depth data
  virtual Image::pointer Process(const Image::pointer& depth_buf,
        const DepthMode& depth_mode, bool from_user) = 0;

 protected:
  void ComputeZDTable(std::shared_ptr<CameraCalibration> calib_params);
  void AdaptU2Raw(unsigned char *src, unsigned char *dst,
      int width, int height);

  bool is_8bits_;
  std::shared_ptr<CameraCalibration> calib_params_;

  std::uint16_t ZD_table_[256];

  Image::pointer depth_buf_cached_;
};

EYS3D_DEPTH_END_NAMESPACE

#endif  // EYS3D_DEVICE_COLORIZER_P_H_
