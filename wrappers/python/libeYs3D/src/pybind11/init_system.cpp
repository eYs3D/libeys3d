#include "EYS3DSystem.h"
#include "CameraDevice.h"

#include "eSPDI.h"
#include "eSPDI_def.h"

#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/complex.h>
#include <pybind11/functional.h>

namespace py=pybind11;

void init_system(py::module &m){
    py::class_<libeYs3D::EYS3DSystem, std::unique_ptr<libeYs3D::EYS3DSystem, py::nodelete>>(m, "System")
        .def("get_eys3d_system", &libeYs3D::EYS3DSystem::getEYS3DSystem)
        .def("get_eys3d_handle", &libeYs3D::EYS3DSystem::getEYS3DDIHandle)
        .def("get_camera_device", &libeYs3D::EYS3DSystem::getCameraDevice)
        .def("get_camera_device_count",&libeYs3D::EYS3DSystem::getCameraDeviceCount);
}
