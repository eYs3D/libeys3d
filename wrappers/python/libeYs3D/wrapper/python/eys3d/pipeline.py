import time

from .device import Device
from .config import Config, ModeConfig


class Pipeline():
    """This class is to manage the color, depth and point cloud stream with pipeline process.

    This class is to manage the color, depth and point cloud stream with pipeline process.
    The purpose is for easier device operation.
    User could start pipeline with user-defined callback function.

    Notice:
        It will call get_color_frame and get_depth_frame if user start with user-defined callback function.

    Args:
        device_index (int): Index for device would like to initialize.
    """
    def __init__(self, device_index=0, device=None):
        if device:
            self.__dev = device
        else:
            self.__dev = Device(device_index)
        self.__config = None
        self.dev_info = self.__dev.get_device_info()

        self.__cframe = dict({'raw_data': None, 'rgb_data': None})
        self.__dframe = dict({
            'raw_data': None,
            'rgb_data': None,
            'z_data': None
        })
        self.__colorFrameCallback = self.__defaultColorFrameCallback
        self.__depthFrameCallback = self.__defaultDepthFrameCallback
        self.__PCFrameCallback = None
        self.__IMUDataCallback = None

        self.__color_frame_shape = None
        self.__depth_frame_shape = None

        self.__depthAccuracyInfo = None

        self.__status = False

    def start(self,
              config=None,
              colorFrameCallback=None,
              depthFrameCallback=None,
              PCFrameCallback=None,
              IMUDataCallback=None):
        """Start the stream with configure and callback function.

        To start the stream with configure and callback function.
        It would use the default callback function to get frame data of color and depth if callback function is None.

        Args:
            config (obj): The class Config.
            colorFrameCallback (func): The callback function to get frame from color stream.
            depthFrameCallback (func): The callback function to get frame from depth stream.
            PCFrameCallback    (func): The callback function to get frame from point cloud stream.
            IMUDataCallback    (func): The callback function to get sensor data from IMU stream.

        """
        if config is None and self.__config is None:
            raise Exception("Config needed for device setting.")
        else:
            self.__config = config

        if colorFrameCallback:
            self.__registerColorCallbackFn(colorFrameCallback)
        else:
            self.__registerColorCallbackFn(self.__defaultColorFrameCallback)

        if depthFrameCallback:
            self.__registerDepthCallbackFn(depthFrameCallback)
        else:
            self.__registerDepthCallbackFn(self.__defaultDepthFrameCallback)

        if PCFrameCallback:
            self.__registerPCCallbackFn(PCFrameCallback)

        if IMUDataCallback:
            self.__registerIMUCallbackFn(IMUDataCallback)

        if not self.__status:
            self.__status = True
            self.__dev.open_device(
                config=self.__config,
                colorFrameCallback=self.__colorFrameCallback,
                depthFrameCallback=self.__depthFrameCallback,
                PCFrameCallback=self.__PCFrameCallback,
                IMUDataCallback=IMUDataCallback)
            self.__color_frame_shape = self.__config.get_color_stream_resolution(
            )
            self.__depth_frame_shape = self.__config.get_depth_stream_resolution(
            )

        self.__dev.enable_stream()

    def get_color_frame(self, raw_data=False):
        """Get the color frame by default callback.

        To get the color frame by default callback.
        It show rgb frame if raw_data is False, otherwise is raw data.
        The shape of rgb frame data is (H, W, 3).
        The shape of raw frame data is (H, W, 2).

        Args:
            raw_data (bool): True is raw data, YUY2 or MJPG. False is rgb data.

        Returns:
            bool : The boolean value is to mean the value has got from callback function.
            np.array : The array of frame data. The shape is (H, W, 3) if rgb data. The shape is (H, W, 2) if raw data.
        """
        if self.__colorFrameCallback != self.__defaultColorFrameCallback:
            raise Exception(
                "This function could not work when defined callback function.")
        try:
            if raw_data:
                return True, self.__cframe['raw_data'].reshape(
                    (*self.__color_frame_shape, 2))
            else:
                return True, self.__cframe['rgb_data'].reshape(
                    (*self.__color_frame_shape, 3))
        except AttributeError:
            return False, None

    def get_depth_frame(self, raw_data=False):
        """Get the depth frame by default callback.

        To get the depth frame by default callback.
        It show rgb frame if raw_data is False, otherwise is raw data.
        The shape of rgb frame data is (H, W, 3).
        The shape of raw frame data is (H, W, 2).

        Args:
            raw_data (bool): True is raw data. False is rgb data.

        Returns:
            bool : The boolean value is to mean the value has got from callback function.
            np.array : The array of frame data. The shape is (H, W, 3) if rgb data. The shape is (H, W, 2) if raw data.
        """
        if self.__depthFrameCallback != self.__defaultDepthFrameCallback:
            raise Exception(
                "This function could not work when defined callback function.")
        try:
            if raw_data:
                return True, self.__dframe['raw_data'].reshape(
                    (*self.__depth_frame_shape, 2))
            else:
                return True, self.__dframe['rgb_data'].reshape(
                    (*self.__depth_frame_shape, 3))
        except AttributeError:
            return False, None

    def get_depth_zValue(self, ):
        """Get the z value for depth frame.

        To get the z value for depth frame.

        Returns:
            np.array : The array of z value of depth frame. The size is (H, W).
        """
        if self.__depthFrameCallback != self.__defaultDepthFrameCallback:
            raise Exception(
                "This function could not work when defined callback function.")
        return self.__dframe['z_data'].reshape((*self.__depth_frame_shape))

    def get_device(self):
        """Get the camera device.

        To get the camera device for operating the device in detail.

        Returns:
            obj : The class Device.
        """
        return self.__dev

    def pause(self):
        """Pause the stream.

        To pause the stream.
        """
        self.__dev.pause_stream()

    def stop(self, ):
        """Stop the stream.

        To stop the stream and then release device.
        """
        self.__status = False
        self.__dev.pause_stream()
        time.sleep(0.7)
        self.__dev.close_stream()
        time.sleep(1.0)
        self.__dev.release()

    def __registerColorCallbackFn(self, cb):
        self.__colorFrameCallback = cb

    def __registerDepthCallbackFn(self, cb):
        self.__depthFrameCallback = cb

    def __registerPCCallbackFn(self, cb):
        self.__PCFrameCallback = cb

    def __registerIMUCallbackFn(self, cb):
        self.__IMUDataCallback = cb

    def __defaultColorFrameCallback(self, frame):
        self.__cframe['rgb_data'] = frame.rgbData
        self.__cframe['raw_data'] = frame.rawData

    def __defaultDepthFrameCallback(self, frame):
        self.__dframe['rgb_data'] = frame.rgbData
        self.__dframe['raw_data'] = frame.rawData
        self.__dframe['z_data'] = frame.zData
        self.__depthAccuracyInfo = frame.depthAccuracyInfo

    def is_interleave_mode_enabled(self):
        """Get the status of interleaveMode.

        To get the status of interleaveMode.

        Returns:
            bool: The return value. True for enable, False otherwise.
        """
        return self.__dev.is_interleave_mode_enabled()

    def enable_interleave_mode(self):
        """Enable interleave mode.

        To enable interleave mode.
        """
        self.__dev.enable_interleave_mode()

    def disable_interleave_mode(self):
        """Disable interleave mode.

        To disable interleave mode.
        """
        self.__dev.disable_interleave_mode()

    def get_depthFilter_options(self, ):
        """Get the class of DepthFilterOptions.

        To get the class of DepthFilterOptions.
        User could read description on `depthFilter` in detail.

        Returns:
            obj : The class DepthFilter.
        """
        return self.__dev.get_depthFilter_options()

    def get_depthAccuracy(self, ):
        """Get the class of DepthAccuracy.

        To get the class of DepthAccuracy.
        User could read description on `depthAccuracy` in detail.

        Returns:
            obj : The class DepthAccuracy.
        """
        return self.__dev.get_depthAccuracy()

    def get_cameraProperty(self, ):
        """Get the class of CameraProperty.

        To get the class of CameraProperty.
        User could read description on `cameraProperty.py` in detail.

        Returns:
            obj : The class CameraProperty.
        """
        return self.__dev.get_cameraProperty()

    def get_accuracy_info(self):
        """Get the information of accruracy.

        To get the information of accuracy as dictionary.

        Returns:
           dict : The dictionary contains the information of accuracy. The key is following:
               * distance
               * fill_rate
               * z_accuracy
               * temporal_noise
               * spatial_noise
               * angle
               * angle_x
               * angle_y
        """
        return self.__depthAccuracyInfo

    def get_IRProperty(self):
        return self.__dev.get_IRProperty()
