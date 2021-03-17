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
#include "eys3d_depth/device/stream_info.h"

EYS3D_DEPTH_BEGIN_NAMESPACE

std::ostream& operator<<(std::ostream& os, const StreamInfo& info) {
  os << "index: " << info.index
    << ", width: " << info.width
    << ", height: " << info.height
    << ", format: " << info.format;
  return os;
}
EYS3D_DEPTH_END_NAMESPACE
