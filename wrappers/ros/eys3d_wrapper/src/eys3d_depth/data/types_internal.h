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
#ifndef EYS3D_DATA_TYPES_INTERNAL_H_
#define EYS3D_DATA_TYPES_INTERNAL_H_
#pragma once

#include <string.h>
#include <cstdint>
#include <vector>
#include <iostream>
#include <map>

#include "eys3d_depth/stubs/global.h"

EYS3D_DEPTH_BEGIN_NAMESPACE

/**
 * @ingroup datatypes
 * Image info packet.
 */
#pragma pack(push, 1)
struct ImgInfoPacket {
  std::uint16_t frame_id;
  std::uint32_t timestamp;
  std::uint16_t exposure_time;

  ImgInfoPacket() = default;
  explicit ImgInfoPacket(std::uint8_t *data) {
    from_data(data);
  }

  void from_data(std::uint8_t *data) {
    timestamp = (*(data + 2)) | (*(data + 3) << 8) | (*(data + 4) << 16) |
                (*(data + 5) << 24);
    frame_id = (*(data + 6)) | (*(data + 7) << 8);
    exposure_time = (*(data + 8)) | (*(data + 9) << 8);
  }
};
#pragma pack(pop)

/**
 * @ingroup datatypes
 * Imu data packet.
 */
#pragma pack(push, 1)
struct ImuDataPacket {
#if defined(EYS3D_IMU_DEVICE)
private:
  float MAX_G = 4.0f;
  float MAX_DPS = 500.0f;
  float BASE_uT = 0.15f;

#define PARSE1Byte(buf) (*buf)
#define PARSE2Bytes(buf) (buf[0] + (buf[1] << 8))
#define PARSE3Bytes(buf) (buf[0] + (buf[1] << 8) + (buf[2] << 16))
#define PARSE4Bytes(buf) (buf[0] + (buf[1] << 8) + (buf[2] << 16) + (buf[3] << 24))

  int parseBuff(unsigned char *&buf, size_t size)
  {
    unsigned char *parseBuf = buf;
    if (size <= 4)
    {
      buf += size;
    }

    if (size == 1)
      return PARSE1Byte(parseBuf);
    if (size == 2)
      return PARSE2Bytes(parseBuf);
    if (size == 3)
      return PARSE3Bytes(parseBuf);
    if (size == 4)
      return PARSE4Bytes(parseBuf);
    return 0;
  }

  float parseBuff_Float(unsigned char *&buf, size_t size)
  {
    unsigned char *parseBuf = buf;
    if (size <= 4)
    {
      buf += size;
    }

    if (size == 1)
      return PARSE1Byte(parseBuf);
    if (size == 2)
      return PARSE2Bytes(parseBuf);
    if (size == 3)
      return PARSE3Bytes(parseBuf);
    if (size == 4)
    {
      int data = PARSE4Bytes(parseBuf);
      return *((float *)&data);
    }
    return 0;
  }

public:
  enum IMUPacketSymbol
  {
    FRAME_COUNT,
    SUB_SECOND,
    SEC,
    MIN,
    HOUR,
    ACCEL_X,
    ACCEL_Y,
    ACCEL_Z,
    TEMPARATURE,
    GYROSCOPE_X,
    GYROSCOPE_Y,
    GYROSCOPE_Z,
    COMPASS_X,
    COMPASS_Y,
    COMPASS_Z,
    COMPASS_X_TBC,
    COMPASS_Y_TBC,
    COMPASS_Z_TBC,
    QUATERNION_0,
    QUATERNION_1,
    QUATERNION_2,
    QUATERNION_3,
    ACCURACY_FLAG
  };

  std::map<IMUPacketSymbol, size_t> sizeTable;
  std::map<IMUPacketSymbol, size_t> sizeTable_DMP;
  std::map<IMUPacketSymbol, size_t> sizeTable_Quaternion;
  ImuDataPacket(){
    sizeTable = {
        {FRAME_COUNT, 2},
        {SUB_SECOND, 2},
        {SEC, 1},
        {MIN, 1},
        {HOUR, 1},
        {ACCEL_X, 2},
        {ACCEL_Y, 2},
        {ACCEL_Z, 2},
        {TEMPARATURE, 2},
        {GYROSCOPE_X, 2},
        {GYROSCOPE_Y, 2},
        {GYROSCOPE_Z, 2},
        {COMPASS_X, 2},
        {COMPASS_Y, 2},
        {COMPASS_Z, 2}};

    sizeTable_DMP = {
        {FRAME_COUNT, 2},
        {SUB_SECOND, 2},
        {SEC, 1},
        {MIN, 1},
        {HOUR, 1},
        {ACCEL_X, 4},
        {ACCEL_Y, 4},
        {ACCEL_Z, 4},
        {TEMPARATURE, 2},
        {GYROSCOPE_X, 4},
        {GYROSCOPE_Y, 4},
        {GYROSCOPE_Z, 4},
        {COMPASS_X, 4},
        {COMPASS_Y, 4},
        {COMPASS_Z, 4},
        {COMPASS_X_TBC, 4},
        {COMPASS_Y_TBC, 4},
        {COMPASS_Z_TBC, 4},
        {ACCURACY_FLAG, 1}};

    sizeTable_Quaternion = {
        {FRAME_COUNT, 2},
        {SUB_SECOND, 2},
        {SEC, 1},
        {MIN, 1},
        {HOUR, 1},
        {QUATERNION_0, 4},
        {QUATERNION_1, 4},
        {QUATERNION_2, 4},
        {QUATERNION_3, 4},
        {ACCURACY_FLAG, 1}};
  }
  ~ImuDataPacket() = default;

  void parsePacket(unsigned char *buf, bool normalization)
  {
    _frameCount = parseBuff(buf, sizeTable[FRAME_COUNT]);
    _subSecond = parseBuff(buf, sizeTable[SUB_SECOND]);
    _sec = parseBuff(buf, sizeTable[SEC]);
    _min = parseBuff(buf, sizeTable[MIN]);
    _hour = parseBuff(buf, sizeTable[HOUR]);
    signed short accelX = parseBuff(buf, sizeTable[ACCEL_X]);
    signed short accelY = parseBuff(buf, sizeTable[ACCEL_Y]);
    signed short accelZ = parseBuff(buf, sizeTable[ACCEL_Z]);
    _temprature = parseBuff(buf, sizeTable[TEMPARATURE]);
    signed short gyroScopeX = parseBuff(buf, sizeTable[GYROSCOPE_X]);
    signed short gyroScopeY = parseBuff(buf, sizeTable[GYROSCOPE_Y]);
    signed short gyroScopeZ = parseBuff(buf, sizeTable[GYROSCOPE_Z]);
    signed short compassX = parseBuff(buf, sizeTable[COMPASS_X]);
    signed short compassY = parseBuff(buf, sizeTable[COMPASS_Y]);
    signed short compassZ = parseBuff(buf, sizeTable[COMPASS_Z]);

    if (normalization)
    {
      int max = (1 << 16) / 2; //+32786 ~ -32786

      _accelX = ((float)accelX / max) * MAX_G;
      _accelY = ((float)accelY / max) * MAX_G;
      _accelZ = ((float)accelZ / max) * MAX_G;

      _gyroScopeX = ((float)gyroScopeX / max) * MAX_DPS;
      _gyroScopeY = ((float)gyroScopeY / max) * MAX_DPS;
      _gyroScopeZ = ((float)gyroScopeZ / max) * MAX_DPS;

      _compassX = compassX * BASE_uT;
      _compassY = compassY * BASE_uT;
      _compassZ = compassZ * BASE_uT;
    }
    else
    {
      _accelX = accelX;
      _accelY = accelY;
      _accelZ = accelZ;

      _gyroScopeX = gyroScopeX;
      _gyroScopeY = gyroScopeX;
      _gyroScopeZ = gyroScopeZ;

      _compassX = compassX;
      _compassY = compassY;
      _compassZ = compassZ;
    }
  }

  void parsePacket_DMP(unsigned char *buf)
  {
    _frameCount = parseBuff(buf, sizeTable_DMP[FRAME_COUNT]);
    _subSecond = parseBuff(buf, sizeTable_DMP[SUB_SECOND]);
    _sec = parseBuff(buf, sizeTable_DMP[SEC]);
    _min = parseBuff(buf, sizeTable_DMP[MIN]);
    _hour = parseBuff(buf, sizeTable_DMP[HOUR]);
    _accelX = parseBuff(buf, sizeTable_DMP[ACCEL_X]);
    _accelY = parseBuff(buf, sizeTable_DMP[ACCEL_Y]);
    _accelZ = parseBuff(buf, sizeTable_DMP[ACCEL_Z]);
    _temprature = parseBuff(buf, sizeTable_DMP[TEMPARATURE]);
    _gyroScopeX = parseBuff(buf, sizeTable_DMP[GYROSCOPE_X]);
    _gyroScopeY = parseBuff(buf, sizeTable_DMP[GYROSCOPE_Y]);
    _gyroScopeZ = parseBuff(buf, sizeTable_DMP[GYROSCOPE_Z]);
    _compassX = parseBuff(buf, sizeTable_DMP[COMPASS_X]);
    _compassY = parseBuff(buf, sizeTable_DMP[COMPASS_Y]);
    _compassZ = parseBuff(buf, sizeTable_DMP[COMPASS_Z]);
    _compassX_TBC = parseBuff(buf, sizeTable_DMP[COMPASS_X_TBC]);
    _compassY_TBC = parseBuff(buf, sizeTable_DMP[COMPASS_Y_TBC]);
    _compassZ_TBC = parseBuff(buf, sizeTable_DMP[COMPASS_Z_TBC]);
    _accuracy_FLAG = parseBuff(buf, sizeTable_DMP[ACCURACY_FLAG]);
  }

  void parsePacket_Quaternion(unsigned char *buf)
  {
    _frameCount = parseBuff(buf, sizeTable_Quaternion[FRAME_COUNT]);
    _subSecond = parseBuff(buf, sizeTable_Quaternion[SUB_SECOND]);
    _sec = parseBuff(buf, sizeTable_Quaternion[SEC]);
    _min = parseBuff(buf, sizeTable_Quaternion[MIN]);
    _hour = parseBuff(buf, sizeTable_Quaternion[HOUR]);
    _quaternion[0] = parseBuff_Float(buf, sizeTable_Quaternion[QUATERNION_0]);
    _quaternion[1] = parseBuff_Float(buf, sizeTable_Quaternion[QUATERNION_1]);
    _quaternion[2] = parseBuff_Float(buf, sizeTable_Quaternion[QUATERNION_2]);
    _quaternion[3] = parseBuff_Float(buf, sizeTable_Quaternion[QUATERNION_3]);
    _accuracy_FLAG = parseBuff(buf, sizeTable_Quaternion[ACCURACY_FLAG]);
  }

  int _frameCount;     // RAW:[0-1] 2byte  // DMP:[0-1] 2byte
  int _subSecond;      // RAW:[2-3] 2byte	// DMP:[2-3] 2byte
  int _sec;            // RAW:[4] 1byte	// DMP:[4] 1byte
  int _min;            // RAW:[5] 1byte	// DMP:[5] 1byte
  int _hour;           // RAW:[6] 1byte	// DMP:[6] 1byte
  float _accelX;       // RAW:[7-8]		// DMP:[7-10]
  float _accelY;       // RAW:[9-10]		// DMP:[11-14]
  float _accelZ;       // RAW:[11-12]		// DMP:[15-18]
  int _temprature;     // RAW:[13-14]		// DMP:[19-20]
  float _gyroScopeX;   // RAW:[15-16]		// DMP:[21-24]
  float _gyroScopeY;   // RAW:[17-18]		// DMP:[25-28]
  float _gyroScopeZ;   // RAW:[19-20]		// DMP:[29-32]
  float _compassX;     // RAW:[21-22]		// DMP:[33-36]
  float _compassY;     // RAW:[23-24]		// DMP:[37-40]
  float _compassZ;     // RAW:[25-26]		// DMP:[41-44]
  float _compassX_TBC; // RAW:[N/A]		// DMP:[45-48]
  float _compassY_TBC; // RAW:[N/A]		// DMP:[49-52]
  float _compassZ_TBC; // RAW:[N/A]		// DMP:[53-56]
  char _accuracy_FLAG; // RAW:[N/A]		// DMP:[57]

  float _quaternion[4]; // 0:[7-10] 1:[11-14] 2:[15-18] 3:[19-22]
  std::uint32_t timestamp;
#else
  std::uint8_t flag;
  std::uint32_t timestamp;
  float temperature;
  float accel[3];
  float gyro[3];

  ImuDataPacket() = default;
  explicit ImuDataPacket(bool is_v2, std::uint8_t *data) {
    if (is_v2)
      from_data_v2(data);
    else
      from_data_v1(data);
  }

  void from_data_v1(std::uint8_t *data) {
    std::uint16_t temp;
    std::int16_t accel_or_gyro[3];
    flag = *data + 1;
    timestamp =
        *(data + 2) | *(data + 3) << 8 | *(data + 4) << 16 | *(data + 5) << 24;
    accel_or_gyro[0] = *(data + 6) | *(data + 7) << 8;
    accel_or_gyro[1] = *(data + 8) | *(data + 9) << 8;
    accel_or_gyro[2] = *(data + 10) | *(data + 11) << 8;
    temp = *(data + 12) | *(data + 13) << 8;
    temperature = temp * 0.125 + 23;
    if (flag == 1) {
      accel[0] = accel_or_gyro[0] * 12.f / 0x10000;
      accel[1] = accel_or_gyro[1] * 12.f / 0x10000;
      accel[2] = accel_or_gyro[2] * 12.f / 0x10000;
      gyro[0] = 0.f;
      gyro[1] = 0.f;
      gyro[2] = 0.f;
    } else if (flag == 2) {
      accel[0] = 0.f;
      accel[1] = 0.f;
      accel[2] = 0.f;
      gyro[0] = accel_or_gyro[0] * 2000.f / 0x10000;
      gyro[1] = accel_or_gyro[1] * 2000.f / 0x10000;
      gyro[2] = accel_or_gyro[2] * 2000.f / 0x10000;
    } else {
      accel[0] = 0.f;
      accel[1] = 0.f;
      accel[2] = 0.f;
      gyro[0] = 0.f;
      gyro[1] = 0.f;
      gyro[2] = 0.f;
    }
  }

  void from_data_v2(std::uint8_t *data) {
    flag = *data + 1;
    timestamp =
        *(data + 2) | *(data + 3) << 8 | *(data + 4) << 16 | *(data + 5) << 24;
    if (flag == 1) {
      accel[0] = *((float*)(data + 6));
      accel[1] = *((float*)(data + 10));
      accel[2] = *((float*)(data + 14));
      gyro[0] = 0.f;
      gyro[1] = 0.f;
      gyro[2] = 0.f;
    } else if (flag == 2) {
      accel[0] = 0.f;
      accel[1] = 0.f;
      accel[2] = 0.f;
      gyro[0] = *((float*)(data + 6));
      gyro[1] = *((float*)(data + 10));
      gyro[2] = *((float*)(data + 14));
    } else if (flag == 11) {
      accel[0] = *((float*)(data + 6));
      accel[1] = *((float*)(data + 10));
      accel[2] = *((float*)(data + 14));
      gyro[0] = *((float*)(data + 18));
      gyro[1] = *((float*)(data + 22));
      gyro[2] = *((float*)(data + 26));
    } else {
      accel[0] = 0.f;
      accel[1] = 0.f;
      accel[2] = 0.f;
      gyro[0] = 0.f;
      gyro[1] = 0.f;
      gyro[2] = 0.f;
    }
  }
#endif
};
#pragma pack(pop)

/**
 * @ingroup datatypes
 * GPS data packet.
 */
#pragma pack(push, 1)
struct GPSDataPacket {
  std::uint8_t flag;
  std::uint64_t device_time;
  double latitude;
  double longitude;
  std::uint8_t NS;
  std::uint8_t EW;

  std::uint16_t year;
  std::uint8_t month;
  std::uint8_t day;
  std::uint8_t hour;
  std::uint8_t minute;
  std::uint8_t second;

  GPSDataPacket() = default;
  explicit GPSDataPacket(std::uint8_t *data) {
    from_data(data);
  }

  void from_data(std::uint8_t *data) {
    flag = *data + 1;
    device_time = *(data + 2) | *(data + 3) << 8 |
      *(data + 4) << 16 | *(data + 5) << 24;
    hour = *(data + 6);
    minute = *(data + 7);
    second = *(data + 8);
    year = *(data + 9) | *(data + 10) << 8;
    month = *(data + 11);
    day = *(data + 12);
    NS = *(data + 13);
    memcpy(&latitude, data + 14, 8);
    EW = *(data + 22);
    memcpy(&longitude, data + 23, 8);
  }
};
#pragma pack(pop)

/**
 * @ingroup datatypes
 * ObstacleDistance data packet.
 */
#pragma pack(push, 1)
struct ObstacleDisPacket {
  std::uint8_t flag;
  std::uint64_t detection_time;
  std::uint16_t distance;

  ObstacleDisPacket() = default;
  explicit ObstacleDisPacket(std::uint8_t *data) {
    from_data(data);
  }

  void from_data(std::uint8_t *data) {
    flag = *data + 1;
    detection_time = *(data + 2) | *(data + 3) << 8 |
      *(data + 4) << 16 | *(data + 5) << 24;
    distance = *(data + 6) | *(data + 7) << 8;
  }
};
#pragma pack(pop)

/**
 * @ingroup datatypes
 * temperature packet.
 */
#pragma pack(push, 1)
struct TemperaturePacket {
  std::uint32_t timestamp;
  std::uint32_t temperature;

  TemperaturePacket() = default;
  explicit TemperaturePacket(std::uint8_t *data) {
    from_data(data);
  }

  void from_data(std::uint8_t *data) {
    timestamp =
        *(data + 2) | *(data + 3) << 8 | *(data + 4) << 16 | *(data + 5) << 24;
    temperature = *((float*)(data + 6));
  }
};
#pragma pack(pop)

EYS3D_DEPTH_END_NAMESPACE

#endif  // EYS3D_DATA_TYPES_INTERNAL_H_
