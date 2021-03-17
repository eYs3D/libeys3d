#include "DepthAccuracyOptions.h"
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/complex.h>
#include <pybind11/functional.h>

#include "eSPDI.h"
#include "eSPDI_def.h"

namespace py=pybind11;


void init_accuracy(py::module &m){
    py::class_<libeYs3D::devices::DepthAccuracyInfo>(m, "DepthAccuracyInfo")
        .def_readonly("distance", &libeYs3D::devices::DepthAccuracyInfo::fDistance)
        .def_readonly("fillRate", &libeYs3D::devices::DepthAccuracyInfo::fFillRate)
        .def_readonly("zAccuracy", &libeYs3D::devices::DepthAccuracyInfo::fZAccuracy)
        .def_readonly("temporalNoise", &libeYs3D::devices::DepthAccuracyInfo::fTemporalNoise)
        .def_readonly("spatialNoise", &libeYs3D::devices::DepthAccuracyInfo::fSpatialNoise)
        .def_readonly("angle", &libeYs3D::devices::DepthAccuracyInfo::fAngle)
        .def_readonly("angleX", &libeYs3D::devices::DepthAccuracyInfo::fAngleX)
        .def_readonly("angleY", &libeYs3D::devices::DepthAccuracyInfo::fAngleY);
    py::class_<libeYs3D::devices::DepthAccuracyOptions>(m, "DepthAccuracyOptions")
        .def("enable", &libeYs3D::devices::DepthAccuracyOptions::enable)
        .def("is_enabled", &libeYs3D::devices::DepthAccuracyOptions::isEnabled)
        .def("set_region_ratio", &libeYs3D::devices::DepthAccuracyOptions::setRegionRatio)
        .def("get_region_ratio", &libeYs3D::devices::DepthAccuracyOptions::getRegionRatio)
        .def("set_groundTruth_distance", &libeYs3D::devices::DepthAccuracyOptions::setGroundTruthDistanceMM)
        .def("get_groundTruth_distance", &libeYs3D::devices::DepthAccuracyOptions::getGroundTruthDistanceMM);
}
