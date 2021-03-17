package com.esp.uvc.camera_modes

import android.content.Context
import com.esp.uvc.utils.loge
import java.lang.Exception

// Integration => CameraMode / PresetMode / DepthRange
object CameraModeManager {

    // 8036, 8037, 8059, 8062, 8062 IMU
    val SUPPORTED_PID_LIST = intArrayOf(0x0120, 0x0121, 0x0146, 0x0162, 0x0163)

    fun getDefaultMode(pid: Int, isUsb3: Boolean, lowSwitch: Boolean): CameraMode? {
        return CameraMode.getDefaultMode(
            pidToIdentifier(pid),
            isUsb3,
            lowSwitch
        )
    }

    fun getPresetMode(
        context: Context,
        pid: Int,
        usbType: String,
        lowSwitch: Boolean
    ): ArrayList<PresetMode>? {
        return PresetMode.getPresetMode(context, pidToIdentifier(pid), usbType, lowSwitch)
    }

    fun isMatchInterLeaveMode(
        context: Context,
        pid: Int,
        usbType: String,
        lResolution: String,
        colorFPS: Int,
        dResolution: String,
        depthFPS: Int,
        lowSwitch: Boolean
    ): Boolean {
        return try {
            getPresetMode(
                context,
                pid,
                usbType,
                lowSwitch
            )?.first {
                it.lResolution?.contains(lResolution) ?: false && it.interLeaveModeFPS == colorFPS &&
                        it.dResolution?.contains(dResolution) ?: false && it.interLeaveModeFPS == depthFPS
            }
            true
        } catch (e: Exception) {
            loge("isMatchInterLeaveMode e : $e")
            false
        }
    }

    fun isSupportedInterLeaveMode(
        context: Context,
        pid: Int,
        usbType: String,
        lowSwitch: Boolean
    ): Boolean {
        return try {
            getPresetMode(
                context,
                pid,
                usbType,
                lowSwitch
            )?.first { it.interLeaveModeFPS != null }
            true
        } catch (e: Exception) {
            loge("isSupportedInterLeaveMode e : $e")
            false
        }
    }

    fun getDepthRanges(serialNumber: String): List<DepthRange> {
        return DepthRange.getDepthRanges(serialNumber)
    }

    private fun pidToIdentifier(pid: Int): String {
        return when (pid) {
            0x0120 -> "8036"
            0x0121 -> "8037"
            0x0146 -> "8059"
            0x0162 -> "8062"
            else -> "-1"
        }
    }
}