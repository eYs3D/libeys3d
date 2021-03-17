#include "CameraDevice.h"
#include "CameraDevice_8053.h"
#include "CameraDevice_8062.h"
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/complex.h>
#include <pybind11/functional.h>
#include <pybind11/numpy.h>

#include "eSPDI.h"
#include "eSPDI_def.h"

namespace py=pybind11;


void init_device(py::module &m){
    py::class_<libeYs3D::devices::CameraDevice, std::shared_ptr<libeYs3D::devices::CameraDevice>>(m, "CameraDevice")
        .def("init_stream", &libeYs3D::devices::CameraDevice::initStream)
        .def("enable_stream", &libeYs3D::devices::CameraDevice::enableStream)
        .def("enable_color_stream", &libeYs3D::devices::CameraDevice::enableColorStream)
        .def("enable_depth_stream", &libeYs3D::devices::CameraDevice::enableDepthStream)
        .def("enable_pc_stream", &libeYs3D::devices::CameraDevice::enablePCStream)
        .def("enable_IMU_stream", &libeYs3D::devices::CameraDevice::enableIMUStream)
        .def("pause_stream", &libeYs3D::devices::CameraDevice::pauseStream)
        .def("pause_color_stream", &libeYs3D::devices::CameraDevice::pauseColorStream)
        .def("pause_depth_stream", &libeYs3D::devices::CameraDevice::pauseDepthStream)
        .def("pause_pc_stream", &libeYs3D::devices::CameraDevice::pausePCStream)
        .def("pause_IMU_stream", &libeYs3D::devices::CameraDevice::pauseIMUStream)
        .def("close_stream", &libeYs3D::devices::CameraDevice::closeStream)
        .def("release", &libeYs3D::devices::CameraDevice::release)
        .def("get_camera_device_info", [](libeYs3D::devices::CameraDevice &device)
	     {
                 auto info = device.getCameraDeviceInfo();
                 py::dict py_info;
                 py::dict py_dev_info;
                 py_info["firmware_version"] = info.firmwareVersion;
                 py_info["serial_number"] = info.serialNumber;
                 py_info["bus_info"] = info.busInfo;
                 py_info["mode_name"] = info.modelName;
                 // DEVINFORMATION
                 py_dev_info["PID"] = info.devInfo.wPID;
                 py_dev_info["VID"] = info.devInfo.wVID;
                 py_dev_info["dev_name"] = info.devInfo.strDevName;
                 py_dev_info["chip_ID"] = info.devInfo.nChipID;
                 py_dev_info["dev_type"] = info.devInfo.nDevType;

                 py_info["dev_info"] = py_dev_info;
                 return py_info;
	     })
        .def("dump_frame_info", &libeYs3D::devices::CameraDevice::dumpFrameInfo)
        //
        // Interleave mode
        .def("is_interleave_mode_enabled", &libeYs3D::devices::CameraDevice::isInterleaveModeEnabled)
        .def("enable_interleave_mode", &libeYs3D::devices::CameraDevice::enableInterleaveMode)
        //
        // DepthFilterOptions
        .def("get_depthFilterOptions", &libeYs3D::devices::CameraDevice::getDepthFilterOptions)
        .def("set_depthFilterOptions", &libeYs3D::devices::CameraDevice::setDepthFilterOptions)
        //
        // DepthAccuracyOptions
        .def("set_depthAccuracyOptions", &libeYs3D::devices::CameraDevice::setDepthAccuracyOptions)
        .def("get_depthAccuracyOptions", &libeYs3D::devices::CameraDevice::getDepthAccuracyOptions)
        .def("get_depthAccuracyRegion", [](libeYs3D::devices::CameraDevice &device)
	     {
                 auto region = device.getDepthAccuracyRegion();
                 py::dict py_region;
                 py_region["x"] = region.x;
                 py_region["y"] = region.y;
                 py_region["width"] = region.width;
                 py_region["height"] = region.height;
                 return py_region;
	     })
        //
        // CameraDeviceProperties
        .def("enable_AE", [](libeYs3D::devices::CameraDevice &device)
	     {
                 device.setCameraDevicePropertyValue(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::AUTO_EXPOSURE, 1);
             })
        .def("disable_AE", [](libeYs3D::devices::CameraDevice &device)
	     {
                 device.setCameraDevicePropertyValue(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::AUTO_EXPOSURE, 0);
             })
        .def("enable_AWB", [](libeYs3D::devices::CameraDevice &device)
	     {
                 device.setCameraDevicePropertyValue(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::AUTO_WHITE_BLANCE, 1);
             })
        .def("disable_AWB", [](libeYs3D::devices::CameraDevice &device)
	     {
                 device.setCameraDevicePropertyValue(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::AUTO_WHITE_BLANCE, 0);
             })
        .def("get_AE_status", [](libeYs3D::devices::CameraDevice &device)
	     {
                 auto property = device.getCameraDeviceProperty(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::AUTO_EXPOSURE);
                 auto nValue = property.nValue;
                 return nValue;
             })
        .def("get_AWB_status", [](libeYs3D::devices::CameraDevice &device)
	     {
                 auto property = device.getCameraDeviceProperty(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::AUTO_WHITE_BLANCE);
                 auto nValue = property.nValue;
                 return nValue;
             })
        .def("get_exposure_value", [](libeYs3D::devices::CameraDevice &device)
	     {
                 auto property = device.getCameraDeviceProperty(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::EXPOSURE_TIME);
                 auto nValue = property.nValue;
                 return nValue;
             })
        .def("set_exposure_value", [](libeYs3D::devices::CameraDevice &device, int value)
	     {
                 device.setCameraDevicePropertyValue(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::EXPOSURE_TIME, value);
             })
        .def("get_white_balance_temperature", [](libeYs3D::devices::CameraDevice &device)
	     {
                 auto property = device.getCameraDeviceProperty(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::WHITE_BLANCE_TEMPERATURE);
                 auto nValue = property.nValue;
                 return nValue;
             })
        .def("set_white_balance_temperature", [](libeYs3D::devices::CameraDevice &device, int value)
	     {
                 device.setCameraDevicePropertyValue(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::WHITE_BLANCE_TEMPERATURE, value);
             })
        .def("set_low_light_compensation", [](libeYs3D::devices::CameraDevice &device, int value)
	     {
                 device.setCameraDevicePropertyValue(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::LOW_LIGHT_COMPENSATION, value);
             })
        .def("get_low_light_compensation_status", [](libeYs3D::devices::CameraDevice &device)
	     {
                 auto property = device.getCameraDeviceProperty(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::LOW_LIGHT_COMPENSATION);
                 auto nValue = property.nValue;
                 return nValue;
             })
        .def("set_light_source", [](libeYs3D::devices::CameraDevice &device, int value)
	     {
                 device.setCameraDevicePropertyValue(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::LIGHT_SOURCE, value);
             })
        .def("get_light_source_status", [](libeYs3D::devices::CameraDevice &device)
	     {
                 auto property = device.getCameraDeviceProperty(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::LIGHT_SOURCE);
                 auto nValue = property.nValue;
                 return nValue;
             })

        .def("get_exposure_range", [](libeYs3D::devices::CameraDevice &device)
	     {
                 auto property = device.getCameraDeviceProperty(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::EXPOSURE_TIME);
                 py::dict dict;
                 dict["Max"] = property.nMax;
                 dict["Min"] = property.nMin;
                 dict["Step"] = property.nStep;
                 dict["Default"] = property.nDefault;
                 // dict["Flag"] = property.nFlags; // Ignore it
                 return dict;
             })
        .def("get_white_balance_temperature_range", [](libeYs3D::devices::CameraDevice &device)
	     {
                 auto property = device.getCameraDeviceProperty(libeYs3D::devices::CameraDeviceProperties::CAMERA_PROPERTY::WHITE_BLANCE_TEMPERATURE);
                 py::dict dict;
                 dict["Max"] = property.nMax;
                 dict["Min"] = property.nMin;
                 dict["Step"] = property.nStep;
                 dict["Default"] = property.nDefault;
                 // dict["Flag"] = property.nFlags; // Ignore it
                 return dict;
             })
        .def("get_camera_device_property", &libeYs3D::devices::CameraDevice::getCameraDeviceProperty)
        .def("set_camera_device_property", &libeYs3D::devices::CameraDevice::setCameraDevicePropertyValue)
        .def("get_manual_exposure_time", &libeYs3D::devices::CameraDevice::getManuelExposureTimeMs)
        .def("set_manual_exposure_time", &libeYs3D::devices::CameraDevice::setManuelExposureTimeMs)
        .def("get_manual_global_gain", &libeYs3D::devices::CameraDevice::getManuelGlobalGain)
        .def("set_manual_global_gain", &libeYs3D::devices::CameraDevice::setManuelGlobalGain)
        // IR property
        .def("get_IR_property", &libeYs3D::devices::CameraDevice::getIRProperty)
        .def("set_IR_property", &libeYs3D::devices::CameraDevice::setIRProperty)
        .def("get_z_range", [](libeYs3D::devices::CameraDevice &device)
	     {
                 uint16_t Near, Far;
                 device.getDepthOfField(&Near, &Far);
                 py::dict py_dict;
                 py_dict["Near"] = Near;
                 py_dict["Far"] = Far;
                 return py_dict;
             })
        .def("set_z_range", &libeYs3D::devices::CameraDevice::setDepthOfField)
        .def("get_usb_port_type", &libeYs3D::devices::CameraDevice::getUsbPortType)
        .def("get_rectify_log_data", [](libeYs3D::devices::CameraDevice &device)
	     {
                 auto logData = device.getRectifyLogData();
                 py::dict  pyRectLogData;   //[KT] HARD CODE ref. eSPDI_def.h
		 pyRectLogData["InImgWidth"] = logData.InImgWidth;
		 pyRectLogData["InImgHeight"] = logData.InImgHeight;
		 pyRectLogData["OutImgWidth"] = logData.OutImgWidth;
		 pyRectLogData["OutImgHeight"] = logData.OutImgHeight;
		 pyRectLogData["RECT_ScaleEnable"] = logData.RECT_ScaleEnable;
		 pyRectLogData["RECT_CropEnable"] = logData.RECT_CropEnable;
		 pyRectLogData["RECT_ScaleWidth"] = logData.RECT_ScaleWidth;
		 pyRectLogData["RECT_ScaleHeight"] = logData.RECT_ScaleHeight;
		 pyRectLogData["CamMat1"] = py::array_t<float>({9}, logData.CamMat1);
		 pyRectLogData["CamDist1"] = py::array_t<float>({8}, logData.CamDist1);
		 pyRectLogData["CamMat2"] = py::array_t<float>({9}, logData.CamMat2);
		 pyRectLogData["CamDist2"] = py::array_t<float>({8}, logData.CamDist2);
		 pyRectLogData["RotaMat"] = py::array_t<float>({9}, logData.RotaMat);
		 pyRectLogData["TranMat"] = py::array_t<float>({3}, logData.TranMat);
		 pyRectLogData["LRotaMat"] = py::array_t<float>({9}, logData.LRotaMat);
		 pyRectLogData["RRotaMat"] = py::array_t<float>({9}, logData.RRotaMat);
		 pyRectLogData["NewCamMat1"] = py::array_t<float>({12}, logData.NewCamMat1);
		 pyRectLogData["NewCamMat2"] = py::array_t<float>({12}, logData.NewCamMat2);
		 pyRectLogData["RECT_Crop_Row_BG"] = logData.RECT_Crop_Row_BG;
		 pyRectLogData["RECT_Crop_Row_ED"] = logData.RECT_Crop_Row_ED;
		 pyRectLogData["RECT_Crop_Col_BG_L"] = logData.RECT_Crop_Col_BG_L;
		 pyRectLogData["RECT_Crop_Col_ED_L"] = logData.RECT_Crop_Col_ED_L;
		 pyRectLogData["RECT_Scale_Col_M"] = logData.RECT_Scale_Col_M;
		 pyRectLogData["RECT_Scale_Col_N"] = logData.RECT_Scale_Col_N;
		 pyRectLogData["RECT_Scale_Row_M"] = logData.RECT_Scale_Row_M;
		 pyRectLogData["RECT_Scale_Row_N"] = logData.RECT_Scale_Row_N;
		 pyRectLogData["RECT_AvgErr"] = logData.RECT_AvgErr;
		 pyRectLogData["nLineBuffers"] = logData.nLineBuffers;
		 pyRectLogData["ReProjectMat"] = py::array_t<float>({16}, logData.ReProjectMat);
                 return pyRectLogData;
             })
        // Register 
        .def("get_register_options", &libeYs3D::devices::CameraDevice::getRegisterReadWriteOptions)
        .def("set_read_register_options", &libeYs3D::devices::CameraDevice::setRegisterReadWriteOptionsForRead)
        .def("set_write_register_options", &libeYs3D::devices::CameraDevice::setRegisterReadWriteOptionsForWrite)
        .def("get_FW_register", [](libeYs3D::devices::CameraDevice &device, uint16_t addr)
	     {   // It need to get options and set address then set into device and get options.
                 auto options = device.getRegisterReadWriteOptions();
                 options.setType(libeYs3D::devices::RegisterReadWriteOptions::FW);
                 options.setRequestAddress(0, addr);
                 options.setAddressSize(FG_Address_1Byte);
                 options.setValueSize(FG_Value_1Byte);
                 device.setRegisterReadWriteOptionsForRead(options);
                 
                 auto options2 = device.getRegisterReadWriteOptions();
                 auto value = options2.getRequestValue(0);
                 return value;
             })
        .def("set_FW_register", [](libeYs3D::devices::CameraDevice &device, uint16_t addr, uint16_t value)
	     {
                 auto options = device.getRegisterReadWriteOptions();
                 options.setType(libeYs3D::devices::RegisterReadWriteOptions::FW);
                 options.setRequestAddress(0, addr);
                 options.setRequestValue(0, value);
                 options.setAddressSize(FG_Address_1Byte);
                 options.setValueSize(FG_Value_1Byte);
                 device.setRegisterReadWriteOptionsForWrite(options);
             })
        .def("get_HW_register", [](libeYs3D::devices::CameraDevice &device, uint16_t addr)
	     {   // It need to get options and set address then set into device and get options.
                 auto options = device.getRegisterReadWriteOptions();
                 options.setType(libeYs3D::devices::RegisterReadWriteOptions::ASIC);
                 options.setRequestAddress(0, addr);
                 options.setAddressSize(FG_Address_2Byte);
                 options.setValueSize(FG_Value_1Byte);
                 device.setRegisterReadWriteOptionsForRead(options);
                 
                 auto options2 = device.getRegisterReadWriteOptions();
                 auto value = options2.getRequestValue(0);
                 return value;
             })
        .def("set_HW_register", [](libeYs3D::devices::CameraDevice &device, uint16_t addr, uint16_t value)
	     {
                 auto options = device.getRegisterReadWriteOptions();
                 options.setType(libeYs3D::devices::RegisterReadWriteOptions::ASIC);
                 options.setRequestAddress(0, addr);
                 options.setRequestValue(0, value);
                 options.setAddressSize(FG_Address_2Byte);
                 options.setValueSize(FG_Value_1Byte);
                 device.setRegisterReadWriteOptionsForWrite(options);
             })
        .def("get_sensor_register", [](libeYs3D::devices::CameraDevice &device, uint16_t addr, SENSORMODE_INFO sensorMode, int slaveID)
	     {   // It need to get options and set address then set into device and get options.
                 auto options = device.getRegisterReadWriteOptions();
                 options.setType(libeYs3D::devices::RegisterReadWriteOptions::IC2);
                 options.setRequestAddress(0, addr);
                 auto info = device.getCameraDeviceInfo();
                 if (info.devInfo.wPID == 0x138 || info.devInfo.wPID == 0x146) //8053
                 {   
                     options.setAddressSize(FG_Address_1Byte);
                     options.setValueSize(FG_Value_1Byte);
                 } else {
                     options.setAddressSize(FG_Address_2Byte);
                     options.setValueSize(FG_Value_2Byte);
                 }
                 options.setSensorMode(sensorMode);
                 options.setSlaveID(slaveID);
                 device.setRegisterReadWriteOptionsForRead(options);
                 auto options2 = device.getRegisterReadWriteOptions();
                 auto value = options2.getRequestValue(0);
                 return value;
             })
        .def("set_sensor_register", [](libeYs3D::devices::CameraDevice &device, uint16_t addr, uint16_t value, SENSORMODE_INFO sensorMode, int slaveID)
	     {
                 auto options = device.getRegisterReadWriteOptions();
                 options.setType(libeYs3D::devices::RegisterReadWriteOptions::IC2);
                 options.setRequestAddress(0, addr);
                 options.setRequestValue(0, value);
                 auto info = device.getCameraDeviceInfo();
                 if (info.devInfo.wPID == 0x138 || info.devInfo.wPID == 0x146) //8053
                 {   
                     options.setAddressSize(FG_Address_1Byte);
                     options.setValueSize(FG_Value_1Byte);
                 } else {
                     options.setAddressSize(FG_Address_2Byte);
                     options.setValueSize(FG_Value_2Byte);
                 }
                 options.setSensorMode(sensorMode);
                 options.setSlaveID(slaveID);
                 device.setRegisterReadWriteOptionsForWrite(options);
             })
         // HWPP
         .def("is_HWPP_supported", &libeYs3D::devices::CameraDevice::isHWPPSupported)
         .def("is_HWPP_enabled", &libeYs3D::devices::CameraDevice::isHWPPEnabled)
         .def("enable_HWPP", &libeYs3D::devices::CameraDevice::enableHWPP)
         // Snapshot
         .def("do_snapshot", &libeYs3D::devices::CameraDevice::doSnapshot)
         // PlyFilter
         .def("is_plyFilter_supported", &libeYs3D::devices::CameraDevice::isPlyFilterSupported)
         .def("enable_plyFilter", &libeYs3D::devices::CameraDevice::enablePlyFilter)
         .def("is_plyFilter_enabled", &libeYs3D::devices::CameraDevice::isPlyFilterEnabled)
	 // IMU
	 .def("is_IMU_device_supported", &libeYs3D::devices::CameraDevice::isIMUDeviceSupported)
	 .def("is_IMU_device_present", &libeYs3D::devices::CameraDevice::isIMUDevicePresent)
	 .def("get_IMU_device_info", [](libeYs3D::devices::CameraDevice &device){
             auto info = device.getIMUDeviceInfo();
             py::dict imu_info;
             imu_info["VID"]=info.nVID;
             imu_info["PID"]=info.nPID;
             imu_info["type"]=info.nType;
             imu_info["serialNumber"]=info.serialNumber;
             imu_info["fwVersion"]=info.fwVersion;
             imu_info["moduleName"]=info.moduleName;
             imu_info["status"]=info.status;
             imu_info["isValid"]=info.isValid;
             return imu_info;
         })
         .def("dump_IMU_data", &libeYs3D::devices::CameraDevice::dumpIMUData)
	 ;
    py::class_<libeYs3D::devices::CameraDeviceInfo>(m, "CameraDeviceInfo")
        .def(py::init<>())
        .def_readonly("devInfo", &libeYs3D::devices::CameraDeviceInfo::devInfo)
        .def_readonly("firmwareVersion", &libeYs3D::devices::CameraDeviceInfo::firmwareVersion)
        .def_readonly("serialNumber", &libeYs3D::devices::CameraDeviceInfo::serialNumber)
        .def_readonly("modelName", &libeYs3D::devices::CameraDeviceInfo::modelName)
        .def_readonly("busInfo", &libeYs3D::devices::CameraDeviceInfo::busInfo);
}
