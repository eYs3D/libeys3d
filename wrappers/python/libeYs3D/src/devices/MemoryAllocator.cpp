/*
 * Copyright (C) 2015-2017 ICL/ITRI
 * All rights reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of ICL/ITRI and its suppliers, if any.
 * The intellectual and technical concepts contained
 * herein are proprietary to ICL/ITRI and its suppliers and
 * may be covered by Taiwan and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from ICL/ITRI.
 */

#include "devices/MemoryAllocator.h"
#include "devices/CameraDevice.h"
#include "debug.h"

#define LOG_TAG    "MemoryAllocator"

namespace libeYs3D {
namespace devices    {

void* MemoryAllocator__allocate(CameraDevice *cameraDevice, size_t size)    {
    LOG_INFO(LOG_TAG, "Allocating memory of size(%" PRIu32 "), mCameraDevice(%p)",
            (uint32_t)size, cameraDevice);

    if(cameraDevice == nullptr)    return nullptr;

#ifdef DEVICE_MEMORY_ALLOCATOR
    return cameraDevice->requestMemory(size);
#else
    return nullptr;
#endif
}

void MemoryAllocator__deallocate(CameraDevice *cameraDevice, void *p, size_t size)    {
    LOG_INFO(LOG_TAG, "De-allocating memory(%p) of size(%" PRIu32 "), mCameraDevice(%p)",
             p, (uint32_t)size, cameraDevice);
    
    if(cameraDevice == nullptr)    return;

#ifdef DEVICE_MEMORY_ALLOCATOR
    cameraDevice->returnMemory(p, size);
#endif
}
    
static int sting = 0;
size_t MemoryAllocator__max_size(CameraDevice *cameraDevice)    {
    if(cameraDevice == nullptr)    return 0;

    return (cameraDevice->mColorWidth * cameraDevice->mColorHeight) << 2;
}

} // namespace devices
} // namespace libeYs3D
