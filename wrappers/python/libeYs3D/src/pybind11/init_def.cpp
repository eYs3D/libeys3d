#include <pybind11/pybind11.h>
#include "eSPDI_def.h"
#include <video/video.h>

namespace py=pybind11;

void init_def(py::module &m){
    py::enum_<CONTROL_MODE>(m, "CONTROL_MODE")
        .value("IMAGE_SN_NONSYNC", CONTROL_MODE::IMAGE_SN_NONSYNC)
        .value("IMAGE_SN_SYNC", CONTROL_MODE::IMAGE_SN_SYNC);
    py::enum_<DEPTH_TRANSFER_CTRL>(m, "DEPTH_TRANSFER_CTRL")
        .value("DEPTH_IMG_NON_TRANSFER", DEPTH_TRANSFER_CTRL::DEPTH_IMG_NON_TRANSFER)
        .value("DEPTH_IMG_GRAY_TRANSFER", DEPTH_TRANSFER_CTRL::DEPTH_IMG_GRAY_TRANSFER)
        .value("DEPTH_IMG_COLORFUL_TRANSFER", DEPTH_TRANSFER_CTRL::DEPTH_IMG_COLORFUL_TRANSFER);
    py::enum_<SENSORMODE_INFO>(m, "SENSORMODE_INFO")
        .value("SENSOR_A", SENSORMODE_INFO::SENSOR_A)
        .value("SENSOR_B", SENSORMODE_INFO::SENSOR_B)
        .value("SENSOR_BOTH", SENSORMODE_INFO::SENSOR_BOTH)
        .value("SENSOR_C", SENSORMODE_INFO::SENSOR_C)
        .value("SENSOR_D", SENSORMODE_INFO::SENSOR_D);

    py::enum_<EtronDIImageType::Value>(m, "EtronDIImageType_Value")
        .value("IMAGE_UNKNOWN", EtronDIImageType::Value::IMAGE_UNKNOWN)
        .value("COLOR_YUY2", EtronDIImageType::Value::COLOR_YUY2)
        .value("COLOR_RGB24", EtronDIImageType::Value::COLOR_RGB24)
        .value("COLOR_MJPG", EtronDIImageType::Value::COLOR_MJPG)
        .value("DEPTH_8BITS", EtronDIImageType::Value::DEPTH_8BITS)
        .value("DEPTH_8BITS_0x80", EtronDIImageType::Value::DEPTH_8BITS_0x80)
        .value("DEPTH_11BITS", EtronDIImageType::Value::DEPTH_11BITS)
        .value("DEPTH_14BITS", EtronDIImageType::Value::DEPTH_14BITS);
    py::enum_<libeYs3D::video::COLOR_RAW_DATA_TYPE>(m, "COLOR_RAW_DATA_TYPE")
        .value("COLOR_RAW_DATA_YUY2", libeYs3D::video::COLOR_RAW_DATA_TYPE::COLOR_RAW_DATA_YUY2)
        .value("COLOR_RAW_DATA_MJPG", libeYs3D::video::COLOR_RAW_DATA_TYPE::COLOR_RAW_DATA_MJPG);
    py::enum_<libeYs3D::video::DEPTH_RAW_DATA_TYPE>(m, "DEPTH_RAW_DATA_TYPE")
        .value("DEPTH_RAW_DATA_8_BITS", libeYs3D::video::DEPTH_RAW_DATA_TYPE::DEPTH_RAW_DATA_8_BITS)
        .value("DEPTH_RAW_DATA_11_BITS", libeYs3D::video::DEPTH_RAW_DATA_TYPE::DEPTH_RAW_DATA_11_BITS)
        .value("DEPTH_RAW_DATA_14_BITS", libeYs3D::video::DEPTH_RAW_DATA_TYPE::DEPTH_RAW_DATA_14_BITS);
    py::class_<DEVINFORMATION>(m, "DEVINFORMATION")
        .def(py::init<>())
        .def_readonly("PID", &DEVINFORMATION::wPID)
        .def_readonly("VID", &DEVINFORMATION::wVID)
        .def_readonly("DevName", &DEVINFORMATION::strDevName)
        .def_readonly("ChipID", &DEVINFORMATION::nChipID)
        .def_readonly("DevType", &DEVINFORMATION::nDevType);
}
