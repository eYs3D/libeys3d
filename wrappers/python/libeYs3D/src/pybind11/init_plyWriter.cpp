#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/functional.h>
#include "PlyWriter.h"

namespace py=pybind11;

void init_plyWriter(py::module &m){
    py::class_<CloudPoint>(m, "CloudPoint")
        .def(py::init<>())
        .def_readwrite("x", &CloudPoint::x)
        .def_readwrite("y", &CloudPoint::y)
        .def_readwrite("z", &CloudPoint::z)
        .def_readwrite("r", &CloudPoint::r)
        .def_readwrite("g", &CloudPoint::g)
        .def_readwrite("b", &CloudPoint::b);
    py::class_<PlyWriter>(m, "PlyWriter")
        .def("writePly", &PlyWriter::writePly);
}
