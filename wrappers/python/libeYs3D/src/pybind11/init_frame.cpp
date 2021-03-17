#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/complex.h>
#include <pybind11/functional.h>
#include <pybind11/numpy.h>
#include <video/Frame.h>

namespace py=pybind11;

void init_frame(py::module &m){
    py::class_<libeYs3D::video::Frame>(m, "Frame")
	.def(py::init<uint64_t, uint64_t, uint8_t>())
        .def_readonly("tsUs", &libeYs3D::video::Frame::tsUs)
        .def_readonly("serialNumber", &libeYs3D::video::Frame::serialNumber)
        .def_readonly("width", &libeYs3D::video::Frame::width)
        .def_readonly("height", &libeYs3D::video::Frame::height)
        .def_readonly("actualDataBufferSize", &libeYs3D::video::Frame::actualDataBufferSize)
        .def_readonly("dataBufferSize", &libeYs3D::video::Frame::dataBufferSize)
        .def_readonly("actualRGBBufferSize", &libeYs3D::video::Frame::actualRGBBufferSize)
        .def_readonly("rgbBufferSize", &libeYs3D::video::Frame::rgbBufferSize)
        .def_readonly("actualZDDepthBufferSize", &libeYs3D::video::Frame::actualZDDepthBufferSize)
        .def_readonly("zdDepthBufferSize", &libeYs3D::video::Frame::zdDepthBufferSize)
        .def_readonly("dataFormat", &libeYs3D::video::Frame::dataFormat)
        .def_readonly("rgbFormat", &libeYs3D::video::Frame::rgbFormat)
        .def_readonly("rgbTranscodingTime", &libeYs3D::video::Frame::rgbTranscodingTime)
        .def_readonly("filteringTime", &libeYs3D::video::Frame::filteringTime)
        .def_readonly("sensorDataSet", &libeYs3D::video::Frame::sensorDataSet)
        // DepthAccuracy
        .def_property_readonly("depthAccuracyInfo",
		     [](const libeYs3D::video::Frame &f)
		      {   auto info = f.extra.depthAccuracyInfo;
	                  py::dict py_info;
	                  py_info["distance"] = info.fDistance;
	                  py_info["fill_rate"] = info.fFillRate;
	                  py_info["z_accuracy"] = info.fZAccuracy;
	                  py_info["temporal_noise"] = info.fTemporalNoise;
	                  py_info["spatial_noise"] = info.fSpatialNoise;
	                  py_info["angle"] = info.fAngle;
	                  py_info["angle_x"] = info.fAngleX;
	                  py_info["angle_y"] = info.fAngleY;
                          return py_info;
                      })
        .def_property_readonly("rgbData", [](const libeYs3D::video::Frame &f){
         auto v = new std::vector<uint8_t>(f.rgbVec);
         auto capsule = py::capsule(v, [](void *v) { delete reinterpret_cast<std::vector<uint8_t>*>(v);});
         return py::array(v->size(), v->data(), capsule);})	
        .def_property_readonly("rawData", [](const libeYs3D::video::Frame &f){
         auto v = new std::vector<uint8_t>(f.dataVec);
         auto capsule = py::capsule(v, [](void *v) { delete reinterpret_cast<std::vector<uint8_t>*>(v);});
         return py::array(v->size(), v->data(), capsule);}) 	
        .def_property_readonly("zData", [](const libeYs3D::video::Frame &f){
         auto v = new std::vector<uint16_t>(f.zdDepthVec);
         auto capsule = py::capsule(v, [](void *v) { delete reinterpret_cast<std::vector<uint8_t>*>(v);});
         return py::array(v->size(), v->data(), capsule);});	
}

