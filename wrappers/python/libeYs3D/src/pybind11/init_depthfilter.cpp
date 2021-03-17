#include "DepthFilterOptions.h"
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/complex.h>
#include <pybind11/functional.h>

#include "eSPDI.h"
#include "eSPDI_def.h"

namespace py=pybind11;


void init_depthfilter(py::module &m){
    py::class_<libeYs3D::devices::DepthFilterOptions>(m, "DepthFilterOptions")
        .def("enable", &libeYs3D::devices::DepthFilterOptions::enable)
        .def("is_enabled", &libeYs3D::devices::DepthFilterOptions::isEnabled)
        // Common
        .def("get_bytes_per_pixel", &libeYs3D::devices::DepthFilterOptions::getBytesPerPixel)
        // SubSample
        .def("enable_subsample", &libeYs3D::devices::DepthFilterOptions::enableSubSample)
        .def("is_subsample_enabled", &libeYs3D::devices::DepthFilterOptions::isSubSampleEnabled)
        .def("set_subsample_mode", &libeYs3D::devices::DepthFilterOptions::setSubSampleMode)
        .def("get_subsample_mode", &libeYs3D::devices::DepthFilterOptions::getSubSampleMode)
        .def("set_subsample_factor", &libeYs3D::devices::DepthFilterOptions::setSubSampleFactor)
        .def("get_subsample_factor", &libeYs3D::devices::DepthFilterOptions::getSubSampleFactor)
        //EdgePreServingFilter
        .def("enable_edgePreServingFilter", &libeYs3D::devices::DepthFilterOptions::enableEdgePreServingFilter)
        .def("is_edgePreServingFilter_enabled", &libeYs3D::devices::DepthFilterOptions::isEdgePreServingFilterEnabled)
        .def("set_edge_level", &libeYs3D::devices::DepthFilterOptions::setEdgeLevel)
        .def("get_edge_level", &libeYs3D::devices::DepthFilterOptions::getEdgeLevel)
        .def("set_sigma", &libeYs3D::devices::DepthFilterOptions::setSigma)
        .def("get_sigma", &libeYs3D::devices::DepthFilterOptions::getSigma)
        .def("set_lambda", &libeYs3D::devices::DepthFilterOptions::setLumda)
        .def("get_lambda", &libeYs3D::devices::DepthFilterOptions::getLumda)
        // HoleFill
        .def("enable_holeFill", &libeYs3D::devices::DepthFilterOptions::enableHoleFill)
        .def("is_holeFill_enabled", &libeYs3D::devices::DepthFilterOptions::isHoleFillEnabled)
        .def("set_kernel_size", &libeYs3D::devices::DepthFilterOptions::setKernelSize)
        .def("get_kernel_size", &libeYs3D::devices::DepthFilterOptions::getKernelSize)
        .def("set_level", &libeYs3D::devices::DepthFilterOptions::setLevel)
        .def("get_level", &libeYs3D::devices::DepthFilterOptions::getLevel)
        .def("set_horizontal", &libeYs3D::devices::DepthFilterOptions::setHorizontal)
        .def("is_horizontal", &libeYs3D::devices::DepthFilterOptions::isHorizontal)
        // TemporalFilter
        .def("enable_temporalFilter", &libeYs3D::devices::DepthFilterOptions::enableTemporalFilter)
        .def("is_temporalFilter_enabled", &libeYs3D::devices::DepthFilterOptions::isTemporalFilterEnabled)
        .def("set_alpha", &libeYs3D::devices::DepthFilterOptions::setAlpha)
        .def("get_alpha", &libeYs3D::devices::DepthFilterOptions::getAlpha)
        .def("set_history", &libeYs3D::devices::DepthFilterOptions::setHistory)
        .def("get_history", &libeYs3D::devices::DepthFilterOptions::getHistory)
        //
        .def("enable_flyingDepthCancellation", &libeYs3D::devices::DepthFilterOptions::enableFlyingDepthCancellation)
        .def("is_flyingDepthCancellation_enabled", &libeYs3D::devices::DepthFilterOptions::isFlyingDepthCancellationEnabled)
        .def("set_FlyingDepthCancellation_locked", &libeYs3D::devices::DepthFilterOptions::setFlyingDepthCancellationLocked)
        .def("is_flyingDepthCancellation_locked", &libeYs3D::devices::DepthFilterOptions::isFlyingDepthCancellationLocked);
}
