import os

# Set environment variable before initialize eys3d system
cfg_path = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        "../../../..")  # The path of python_wrapper
os.environ['EYS3D_SDK_HOME'] = cfg_path

from .eys3d_def import *
from .pipeline import Pipeline
from .config import Config, ModeConfig
from .device import Device, EYS3DSystem
from .depthFilter import DepthFilterOptions
from .depthAccuracy import DepthAccuracy
from .cameraProperty import CameraProperty

from eys3dPy import LIGHT_SOURCE_VALUE, COLOR_RAW_DATA_TYPE, DEPTH_RAW_DATA_TYPE, SENSORMODE_INFO, DEPTH_TRANSFER_CTRL
__all__ = [
    "Pipeline", "Config", "ModeConfig", "Device", "DepthFilterOptions",
    "DepthAccuracy", "CameraProperty"
]
