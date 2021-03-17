#include <pybind11/pybind11.h>
#include <pybind11/stl_bind.h>
#include <pybind11/numpy.h>
#include <video/PCFrame.h>

namespace py=pybind11;

PYBIND11_MAKE_OPAQUE(std::vector<uint8_t>);
PYBIND11_MAKE_OPAQUE(std::vector<float>);

void init_pcframe(py::module &m){
    py::bind_vector<std::vector<uint8_t>>(m,"rgbDataVec");
    py::bind_vector<std::vector<float>>(m,"xyzDataVec");
    py::class_<libeYs3D::video::PCFrame>(m, "PCFrame",  py::buffer_protocol())
	.def(py::init<uint32_t, uint8_t, float>(), py::keep_alive<1, 2>())
        .def_readonly("tsUs", &libeYs3D::video::PCFrame::tsUs)
        .def_readonly("serialNumber", &libeYs3D::video::PCFrame::serialNumber)
        .def_readonly("width", &libeYs3D::video::PCFrame::width)
        .def_readonly("height", &libeYs3D::video::PCFrame::height)
        .def_readonly("rgbDataVec", &libeYs3D::video::PCFrame::rgbDataVec, py::keep_alive<0, 1>(), py::return_value_policy::reference) // It's no useful.
        .def_readonly("xyzDataVec", &libeYs3D::video::PCFrame::xyzDataVec, py::keep_alive<0, 1>(), py::return_value_policy::reference) // It's no useful
        .def_readonly("transcodingTime", &libeYs3D::video::PCFrame::transcodingTime)
        .def_readonly("sensorDataSet", &libeYs3D::video::PCFrame::sensorDataSet)
        .def_property_readonly("rgbData", [](const libeYs3D::video::PCFrame &pcf){
         auto v = new std::vector<uint8_t>(pcf.rgbDataVec);
         auto capsule = py::capsule(v, [](void *v) { delete reinterpret_cast<std::vector<uint8_t>*>(v);});
         return py::array(v->size(), v->data(), capsule);})	
        .def_property_readonly("xyzData", [](const libeYs3D::video::PCFrame &pcf){
         auto v = new std::vector<float>(pcf.xyzDataVec);
         auto capsule = py::capsule(v, [](void *v) { delete reinterpret_cast<std::vector<float>*>(v);});
         return py::array(v->size(), v->data(), capsule);});	
}
