package com.esp.uvc.main.settings

import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Environment
import com.esp.android.usb.camera.core.EtronCamera
import com.esp.android.usb.camera.core.EtronCamera.EYS_OK
import com.esp.android.usb.camera.core.USBMonitor
import com.esp.uvc.R
import com.esp.uvc.camera_modes.CameraModeManager
import com.esp.uvc.utils.loge
import com.esp.uvc.utils.logi
import com.esp.uvc.utils.toggleTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*

class FirmwareTablePresenter(val context: Context) : FirmwareTableContract.Presenter {

    private var canNavigateFlag = true

    private var indexValues = Array(0) { it }

    private val ETronDI_CALIB_LOG_FILE_ID_0 = 240

    private val KEY_RECTIFY_LOG_READ = "rectify_log_read"

    private val mDirPath = Environment.getExternalStorageDirectory().toString() + "/eYs3D/"

    private var usbMonitor: USBMonitor? = null
    private var etronCamera: EtronCamera? = null

    var index = 0

    override fun canNavigate(): Boolean {
        return canNavigateFlag
    }

    override fun onIndexSelected(index: Int) {
        this.index = indexValues[index]
        logi("Selected index: ${this.index}")
    }

    override lateinit var view: FirmwareTableContract.View

    override fun attach(view: FirmwareTableContract.View) {
        this.view = view
        view.setIndexValues(indexValues)
    }

    override fun unattach() {
        etronCamera?.destroy()
        etronCamera = null
        usbMonitor?.destroy()
        usbMonitor = null
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

    override fun onRectifyLogReadClicked() {
        if (etronCamera == null) {
            view.toast(context.getString(R.string.general_camera_not_connected_toast))
            return
        }

        GlobalScope.launch {
            view.showWaitingSpinner(true)
            var serialNumber = etronCamera!!.serialNumberValue
            val last = serialNumber.last()
            if (last.category == CharCategory.CONTROL) {
                serialNumber = serialNumber!!.substring(0, serialNumber!!.length - 1)
            }
            val rectifyLogData = etronCamera!!.getRectifyLogData(index)
            val buffer = etronCamera!!.getFileData(ETronDI_CALIB_LOG_FILE_ID_0 + index)
            if (rectifyLogData == null) {
                view.toast(context.getString(R.string.firmware_table_failed_to_read_rectify_log))
            } else {
                logi("rectifyLogData " + index + ":" + rectifyLogData!!.toString())
                val message = context.getString(
                    R.string.firmware_table_rectify_log_dialog_message,
                    index,
                    rectifyLogData.toString()
                )
                val fileName = "$serialNumber$KEY_RECTIFY_LOG_READ$index"
                writeToFile(mDirPath, fileName, buffer)
                val fileNameText = "$serialNumber$KEY_RECTIFY_LOG_READ$index.txt"
                writeToFile(mDirPath, fileNameText, message)
                view.toast(
                    context.getString(
                        R.string.firmware_table_write_path_toast,
                        "$mDirPath$fileName"
                    )
                )
                view.showDialog(message)
            }
            view.showWaitingSpinner(false)
        }
    }

    private fun writeToFile(dirPath: String, fileName: String, buffer: ByteArray) {
        val dir = File(dirPath)
        if (!dir.exists()) {
            dir.mkdirs()
        } else if (!dir.isDirectory && dir.canWrite()) {
            dir.delete()
            dir.mkdirs()
        }
        val file = File(dirPath + fileName)

        val bos: BufferedOutputStream
        try {
            bos = BufferedOutputStream(FileOutputStream(file))
            bos.write(buffer)
            bos.flush()
            bos.close()
            logi("Read Complete...")
        } catch (e: IOException) {
            loge("write read buffer to file error:$e")
        }
    }

    private fun writeToFile(dirPath: String, fileName: String, buffer: String) {
        val dir = File(dirPath)
        if (!dir.exists()) {
            dir.mkdirs()
        } else if (!dir.isDirectory && dir.canWrite()) {
            dir.delete()
            dir.mkdirs()
        }
        val file = File(dirPath + fileName)
        val bos: OutputStreamWriter
        try {
            bos = OutputStreamWriter(FileOutputStream(file))
            bos.write(buffer)
            bos.flush()
            bos.close()
            logi("Read Complete...")
        } catch (e: IOException) {
            loge("write read buffer to file error:$e")
        }
    }

    private val usbDeviceListener: USBMonitor.OnDeviceConnectListener =
        object : USBMonitor.OnDeviceConnectListener {

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

            override fun onConnect(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
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
                    indexValues = Array(10) { it }
                    view.setIndexValues(indexValues)
                    canNavigateFlag = true
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
                indexValues = Array(0) { it }
                view.setIndexValues(indexValues)
                canNavigateFlag = true
            }

            override fun onDetach(device: UsbDevice?) =
                view.toast(context.getString(R.string.usb_device_detached))

            override fun onCancel() = logi("usb device - onCancel")
        }
}