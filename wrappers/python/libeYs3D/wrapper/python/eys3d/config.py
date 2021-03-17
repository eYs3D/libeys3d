import numpy as np
import json

import eys3dPy

from .eys3d_def import EYS3D_OK  # TODO


class Config():
    """This class is to set the configuration of streaming.

    This class is to set the configuration of streaming.
    It could set the resolution and format of color and depth frame.
    """
    def __init__(self, ):

        self.colorStreamFormat = eys3dPy.COLOR_RAW_DATA_TYPE.COLOR_RAW_DATA_YUY2
        self.depthStreamFormat = eys3dPy.DEPTH_TRANSFER_CTRL.DEPTH_IMG_COLORFUL_TRANSFER
        self.depthDataType = 0
        self.ep0Width = 0
        self.ep0Height = 0
        self.ep1Width = 0
        self.ep1Height = 0
        self.ep0fps = 0
        self.ep1fps = 0
        self.interleavefps = 0

    def set_color_stream(self, streamFormat, nEP0Width, nEP0Height, fps):
        """Set the format and resolution of color stream.

        To set the format, resolution, and fps of color stream.
        Please refer PIF of camera module or ModeConfig.db.

        Args:
            streamFormat (int): The format of color stream. 0: YUY2. 1: MJPG.
                User could refer COLOR_RAW_DATA_YUY2 and COLOR_RAW_DATA_MJPG in COLOR_RAW_DATA_TYPE.
            nEP0Width (int): The width of the color stream.
            nEP0Height (int): The height of the color stream.
            fps (int): The fps of color stream.
        """
        self.colorStreamFormat = streamFormat
        self.ep0Width = nEP0Width
        self.ep0Height = nEP0Height
        self.ep0fps = fps

    def set_depth_stream(self, streamFormat, nEP1Width, nEP1Height, fps):
        """Set the format and resolution of depth stream.

        To set the format, resolution, and fps of depth stream.
        Please refer PIF of camera module or ModeConfig.db.

        Args:
            streamFormat (int): The format of depth stream. User could refer DEPTH_TRANSFER_CTRL.
                0: DEPTH_IMG_NON_TRANSFER, 1: DEPTH_IMG_GRAY_TRANSFER, 2: DEPTH_IMG_COLORFUL_TRANSFER.
            nEP0Width (int): The width of the depth stream.
            nEP0Height (int): The height of the depth stream.
            fps (int): The fps of depth stream.
        """
        self.depthStreamFormat = streamFormat
        self.ep1Width = nEP1Width
        self.ep1Height = nEP1Height
        self.ep1fps = fps

    def set_depth_dataType(self, DepthDataType):
        """Set the data type of depth stream.

        To set the data type for depth stream.
        Please refer DEPTH_RAW_DATA_TYPE.
        The options are:
        * DEPTH_RAW_DATA_8_BITS
        * DEPTH_RAW_DATA_11_BITS
        * DEPTH_RAW_DATA_14_BITS
        """
        self.depthDataType = DepthDataType

    def set_fps(self, fps):
        """Set the fps in manual.

        To set the fps in manual.
        """
        self.ep1fps = fps

    def get_config(self):
        """Get the dictionary of the user-provided config.

        To get the dictionary of the user-provided config.

        Returns:
            dict: The user-provided config. The key is following:
                * colorFormat: The format of color stream.
                * colorWidth: The width of color stream.
                * colorHeight: The height of color stream.
                * depthStreamFormat: The format of depth stream.
                * depthWidth: The width of depth stream.
                * depthHeight: The width of depth stream.
                * actualFps: The actual fps.
                * depthFormat: The depth data type.
        """
        actualFps = self.ep0fps if self.ep0fps else self.ep1fps
        conf = dict({
            "colorFormat": self.colorStreamFormat,
            "colorWidth": self.ep0Width,
            "colorHeight": self.ep0Height,
            "depthStreamFormat": self.depthStreamFormat,
            "depthWidth": self.ep1Width,
            "depthHeight": self.ep1Height,
            "actualFps": actualFps,
            "depthFormat": self.depthDataType,
        })

        return conf

    def get_color_stream_resolution(self):
        """Get the resolution of color stream.

        Get the resolution of color frame.
        The return order is height, width.

        Returns:
            tuple: The tuple contains height and width.
        """
        return (self.ep0Height, self.ep0Width)

    def get_depth_stream_resolution(self):
        """Get the resolution of depth stream.

        Get the resolution of depth frame.
        The return order is height, width.

        Returns:
            tuple: The tuple contains height and width.
        """
        return (self.ep1Height, self.ep1Width)

    def set_preset_mode_config(
        self,
        pid,
        index,
    ):
        """Set the preset mode config.

        This function is to make the mode config setting easier. 
        User could select mode index from modeConfig.db to complete config setting.

        Args:
            pid (int): The product id of camera module. Please refer the PIF. It is availabel for integer or heximal.
            index (int): The mode index in modeConfig.db. 
        """
        modeConfig = ModeConfig(pid, index)
        mode_info = modeConfig.get_current_mode_info()
        self.__update_config(mode_info)
        # self.set_depth_dataType(self.depthDataType)

    def __update_config(self, mode_info):
        self.colorStreamFormat = eys3dPy.COLOR_RAW_DATA_TYPE.COLOR_RAW_DATA_YUY2 if mode_info.eDecodeType_L == 0 else eys3dPy.COLOR_RAW_DATA_TYPE.COLOR_RAW_DATA_MJPG
        self.ep0Height = mode_info.L_Resolution.Height
        self.ep0Width = mode_info.L_Resolution.Width
        self.ep1Height = mode_info.D_Resolution.Height
        self.ep1Width = mode_info.D_Resolution.Width
        self.ep0fps = 0 if not mode_info.vecColorFps else mode_info.vecColorFps[
            0]
        self.ep1fps = 0 if not mode_info.vecDepthFps else mode_info.vecDepthFps[
            0]
        self.depthDataType = self.__DepthDataToFormatType(
            mode_info.vecDepthType[0])
        self.interleavefps = mode_info.iInterLeaveModeFPS

    def __DepthDataToFormatType(self, DepthData):
        if DepthData == 11:
            return eys3dPy.DEPTH_RAW_DATA_TYPE.DEPTH_RAW_DATA_11_BITS
        elif DepthData == 8:
            return eys3dPy.DEPTH_RAW_DATA_TYPE.DEPTH_RAW_DATA_8_BITS
        elif DepthData == 14:
            return eys3dPy.DEPTH_RAW_DATA_TYPE.DEPTH_RAW_DATA_14_BITS
        else:
            return 0


class ModeConfig():
    """This class is to link database `modeConfig.db`.

    This class is to link database `modeConfig.db`.

    Args:
        pid (hex): The product id of camera module. Please refer the PIF.
        index (int): The mode index in modeConfig.db. 
        
    """
    def __init__(self, pid, index=None):
        # it's no meaning but just pass a argument.
        self.__usb_type = eys3dPy.USB_PORT_TYPE.USB_PORT_TYPE_3_0
        self.__pid = pid
        self.__maxIndex = 0
        self.__index = index if index is not None else 0

        try:
            self.__modeConfig = eys3dPy.ModeConfigOptions(self.__usb_type, pid)
            self.__maxIndex = self.__modeConfig.get_mode_count()
        except Exception as e:
            raise e
        if EYS3D_OK != self.__modeConfig.select_current_index(self.__index):
            self.__index = self.__modeConfig.get_current_index()
            print(
                "Alert!!Index is not in database. Default is the first config setting."
            )
        self.__mode_info = self.__modeConfig.get_current_mode_info()

    def get_current_index(self):
        """Get the index of current mode.

        To get the index of current mode.

        Returns:
            int: The index of current mode.
        """
        current_index = self.__modeConfig.get_current_index()
        self.__index = current_index
        return self.__index

    def get_current_mode_info(self):
        """Get the information of current mode.

        To get the information of current mode.
        It return a object of `MODE_CONFIG`.

        Returns:
            obj: The MODE_CONFIG. It contains 
                * iMode
                * iUSB_Type
                * iInterLeaveModeFPS
                * eDecodeType_L
                * eDecodeType_K
                * eDecodeType_T
                * L_Resolution
                * D_Resolution
                * K_Resolution
                * T_Resolution
                * vecDepthType
                * vecColorFps
                * vecDepthFps
        """
        return self.__mode_info

    def get_mode_count(self):
        """Get the total number of mode in modeConfig.db.

        To the the total number of mode on camera module in modeConfig.db.
        """
        return self.__maxIndex

    def get_modes(self):
        """Get the modes.

        To Get the modes.
        It is not to be implemented.

        Raise:
            NotImplementedError:
        """
        raise NotImplementedError

    def select_current_index(self, index):
        """Select the index that user wanted.

        To select the index that user wanted.
        It raised error if index is not in modeConfig.db.

        Args:
            index (int): The index user wanted to select.

        Raises:
            IndexError: If no index in modeConfig.db.
        """
        if EYS3D_OK != self.__modeConfig.select_current_index(index):
            raise IndexError
        self.__mode_info = self.__update_mode_info()

    def __update_mode_info(self):
        return self.__modeConfig.get_current_mode_info()


class RectLogData:
    """This class is to save rectify log data.

    This class is to save rectify log data.
    Its attribute is follow the `eSPCtrl_RectLogData` in eSPDI_def.h.

    Args:
        dictionary: The dictonary of rectyLogData from eys3dPy.
        nRectifyLogIndex: The index user wanted to select.

    Raise:
        Exception: No rectify log.Please check rectify index.
    """
    def __init__(self, dictionary, nRectifyLogIndex):
        try:
            self.rectifyLogIndex = nRectifyLogIndex
            self.dictLog = self.__to_list(dictionary)
            for k, v in dictionary.items():
                setattr(self, k, v)
        except TypeError as e:
            raise Exception("No rectify log. Please check rectify index")

    def save_json(self, fname=None):
        """Save rectify log data as json file.

        To save rectify log data as json file.
        It save as `RectfyLog-{index}.json` if fname is not provided.

        Args:
            fname (str): The filename for json file. Default is RectfyLog-{index}.json if fname is None. 
        """
        if fname is None:
            fname = "RectfyLog-{}.json".format(self.rectifyLogIndex)
        elif not fname.endswith(".json"):
            fname = "{}.json".format(fname)
        with open(fname, "w") as fp:
            json.dump(self.dictLog, fp)

    def get_dict(self, ):
        """Get the dictionary of rectify log data.

        To get the dictionary of rectify log data.
        The key is refer to `eSPCtrl_RectLogData` in eSPDI_def.h.

        Returns:
            dict: The rectify log data as dictionary.
        """
        return self.dictLog

    def __to_list(self, dictLog):
        ret = dict({})
        for k, v in dictLog.items():
            if isinstance(v, np.ndarray):
                ret[k] = v.tolist()
            else:
                ret[k] = v
        return ret


class PointCloud:
    def __init__(self):
        pass

    def generate_point_cloud(self, depthFrame, colorFrame, plyFilter):
        try:
            point_cloud = eys3dPy.generate_point_cloud(
                depthFrame, depthFrame.shape[1], depthFrame.shape[0],
                colorFrame, colorFrame.shape[1], colorFrame.shape[0],
                plyFilter)
        except Exception as e:
            raise e
        return point_cloud

    def generate_point_cloud_gpu(self, depthFrame, colorFrame):
        try:
            point_cloud = eys3dPy.generate_point_cloud_gpu(
                depthFrame,
                depthFrame.shape[1],
                depthFrame.shape[0],
                colorFrame,
                colorFrame.shape[1],
                colorFrame.shape[0],
            )
        except Exception as e:
            raise e
        return point_cloud

    def write_ply(self, point_cloud, f_name):
        if point_cloud is None or f_name is None:
            return False
        try:
            eys3dPy.PlyWriter.writePly(point_cloud, f_name)
        except Exception as e:
            raise e
        return True


#
