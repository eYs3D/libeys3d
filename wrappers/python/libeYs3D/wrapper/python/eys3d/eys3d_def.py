from dataclasses import dataclass  # py3.7
import enum
import numpy as np


@dataclass
class Frame:
    nwidth: int
    nheight: int
    nch: int
    data: np.array


class CStream(enum.IntEnum):
    YUYV = 0
    MJPEG = 1


class DStream(enum.IntEnum):
    DEFAULT = 0
    GRAY = 1
    COLORFUL = 2


class Register(enum.IntEnum):
    FW = 0
    HW = 1
    SENSOR = 2


class DepthDataFormat(enum.IntEnum):
    DEPTH_DATA_OFF_RAW = 0  # raw (depth off, only raw color)
    DEPTH_DATA_DEFAULT = 0  # raw (depth off, only raw color)
    DEPTH_DATA_8_BITS = 1  # rectify, 1 byte per pixel
    DEPTH_DATA_14_BITS = 2  # rectify, 2 byte per pixel
    DEPTH_DATA_8_BITS_x80 = 3  # rectify, 2 byte per pixel but using 1 byte only
    DEPTH_DATA_11_BITS = 4  # rectify, 2 byte per pixel but using 11 bit only
    DEPTH_DATA_OFF_RECTIFY = 5  # rectify (depth off, only rectify color)
    DEPTH_DATA_8_BITS_RAW = 6  # raw
    DEPTH_DATA_14_BITS_RAW = 7  # raw
    DEPTH_DATA_8_BITS_x80_RAW = 8  # raw
    DEPTH_DATA_11_BITS_RAW = 9  # raw
    DEPTH_DATA_14_BITS_COMBINED_RECTIFY = 11
    DEPTH_DATA_11_BITS_COMBINED_RECTIFY = 13  # multi-baseline


class SensorMode(enum.IntEnum):
    SENSOR_A = 0
    SENSOR_B = 1
    SENSOR_BOTH = 2
    SENSOR_C = 3
    SENSOR_D = 3


# Error code ref eSPDI_def.h
EYS3D_OK = 0

#
