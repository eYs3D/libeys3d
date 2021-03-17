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
#ifndef EYS3D_DEVICE_TYPES_INTERNAL_H_
#define EYS3D_DEVICE_TYPES_INTERNAL_H_
#pragma once

#include "eys3d_depth/stubs/global.h"

EYS3D_DEPTH_BEGIN_NAMESPACE

enum class ControlParams : std::int32_t {
  AUTO_EXPOSURE = 0,
  AUTO_WHITE_BALANCE,
  IR_DEPTH_ONLY,
  IR_INTENSITY,
  GLOBAL_GAIN,
  EXPOSURE_TIME,
  HW_REGISTER,
  FW_REGISTER,
};

EYS3D_DEPTH_END_NAMESPACE

#endif  // EYS3D_DEVICE_TYPES_INTERNAL_H_
