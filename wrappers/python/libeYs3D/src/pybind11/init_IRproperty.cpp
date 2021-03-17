#include "IRProperty.h"
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/complex.h>
#include <pybind11/functional.h>

#include "eSPDI.h"
#include "eSPDI_def.h"

namespace py=pybind11;


void init_IRproperty(py::module &m){
    py::class_<libeYs3D::devices::IRProperty>(m, "IRProperty")
        .def("enable_extendIR", &libeYs3D::devices::IRProperty::enableExtendIR)
        .def("is_extendIR_enabled", &libeYs3D::devices::IRProperty::isExtendIREnabled)
        .def("set_IR_value", &libeYs3D::devices::IRProperty::setIRValue)
        .def("get_IR_value", &libeYs3D::devices::IRProperty::getIRValue)
        .def("get_IR_max", &libeYs3D::devices::IRProperty::getIRMax)
        .def("get_IR_min", &libeYs3D::devices::IRProperty::getIRMin);
}
