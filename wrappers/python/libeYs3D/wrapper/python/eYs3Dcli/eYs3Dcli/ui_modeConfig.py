'''
 Copyright (C) 2015-2019 ICL/ITRI
 All rights reserved.

 NOTICE:  All information contained herein is, and remains
 the property of ICL/ITRI and its suppliers, if any.
 The intellectual and technical concepts contained
 herein are proprietary to ICL/ITRI and its suppliers and
 may be covered by Taiwan and Foreign Patents,
 patents in process, and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden unless prior written permission is obtained
 from ICL/ITRI.
'''

from configshell_fb import ExecutionError

from .ui_node import UINode
from .ui_control import CVDemoControl, ModeControl, DepthFilteringControl, InterleaveControl, PCDemoControl, CBDemoControl, AccuracyDemoControl, RPDemoControl

from eys3d import ModeConfig, Config


class ModeConfigUI(UINode):
    '''
    A ModeConfig UI.
    '''
    def __init__(self, name, parent, camera_device):
        UINode.__init__(self, name, parent)
        self.camera_device = camera_device
        self.pid = int(self.camera_device.get_device_info()['dev_info']['PID'])
        self.mode_config = ModeConfig(self.pid)

        self.refresh()

    def refresh(self):
        self._children = set([])
        max_index = self.mode_config.get_mode_count()
        base_index = self.mode_config.get_current_index()
        for i in range(max_index):
            self.mode_config.select_current_index(base_index + i)
            config = Config()
            config.set_preset_mode_config(self.pid, (base_index + i))
            #mode_info = self.mode_config.get_current_mode_info().iMode
            if self.camera_device.get_usb_type() == self.mode_config.get_current_mode_info().iUSB_Type:
                Mode(self.camera_device, config, self.mode_config, self)


class Mode(UINode):
    '''
    A Mode UI
    '''
    def __init__(self, device, config, mode_config, parent):
        self.dev = device
        self.conf = config
        self.mode_config = mode_config
        self.mode_info = self.mode_config.get_current_mode_info()
        node_name = "Mode_{}".format(self.mode_info.iMode)
        UINode.__init__(self, node_name, parent)
        self.refresh()
        self.config_dict = {}  # dictionary for config
        self.__update_config()

    def refresh(self):
        self._children = set([])
        
        if self.conf.get_config()['colorHeight'] is not 0:
            PCDemoControl("PC_demo", self, self.dev, self.conf, self.mode_info)
            RPDemoControl("Record_Playback_demo", self, self.dev, self.conf, self.mode_info)
        CVDemoControl("CV_demo", self, self.dev, self.conf, self.mode_info)
        CBDemoControl("Callback_demo", self, self.dev, self.conf,
                      self.mode_info)
        AccuracyDemoControl("Accuracy_demo", self, self.dev, self.conf,
                             self.mode_info)

    def summary(self):
        summary = ""
        if self.config_dict["color_width"] != 0:
            summary += "Color: {0}*{1} {2}, ".format(
                self.config_dict["color_width"],
                self.config_dict["color_height"], self.config_dict["format"])
        if self.config_dict["depth_width"] != 0:
            summary += ("Depth: {0}*{1} {2}, ".format(
                self.config_dict["depth_width"],
                self.config_dict["depth_height"], self.config_dict["format"]))
        if self.config_dict["fps"]:
            summary += ("Fps: {}, ".format(self.config_dict["fps"]))

        if self.config_dict["usb"] != 0:
            summary += "USB: {}".format(self.config_dict["usb"])

        return (summary, True)

    def __update_config(self):
        self.config_dict["color_width"] = self.mode_info.L_Resolution.Width
        self.config_dict["color_height"] = self.mode_info.L_Resolution.Height
        self.config_dict["depth_width"] = self.mode_info.D_Resolution.Width
        self.config_dict["depth_height"] = self.mode_info.D_Resolution.Height
        self.config_dict["format"] = self.mode_info.eDecodeType_L
        ep0fps = 0 if not self.mode_info.vecColorFps else self.mode_info.vecColorFps[
            0]
        ep1fps = 0 if not self.mode_info.vecDepthFps else self.mode_info.vecDepthFps[
            0]
        self.config_dict["fps"] = ep0fps if ep0fps else ep1fps
        self.config_dict["usb"] = self.mode_info.iUSB_Type
