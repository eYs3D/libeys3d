#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/complex.h>
#include <pybind11/functional.h>
#include <pybind11/numpy.h>

#include "eSPDI.h"
#include "eSPDI_def.h"
#include "IMUDevice.h"
#include "SensorData.h"

namespace py=pybind11;


void init_IMU(py::module &m){
    py::enum_<libeYs3D::devices::IMUDevice::IMU_TYPE>(m, "IMU_TYPE")
        .value("IMU_6_AXIS", libeYs3D::devices::IMUDevice::IMU_TYPE::IMU_6_AXIS)
	.value("IMU_9_AXIS", libeYs3D::devices::IMUDevice::IMU_TYPE::IMU_9_AXIS)
        .value("IMU_UNKNOWN", libeYs3D::devices::IMUDevice::IMU_TYPE::IMU_UNKNOWN);
    py::enum_<libeYs3D::devices::IMUDevice::IMU_DATA_FORMAT>(m, "IMU_DATA_FORMAT")
        .value("RAW_DATA_WITHOUT_OFFSET", libeYs3D::devices::IMUDevice::IMU_DATA_FORMAT::RAW_DATA_WITHOUT_OFFSET)
        .value("RAW_DATA_WITH_OFFSET", libeYs3D::devices::IMUDevice::IMU_DATA_FORMAT::RAW_DATA_WITH_OFFSET)
        .value("OFFSET_DATA", libeYs3D::devices::IMUDevice::IMU_DATA_FORMAT::OFFSET_DATA)
        .value("DMP_DATA_WITHOT_OFFSET", libeYs3D::devices::IMUDevice::IMU_DATA_FORMAT::DMP_DATA_WITHOT_OFFSET)
        .value("DMP_DATA_WITH_OFFSET", libeYs3D::devices::IMUDevice::IMU_DATA_FORMAT::DMP_DATA_WITH_OFFSET);
    py::class_<libeYs3D::devices::IMUDevice::IMUDeviceInfo>(m, "IMUDeviceInfo")
        .def_readonly("nVID", &libeYs3D::devices::IMUDevice::IMUDeviceInfo::nVID)
        .def_readonly("nPID", &libeYs3D::devices::IMUDevice::IMUDeviceInfo::nPID)
        .def_readonly("nType", &libeYs3D::devices::IMUDevice::IMUDeviceInfo::nType);
    py::class_<libeYs3D::sensors::SensorData>(m, "SensorData")
        .def_readonly("data", &libeYs3D::sensors::SensorData::data)
        .def_readonly("type", &libeYs3D::sensors::SensorData::type)
        .def_readonly("serialNumber", &libeYs3D::sensors::SensorData::serialNumber);
    py::enum_<libeYs3D::sensors::SensorData::SensorDataType>(m, "SensorDataType")
        .value("IMU_DATA", libeYs3D::sensors::SensorData::SensorDataType::IMU_DATA)
        .value("UNKNOWN", libeYs3D::sensors::SensorData::SensorDataType::UNKNOWN);
    py::class_<libeYs3D::sensors::SensorDataSet>(m, "SensorDataSet")
        .def(py::init<libeYs3D::sensors::SensorData::SensorDataType, int32_t>())
        .def_readonly("tsUs", &libeYs3D::sensors::SensorDataSet::tsUs)
        .def_readonly("serialNumber", &libeYs3D::sensors::SensorDataSet::serialNumber)
        .def_readonly("sensorDataType", &libeYs3D::sensors::SensorDataSet::sensorDataType)
        .def_readonly("actualDataCount", &libeYs3D::sensors::SensorDataSet::actualDataCount)
        .def_readonly("sensorDataVec", &libeYs3D::sensors::SensorDataSet::sensorDataVec);
}
