# Find the eYs3D/eSPDI includes and library
#
# This module defines:
#
#  eSPDI_FOUND        -- Set to true, if eSPDI found.
#  eSPDI_INCLUDE_DIRS -- Include directory for eSPDI headers.
#  eSPDI_LIBRARY      -- eSPDI library.
#  eSPDI_LIBS         -- eSPDI all libraries.
if(NOT PRO_DIR)
  set(PRO_DIR ${CMAKE_CURRENT_LIST_DIR}/..)
endif()

set(eSPDI_ROOT ${PRO_DIR}/eYs3D/eSPDI)

if(UNIX)
  set(eSPDI_INCLUDE_DIR ${eSPDI_ROOT}/linux/include)

  execute_process(COMMAND uname -m COMMAND tr -d '\n' OUTPUT_VARIABLE __arch)
  message(STATUS "__arch: ${__arch}")

  if(${__arch} STREQUAL "aarch64")
    set(eSPDI_LIB_DIR ${eSPDI_ROOT}/linux/aarch64)
    set(eSPDI_LIBRARY_NAME "eSPDI_NVIDIA_64")
  elseif(${__arch} STREQUAL "arm")
    set(eSPDI_LIB_DIR ${eSPDI_ROOT}/linux/armhf64)
  elseif(${__arch} STREQUAL "armv7l")
    set(eSPDI_LIB_DIR ${eSPDI_ROOT}/linux/arm32)
  else()
    set(eSPDI_LIB_DIR ${eSPDI_ROOT}/linux/x64)
    set(eSPDI_LIBRARY_NAME "eSPDI_X86_64")
  endif()
  set(CMAKE_FIND_LIBRARY_SUFFIXES ".so")
else()
  message(FATAL_ERROR "This platform not support now.")
endif()

find_path(eSPDI_INCLUDE_DIRS
  NAMES eSPDI.h
  PATHS ${eSPDI_INCLUDE_DIR} ${CMAKE_EXTRA_INCLUDES}
  NO_SYSTEM_PATH
)

find_library(eSPDI_LIBRARY
  NAMES ${eSPDI_LIBRARY_NAME}
  PATHS ${eSPDI_LIB_DIR} ${CMAKE_EXTRA_LIBRARIES}
  NO_SYSTEM_PATH
)
set(eSPDI_LIBS ${eSPDI_LIBRARY})

if(eSPDI_INCLUDE_DIRS AND eSPDI_LIBS)
  set(eSPDI_FOUND TRUE)
else()
  if(NOT eSPDI_FIND_QUIETLY)
    if(NOT eSPDI_INCLUDE_DIRS)
      message(STATUS "Unable to find eSPDI header files!")
    endif()
    if(NOT eSPDI_LIBS)
      message(STATUS "Unable to find eSPDI library files!")
    endif()
  endif()
endif()

if(eSPDI_FOUND)
  if(NOT eSPDI_FIND_QUIETLY)
    message(STATUS "Found components for eSPDI")
    message(STATUS "eSPDI_INCLUDE_DIRS: ${eSPDI_INCLUDE_DIRS}")
    message(STATUS "eSPDI_LIBS: ${eSPDI_LIBS}")
  endif()
else()
  if(eSPDI_FIND_REQUIRED)
    message(FATAL_ERROR "Could not find eSPDI!")
  endif()
endif()

mark_as_advanced(
  eSPDI_FOUND
  eSPDI_INCLUDE_DIRS
  eSPDI_LIBRARY
  eSPDI_LIBS
  eSPDI_LIB_DIR
)
