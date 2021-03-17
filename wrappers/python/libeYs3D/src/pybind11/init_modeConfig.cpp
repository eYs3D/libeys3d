#include "ModeConfig.h"
#include "ModeConfigOptions.h"
#include "eSPDI_def.h"
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/functional.h>
#include <pybind11/complex.h>


namespace py=pybind11;

void init_modeConfig(py::module &m){
    py::class_<ModeConfig>(m, "ModeConfig")
        .def(py::init<>())       
        .def("get_mode_config_list", &ModeConfig::GetModeConfigList)
        .def("get_mode_config", &ModeConfig::GetModeConfig);
    py::class_<ModeConfig::MODE_CONFIG>(m, "MODE_CONFIG")
         .def(py::init<>())
         .def_readwrite("iMode", &ModeConfig::MODE_CONFIG::iMode)
         .def_readwrite("iUSB_Type", &ModeConfig::MODE_CONFIG::iUSB_Type)
         .def_readwrite("iInterLeaveModeFPS", &ModeConfig::MODE_CONFIG::iInterLeaveModeFPS)
         .def_readwrite("eDecodeType_L", &ModeConfig::MODE_CONFIG::eDecodeType_L)
         .def_readwrite("eDecodeType_K", &ModeConfig::MODE_CONFIG::eDecodeType_K)
         .def_readwrite("eDecodeType_T", &ModeConfig::MODE_CONFIG::eDecodeType_T)
         .def_readwrite("L_Resolution", &ModeConfig::MODE_CONFIG::L_Resolution)
         .def_readwrite("D_Resolution", &ModeConfig::MODE_CONFIG::D_Resolution)
         .def_readwrite("K_Resolution", &ModeConfig::MODE_CONFIG::K_Resolution)
         .def_readwrite("T_Resolution", &ModeConfig::MODE_CONFIG::T_Resolution)
         .def_readwrite("vecDepthType", &ModeConfig::MODE_CONFIG::vecDepthType)
         .def_readwrite("vecColorFps", &ModeConfig::MODE_CONFIG::vecColorFps)
         .def_readwrite("vecDepthFps", &ModeConfig::MODE_CONFIG::vecDepthFps);
    py::class_<ModeConfig::MODE_CONFIG::RESOLUTION>(m, "RESOLUTION")
        .def(py::init<>())
        .def_readwrite("Width", &ModeConfig::MODE_CONFIG::RESOLUTION::Width)
        .def_readwrite("Height", &ModeConfig::MODE_CONFIG::RESOLUTION::Height);
    py::enum_<ModeConfig::MODE_CONFIG::DECODE_TYPE>(m, "DECODE_TYPE")
        .value("YUYV", ModeConfig::MODE_CONFIG::DECODE_TYPE::YUYV)
        .value("MJPEG", ModeConfig::MODE_CONFIG::DECODE_TYPE::MJPEG);
    py::class_<ModeConfigOptions>(m, "ModeConfigOptions")
        .def(py::init<USB_PORT_TYPE, unsigned short>())
        .def("get_mode_count", &ModeConfigOptions::GetModeCount)
        .def("get_modes", &ModeConfigOptions::GetModes)
        .def("select_current_index", &ModeConfigOptions::SelectCurrentIndex)
        .def("get_current_index", &ModeConfigOptions::GetCurrentIndex)
        .def("get_current_mode_info", &ModeConfigOptions::GetCurrentModeInfo);
    py::enum_<USB_PORT_TYPE>(m, "USB_PORT_TYPE")
        .value("USB_PORT_TYPE_2_0", USB_PORT_TYPE::USB_PORT_TYPE_2_0)
        .value("USB_PORT_TYPE_3_0", USB_PORT_TYPE::USB_PORT_TYPE_3_0)
        .value("USB_PORT_TYPE_UNKNOW", USB_PORT_TYPE::USB_PORT_TYPE_UNKNOW);
}
