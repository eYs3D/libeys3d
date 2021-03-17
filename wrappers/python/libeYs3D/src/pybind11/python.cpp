#include <pybind11/pybind11.h>

namespace py = pybind11;

void init_frame(py::module &); 
void init_pcframe(py::module &); 
void init_device(py::module &); 
void init_def(py::module &m);
void init_system(py::module &); 
void init_depthfilter(py::module &); 
void init_accuracy(py::module &); 
void init_modeConfig(py::module &); 
void init_plyWriter(py::module &); 
void init_property(py::module &); 
void init_IRproperty(py::module &); 
void init_register(py::module &); 
void init_IMU(py::module &); 

PYBIND11_MODULE(eys3dPy, m){ 
    init_frame(m);
    init_pcframe(m);
    init_device(m);
    init_def(m);
    init_system(m);
    init_depthfilter(m);
    init_accuracy(m);
    init_modeConfig(m);			    
    init_plyWriter(m);
    init_property(m);
    init_IRproperty(m);
    init_register(m);
    init_IMU(m);
}
