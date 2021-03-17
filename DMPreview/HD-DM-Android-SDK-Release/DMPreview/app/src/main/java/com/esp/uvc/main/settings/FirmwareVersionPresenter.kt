package com.esp.uvc.main.settings

import android.content.Context
import android.hardware.usb.UsbDevice
import com.esp.android.usb.camera.core.EtronCamera
import com.esp.android.usb.camera.core.EtronCamera.EYS_OK
import com.esp.android.usb.camera.core.USBMonitor
import com.esp.android.usb.camera.core.USBMonitor.OnDeviceConnectListener
import com.esp.uvc.R
import com.esp.uvc.camera_modes.CameraModeManager
import com.esp.uvc.utils.loge
import com.esp.uvc.utils.logi
import com.esp.uvc.utils.toggleTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FirmwareVersionPresenter(private val context: Context) : FirmwareVersionContract.Presenter {
    private var canNavigateFlag: Boolean = true
    private var usbMonitor: USBMonitor? = null
    private var etronCamera: EtronCamera? = null

    private val UNKNOWN_VALUE = context.getString(R.string.firmware_version_value_unknown)

    override fun attach(view: FirmwareVersionContract.View) {
        this.view = view
    }

    override fun onStart() {
        usbMonitor = USBMonitor(context, usbDeviceListener)
        usbMonitor?.register()
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onStop() {
        usbMonitor?.unregister()
        usbMonitor?.destroy()
        usbMonitor = null
        etronCamera?.destroy()
        etronCamera = null
    }

    override fun unattach() {
        etronCamera?.destroy()
        etronCamera = null
        usbMonitor?.destroy()
        usbMonitor = null
    }

    override fun canNavigate(): Boolean {
        return canNavigateFlag
    }

    private val usbDeviceListener: OnDeviceConnectListener = object : OnDeviceConnectListener {

        override fun onAttach(device: UsbDevice?) {
            logi("usb device attached: $device")
            if (device != null && CameraModeManager.SUPPORTED_PID_LIST.contains(device.productId)) {
                usbMonitor!!.requestPermission(device)
            } else {
                usbMonitor!!.deviceList.forEach {
                    if (CameraModeManager.SUPPORTED_PID_LIST.contains(it!!.productId)) {
                        usbMonitor!!.requestPermission(it)
                    }
                }
            }
        }

        override fun onConnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?, createNew: Boolean) {
            if(ctrlBlock!!.isIMU)
                return
            GlobalScope.launch(Dispatchers.IO) {
                ::canNavigateFlag.toggleTimeout()
                if (etronCamera != null) {
                    loge("Camera exists, not re-creating")
                    return@launch
                } else {
                    etronCamera = EtronCamera()
                    //open camera
                    if (etronCamera!!.open(ctrlBlock!!) != EYS_OK) {
                        view.showSnack(context.getString(R.string.camera_error_io)) {}
                        etronCamera = null
                        readValues()
                        canNavigateFlag = true
                        return@launch //jump out as we cannot continue no camera to work with
                    }
                    readValues()
                }
            }
        }

        override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
            logi("usb device disconnected: device is null? ${device == null}")
            if (device == null) {
                return
            }
            if (etronCamera != null && device == etronCamera!!.device) {
                //todo cleanup device & screen
                etronCamera!!.close()
                etronCamera!!.destroy()
                etronCamera = null
            }
            canNavigateFlag = true
            readValues()
        }

        override fun onDetach(device: UsbDevice?) = view.toast(context.getString(R.string.usb_device_detached))
        override fun onCancel() = logi("usb device - onCancel")
    }

    override lateinit var view: FirmwareVersionContract.View

    private fun readValues() {
        view.displayFirmwareVersion(etronCamera?.fwVersionValue ?: UNKNOWN_VALUE)
        val pid = etronCamera?.pidValue
        view.displayProductId(if (pid != null) "0x$pid" else UNKNOWN_VALUE)
        val vid = etronCamera?.vidValue
        view.displayVendorId(if (vid != null) "0x$vid" else UNKNOWN_VALUE)
        view.displaySerialNumber(etronCamera?.serialNumberValue ?: UNKNOWN_VALUE)
    }
}