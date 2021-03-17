#include "RegisterReadWriteOptions.h"
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/complex.h>
#include <pybind11/functional.h>

#include "eSPDI.h"
#include "eSPDI_def.h"

namespace py=pybind11;


void init_register(py::module &m){
    py::enum_<libeYs3D::devices::RegisterReadWriteOptions::TYPE>(m, "TYPE")
        .value("IC2", libeYs3D::devices::RegisterReadWriteOptions::TYPE::IC2)
        .value("ASIC", libeYs3D::devices::RegisterReadWriteOptions::TYPE::ASIC)
        .value("FW", libeYs3D::devices::RegisterReadWriteOptions::TYPE::FW)
        .value("TYPE_NONE", libeYs3D::devices::RegisterReadWriteOptions::TYPE::TYPE_NONE);
    py::class_<libeYs3D::devices::RegisterReadWriteOptions>(m, "RegisterReadWriteOptions")
        .def("get_type", &libeYs3D::devices::RegisterReadWriteOptions::getType)
        .def("set_type", &libeYs3D::devices::RegisterReadWriteOptions::setType)
        .def("get_slave_ID", &libeYs3D::devices::RegisterReadWriteOptions::getSlaveID)
        .def("set_slave_ID", &libeYs3D::devices::RegisterReadWriteOptions::setSlaveID)
        .def("get_address_size", &libeYs3D::devices::RegisterReadWriteOptions::getAddressSize)
        .def("set_address_size", &libeYs3D::devices::RegisterReadWriteOptions::setAddressSize)
        .def("set_value_size", &libeYs3D::devices::RegisterReadWriteOptions::setValueSize)
        .def("get_value_size", &libeYs3D::devices::RegisterReadWriteOptions::getValueSize)
        .def("get_sensor_mode", &libeYs3D::devices::RegisterReadWriteOptions::getSensorMode)
        .def("set_sensor_mode", &libeYs3D::devices::RegisterReadWriteOptions::setSensorMode)
        .def("set_request_address", &libeYs3D::devices::RegisterReadWriteOptions::setRequestAddress)
        .def("get_request_address", &libeYs3D::devices::RegisterReadWriteOptions::getRequestAddress)
        .def("get_request_value", &libeYs3D::devices::RegisterReadWriteOptions::getRequestValue)
        .def("set_request_value", &libeYs3D::devices::RegisterReadWriteOptions::setRequestValue)
        .def("is_periodic_read", &libeYs3D::devices::RegisterReadWriteOptions::isPeriodicRead)
        .def("enable_periodic_read", &libeYs3D::devices::RegisterReadWriteOptions::enablePeriodicRead)
        .def("get_period_time", &libeYs3D::devices::RegisterReadWriteOptions::getPeriodTimeMs)
        .def("set_period_time", &libeYs3D::devices::RegisterReadWriteOptions::setPeriodTimeMs)
        .def("is_save_log", &libeYs3D::devices::RegisterReadWriteOptions::isSaveLog)
        .def("enable_save_log", &libeYs3D::devices::RegisterReadWriteOptions::enableSaveLog);
}
