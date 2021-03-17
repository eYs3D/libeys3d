package com.esp.uvc.camera_modes

import com.esp.uvc.utils.loge

data class DepthRange(val range: String, val distance: Int) {

    companion object {

        const val NEAR = 0
        const val MID = 1
        const val FAR = 2

        val DEPTH_RANGES =
            arrayListOf(
                DepthRange("NEAR", 30),
                DepthRange("MID", 60),
                DepthRange("FAR", 150)
            )

        fun getDepthRanges(serialNumber: String): List<DepthRange> {
            return if (!serialNumber.startsWith("8038")) {
                loge("Depth range selection not supported for this camera")
                ArrayList()
            } else {
                DEPTH_RANGES
            }
        }
    }
}