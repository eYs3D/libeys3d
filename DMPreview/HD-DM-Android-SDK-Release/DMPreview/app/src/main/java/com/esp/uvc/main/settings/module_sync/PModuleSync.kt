package com.esp.uvc.main.settings.module_sync

import android.hardware.usb.UsbDevice
import com.esp.android.usb.camera.core.IMUData
import com.esp.android.usb.camera.core.USBMonitor
import com.esp.uvc.widget.UVCCameraTextureView
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

class PModuleSync(v: IModuleSync.View, usbMonitor: USBMonitor) : IModuleSync.Presenter,
    KoinComponent {

    private val mIView = v

    private val mModel: IModuleSync.Model by inject { parametersOf(this, usbMonitor) }

    override fun onStart() {
        mModel.registerUsbMonitor()
    }

    override fun onStop() {
        mModel.unregisterUsbMonitor()
        mModel.destroy()
    }

    override fun onResetClick(index: Int) {
        mModel.onResetClick(index)
    }

    override fun onDeviceAttach(device: UsbDevice?) {
        mModel.onDeviceAttach(device)
    }

    override fun onDeviceDetach(device: UsbDevice?) {
        mModel.onDeviceDetach(device)
    }

    override fun onDeviceConnect(
        device: UsbDevice?,
        ctrlBlock: USBMonitor.UsbControlBlock?,
        createNew: Boolean
    ) {
        mModel.onDeviceConnect(device, ctrlBlock, createNew)
    }

    override fun onDeviceDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
        mModel.onDeviceDisconnect(device, ctrlBlock)
    }

    override fun getTextureView(index: Int): UVCCameraTextureView {
        return mIView.getTextureView(index)
    }

    override fun onFpsSN(index: Int, render: Double, uvc: Double, frame: Int) {
        mIView.onFpsSN(index, render, uvc, frame)
    }

    override fun onResetFail() {
        mIView.onResetFail()
    }

    override fun onIMUConnected(index: Int) {
        mIView.onIMUConnected(index)
    }

    override fun onIMUData(index: Int, data: IMUData) {
        mIView.onIMUData(index, data)
    }
}