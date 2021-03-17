package com.esp.uvc.roi_size

import android.content.Context
import android.content.SharedPreferences

class RoiSizeSharedPrefsProvider(val context: Context): RoiSizeProvider {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)

    override fun getRoiSize(): RoiSize {
        val ordinal = sharedPrefs.getInt(roiSizeKey, RoiSize.Roi20.ordinal)
        return RoiSize.values()[ordinal]
    }

    override fun storeRoiSize(size: RoiSize) {
        sharedPrefs.edit().putInt(roiSizeKey, size.ordinal).apply()
    }

    override fun clearRoiSize() {
        sharedPrefs.edit().clear().apply()
    }

    companion object {
        const val sharedPrefsName = "RoiSizeSharedPreferences"
        const val roiSizeKey = "roiSize"
    }
}