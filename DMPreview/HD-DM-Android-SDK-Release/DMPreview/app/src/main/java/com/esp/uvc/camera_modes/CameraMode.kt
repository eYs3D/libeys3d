package com.esp.uvc.camera_modes

import android.util.Size
import com.esp.android.usb.camera.core.EtronCamera

private val DEFAULT_EX8036_U2_MODE = CameraMode(
    8036,
    false,
    19,
    EtronCamera.VideoMode.RECTIFY_8_BITS,
    CameraState(false, Size(640, 480), 15),
    CameraState(false, Size(320, 480), 15)
)

private val DEFAULT_EX8036_U3_MODE = CameraMode(
    8036,
    true,
    2,
    EtronCamera.VideoMode.RECTIFY_11_BITS,
    CameraState(false, Size(640, 480), 30),
    CameraState(false, Size(640, 480), 30)
)

private val DEFAULT_EX8036_L_U2_MODE = CameraMode(
    8036,
    false,
    36,
    EtronCamera.VideoMode.RECTIFY_8_BITS,
    CameraState(false, Size(640, 480), 15),
    CameraState(false, Size(320, 480), 15)
)

private val DEFAULT_EX8036_L_U3_MODE = CameraMode(
    8036,
    true,
    6,
    EtronCamera.VideoMode.RECTIFY_11_BITS,
    CameraState(false, Size(640, 480), 30),
    CameraState(false, Size(640, 480), 30)
)

private val DEFAULT_EX8037_U2_MODE = CameraMode(
    8037,
    false,
    31,
    EtronCamera.VideoMode.RECTIFY_8_BITS,
    CameraState(true, Size(1280, 720), 10),
    CameraState(false, Size(640, 720), 10)
)

private val DEFAULT_EX8059_U2_MODE = CameraMode(
    8059,
    false,
    9,
    EtronCamera.VideoMode.RECTIFY_11_BITS_INTERLEAVE_MODE,
    CameraState(true, Size(1280, 720), 24),
    CameraState(false, Size(640, 360), 24)
)

private val DEFAULT_EX8059_U3_MODE = CameraMode(
    8059,
    true,
    6,
    EtronCamera.VideoMode.RECTIFY_11_BITS,
    CameraState(false, Size(640, 400), 60),
    CameraState(false, Size(640, 400), 60)
)

private val DEFAULT_YX8062_U2_MODE = CameraMode(
    8062,
    false,
    6,
    EtronCamera.VideoMode.RECTIFY_11_BITS,
    CameraState(true, Size(1280, 720), 30),
    CameraState(false, Size(640, 360), 30)
)

private val DEFAULT_YX8062_U3_MODE = CameraMode(
    8062,
    true,
    3,
    EtronCamera.VideoMode.RECTIFY_11_BITS,
    CameraState(false, Size(1280, 720), 30),
    CameraState(false, Size(1280, 720), 30)
)

//    CameraDefaults(8038, 1,Size(1280, 720), Size(1280, 720), DEPTH_DATA_11_BITS.toInt(), 10, 10, 1, -1, listOf(CameraType.RGB,  CameraType.DEPTH)))

private val DEFAULT_CAMERA_MODES = arrayListOf(
    DEFAULT_EX8036_U2_MODE,
    DEFAULT_EX8036_U3_MODE,
    DEFAULT_EX8036_L_U2_MODE,
    DEFAULT_EX8036_L_U3_MODE,
    DEFAULT_EX8037_U2_MODE,
    DEFAULT_EX8059_U2_MODE,
    DEFAULT_EX8059_U3_MODE,
    DEFAULT_YX8062_U2_MODE,
    DEFAULT_YX8062_U3_MODE
)

data class CameraMode(
    val identifier: Int,
    val isUsb3: Boolean,
    val mode: Int,
    var videoMode: Int,
    var rgbCameraState: CameraState?,
    var depthCameraState: CameraState?
) {

    companion object {

        fun getDefaultMode(
            identifier: String,
            isUsb3: Boolean,
            lowSwitch: Boolean
        ): CameraMode? {
            return if (lowSwitch) {
                generateNewMode(
                    DEFAULT_CAMERA_MODES.lastOrNull {
                        identifier.contains(it.identifier.toString()) && isUsb3 == it.isUsb3
                    })
            } else {
                generateNewMode(
                    DEFAULT_CAMERA_MODES.firstOrNull {
                        identifier.contains(it.identifier.toString()) && isUsb3 == it.isUsb3
                    })
            }
        }

        // Avoid reference
        private fun generateNewMode(cameraMode: CameraMode?): CameraMode? {
            if (cameraMode == null)
                return null
            return CameraMode(
                cameraMode.identifier,
                cameraMode.isUsb3,
                cameraMode.mode,
                cameraMode.videoMode,
                CameraState(
                    cameraMode.rgbCameraState!!.isMJPEG,
                    Size(
                        cameraMode.rgbCameraState!!.resolution.width,
                        cameraMode.rgbCameraState!!.resolution.height
                    ),
                    cameraMode.rgbCameraState!!.fps
                ),
                CameraState(
                    cameraMode.depthCameraState!!.isMJPEG,
                    Size(
                        cameraMode.depthCameraState!!.resolution.width,
                        cameraMode.depthCameraState!!.resolution.height
                    ),
                    cameraMode.depthCameraState!!.fps
                )
            )
        }
    }
}

data class CameraState(
    var isMJPEG: Boolean,
    var resolution: Size,
    val fps: Int
) {
    fun getAspectRatio(): Double {
        return resolution.width.toDouble() / resolution.height.toDouble()
    }
}