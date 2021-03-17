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

#pragma once
#include <functional>
#include <string>
#include <iostream>
#include <mutex>
#include "eys3d_depth/types.h"

#define MAX_CONFIG_LENGTH 256

EYS3D_DEPTH_BEGIN_NAMESPACE

class EYS3D_API BaseFilter : public std::enable_shared_from_this<BaseFilter> {
 protected:
  BaseFilter();

 public:
  virtual bool ProcessFrame(
      std::shared_ptr<Image> out,
      const std::shared_ptr<Image> in) = 0; // NOLINT
  virtual bool LoadConfig(void* data);
  inline bool TurnOn() {
    return Enable(true);
  }
  inline bool TurnOff() {
    return Enable(false);
  }
  inline bool Enable(bool is_enable) {
    if (_is_enable != is_enable) {
      _is_enable = is_enable;
      return true;
    }
    return false;
  }
  inline bool IsEnable() const {
    return _is_enable;
  }

 private:
  bool _is_enable;
};

EYS3D_DEPTH_END_NAMESPACE
