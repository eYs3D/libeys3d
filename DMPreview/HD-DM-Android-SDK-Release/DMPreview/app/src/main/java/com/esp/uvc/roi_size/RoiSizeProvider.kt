package com.esp.uvc.roi_size

interface RoiSizeProvider {

    fun getRoiSize(): RoiSize;
    fun storeRoiSize(size: RoiSize)
    fun clearRoiSize()
}