#include "CameraDeviceProperties.h"
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/complex.h>
#include <pybind11/functional.h>

#include "eSPDI.h"
#include "eSPDI_def.h"

namespace py=pybind11;


void init_property(py::module &m){
    py::class_<libeYs3D::devices::CameraDeviceProperties::CameraPropertyItem>(m, "CameraPropertyItem")
        .def_readonly("support", &libeYs3D::devices::CameraDeviceProperties::CameraPropertyItem::bSupport)
        .def_readonly("valid", &libeYs3D::devices::CameraDeviceProperties::CameraPropertyItem::bValid)
        .def_readonly("value", &libeYs3D::devices::CameraDeviceProperties::CameraPropertyItem::nValue)
        .def_readonly("min", &libeYs3D::devices::CameraDeviceProperties::CameraPropertyItem::nMin)
        .def_readonly("max", &libeYs3D::devices::CameraDeviceProperties::CameraPropertyItem::nMax)
        .def_readonly("default", &libeYs3D::devices::CameraDeviceProperties::CameraPropertyItem::nDefault)
        .def_readonly("flag", &libeYs3D::devices::CameraDeviceProperties::CameraPropertyItem::nFlags)
        .def_readonly("step", &libeYs3D::devices::CameraDeviceProperties::CameraPropertyItem::nStep);
    py::enum_<libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY>(m, "CAMERA_PROPERTY")
        .value("AUTO_EXPOSURE", libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::AUTO_EXPOSURE)
        .value("AUTO_WHITE_BLANCE", libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::AUTO_WHITE_BLANCE)
        .value("LOW_LIGHT_COMPENSATION", libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::LOW_LIGHT_COMPENSATION)
        .value("LIGHT_SOURCE", libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::LIGHT_SOURCE)
        .value("EXPOSURE_TIME", libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::EXPOSURE_TIME)
        .value("WHITE_BLANCE_TEMPERATURE", libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::WHITE_BLANCE_TEMPERATURE)
        .value("CAMERA_PROPERTY_COUNT", libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::CAMERA_PROPERTY_COUNT);
    py::enum_<libeYs3D::devices::CameraDeviceProperties::LIGHT_SOURCE_VALUE>(m, "LIGHT_SOURCE_VALUE")
        .value("VALUE_50HZ", libeYs3D::devices::CameraDeviceProperties::LIGHT_SOURCE_VALUE::VALUE_50HZ)
        .value("VALUE_60HZ", libeYs3D::devices::CameraDeviceProperties::LIGHT_SOURCE_VALUE::VALUE_60HZ);
}
