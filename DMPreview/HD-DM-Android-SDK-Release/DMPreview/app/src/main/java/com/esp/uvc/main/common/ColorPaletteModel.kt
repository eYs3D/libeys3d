package com.esp.uvc.main.common

import com.esp.android.usb.camera.core.EtronCamera
import com.esp.android.usb.camera.core.EtronCamera.DistanceLimit
import androidx.annotation.NonNull
import com.esp.uvc.utils.loge

class ColorPaletteModel : IColorPaletteModel {
    val DEFAULT_Z_FARTHEST_MM = 1000
    val IMPOSSIBLE_Z_MM = -1
    private var mDefaultZFarthest: Int = 1000
    private var mDefaultZNearest: Int = 0
    var mCurrentZNearest: Int = 0
    var mCurrentZFarthest: Int = 0
    var mCamera: EtronCamera? = null

    fun init(@NonNull etronCamera: EtronCamera) {
        mCamera = etronCamera
        val currentLimits: DistanceLimit = etronCamera.getDistanceLimitInZDTable()
        mDefaultZNearest = currentLimits.nearest.toInt()
        mCurrentZNearest = mDefaultZNearest

        mDefaultZFarthest = currentLimits.farthest.toInt()
        mCurrentZFarthest = DEFAULT_Z_FARTHEST_MM
    }

    override fun setZDistance(zNear: Int, zFar: Int) {
        mCamera?.setDistanceFilter(zNear, zFar)
        val currentLimits: DistanceLimit? = mCamera?.getDistanceLimit()
        mCurrentZNearest = currentLimits?.nearest?.toInt() ?: IMPOSSIBLE_Z_MM
        mCurrentZFarthest = currentLimits?.farthest?.toInt() ?: IMPOSSIBLE_Z_MM
        loge("[esp_palette_md] setZDistance $mCurrentZNearest $mCurrentZFarthest")
    }

    override fun resetZDistance() {
        loge("[esp_palette_md] resetZDistance")
        mCurrentZNearest = mDefaultZNearest
        mCurrentZFarthest = DEFAULT_Z_FARTHEST_MM
        mCamera?.setDistanceFilter(mCurrentZNearest, mCurrentZFarthest)
    }

    override fun enableRegionAccuracy(enabled: Boolean) {
        if (enabled) {
            mCamera?.setDistanceFilter(1, 16384)
        } else {
            mCamera?.setDistanceFilter(mCurrentZNearest, mCurrentZFarthest)
        }
    }
}