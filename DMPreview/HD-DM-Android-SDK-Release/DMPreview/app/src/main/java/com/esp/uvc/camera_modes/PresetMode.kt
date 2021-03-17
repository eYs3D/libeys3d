package com.esp.uvc.camera_modes

import android.content.Context
import com.esp.uvc.utils.logd
import com.esp.uvc.utils.loge
import java.io.FileNotFoundException

// 7;L+R+D;1280x480_YUYV;640x480;null;null;11,14;30,60,90;30,60,90;3;0;null
// 2;L'+D interleave mode;1280x720_YUYV;1280x720;null;null;11,14;60;60;3;1;60
private const val CSV_MODE = 0
private const val CSV_DESCRIPTION = 1
private const val CSV_L_RESOLUTION = 2
private const val CSV_D_RESOLUTION = 3
private const val CSV_K_RESOLUTION = 4
private const val CSV_T_RESOLUTION = 5
private const val CSV_DEPTH_TYPES = 6
private const val CSV_COLOR_FPS = 7
private const val CSV_DEPTH_FPS = 8
private const val CSV_USB_TYPE = 9
private const val CSV_RECTIFY = 10
private const val CSV_INTERLEAVE_FPS = 11

data class PresetMode(
    val mode: String,
    val description: String,
    val lResolution: String?,
    val dResolution: String?,
    val kResolution: String?,
    val tResolution: String?,
    val depthTypes: List<Int>?,
    val colorFPS: List<Int>?,
    val depthFPS: List<Int>?,
    val usbType: String,
    val rectifyMode: Int,
    val interLeaveModeFPS: Int?
) {

    // For UI
    override fun toString(): String {
        return "Mode $mode ($description)"
    }

    companion object {

        fun getPresetMode(context: Context, identifier: String): ArrayList<PresetMode>? {
            val path = "CameraModes/camera_modes_$identifier.csv"
            logd("csv path : $path")
            return try {
                val lines = context.assets.open(path).bufferedReader().readLines()
                parseCSV(lines)
            } catch (e: FileNotFoundException) {
                loge("No preset found for the connected camera module")
                null
            }
        }

        fun getPresetMode(
            context: Context,
            identifier: String,
            usbType: String,
            lowSwitch: Boolean
        ): ArrayList<PresetMode>? {
            return if (lowSwitch) {
                getPresetMode(
                    context,
                    "${identifier}_l"
                )?.filter { it.usbType == usbType } as ArrayList<PresetMode>
            } else {
                getPresetMode(
                    context,
                    identifier
                )?.filter { it.usbType == usbType } as ArrayList<PresetMode>
            }
        }

        private fun parseCSV(lines: List<String>): ArrayList<PresetMode> {
            val presetModes = ArrayList<PresetMode>()
            val sNull = "null"
            for (line in lines) {
                val parameters = line.split(";")
                val mode = parameters[CSV_MODE]
                val description = parameters[CSV_DESCRIPTION]
                val lResolution =
                    if (parameters[CSV_L_RESOLUTION] == sNull) null else parameters[CSV_L_RESOLUTION]
                val dResolution =
                    if (parameters[CSV_D_RESOLUTION] == sNull) null else parameters[CSV_D_RESOLUTION]
                val kResolution =
                    if (parameters[CSV_K_RESOLUTION] == sNull) null else parameters[CSV_K_RESOLUTION]
                val tResolution =
                    if (parameters[CSV_T_RESOLUTION] == sNull) null else parameters[CSV_T_RESOLUTION]
                val depthTypes =
                    if (parameters[CSV_DEPTH_TYPES] == sNull) null else parameters[CSV_DEPTH_TYPES].split(
                        ","
                    ).map { it.toInt() }
                val colorFPS =
                    if (parameters[CSV_COLOR_FPS] == sNull) null else parameters[CSV_COLOR_FPS].split(
                        ","
                    )
                        .map { it.toInt() }
                val depthFPS =
                    if (parameters[CSV_DEPTH_FPS] == sNull) null else parameters[CSV_DEPTH_FPS].split(
                        ","
                    )
                        .map { it.toInt() }
                val usbType = parameters[CSV_USB_TYPE]
                val rectifyMode = parameters[CSV_RECTIFY].toInt()
                val interLeaveModeFPS =
                    if (parameters[CSV_INTERLEAVE_FPS] == sNull) null else parameters[CSV_INTERLEAVE_FPS].toInt()
                presetModes.add(
                    PresetMode(
                        mode,
                        description,
                        lResolution,
                        dResolution,
                        kResolution,
                        tResolution,
                        depthTypes,
                        colorFPS,
                        depthFPS,
                        usbType,
                        rectifyMode,
                        interLeaveModeFPS
                    )
                )
            }
            return presetModes
        }
    }
}