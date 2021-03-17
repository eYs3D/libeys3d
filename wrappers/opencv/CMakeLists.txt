cmake_minimum_required(VERSION 3.10)
project(eys3d_opencv)

set(CMAKE_FIND_DEBUG_MODE TRUE)
set(CMAKE_DEBUG_TARGET_PROPERTIES TRUE)
add_compile_options(-fPIC)
add_definitions(-D_DEBUG)
add_definitions(-DDEBUG)
add_definitions("$ENV{CXXFLAGS} -O0 -Wall -g -ggdb")
SET(CMAKE_CXX_FLAGS_DEBUG "$ENV{CXXFLAGS} -O0 -Wall -g -ggdb")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -std=c++14")
message(CMAKE_CURRENT_LIST_DIR:${CMAKE_CURRENT_LIST_DIR})


## eSPDI dependencies ##
include_directories(
        ${CMAKE_CURRENT_LIST_DIR}/include/
        ${CMAKE_CURRENT_LIST_DIR}/eSPDI/
)

link_directories(
        ${CMAKE_CURRENT_LIST_DIR}/eSPDI/
)

set(CMAKE_INSTALL_RPATH $ORIGIN)
set(CMAKE_INSTALL_RPATH ${CMAKE_INSTALL_RPATH}:$ORIGIN/../eSPDI)
if(Support STREQUAL "default")
    message("system default path")
    find_package(OpenCV REQUIRED)
elseif(Support STREQUAL "gtk")
    message("support gtk")
    set(OPENCV_PATH ${CMAKE_CURRENT_LIST_DIR}/opencv_libs/opencv3.4.2_gtk)
    message(OPENCV_PATH:${OPENCV_PATH})
    find_package(OpenCV PATHS ${OPENCV_PATH} NO_DEFAULT_PATH REQUIRED)
    set(CMAKE_INSTALL_RPATH ${CMAKE_INSTALL_RPATH}:$ORIGIN/../opencv_libs/opencv3.4.2_gtk/lib/)
elseif(Support STREQUAL "opengl")
    message("support opengl")
    set(OPENCV_PATH ${CMAKE_CURRENT_LIST_DIR}/opencv_libs/opencv3.4.2_QT_GL)
    message(OPENCV_PATH:${OPENCV_PATH})
    find_package(OpenCV PATHS ${OPENCV_PATH} NO_DEFAULT_PATH REQUIRED)
    set(CMAKE_INSTALL_RPATH ${CMAKE_INSTALL_RPATH}:$ORIGIN/../opencv_libs/opencv3.4.2_QT_GL/lib/)
    add_definitions("-DSUPPORT_QT_OPENGL")
    find_package(OpenGL REQUIRED)
    find_package(GLUT REQUIRED)
    set(DEPENDENCIES ${OPENGL_LIBRARIES} ${GLUT_LIBRARY} ${DEPENDENCIES})
endif()
set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)
set(CMAKE_MODULE_PATH ${CMAKE_CURRENT_LIST_DIR}/cmake/modules)
message(CMAKE_MODULE_PATH:${CMAKE_MODULE_PATH})

# Third party
find_package(UDev REQUIRED)
message(UDEV_LIBRARIES:${UDEV_LIBRARIES} UDEV_INCLUDE_DIRS:${UDEV_INCLUDE_DIRS})
include_directories(${UDEV_INCLUDE_DIRS})
## ##

## source directory ##
message(DEPENDENCIES:${DEPENDENCIES})
aux_source_directory(src DIR_SRCS)
add_executable(eys3d_opencv ${DIR_SRCS} include/main.h)
target_link_libraries(eys3d_opencv unity_wrapper pthread ${UDEV_LIBRARIES} ${OpenCV_LIBS} ${DEPENDENCIES})

install(
        TARGETS eys3d_opencv
        RUNTIME DESTINATION ${CMAKE_CURRENT_LIST_DIR}/bin
)
