package com.esp.uvc.main.common

interface IColorPaletteModel {
    fun setZDistance(zNear: Int, zFar: Int)
    fun resetZDistance()
    fun enableRegionAccuracy(enabled: Boolean)
}