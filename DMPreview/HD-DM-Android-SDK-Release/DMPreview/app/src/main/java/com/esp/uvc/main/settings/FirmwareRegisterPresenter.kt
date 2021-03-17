package com.esp.uvc.main.settings

import android.app.Activity
import android.content.Context
import android.hardware.usb.UsbDevice
import com.esp.android.usb.camera.core.EtronCamera
import com.esp.android.usb.camera.core.EtronCamera.*
import com.esp.android.usb.camera.core.USBMonitor
import com.esp.uvc.R
import com.esp.uvc.camera_modes.CameraModeManager
import com.esp.uvc.old.usbcamera.CameraDialog
import com.esp.uvc.utils.loge
import com.esp.uvc.utils.logi
import com.esp.uvc.utils.toggleTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FirmwareRegisterPresenter(val context: Context) : FirmwareRegisterContract.Presenter {
    private enum class RegisterType {
        FW, ASIC, I2C;
    }

    private var usbMonitor: USBMonitor? = null
    private var etronCamera: EtronCamera? = null

    private lateinit var registerType: RegisterType
    override lateinit var view: FirmwareRegisterContract.View

    private var address2bytes = false
    private var value2bytes = false
    private var canNavigateFlag = true

    override fun attach(view: FirmwareRegisterContract.View) {
        this.view = view
        onRegisterTypeSelected(RegisterType.FW);
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

    override fun onFwRegisterSelected() {
        onRegisterTypeSelected(RegisterType.FW)
    }

    override fun onAsicRegisterSelected() {
        onRegisterTypeSelected(RegisterType.ASIC)
    }

    override fun onI2cRegisterSelected() {
        onRegisterTypeSelected(RegisterType.I2C)
    }

    private fun onRegisterTypeSelected(type: RegisterType) {
        registerType = type
        view.enableAddressLengthSelection(type == RegisterType.I2C)
        view.enableValueLengthSelection(type == RegisterType.I2C)
        view.enableSlaveAddressInput(type == RegisterType.I2C)

        view.checkAddress2Bytes(type == RegisterType.ASIC)
        view.checkValue2Bytes(false)
    }

    private val usbDeviceListener: USBMonitor.OnDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {

        override fun onAttach(device: UsbDevice?) {
            logi("usb device attached: $device")
            val usbDevice: UsbDevice? =
                if (device != null && CameraModeManager.SUPPORTED_PID_LIST.contains(device.productId)) {
                    device
                } else {
                    usbMonitor!!.deviceList.forEach {
                        if (CameraModeManager.SUPPORTED_PID_LIST.contains(it!!.productId)) {
                            usbMonitor!!.requestPermission(it)
                        }
                    }
                    return
                }
            if (usbDevice == null) {
                CameraDialog.showDialog(context as Activity, usbMonitor)
            } else {
                usbMonitor!!.requestPermission(usbDevice)
            }
        }

        override fun onConnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?, createNew: Boolean) {
            if(ctrlBlock!!.isIMU)
                return
            GlobalScope.launch(Dispatchers.IO) {
                if (etronCamera != null) {
                    loge("Camera exists, not re-creating")
                    return@launch
                } else {
                    ::canNavigateFlag.toggleTimeout()
                    etronCamera = EtronCamera()
                    //open camera
                    if (etronCamera!!.open(ctrlBlock!!) != EYS_OK) {
                        view.showSnack(context.getString(R.string.camera_error_io)) {}
                        etronCamera = null
                        return@launch //jump out as we cannot continue no camera to work with
                    }
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
        }

        override fun onDetach(device: UsbDevice?) = view.toast(context.getString(R.string.usb_device_detached))
        override fun onCancel() = logi("usb device - onCancel")
    }

    override fun canNavigate(): Boolean {
        return canNavigateFlag
    }

    override fun onGetClicked(addr: Int, slave: Int) {
        if (etronCamera == null) {
            view.toast(context.getString(R.string.general_camera_not_connected_toast))
            return
        }

        if (addr == 0) {
            view.toast(context.getString(R.string.general_address_not_set_toast))
            return
        }

        if (registerType == RegisterType.I2C && slave == 0) {
            view.toast(context.getString(R.string.general_slave_address_not_set_toast))
            return
        }

        val ret = arrayOfNulls<String>(1)

        val status: Int?
        when (registerType) {
            RegisterType.FW -> {
                status = etronCamera?.getFWRegisterValue(ret, addr)
            }
            RegisterType.ASIC -> {
                status = etronCamera?.getHWRegisterValue(ret, addr)
            }
            RegisterType.I2C -> {
                var flag = if (address2bytes) FG_Address_2Byte else FG_Address_1Byte
                flag = flag or if (value2bytes) FG_Value_2Byte else FG_Value_1Byte
                status = etronCamera?.getSensorRegisterValue(ret, slave, addr, flag)
            }
        }
        logi("Read register status: $status")
        view.showRegisterValue(ret[0]?.toInt(16) ?: 0)
    }

    override fun onSetClicked(addr: Int, value: Int, slave: Int) {
        if (etronCamera == null) {
            view.toast(context.getString(R.string.general_camera_not_connected_toast))
            return
        }

        if (addr == 0) {
            view.toast(context.getString(R.string.general_address_not_set_toast))
            return
        }

        if (value == 0) {
            view.toast(context.getString(R.string.general_value_not_set_toast))
            return
        }

        if (registerType == RegisterType.I2C && slave == 0) {
            view.toast(context.getString(R.string.general_slave_address_not_set_toast))
            return
        }

        var ret: Int?
        when (registerType) {
            RegisterType.FW -> {
                ret = etronCamera?.SetFWRegisterValue(addr, value)
            }
            RegisterType.ASIC -> {
                ret = etronCamera?.setHWRegisterValue(addr, value)
            }
            RegisterType.I2C -> {
                var flag = if (address2bytes) FG_Address_2Byte else FG_Address_1Byte
                flag = flag or if (value2bytes) FG_Value_2Byte else FG_Value_1Byte
                ret = etronCamera?.setSensorRegisterValue(slave, addr, value, flag)
            }
        }
        logi("Saving register status: $ret")
    }

    override fun onAddress2BytesChecked(checked: Boolean) {
        address2bytes = checked
    }

    override fun onValue2BytesChecked(checked: Boolean) {
        value2bytes = checked
    }

}