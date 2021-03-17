cmake_minimum_required(VERSION 3.10)
project(eys3dPy)

set(ESPDI_LOC ${CMAKE_CURRENT_LIST_DIR}/../eSPDI)
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_FIND_DEBUG_MODE TRUE)

set(CMAKE_BUILD_TYPE RelWithDebInfo)

include_directories(src)
include_directories(src/DMPreview_utility src/DMPreview_utility/sqlite3)
include_directories(src/devices/ src/devices/model src/sensors)

include_directories(${ESPDI_LOC})
link_directories(${ESPDI_LOC})

# TurboJpeg should be removed because eys3d have included in the SDK.
link_directories(${ESPDI_LOC}/turbojpeg/x86_64/lib/)
include_directories(${ESPDI_LOC}/turbojpeg/x86_64/include)

# libudev
set(CMAKE_MODULE_PATH ${CMAKE_CURRENT_LIST_DIR}/cmake/modules)
message(Set CMAKE_MODULE_PATH:${CMAKE_MODULE_PATH})
find_package(UDev REQUIRED)
message(UDEV_LIBRARIES:${UDEV_LIBRARIES} UDEV_INCLUDE_DIRS:${UDEV_INCLUDE_DIRS})
include_directories(${UDEV_INCLUDE_DIRS})

set(CMAKE_INSTALL_RPATH
        $ORIGIN:
        $ORIGIN/../../eSPDI/:
        $ORIGIN/../../eSPDI/turbojpeg/x86_64/lib:)

set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)

# These source related to PyBind11
set(SRC_CPP
        src/base/threads/Async.cpp
        src/base/threads/ThreadStore.cpp
        src/base/threads/FunctorThread.cpp
        src/base/threads/Thread_pthread.cpp
        src/base/synchronization/MessageChannel.cpp
        src/video/coders.cpp
	src/video/pc_coders.cpp
        src/video/Frame.cpp
        src/video/PCFrame.cpp
        src/video/FrameProducer.cpp
        src/video/ColorFrameProducer.cpp
        src/video/DepthFrameProducer.cpp
        src/video/PCFrameProducer.cpp
        src/utils.cpp
        src/EYS3DSystem.cpp
        src/devices/MemoryAllocator.cpp
        src/devices/CameraDevice.cpp
        src/devices/CameraDevice_8053.cpp
        src/devices/CameraDevice_8037.cpp
        src/devices/CameraDevice_8053.cpp
        src/devices/CameraDevice_8036.cpp
        src/devices/CameraDevice_8052.cpp
        src/devices/CameraDevice_8062.cpp
        src/devices/CameraDevice_Hypatia.cpp
	src/devices/IMUDevice.cpp
	src/devices/IMUDevice_8062.cpp
	src/DMPreview_utility/hidapi/hid_udev.c
	src/devices/model/DepthFilterOptions.cpp
        src/devices/model/DepthAccuracyOptions.cpp
        src/devices/model/CameraDeviceProperties.cpp
        src/devices/model/IRProperty.cpp
        src/devices/model/RegisterReadWriteOptions.cpp
        src/devices/RegisterReadWriteController.cpp
	src/sensors/SensorData.cpp
	src/sensors/SensorDataProducer.cpp
	src/sensors/IMUDataProducer.cpp
        src/DMPreview_utility/ColorPaletteGenerator.cpp
	src/DMPreview_utility/RegisterSettings.cpp
        src/DMPreview_utility/PlyWriter.cpp
        src/DMPreview_utility/PlyFilter.cpp
        src/DMPreview_utility/ModeConfigOptions.cpp
        src/DMPreview_utility/ModeConfig.cpp
	src/DMPreview_utility/IMUData.cpp
        src/DMPreview_utility/sqlite3/sqlite3.c
	src/DMPreview_utility/hidapi/hid_udev.c)

# These source related to eys3d
set(PYBIND_DEF_SRC
        src/pybind11/python.cpp
        src/pybind11/init_device.cpp
        src/pybind11/init_def.cpp
        src/pybind11/init_frame.cpp
        src/pybind11/init_system.cpp
        src/pybind11/init_pcframe.cpp
        src/pybind11/init_depthfilter.cpp
        src/pybind11/init_accuracy.cpp
        src/pybind11/init_plyWriter.cpp
        src/pybind11/init_modeConfig.cpp
	src/pybind11/init_property.cpp
	src/pybind11/init_IRproperty.cpp
	src/pybind11/init_register.cpp
	src/pybind11/init_IMU.cpp)

add_subdirectory(pybind11)

add_library(eys3dPy MODULE ${SRC_CPP} ${PYBIND_DEF_SRC})

target_link_libraries(eys3dPy PRIVATE
        pybind11::module
        pybind11::lto
        pybind11::windows_extras
        eSPDI_X86_64
        turbojpeg
        ${UDEV_LIBRARIES})

pybind11_extension(eys3dPy)

#pybind11_strip(eys3dPy) # Remove all symbols in the shared object

set_target_properties(eys3dPy PROPERTIES
        CXX_VISIBILITY_PRESET "hidden"
        CUDA_VISIBILITY_PRESET "hidden")


## Test file source
set(TEST_SRC src/main.cpp)
add_executable(eYs3D.test ${TEST_SRC} ${SRC_CPP} ${PYBIND_DEF_SRC})
target_link_libraries(eYs3D.test
        PRIVATE pybind11::embed
        eSPDI_X86_64
        turbojpeg
        ${UDEV_LIBRARIES}
        dl
        pthread
	usb-1.0)

# Install eys3dPy and eYs3D.test to out folder
install(TARGETS eys3dPy eYs3D.test
        LIBRARY DESTINATION ${CMAKE_CURRENT_LIST_DIR}/out
        RUNTIME DESTINATION ${CMAKE_CURRENT_LIST_DIR}/out)
