package com.esp.uvc.main.common

import com.esp.android.usb.camera.core.EtronCamera
import com.esp.android.usb.camera.core.EtronCamera.*
import java.text.SimpleDateFormat
import java.util.*

object CameraUtils {
    fun isIRSupported(productVersion: String): Boolean {
        return when (productVersion) {
            PRODUCT_VERSION_EX8029 -> true
            PRODUCT_VERSION_EX8036 -> true
            PRODUCT_VERSION_EX8037 -> true
            PRODUCT_VERSION_EX8038 -> true
            PRODUCT_VERSION_EX8059 -> true
            PRODUCT_VERSION_YX8059 -> true
            else -> false
        }
    }

    fun getIRMAXValue(etronCamera: EtronCamera): Int {
        var value: Int
        value = etronCamera.irMaxValue
        if (value == -1 && etronCamera.productVersion == PRODUCT_VERSION_EX8029) {
            value = 0x10
        }
        return value
    }

    fun getCurrentIRValue(etronCamera: EtronCamera): Int {
        return etronCamera.irCurrentValue
    }

    fun getDateTimeString(): String {
        val mDateTimeFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
        val now = GregorianCalendar()
        return mDateTimeFormat.format(now.time)
    }


    fun getZDTableValue(etronCamera: EtronCamera, depthHeight: Int, color: Int): IntArray {
        val mProductVersion = etronCamera.productVersion
        if (mProductVersion == PRODUCT_VERSION_EX8036) {
            if (!etronCamera.isUSB3 && color != 0 && depthHeight != 0 && (color % depthHeight != 0)) {
                // For mode 34 35 on PIF
                return etronCamera.getZDTableValue(2)
            }
            if (depthHeight == 720) {
                return etronCamera.zdTableValue
            } else if (depthHeight >= 480) {
                return etronCamera.getZDTableValue(1)
            }
            return etronCamera.zdTableValue
        }
        if (mProductVersion == PRODUCT_VERSION_EX8037) {
            return when {
                depthHeight >= 720 -> {
                    etronCamera.zdTableValue
                }
                depthHeight >= 480 -> {
                    etronCamera.getZDTableValue(1)
                }
                else -> {
                    etronCamera.zdTableValue
                }
            }
        } else {
            return if (!etronCamera.isUSB3 || depthHeight >= 720) {
                etronCamera.zdTableValue
            } else {
                etronCamera.getZDTableValue(1)
            }
        }
    }

    fun getMinIRValue(etronCamera: EtronCamera): Int {
        return etronCamera.irMinValue
    }


}