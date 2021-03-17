package com.esp.uvc.manager

import android.content.Context
import androidx.annotation.NonNull
import com.esp.android.usb.camera.core.EtronCamera
import com.esp.android.usb.camera.core.EtronCamera.*
import com.esp.uvc.usbcamera.AppSettings
import com.esp.uvc.utils.loge
import com.esp.uvc.utils.logi

class IRManager(context: Context) {

    private val DEBUG_IR = false
    private val IR_UNINITIALIZED_VALUE = -2
    private val IR_MIN_VALUE = 0
    private val IR_DEF_VALUE = 3
    private val IR_DEF_MAX_VALUE = 6
    private val IR_EXT_MAX_VALUE = 15
    private val IRMAX_NOT_SUP_CTRL = 0xff

    var appSettings = AppSettings.getInstance(context)!!
    var irMin = IR_UNINITIALIZED_VALUE
    var irMax = IR_UNINITIALIZED_VALUE
    var irCurrentVal = IR_UNINITIALIZED_VALUE
    var irExtendedFlag = false

    /**
     * Return if setting firmware registers successfully
     */
    fun setIrCurrentVal(etronCamera: EtronCamera?, value: Int): Boolean {
        return if (etronCamera?.setIRCurrentValue(value) != EYS_ERROR) {
            logi("[ir_ext] IRManager setIrCurrentVal $value")
            irCurrentVal = value
            dump()
            true
        } else {
            logi("[ir_ext] IRManager setIrCurrentVal failed $value")
            dump()
            false
        }
    }

    /**
     * Return if setting firmware registers successfully
     */
    fun setIRExtension(etronCamera: EtronCamera?, isEnable: Boolean): Boolean {
        if (etronCamera == null) {
            loge("[ir_ext] err camera null failing setting ir extension")
            dump()
            return false
        }

        val currentIrMax = etronCamera.irMaxValue
        if (currentIrMax != IRMAX_NOT_SUP_CTRL) {
            val result: Int = if (isEnable) {
                loge("[ir_ext] setIRExtension 15")
                etronCamera.setIRMaxValue(IR_EXT_MAX_VALUE)
            } else {
                loge("[ir_ext] setIRExtension 6")
                etronCamera.setIRMaxValue(IR_DEF_MAX_VALUE)
            }

            if (result != EYS_ERROR) {
                irMax = if (isEnable) IR_EXT_MAX_VALUE
                else IR_DEF_MAX_VALUE
                irExtendedFlag = isEnable
                dump()
                return true
            } else {
                loge("[ir_ext] err camera setIRExtension EYS_ERROR")
                dump()
                return false
            }
        }
        loge("[ir_ext] err camera not support IR control")
        dump()
        return false
    }

    fun initIR(@NonNull etronCamera: EtronCamera, isFirstLaunch: Boolean) {
        loge("[ir_ext] initIR firstLaunch $isFirstLaunch")
        if (isFirstLaunch) {
            setIrCurrentVal(etronCamera, IR_DEF_VALUE)
            if (!setIRExtension(etronCamera, false)) {
                loge("[ir_ext] startCameraViaDefaults set false failed")
            }
            irMin = etronCamera.irMinValue
            irMax = etronCamera.irMaxValue
            dump()
            return
        }

        readIRFromSharePref(etronCamera)
        dump()
    }

    fun clear() {
        irMin = IR_UNINITIALIZED_VALUE
        irMax = IR_UNINITIALIZED_VALUE
        irCurrentVal = IR_UNINITIALIZED_VALUE
        irExtendedFlag = false
        loge("[ir_ext] cleared!!")
    }

    private fun readIRFromSharePref(etronCamera: EtronCamera?) {
        loge("[ir_ext] readIRFromSharePref")
        val isIRExtendSharedPref = appSettings.get(AppSettings.IR_EXTENDED, irExtendedFlag)
        setIRExtension(etronCamera, isIRExtendSharedPref)

        val irValueSharedPref = appSettings.get(AppSettings.IR_VALUE, irCurrentVal)
        if (irCurrentVal != irValueSharedPref) {
            setIrCurrentVal(etronCamera, irValueSharedPref)
        }

        irMin = appSettings.get(AppSettings.IR_MIN, irMin)
        dump()
    }

    fun saveSharedPref() {
        appSettings.put(AppSettings.IR_VALUE, irCurrentVal)
        appSettings.put(AppSettings.IR_MIN, irMin)
        appSettings.put(AppSettings.IR_MAX, irMax)
        appSettings.put(AppSettings.IR_EXTENDED, irExtendedFlag)
        loge("[ir_ext] IR_EXTENDED @0 $irExtendedFlag")
        dump()
    }

    fun dump() {
        if (!DEBUG_IR) return
        if (irMin < 0 || irMax < 0 || irCurrentVal < 0) {
            loge("[ir_ext] dump err $irExtendedFlag")
        }
        loge("[ir_ext] IR: $irMin, $irMax, $irCurrentVal, $irExtendedFlag")
    }

    fun validIR(): Boolean {
        return irMax >= 0 && irMin >= 0 && irMax >= irMin && irCurrentVal in irMin..irMax
    }

}