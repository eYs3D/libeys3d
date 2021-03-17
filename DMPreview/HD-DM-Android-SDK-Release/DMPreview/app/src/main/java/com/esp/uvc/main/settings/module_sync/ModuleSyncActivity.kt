package com.esp.uvc.main.settings.module_sync

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.esp.android.usb.camera.core.IMUData
import com.esp.android.usb.camera.core.USBMonitor
import com.esp.uvc.BuildConfig
import com.esp.uvc.R
import com.esp.uvc.utils.gdx.Quaternion
import com.esp.uvc.utils.roUI
import com.esp.uvc.widget.UVCCameraTextureView
import kotlinx.android.synthetic.main.activity_module_sync.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class ModuleSyncActivity : AppCompatActivity(), IModuleSync.View {

    companion object {
        fun startInstance(context: Context) {
            context.startActivity(Intent(context, ModuleSyncActivity::class.java))
        }
    }

    private val mPresenter: IModuleSync.Presenter by inject {
        parametersOf(
            this,
            USBMonitor(this, mOnDeviceConnectListener)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_module_sync)
        bt_reset_one.setOnClickListener(mOnClickListener)
        bt_reset_two.setOnClickListener(mOnClickListener)
        ib_imu_one.setOnClickListener(mOnClickListener)
        ib_imu_two.setOnClickListener(mOnClickListener)
        ib_imu_one.isEnabled = false
        ib_imu_two.isEnabled = false
    }

    override fun onStart() {
        super.onStart()
        mPresenter.onStart()
    }

    override fun onStop() {
        super.onStop()
        mPresenter.onStop()
    }

    override fun getTextureView(index: Int): UVCCameraTextureView {
        return if (index == 0)
            camera_view_one
        else
            camera_view_two
    }

    override fun onFpsSN(
        index: Int,
        render: Double,
        uvc: Double,
        frame: Int
    ) = roUI {
        if (index == 0) {
            if (frame == -1) {
                tv_fps_one.text = ""
                tv_frame_count_one.text = ""
            } else
                setFps(tv_fps_one, render, uvc, tv_frame_count_one, frame)
        } else {
            if (frame == -1) {
                tv_fps_two.text = ""
                tv_frame_count_two.text = ""
            } else
                setFps(tv_fps_two, render, uvc, tv_frame_count_two, frame)
        }
    }

    override fun onResetFail() = roUI {
        Toast.makeText(this, "Set \"reset HW register\" fail", Toast.LENGTH_SHORT).show()
    }

    override fun onIMUConnected(index: Int) {
        if (index == 0) {
            ib_imu_one.isEnabled = true
            ib_imu_one.alpha = 1f
        } else {
            ib_imu_two.isEnabled = true
            ib_imu_two.alpha = 1f
        }
    }

    override fun onIMUData(index: Int, data: IMUData) = roUI {
        if (index == 0 && layout_imu_one.visibility == View.VISIBLE) {
            setIMUData(
                tv_imu_frame_count_one,
                tv_time_one,
                tv_RPY_one,
                tv_quaternion_0_one,
                tv_quaternion_1_one,
                tv_quaternion_2_one,
                tv_quaternion_3_one,
                data
            )
        } else if (index == 1 && layout_imu_two.visibility == View.VISIBLE) {
            setIMUData(
                tv_imu_frame_count_two,
                tv_time_two,
                tv_RPY_two,
                tv_quaternion_0_two,
                tv_quaternion_1_two,
                tv_quaternion_2_two,
                tv_quaternion_3_two,
                data
            )
        }
    }

    private fun setFps(
        tv_fps: TextView,
        render: Double,
        uvc: Double,
        tv_frame: TextView,
        frame: Int
    ) {
        if (BuildConfig.DEBUG) {
            tv_fps.text = getString(R.string.camera_preview_fps_measurement_debug, render, uvc)
        } else {
            tv_fps.text = getString(R.string.camera_preview_fps_measurement_release, uvc)
        }
        tv_frame.text = getString(R.string.camera_preview_sn, frame)
    }

    private fun setIMUData(
        tv_imu_frame_count: TextView,
        tv_time: TextView,
        tv_RPY: TextView,
        tv_quaternion_0: TextView,
        tv_quaternion_1: TextView,
        tv_quaternion_2: TextView,
        tv_quaternion_3: TextView,
        data: IMUData
    ) {
        tv_imu_frame_count.text =
            getString(R.string.dialog_imu_frame_count_serial, data.mFrameCount)
        tv_time.text = getString(
            R.string.dialog_imu_time,
            data.mHour,
            data.mMin,
            data.mSec,
            data.mSubSecond
        )
        // Roll, Pitch, Yaw
        // Pitch, Yaw, Roll
        val q = Quaternion(
            data.mQuaternion[1],
            data.mQuaternion[2],
            data.mQuaternion[3],
            data.mQuaternion[0]
        )
        tv_RPY.text = getString(R.string.dialog_imu_rpy, q.pitch, q.yaw, q.roll)
        tv_quaternion_0.text =
            getString(R.string.dialog_imu_quaternion_0, data.mQuaternion[0])
        tv_quaternion_1.text =
            getString(R.string.dialog_imu_quaternion_1, data.mQuaternion[1])
        tv_quaternion_2.text =
            getString(R.string.dialog_imu_quaternion_2, data.mQuaternion[2])
        tv_quaternion_3.text =
            getString(R.string.dialog_imu_quaternion_3, data.mQuaternion[3])
    }

    private val mOnClickListener = View.OnClickListener { p0 ->
        when (p0!!.id) {
            R.id.bt_reset_one -> mPresenter.onResetClick(0)
            R.id.bt_reset_two -> mPresenter.onResetClick(1)
            R.id.ib_imu_one -> layout_imu_one.visibility =
                if (layout_imu_one.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            R.id.ib_imu_two -> layout_imu_two.visibility =
                if (layout_imu_two.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private val mOnDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {

        override fun onAttach(device: UsbDevice?) {
            mPresenter.onDeviceAttach(device)
        }

        override fun onDetach(device: UsbDevice?) {
            mPresenter.onDeviceDetach(device)
        }

        override fun onConnect(
            device: UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?,
            createNew: Boolean
        ) {
            mPresenter.onDeviceConnect(device, ctrlBlock, createNew)
        }

        override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
            mPresenter.onDeviceDisconnect(device, ctrlBlock)
        }

        override fun onCancel() {
        }
    }
}