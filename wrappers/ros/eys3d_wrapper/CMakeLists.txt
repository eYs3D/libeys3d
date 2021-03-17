cmake_minimum_required(VERSION 3.0.2)

project(eys3d_depth VERSION 1.0.0.2 LANGUAGES C CXX)

message("CMAKE_SOURCE_DIR= ${CMAKE_SOURCE_DIR}")
message("CMAKE_BINARY_DIR= ${CMAKE_BINARY_DIR}")
message("PROJECT_SOURCE_DIR= ${PROJECT_SOURCE_DIR}")
message("PROJECT_BINARY_DIR= ${PROJECT_BINARY_DIR}")
message("CMAKE_CURRENT_SOURCE_DIR= ${CMAKE_CURRENT_SOURCE_DIR}")
message("CMAKE_CURRENT_BINARY_DIR= ${CMAKE_CURRENT_BINARY_DIR}")

include(cmake/Common.cmake)

if(NOT CMAKE_DEBUG_POSTFIX)
  set(CMAKE_DEBUG_POSTFIX d)
endif()

# config
set(EYS3D_DEPTH_NAME "${PROJECT_NAME}")
set(EYS3D_DEPTH_ROOT "${PROJECT_SOURCE_DIR}")

if(NOT EYS3D_DEPTH_NAMESPACE)
  set(EYS3D_DEPTH_NAMESPACE "${EYS3D_DEPTH_NAME}")
endif()

configure_file(
  include/eys3d_depth/stubs/global_config.h.in
  include/eys3d_depth/stubs/global_config.h @ONLY
)

# options
option(DEBUG "Enable Debug Log" OFF)
option(TIMECOST "Enable Time Cost" OFF)

add_definitions(-DLOG_TAG=eYs3D)

if(DEBUG)
  add_definitions(-DDEBUG)
  message(STATUS "Using macro DEBUG")
endif()
if(TIMECOST)
  add_definitions(-DTIME_COST)
  message(STATUS "Using macro TIME_COST")
endif()

# flags
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -O3")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -O3")

include(cmake/DetectCXX11.cmake)

string(STRIP "${CMAKE_C_FLAGS}" CMAKE_C_FLAGS)
string(STRIP "${CMAKE_CXX_FLAGS}" CMAKE_CXX_FLAGS)
message(STATUS "C_FLAGS: ${CMAKE_C_FLAGS}")
message(STATUS "CXX_FLAGS: ${CMAKE_CXX_FLAGS}")

# packages
LIST(APPEND CMAKE_MODULE_PATH ${EYS3D_DEPTH_ROOT}/cmake)

set(OpenCV_FIND_QUIET TRUE)
include(${EYS3D_DEPTH_ROOT}/cmake/DetectOpenCV.cmake)

set(JPEG_FIND_QUIET TRUE)
include(${EYS3D_DEPTH_ROOT}/cmake/DetectJPEG.cmake)

find_package(eSPDI REQUIRED)

# rpath
set(CMAKE_INSTALL_PREFIX "${CMAKE_SOURCE_DIR}/../eys3d_ros/eys3d_ros_ws/src/dm_preview/eYs3D")
message("CMAKE_INSTALL_PREFIX= ${CMAKE_INSTALL_PREFIX}")

set(EYS3D_DEPTH_CMAKE_RPATH "${CMAKE_INSTALL_PREFIX}/lib/eYs3D")
if(WITH_OPENCV)
  message(STATUS "OpenCV_LIB_PATH: ${OpenCV_LIB_PATH}")
  list(APPEND EYS3D_DEPTH_CMAKE_RPATH ${OpenCV_LIB_PATH})
endif()
set(CMAKE_INSTALL_RPATH "${EYS3D_DEPTH_CMAKE_RPATH}")

# targets
set(OUTPUT_DIR "${EYS3D_DEPTH_ROOT}/_lib_output")

set_outdir(
  ARCHIVE ${OUTPUT_DIR}/lib
  LIBRARY ${OUTPUT_DIR}/lib
  RUNTIME ${OUTPUT_DIR}/bin
)

if(WITH_OPENCV)
  include_directories(
    ${OpenCV_INCLUDE_DIRS}
  )
endif()
if(WITH_JPEG)
  include_directories(
    ${JPEG_INCLUDE_DIR}
  )
endif()
include_directories(
  ${eSPDI_INCLUDE_DIRS}
  ${EYS3D_DEPTH_ROOT}/include
  ${CMAKE_CURRENT_BINARY_DIR}/include
)

add_definitions(-DEYS3D_DEPTH_EXPORTS)

## eys3d_depth_ros source code
set(EYS3D_DEPTH_ROS eys3d_depth)

set(EYS3D_DEPTH_ROS_SRCS
  src/eys3d_depth/data/channels.cpp
  src/eys3d_depth/device/colorizer_p.cpp
  src/eys3d_depth/device/convertor.cpp
  src/eys3d_depth/device/data_caches.cpp
  src/eys3d_depth/device/device_info.cpp
  src/eys3d_depth/device/device.cpp
  src/eys3d_depth/device/image.cpp
  src/eys3d_depth/device/open_params.cpp
  src/eys3d_depth/device/stream_info.cpp
  src/eys3d_depth/device/types.cpp
  src/eys3d_depth/stubs/types_calib.cpp
  src/eys3d_depth/util/rate.cpp
  src/eys3d_depth/util/strings.cpp
  src/eys3d_depth/camera.cpp
  src/eys3d_depth/types_data.cpp
  src/eys3d_depth/utils.cpp
  src/eys3d_depth/internal/camera_p.cpp
  src/eys3d_depth/internal/image_utils.cpp
  src/eys3d_depth/internal/streams.cpp
  src/eys3d_depth/internal/match.cpp
  src/eys3d_depth/internal/motions.cpp
  src/eys3d_depth/internal/location.cpp
  src/eys3d_depth/internal/distance.cpp
  src/eys3d_depth/filter/base_filter.cpp
  src/eys3d_depth/filter/filter_spigot.cpp
  src/eys3d_depth/filter/spatial_filter.cpp
  src/eys3d_depth/filter/temporal_filter.cpp
  src/eys3d_depth/data/hid_linux.cpp
  src/eys3d_depth/device/device_linux.cpp
  src/eys3d_depth/device/color_palette_generator.cpp
  src/eys3d_depth/device/colorizer_linux.cpp
  src/eys3d_depth/device/RegisterSettings.cpp
)

set(EYS3D_DEPTH_ROS_LINK_LIBS ${eSPDI_LIBS} "usb" -lpthread)

if(WITH_OPENCV)
  #list(APPEND EYS3D_DEPTH_ROS_LINK_LIBS ${OpenCV_LIBS})
endif()

if(WITH_JPEG)
  list(APPEND EYS3D_DEPTH_ROS_LINK_LIBS ${JPEG_LIBRARIES})
endif()

make_shared_library(${EYS3D_DEPTH_ROS}
  SRCS ${EYS3D_DEPTH_ROS_SRCS}
  LINK_LIBS ${EYS3D_DEPTH_ROS_LINK_LIBS}
)

target_include_directories(${EYS3D_DEPTH_ROS} PUBLIC
  "$<BUILD_INTERFACE:${EYS3D_DEPTH_ROOT}/src>"
  "$<INSTALL_INTERFACE:include>"
)

set_target_properties(${EYS3D_DEPTH_ROS} PROPERTIES
  VERSION ${PROJECT_VERSION}
  SOVERSION ${PROJECT_VERSION_MAJOR}
)

# copy eYs3D libraries
set(__lib_out_dir ${OUTPUT_DIR}/lib/eYs3D)

set(__expr "*.so*")

if(eSPDI_LIBRARY)
  set(__espdi_libs "")
  set(__espdi_symlinks "")
  list(APPEND eSPDI_LIBS ${eSPDI_3PARTY_LIBS})
  foreach(__espdi_lib ${eSPDI_LIBS})
    string(REGEX MATCH "^${EYS3D_DEPTH_ROOT}/*" __match "${__espdi_lib}")
    if(__match)
      string(REGEX REPLACE "\\.so" "${__expr}" __espdi_lib ${__espdi_lib})
      file(GLOB __espdi_lib2 "${__espdi_lib}")

      foreach(__espdi_lib ${__espdi_lib2})
        if(IS_SYMLINK "${__espdi_lib}")
          list(APPEND __espdi_symlinks ${__espdi_lib})
        else()
          list(APPEND __espdi_libs ${__espdi_lib})
        endif()
      endforeach()
    endif()
  endforeach()

  # Copy libs before symlinks
  list(APPEND __espdi_libs ${__espdi_symlinks})

  message(STATUS "Copy eSPDI_LIBS to ${__lib_out_dir}")
  foreach(__espdi_lib ${__espdi_libs})
    get_filename_component(__espdi_name ${__espdi_lib} NAME)
    if(EXISTS "${__lib_out_dir}/${__espdi_name}")
      message(STATUS "  Skip ${__espdi_lib}")
    else()
      message(STATUS "  Copy ${__espdi_lib}")
      file(COPY ${__espdi_lib} DESTINATION ${__lib_out_dir})
    endif()
  endforeach()
endif()

# platforms/linux/sdk.cfg
#set(SDK_OpenCV_LIB_DIR ${OpenCV_LIB_PATH})

# install

set(EYS3D_DEPTH_CMAKE_INCLUDE_DIR "${CMAKE_INSTALL_PREFIX}/include/${EYS3D_DEPTH_NAME}")
set(EYS3D_DEPTH_CMAKE_BINDIR "${CMAKE_INSTALL_PREFIX}/bin")
set(EYS3D_DEPTH_CMAKE_LIBDIR "${CMAKE_INSTALL_PREFIX}/lib")
set(EYS3D_DEPTH_CMAKE_INSTALLDIR "${EYS3D_DEPTH_CMAKE_LIBDIR}/cmake/${EYS3D_DEPTH_NAME}")

install(DIRECTORY
    "${EYS3D_DEPTH_ROOT}/include"
    "${CMAKE_CURRENT_BINARY_DIR}/include"
  DESTINATION ${CMAKE_INSTALL_PREFIX}
  FILES_MATCHING PATTERN "*.h" PATTERN "*.hpp"
)

set(__lib_dst_dir ${EYS3D_DEPTH_CMAKE_LIBDIR})

install(DIRECTORY "${__lib_out_dir}"
  DESTINATION "${__lib_dst_dir}"
  FILES_MATCHING PATTERN "*.so*" PATTERN "*.dll" PATTERN "*.dylib"
)

install(TARGETS ${EYS3D_DEPTH_ROS}
  EXPORT eys3d_depth-targets
  ARCHIVE DESTINATION ${EYS3D_DEPTH_CMAKE_LIBDIR}
  LIBRARY DESTINATION ${EYS3D_DEPTH_CMAKE_LIBDIR}
  RUNTIME DESTINATION ${EYS3D_DEPTH_CMAKE_BINDIR}
)

## cmake files
include(CMakePackageConfigHelpers)

configure_package_config_file(eys3d_depth-config.cmake.in
  ${CMAKE_CURRENT_BINARY_DIR}/eys3d_depth-config.cmake
  INSTALL_DESTINATION ${EYS3D_DEPTH_CMAKE_INSTALLDIR}
  NO_CHECK_REQUIRED_COMPONENTS_MACRO
)

write_basic_package_version_file(eys3d_depth-config-version.cmake
  VERSION ${PROJECT_VERSION} COMPATIBILITY SameMajorVersion
)

#install(FILES
#  ${CMAKE_CURRENT_BINARY_DIR}/eys3d_depth-config.cmake
#  ${CMAKE_CURRENT_BINARY_DIR}/eys3d_depth-config-version.cmake
#  DESTINATION ${EYS3D_DEPTH_CMAKE_INSTALLDIR}
#)

#install(EXPORT eys3d_depth-targets
#  DESTINATION ${EYS3D_DEPTH_CMAKE_INSTALLDIR}
#)

# summary

include(${EYS3D_DEPTH_ROOT}/cmake/Summary.cmake)
