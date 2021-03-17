package com.esp.uvc.main.settings.module_sync

import android.hardware.usb.UsbDevice
import android.view.Surface
import com.esp.android.usb.camera.core.*
import com.esp.uvc.camera_modes.CameraModeManager
import com.esp.uvc.utils.logd
import com.esp.uvc.utils.loge
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

private const val FPS_THROTTLE = 500L

class MModuleSync(p: IModuleSync.Presenter, usbMonitor: USBMonitor) : IModuleSync.Model {

    private val mPresenter = p
    private val mUsbMonitor = usbMonitor

    // Support two devices now
    private var mCameras = Array<EtronCamera?>(2) { null }

    private var mMasterIndex = -1

    private var mTimer: Timer? = null // 30 sec reset

    override fun registerUsbMonitor() {
        mUsbMonitor.register()
    }

    override fun unregisterUsbMonitor() {
        mUsbMonitor.unregister()
    }

    override fun destroy() {
        mTimer?.cancel()
        mTimer = null
        for (i in mCameras.indices) {
            mCameras[i]?.closeIMU()
        }
        for (i in mCameras.indices) {
            mCameras[i]?.destroy()
            mCameras[i] = null
            mPresenter.onFpsSN(i, -1.0, -1.0, -1)
        }
    }

    override fun onDeviceAttach(device: UsbDevice?) {
        mUsbMonitor.deviceList.forEach {
            logd("onDeviceAttach : ${it.deviceName}, ${it.productName}")
            mUsbMonitor.requestPermission(it)
        }
    }

    override fun onDeviceDetach(device: UsbDevice?) {
        // close IMU first
        for (i in mCameras.indices) {
            if (mCameras[i]?.getDevice(false) == device ||
                mCameras[i]?.getDevice(true) == device
            ) {
                mCameras[i]?.closeIMU()
            }
        }
        for (i in mCameras.indices) {
            if (mCameras[i]?.getDevice(false) == device ||
                mCameras[i]?.getDevice(true) == device
            ) {
                mCameras[i]?.destroy()
                mCameras[i] = null
                mPresenter.onFpsSN(i, -1.0, -1.0, -1)
            }
        }
    }

    override fun onDeviceConnect(
        device: UsbDevice?,
        ctrlBlock: USBMonitor.UsbControlBlock?,
        createNew: Boolean
    ) {
        if (!createNew) {
            return
        }
        val tmpCamera: EtronCamera?
        var index = getCameraIndex(device!!, ctrlBlock!!)
        if (index == -1) {
            tmpCamera = EtronCamera()
            index = mCameras.indexOfFirst { it == null }
            if (index == -1) {
                loge("USB parser error")
            }
            mCameras[index] = tmpCamera
        } else {
            tmpCamera = mCameras[index]
        }
        if (tmpCamera!!.open(ctrlBlock) != EtronCamera.EYS_OK) {
            return
        } else {
            if (ctrlBlock.isIMU) {
                mPresenter.onIMUConnected(index)
                tmpCamera.enableIMUDataOutput(true)
                tmpCamera.readIMUData(mIMUDataCallbacks[index])
            } else {
                tmpCamera.setModuleSync() // frame count to serial count
                val defaultMode = CameraModeManager.getDefaultMode(
                    tmpCamera.pid,
                    isUsb3 = true,
                    lowSwitch = false
                ) // Low switch not support sync now
                tmpCamera.videoMode = defaultMode!!.videoMode
                tmpCamera.getStreamInfoList(EtronCamera.INTERFACE_NUMBER_COLOR).forEach { info ->
                    if (info.height == defaultMode.rgbCameraState!!.resolution.height && info.width == defaultMode.rgbCameraState!!.resolution.width && info.bIsFormatMJPEG == defaultMode.rgbCameraState!!.isMJPEG)
                        tmpCamera.setPreviewSize(info, defaultMode.rgbCameraState!!.fps)
                }
                val surface = Surface(mPresenter.getTextureView(index).surfaceTexture)
                tmpCamera.setPreviewDisplay(surface, UVCCamera.CAMERA_COLOR)
                tmpCamera.setMonitorFrameRate(true, UVCCamera.CAMERA_COLOR)
                tmpCamera.setFrameCallback(
                    mFrameCallbacks[index],
                    EtronCamera.PIXEL_FORMAT_RGBX,
                    UVCCamera.CAMERA_COLOR
                )
                tmpCamera.startPreview(UVCCamera.CAMERA_COLOR)
            }
        }
    }

    override fun onDeviceDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
        for (i in mCameras.indices) {
            if (mCameras[i]?.getDevice(false) == device ||
                mCameras[i]?.getDevice(true) == device
            ) {
                mCameras[i]?.closeIMU()
            }
        }
        for (i in mCameras.indices) {
            if (mCameras[i]?.getDevice(false) == device ||
                mCameras[i]?.getDevice(true) == device
            ) {
                mCameras[i]?.destroy()
                mCameras[i] = null
                mPresenter.onFpsSN(i, -1.0, -1.0, -1)
            }
        }
    }

    override fun onResetClick(index: Int) {
        mMasterIndex = index
        setRestRegister(index)
        mTimer?.cancel()
        mTimer = null
        mTimer = Timer()
        mTimer!!.schedule(object : TimerTask() {

            override fun run() {
                setRestRegister(mMasterIndex)
            }

        }, 30000, 30000)
    }

    private fun getCameraIndex(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock): Int {
        mCameras.forEachIndexed { i, etronCamera ->
            if (etronCamera != null) {
                val cameraDevice = etronCamera.getDevice(false)
                val hidDevice = etronCamera.getDevice(true)
                if (ctrlBlock.isIMU && hidDevice == null) {
                    if (cameraDevice.deviceName.split("/")[5] == device.deviceName.split("/")[5]) {
                        return i
                    }
                } else if (!ctrlBlock.isIMU && cameraDevice == null) {
                    if (hidDevice.deviceName.split("/")[5] == device.deviceName.split("/")[5]) {
                        return i
                    }
                }
            }
        }
        return -1
    }

    private fun setRestRegister(index: Int) {
        if (mCameras[index] == null)
            return
        val tmp = arrayOfNulls<String>(1)
        mCameras[index]!!.getHWRegisterValue(tmp, 0xF079)
        val wE079 = tmp[0]!!.toInt(16).and(0xFC)
        if (mCameras[index]!!.setHWRegisterValue(0xF079, wE079) == 0) {
            if (mCameras[index]!!.setHWRegisterValue(0xF079, wE079 + 1) == 0) {
                return
            }
        }
        mPresenter.onResetFail()
    }

    private var mIFrameCallbackOne = IFrameCallback { _, frameCount ->
        GlobalScope.launch {
            val rgbFPS = mCameras[0]?.getCurrentFrameRate(UVCCamera.CAMERA_COLOR)
            if (rgbFPS != null) {
                mPresenter.onFpsSN(
                    0,
                    rgbFPS.mFrameRatePreview,
                    rgbFPS.mFrameRateUvc,
                    frameCount
                )
            }
            delay(FPS_THROTTLE)
        }
    }

    private var mIFrameCallbackTwo = IFrameCallback { _, frameCount ->
        GlobalScope.launch {
            val rgbFPS = mCameras[1]?.getCurrentFrameRate(UVCCamera.CAMERA_COLOR)
            if (rgbFPS != null) {
                mPresenter.onFpsSN(
                    1,
                    rgbFPS.mFrameRatePreview,
                    rgbFPS.mFrameRateUvc,
                    frameCount
                )
            }
            delay(FPS_THROTTLE)
        }
    }

    private val mIMUDataCallbackOne = object : IIMUCallback {

        override fun onData(data: IMUData?) {
            mPresenter.onIMUData(0, data!!)
        }

        override fun onCalibration(isSuccess: Boolean) {
        }

    }

    private val mIMUDataCallbackTwo = object : IIMUCallback {

        override fun onData(data: IMUData?) {
            mPresenter.onIMUData(1, data!!)
        }

        override fun onCalibration(isSuccess: Boolean) {
        }

    }

    // Need declare below instance
    private val mFrameCallbacks = arrayOf(mIFrameCallbackOne, mIFrameCallbackTwo)
    private val mIMUDataCallbacks = arrayOf(mIMUDataCallbackOne, mIMUDataCallbackTwo)
}