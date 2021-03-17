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
#ifndef EYS3D_DATA_CHANNELS_H_
#define EYS3D_DATA_CHANNELS_H_
#pragma once

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <functional>
#include <map>
#include <memory>
#include <thread>
#include <vector>

#include "eys3d_depth/stubs/global.h"

#ifdef EYS3D_OS_WIN
#include <conio.h>
#include <io.h>
#else
#include <sys/ioctl.h>
#include <termios.h>
#include <unistd.h>
#endif

#include "eys3d_depth/data/types_internal.h"
#include "eys3d_depth/types_data.h"

EYS3D_DEPTH_BEGIN_NAMESPACE

namespace hid {

class hid_device;

}  // namespace hid

class EYS3D_API Channels {
 public:
  typedef enum FileId {
    FID_DEVICE_DESC = 1,  // device desc
    FID_RESERVE = 2,      // reserve
    FID_IMU_PARAMS = 4,   // imu params
    FID_LAST,
  } file_id_t;

  typedef enum DataId {
    ACCEL = 0,
    GYRO,
    FRAME,
    DISTANCE,
    LOCATION,
    TEMPERATURE,
    ACCEL_AND_GYRO = 10
  } data_id_t;

  using device_desc_t = device::Descriptors;
  using imu_params_t = device::ImuParams;

  using img_packets_t = std::vector<ImgInfoPacket>;
  using imu_packets_t = std::vector<ImuDataPacket>;
  using gps_packets_t = std::vector<GPSDataPacket>;
  using dis_packets_t = std::vector<ObstacleDisPacket>;

  using img_callback_t = std::function<void(const ImgInfoPacket &packet)>;
  using imu_callback_t = std::function<void(const ImuDataPacket &packet)>;
  using gps_callback_t = std::function<void(const GPSDataPacket &packet)>;
  using dis_callback_t = std::function<void(const ObstacleDisPacket &packet)>;

  Channels();
  virtual ~Channels();

  bool IsAvaliable() const;
  bool IsOpened() const;

  void SetImgInfoCallback(img_callback_t callback);
  void SetImuDataCallback(imu_callback_t callback);
  void SetGPSDataCallback(gps_callback_t callback);
  void SetDisDataCallback(dis_callback_t callback);

  bool IsHidAvaliable() const;
  bool IsHidOpened() const;
  bool IsHidTracking() const;

  bool ImuCalibration();

  bool StartHidTracking(device_desc_t *desc);
  bool StopHidTracking();

  bool GetFiles(device_desc_t *desc,
      imu_params_t *imu_params,
      Version *spec_version = nullptr);

  bool SetFiles(device_desc_t *desc,
      imu_params_t *imu_params,
      Version *spec_version);

  bool IsBetaDevice() const;

  bool HidFirmwareUpdate(const char *filepath);

  bool OpenHid();
  void CloseHid();

  inline void EnableImuCorrespondence(bool is_enable) {
    enable_imu_correspondence = is_enable;
  }

 protected:
  void Detect();
  bool Open();
  void Close();

  void DetectHid();

 private:
  bool DoHidTrack(device_desc_t *desc);
  bool DoHidTrack2(device_desc_t *desc);
  bool DoHidDataExtract(device_desc_t *desc, imu_packets_t &imu, img_packets_t &img, gps_packets_t &gps, dis_packets_t &dis);  // NOLINT
  bool DoHidDataExtract2(device_desc_t *desc, imu_packets_t &imu, img_packets_t &img, gps_packets_t &gps, dis_packets_t &dis);  // NOLINT

  bool PullFileData(bool device_info,
      bool reserve,
      bool imu_params,
      std::uint8_t *data,
      std::uint16_t &file_size);  // NOLINT
  bool PushFileData(std::uint8_t *data, std::uint16_t size);

  std::shared_ptr<hid::hid_device> hid_;

  bool is_hid_exist_ = false;
  bool is_hid_opened_ = false;
  bool is_hid_tracking_ = false;
  bool enable_imu_correspondence = false;

  img_callback_t img_callback_;
  imu_callback_t imu_callback_;
  gps_callback_t gps_callback_;
  dis_callback_t dis_callback_;

  std::thread hid_track_thread_;

  std::uint16_t package_sn_ = 0;

  struct stat stat_;
  int req_count_ = 0;
  off_t file_size_;
  std::uint32_t packets_sum_ = 0;
  std::uint32_t packets_index_ = 0;
};

EYS3D_DEPTH_END_NAMESPACE

#endif  // EYS3D_DATA_CHANNELS_H_
