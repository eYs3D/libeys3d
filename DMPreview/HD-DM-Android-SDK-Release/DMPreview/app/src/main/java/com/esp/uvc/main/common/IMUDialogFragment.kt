package com.esp.uvc.main.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.esp.android.usb.camera.core.IMUData
import com.esp.uvc.R
import com.esp.uvc.application.AndroidApplication
import com.esp.uvc.ply.viewer.IMUGLRenderer
import com.esp.uvc.utils.gdx.Quaternion
import kotlinx.android.synthetic.main.dialog_imu.view.*

private const val BOX_SIZE = 1.0f
private const val BOX_ALPHA = 1.0f
private const val COLOR_INTENSITY = 1.0f

private const val AXIS_HEIGHT = 2.0f
private const val AXIS_WIDTH = 0.12f
private const val AXIS_ARROW_HEIGHT = 0.2f
private const val AXIS_ARROW_WIDTH = 0.16f

private val BOX_VERTEX = arrayOf(
    // front, 2 triangle, 6 vertex
    -BOX_SIZE / 2, -BOX_SIZE / 2, BOX_SIZE / 2, // Left, bottom, front
    BOX_SIZE / 2, -BOX_SIZE / 2, BOX_SIZE / 2, // Right, bottom, front
    BOX_SIZE / 2, BOX_SIZE / 2, BOX_SIZE / 2, // Right, Top, front
    BOX_SIZE / 2, BOX_SIZE / 2, BOX_SIZE / 2,
    -BOX_SIZE / 2, BOX_SIZE / 2, BOX_SIZE / 2,
    -BOX_SIZE / 2, -BOX_SIZE / 2, BOX_SIZE / 2,

    // bottom
    -BOX_SIZE / 2, -BOX_SIZE / 2, BOX_SIZE / 2,
    -BOX_SIZE / 2, -BOX_SIZE / 2, -BOX_SIZE / 2,
    BOX_SIZE / 2, -BOX_SIZE / 2, -BOX_SIZE / 2,
    BOX_SIZE / 2, -BOX_SIZE / 2, -BOX_SIZE / 2,
    BOX_SIZE / 2, -BOX_SIZE / 2, BOX_SIZE / 2,
    -BOX_SIZE / 2, -BOX_SIZE / 2, BOX_SIZE / 2,

    // left
    -BOX_SIZE / 2, -BOX_SIZE / 2, BOX_SIZE / 2,
    -BOX_SIZE / 2, BOX_SIZE / 2, BOX_SIZE / 2,
    -BOX_SIZE / 2, BOX_SIZE / 2, -BOX_SIZE / 2,
    -BOX_SIZE / 2, BOX_SIZE / 2, -BOX_SIZE / 2,
    -BOX_SIZE / 2, -BOX_SIZE / 2, -BOX_SIZE / 2,
    -BOX_SIZE / 2, -BOX_SIZE / 2, BOX_SIZE / 2,

    // right
    BOX_SIZE / 2, BOX_SIZE / 2, -BOX_SIZE / 2,
    BOX_SIZE / 2, BOX_SIZE / 2, BOX_SIZE / 2,
    BOX_SIZE / 2, -BOX_SIZE / 2, BOX_SIZE / 2,
    BOX_SIZE / 2, -BOX_SIZE / 2, BOX_SIZE / 2,
    BOX_SIZE / 2, -BOX_SIZE / 2, -BOX_SIZE / 2,
    BOX_SIZE / 2, BOX_SIZE / 2, -BOX_SIZE / 2,

    // back
    BOX_SIZE / 2, BOX_SIZE / 2, -BOX_SIZE / 2,
    BOX_SIZE / 2, -BOX_SIZE / 2, -BOX_SIZE / 2,
    -BOX_SIZE / 2, -BOX_SIZE / 2, -BOX_SIZE / 2,
    -BOX_SIZE / 2, -BOX_SIZE / 2, -BOX_SIZE / 2,
    -BOX_SIZE / 2, BOX_SIZE / 2, -BOX_SIZE / 2,
    BOX_SIZE / 2, BOX_SIZE / 2, -BOX_SIZE / 2,

    // top
    BOX_SIZE / 2, BOX_SIZE / 2, -BOX_SIZE / 2,
    -BOX_SIZE / 2, BOX_SIZE / 2, -BOX_SIZE / 2,
    -BOX_SIZE / 2, BOX_SIZE / 2, BOX_SIZE / 2,
    -BOX_SIZE / 2, BOX_SIZE / 2, BOX_SIZE / 2,
    BOX_SIZE / 2, BOX_SIZE / 2, BOX_SIZE / 2,
    BOX_SIZE / 2, BOX_SIZE / 2, -BOX_SIZE / 2
)

private val BOX_COLOR = arrayOf(
    0.0f, COLOR_INTENSITY, 0.0f, BOX_ALPHA,
    0.0f, COLOR_INTENSITY, 0.0f, BOX_ALPHA,
    0.0f, COLOR_INTENSITY, 0.0f, BOX_ALPHA,
    0.0f, COLOR_INTENSITY, 0.0f, BOX_ALPHA,
    0.0f, COLOR_INTENSITY, 0.0f, BOX_ALPHA,
    0.0f, COLOR_INTENSITY, 0.0f, BOX_ALPHA,

    0.0f, COLOR_INTENSITY, COLOR_INTENSITY, BOX_ALPHA,
    0.0f, COLOR_INTENSITY, COLOR_INTENSITY, BOX_ALPHA,
    0.0f, COLOR_INTENSITY, COLOR_INTENSITY, BOX_ALPHA,
    0.0f, COLOR_INTENSITY, COLOR_INTENSITY, BOX_ALPHA,
    0.0f, COLOR_INTENSITY, COLOR_INTENSITY, BOX_ALPHA,
    0.0f, COLOR_INTENSITY, COLOR_INTENSITY, BOX_ALPHA,

    COLOR_INTENSITY, 0.0f, COLOR_INTENSITY, BOX_ALPHA,
    COLOR_INTENSITY, 0.0f, COLOR_INTENSITY, BOX_ALPHA,
    COLOR_INTENSITY, 0.0f, COLOR_INTENSITY, BOX_ALPHA,
    COLOR_INTENSITY, 0.0f, COLOR_INTENSITY, BOX_ALPHA,
    COLOR_INTENSITY, 0.0f, COLOR_INTENSITY, BOX_ALPHA,
    COLOR_INTENSITY, 0.0f, COLOR_INTENSITY, BOX_ALPHA,

    COLOR_INTENSITY, COLOR_INTENSITY, 0.0f, BOX_ALPHA,
    COLOR_INTENSITY, COLOR_INTENSITY, 0.0f, BOX_ALPHA,
    COLOR_INTENSITY, COLOR_INTENSITY, 0.0f, BOX_ALPHA,
    COLOR_INTENSITY, COLOR_INTENSITY, 0.0f, BOX_ALPHA,
    COLOR_INTENSITY, COLOR_INTENSITY, 0.0f, BOX_ALPHA,
    COLOR_INTENSITY, COLOR_INTENSITY, 0.0f, BOX_ALPHA,

    0.0f, 0.0f, COLOR_INTENSITY, BOX_ALPHA,
    0.0f, 0.0f, COLOR_INTENSITY, BOX_ALPHA,
    0.0f, 0.0f, COLOR_INTENSITY, BOX_ALPHA,
    0.0f, 0.0f, COLOR_INTENSITY, BOX_ALPHA,
    0.0f, 0.0f, COLOR_INTENSITY, BOX_ALPHA,
    0.0f, 0.0f, COLOR_INTENSITY, BOX_ALPHA,

    COLOR_INTENSITY, 0.0f, 0.0f, BOX_ALPHA,
    COLOR_INTENSITY, 0.0f, 0.0f, BOX_ALPHA,
    COLOR_INTENSITY, 0.0f, 0.0f, BOX_ALPHA,
    COLOR_INTENSITY, 0.0f, 0.0f, BOX_ALPHA,
    COLOR_INTENSITY, 0.0f, 0.0f, BOX_ALPHA,
    COLOR_INTENSITY, 0.0f, 0.0f, BOX_ALPHA
)

private val AXIS_VERTEX = arrayOf(
    0.0f, -AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    0.0f, -AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    0.0f, AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    0.0f, AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    0.0f, -AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    0.0f, AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,

    0.0f, AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    0.0f, AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    0.0f, AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,

    0.0f, AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    0.0f, -AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    0.0f, -AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, -AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,

    AXIS_HEIGHT, -AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    0.0f, -AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, -AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, -AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    0.0f, -AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    0.0f, -AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,

    0.0f, -AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    0.0f, AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, -AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, -AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    0.0f, AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,

    AXIS_HEIGHT, -AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, -AXIS_WIDTH / 2.0f, AXIS_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_WIDTH / 2.0f, -AXIS_WIDTH / 2.0f,

    //Arrow
    AXIS_HEIGHT, -AXIS_ARROW_WIDTH / 2.0f, -AXIS_ARROW_WIDTH / 2.0f,
    AXIS_HEIGHT, -AXIS_ARROW_WIDTH / 2.0f, AXIS_ARROW_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_ARROW_WIDTH / 2.0f, -AXIS_ARROW_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_ARROW_WIDTH / 2.0f, -AXIS_ARROW_WIDTH / 2.0f,
    AXIS_HEIGHT, -AXIS_ARROW_WIDTH / 2.0f, AXIS_ARROW_WIDTH / 2.0f,
    AXIS_HEIGHT, AXIS_ARROW_WIDTH / 2.0f, AXIS_ARROW_WIDTH / 2.0f,

    AXIS_HEIGHT, AXIS_ARROW_WIDTH / 2.0f, AXIS_ARROW_WIDTH / 2.0f,
    AXIS_HEIGHT + AXIS_ARROW_HEIGHT, 0.0f, 0.0f,
    AXIS_HEIGHT, -AXIS_ARROW_WIDTH / 2.0f, AXIS_ARROW_WIDTH / 2.0f,

    AXIS_HEIGHT, AXIS_ARROW_WIDTH / 2.0f, AXIS_ARROW_WIDTH / 2.0f,
    AXIS_HEIGHT + AXIS_ARROW_HEIGHT, 0.0f, 0.0f,
    AXIS_HEIGHT, AXIS_ARROW_WIDTH / 2.0f, -AXIS_ARROW_WIDTH / 2.0f,

    AXIS_HEIGHT, -AXIS_ARROW_WIDTH / 2.0f, -AXIS_ARROW_WIDTH / 2.0f,
    AXIS_HEIGHT + AXIS_ARROW_HEIGHT, 0.0f, 0.0f,
    AXIS_HEIGHT, AXIS_ARROW_WIDTH / 2.0f, -AXIS_ARROW_WIDTH / 2.0f,

    AXIS_HEIGHT, -AXIS_ARROW_WIDTH / 2.0f, -AXIS_ARROW_WIDTH / 2.0f,
    AXIS_HEIGHT + AXIS_ARROW_HEIGHT, 0.0f, 0.0f,
    AXIS_HEIGHT, -AXIS_ARROW_WIDTH / 2.0f, AXIS_ARROW_WIDTH / 2.0f
)

private val AXIS_COLOR = arrayOf(
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,

    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,

    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,

    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,

    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,

    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,

    //Arrow
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,

    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,

    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,

    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,

    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f
)

class IMUDialogFragment : DialogFragment() {

    companion object {
        const val TAG_STATUS = "TAG_STATUS"
        const val TAG_ENABLE_IMU = "TAG_ENABLE_IMU"
        const val TAG_DISABLE_IMU = "TAG_DISABLE_IMU"
        const val TAG_GET_MODULE_NAME = "TAG_GET_MODULE_NAME"
        const val TAG_GET_FW_VERSION = "TAG_GET_FW_VERSION"
        const val TAG_CALIBRATION = "TAG_CALIBRATION"
        const val TAG_SAVE_RAW_DATA = "TAG_SAVE_RAW_DATA"
        const val TAG_RESET = "TAG_RESET"
    }

    private lateinit var mView: View

    private var mSync = false
    private var mIsSaveRawData = false

    private val mGLRenderer by lazy { IMUGLRenderer() }

    private var mListener: OnListener? = null

    interface OnListener {

        fun onDismiss()

        fun onSync(sync: Boolean)

        fun onClicked(tag: String)
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mView = activity!!.layoutInflater.inflate(R.layout.dialog_imu, null)
        setUpUI()
        val builder = AlertDialog.Builder(activity!!)
        builder.setView(mView)
        return builder.create()
    }

    // Full screen and bg no dim
    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val window = dialog.window
            if (window != null) {
                window.attributes?.gravity = Gravity.TOP
                window.attributes?.dimAmount = 0f
                window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mListener?.onDismiss()
    }

    fun setConfiguration(
        listener: OnListener,
        syncFrameCnt: Boolean,
        isSaveRawData: Boolean
    ) {
        mListener = listener
        mSync = syncFrameCnt
        mIsSaveRawData = isSaveRawData
    }

    fun onSyncFrameCnt(cnt: Int) {
        if (context == null) return
        mView.tv_frame_count.text = getString(R.string.dialog_imu_frame_count, cnt)
    }

    fun onStatus(isEnabled: Boolean) {
        if (isEnabled) {
            mView.tv_result.text = "Enable"
        } else {
            mView.tv_result.text = "Disable"
        }
    }

    fun onModuleName(name: String) {
        mView.tv_result.text = name
    }

    fun onFWVersion(version: String) {
        mView.tv_result.text = version
    }

    fun onCalibration(isSuccess: Boolean) {
        mView.layout.isEnabled = true
        mView.pb_calibration.visibility = View.GONE
        mView.tv_result.text = if (isSuccess) "Succeed" else "Failed"
    }

    fun onSaveRawData(isSave: Boolean) {
        mIsSaveRawData = isSave
        mView.bt_save_data.text =
            if (mIsSaveRawData) getString(R.string.dialog_imu_stop_save_data) else getString(R.string.dialog_imu_save_data)
    }

    fun onIMUInfo(data: IMUData) {
        if (context == null) return
        if (!mSync) {
            mView.tv_frame_count.text =
                getString(R.string.dialog_imu_frame_count, data.mFrameCount)
        }
        mView.tv_time.text = getString(
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
        mGLRenderer.setQuaternion(q)
        mView.tv_RPY.text = getString(R.string.dialog_imu_rpy, q.pitch, q.yaw, q.roll)
        mView.tv_quaternion_0.text =
            getString(R.string.dialog_imu_quaternion_0, data.mQuaternion[0])
        mView.tv_quaternion_1.text =
            getString(R.string.dialog_imu_quaternion_1, data.mQuaternion[1])
        mView.tv_quaternion_2.text =
            getString(R.string.dialog_imu_quaternion_2, data.mQuaternion[2])
        mView.tv_quaternion_3.text =
            getString(R.string.dialog_imu_quaternion_3, data.mQuaternion[3])
    }

    private fun setUpUI() {
        mView.cb_sync_frame_cnt.isChecked = mSync
        mView.cb_sync_frame_cnt.setOnCheckedChangeListener(mOnCheckedChangeListener)
        setUpButtonUI()
        setUpGLSurfaceView()
    }

    private fun setUpButtonUI() {
        mView.bt_save_data.text =
            if (mIsSaveRawData) getString(R.string.dialog_imu_stop_save_data) else getString(R.string.dialog_imu_save_data)
        mView.bt_status.setOnClickListener(mOnClickListener)
        mView.bt_enable.setOnClickListener(mOnClickListener)
        mView.bt_disable.setOnClickListener(mOnClickListener)
        mView.bt_get_module_name.setOnClickListener(mOnClickListener)
        mView.bt_get_fw_version.setOnClickListener(mOnClickListener)
        mView.bt_calibration.setOnClickListener(mOnClickListener)
        mView.bt_save_data.setOnClickListener(mOnClickListener)
    }

    private fun setUpGLSurfaceView() {
        if (setUpGlEs()) {
            val r = AXIS_COLOR.clone().toFloatArray()
            val g = AXIS_COLOR.clone().toFloatArray()
            val b = AXIS_COLOR.clone().toFloatArray()
            g.forEachIndexed { index, fl ->
                when (index % 4) {
                    0 -> g[index] = 0f
                    1 -> g[index] = 1f
                    2 -> g[index] = 0f
                }
            }
            b.forEachIndexed { index, fl ->
                when (index % 4) {
                    0 -> b[index] = 0f
                    1 -> b[index] = 0f
                    2 -> b[index] = 1f
                }
            }
            val axisColor = arrayOf(r, g, b)
            mGLRenderer.setData(
                BOX_VERTEX.toFloatArray(),
                BOX_COLOR.toFloatArray(),
                36,
                AXIS_VERTEX.toFloatArray(),
                axisColor,
                54
            )
            mView.gl_surfaceView.visibility = View.VISIBLE
            mView.gl_surfaceView.onResume()
            mView.bt_reset.visibility = View.VISIBLE
            mView.bt_reset.setOnClickListener(mOnClickListener)
        }
    }

    private fun setUpGlEs(): Boolean {
        val supportGlEs = AndroidApplication.eglMajorVersion
        when {
            supportGlEs >= 0x30000 -> mView.gl_surfaceView.setEGLContextClientVersion(3)
            supportGlEs >= 0x20000 -> mView.gl_surfaceView.setEGLContextClientVersion(2)
            else -> return false
        }
        mView.gl_surfaceView.setRenderer(mGLRenderer)
        return true
    }

    private val mOnCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            mSync = isChecked
            mListener?.onSync(isChecked)
        }

    private val mOnClickListener = View.OnClickListener { p0 ->
        when (p0!!.id) {
            R.id.bt_status -> mListener?.onClicked(TAG_STATUS)
            R.id.bt_enable -> mListener?.onClicked(TAG_ENABLE_IMU)
            R.id.bt_disable -> mListener?.onClicked(TAG_DISABLE_IMU)
            R.id.bt_get_module_name -> mListener?.onClicked(TAG_GET_MODULE_NAME)
            R.id.bt_get_fw_version -> mListener?.onClicked(TAG_GET_FW_VERSION)
            R.id.bt_calibration -> {
                mListener?.onClicked(TAG_CALIBRATION)
                mView.layout.isEnabled = false
                mView.pb_calibration.visibility = View.VISIBLE
            }
            R.id.bt_save_data -> mListener?.onClicked(TAG_SAVE_RAW_DATA)
            R.id.bt_reset -> mListener?.onClicked(TAG_RESET)
        }
    }
}