package com.esp.uvc.main.settings.module_sync

import android.hardware.usb.UsbDevice
import com.esp.android.usb.camera.core.IMUData
import com.esp.android.usb.camera.core.USBMonitor
import com.esp.uvc.widget.UVCCameraTextureView

interface IModuleSync {

    interface Model {

        fun registerUsbMonitor()

        fun unregisterUsbMonitor()

        fun destroy()

        fun onDeviceAttach(device: UsbDevice?)

        fun onDeviceDetach(device: UsbDevice?)

        fun onDeviceConnect(
            device: UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?,
            createNew: Boolean
        )

        fun onDeviceDisconnect(
            device: UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?
        )

        fun onResetClick(index: Int)

    }

    interface View {

        fun getTextureView(index: Int): UVCCameraTextureView

        fun onFpsSN(index: Int, render: Double, uvc: Double, frame: Int)

        fun onResetFail()

        fun onIMUConnected(index: Int)

        fun onIMUData(index: Int, data: IMUData)

    }

    interface Presenter {

        fun onStart()

        fun onStop()

        fun onResetClick(index: Int)

        fun onDeviceAttach(device: UsbDevice?)

        fun onDeviceDetach(device: UsbDevice?)

        fun onDeviceConnect(
            device: UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?,
            createNew: Boolean
        )

        fun onDeviceDisconnect(
            device: UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?
        )

        fun getTextureView(index: Int): UVCCameraTextureView

        fun onFpsSN(index: Int, render: Double, uvc: Double, frame: Int)

        fun onResetFail()

        fun onIMUConnected(index: Int)

        fun onIMUData(index: Int, data: IMUData)

    }
}