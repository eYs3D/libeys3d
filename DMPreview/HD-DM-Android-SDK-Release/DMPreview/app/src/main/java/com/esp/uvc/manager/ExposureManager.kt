package com.esp.uvc.manager

import com.esp.android.usb.camera.core.EtronCamera
import com.esp.android.usb.camera.core.EtronCamera.*
import com.esp.uvc.application.AndroidApplication
import com.esp.uvc.usbcamera.AppSettings

private const val MAX_EXPOSURE_ABSOLUTE_TIME = 3
private const val MIN_EXPOSURE_ABSOLUTE_TIME = -13

private const val DEFAULT_AUTO_EXPOSURE = EXPOSURE_MODE_AUTO_APERTURE
private const val DEFAULT_EXPOSURE_ABSOLUTE_TIME = -13

object ExposureManager {

    const val INDEX_AUTO_EXPOSURE = 0
    const val INDEX_EXPOSURE_ABSOLUTE_TIME = 1

    private val mAppSettings by lazy { AppSettings.getInstance(AndroidApplication.applicationContext()) }

    /**
     * Auto Exposure
     **/

    fun getAE(etronCamera: EtronCamera?): Int {
        return etronCamera?.exposureMode ?: EYS_ERROR
    }

    fun getAEBySharedPrefs(): Int {
        return mAppSettings.get(AppSettings.KEY_AUTO_EXPOSURE, EYS_ERROR)
    }

    fun isAE(etronCamera: EtronCamera?): Boolean {
        return getAE(etronCamera) == EXPOSURE_MODE_AUTO_APERTURE
    }

    fun setAE(etronCamera: EtronCamera?, enabled: Boolean): Int {
        val value = if (enabled) EXPOSURE_MODE_AUTO_APERTURE else EXPOSURE_MODE_MANUAL
        return etronCamera?.setExposureMode(value) ?: EYS_ERROR
    }

    fun setAEBySharedPrefs(etronCamera: EtronCamera?) {
        val ae = getAEBySharedPrefs()
        if (ae >= 0) {
            if (ae != getAE(etronCamera)) {
                setAE(etronCamera, ae == EXPOSURE_MODE_AUTO_APERTURE)
            }
            if (ae == EXPOSURE_MODE_MANUAL) {
                setExposureAbsoluteTimeBySharedPrefs(etronCamera)
            }
        }
    }

    /**
     * Exposure
     **/

    fun getExposureAbsoluteTime(etronCamera: EtronCamera?): Int {
        return etronCamera?.exposureAbsoluteTime ?: DEVICE_FIND_FAIL
    }

    fun getExposureAbsoluteTimeBySharedPrefs(): Int {
        return mAppSettings.get(AppSettings.KEY_EXPOSURE_ABSOLUTE_TIME, DEVICE_FIND_FAIL)
    }

    fun setExposureAbsoluteTime(etronCamera: EtronCamera?, current: Int): Int {
        return etronCamera?.setExposureAbsoluteTime(current) ?: EYS_ERROR
    }

    fun setExposureAbsoluteTimeBySharedPrefs(etronCamera: EtronCamera?, force: Boolean = false) {
        val time =
            getExposureAbsoluteTimeBySharedPrefs()
        if (force || (time != DEVICE_FIND_FAIL && time != DEVICE_NOT_SUPPORT && time != getExposureAbsoluteTime(
                etronCamera
            ))
        ) {
            setExposureAbsoluteTime(etronCamera, time)
        }
    }

    fun getExposureAbsoluteTimeLimit(): IntArray {
        val intArray = IntArray(2)
        intArray[0] = MIN_EXPOSURE_ABSOLUTE_TIME
        intArray[1] = MAX_EXPOSURE_ABSOLUTE_TIME
        return intArray
    }


    /**
     * Common
     **/

    fun setSharedPrefs(index: Int, value: Any) {
        when (index) {
            INDEX_AUTO_EXPOSURE -> mAppSettings.put(AppSettings.KEY_AUTO_EXPOSURE, value)
            INDEX_EXPOSURE_ABSOLUTE_TIME -> mAppSettings.put(
                AppSettings.KEY_EXPOSURE_ABSOLUTE_TIME,
                value
            )
        }
        mAppSettings.saveAll()
    }

    fun setupSharedPrefs(etronCamera: EtronCamera?) {
        mAppSettings.put(AppSettings.KEY_AUTO_EXPOSURE, getAE(etronCamera))
        // Avoid after setting EXPOSURE_ABSOLUTE_TIME by SensorSettingsActivity and the ae is on, then go preview and back to SensorSettingsActivity
        if (getExposureAbsoluteTimeBySharedPrefs() == DEVICE_FIND_FAIL) {
            mAppSettings.put(
                AppSettings.KEY_EXPOSURE_ABSOLUTE_TIME,
                getExposureAbsoluteTime(etronCamera)
            )
        }
        mAppSettings.saveAll()
    }

    fun defaultSharedPrefs(): IntArray {
        var ae = getAEBySharedPrefs()
        if (ae >= 0) {
            mAppSettings.put(AppSettings.KEY_AUTO_EXPOSURE, DEFAULT_AUTO_EXPOSURE)
            ae = DEFAULT_AUTO_EXPOSURE
        }
        var time = getExposureAbsoluteTimeBySharedPrefs()
        if (time != DEVICE_FIND_FAIL && time != DEVICE_NOT_SUPPORT) {
            mAppSettings.put(AppSettings.KEY_EXPOSURE_ABSOLUTE_TIME, DEFAULT_EXPOSURE_ABSOLUTE_TIME)
            time = DEFAULT_EXPOSURE_ABSOLUTE_TIME
        }
        mAppSettings.saveAll()
        val result = IntArray(2)
        result[INDEX_AUTO_EXPOSURE] = ae
        result[INDEX_EXPOSURE_ABSOLUTE_TIME] = time
        return result
    }
}