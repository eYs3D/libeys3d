# Copyright 2020 eYs3D Co., Ltd. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

include(${CMAKE_CURRENT_LIST_DIR}/IncludeGuard.cmake)
cmake_include_guard()

if(JPEG_FIND_QUIET)
  find_package(JPEG QUIET)
else()
  find_package(JPEG REQUIRED)
endif()

if(JPEG_FOUND)

# After cmake version >= 3.12
#message(STATUS "Found JPEG: ${JPEG_VERSION}")

set(WITH_JPEG TRUE)
add_definitions(-DWITH_JPEG)

else()

set(WITH_JPEG FALSE)

endif()

