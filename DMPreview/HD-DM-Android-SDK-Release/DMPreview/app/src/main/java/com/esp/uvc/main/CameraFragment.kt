package com.esp.uvc.main

import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.esp.android.usb.camera.core.IMUData
import com.esp.uvc.BaseFragment
import com.esp.uvc.BuildConfig
import com.esp.uvc.R
import com.esp.uvc.about.DialogGenerator
import com.esp.uvc.application.AndroidApplication
import com.esp.uvc.main.common.*
import com.esp.uvc.main.settings.SettingsMainActivity
import com.esp.uvc.main.settings.SoftwareVersionDialogFragment
import com.esp.uvc.ply.viewer.LivePlyGLRenderer
import com.esp.uvc.usbcamera.AppSettings
import com.esp.uvc.utils.loge
import com.esp.uvc.utils.roUI
import com.esp.uvc.widget.UVCCameraTextureView
import com.google.android.material.snackbar.Snackbar
import com.warkiz.widget.IndicatorSeekBar
import kotlinx.android.synthetic.main.dialog_imu.*
import kotlinx.android.synthetic.main.fragment_camera_main.*
import kotlin.math.abs

const val ALPHA_ENABLED = 1.0f
const val ALPHA_DISABLED = 0.3f
const val LANDSCAPE_ROTATION = 90f
const val NORMAL_ROTATION = 0f

@ExperimentalUnsignedTypes
class CameraFragment : BaseFragment(), IMain.View {

    companion object {
        fun newInstance() = CameraFragment()
    }

    val mPresenter by lazy { CameraPresenter(this, context!!) }

    private var mDepthFilterDialog: DepthFilterDialogFragment? = null

    private var mIMUDialog: IMUDialogFragment? = null

    private var mSensorDialog: SensorDialogFragment? = null

    private var mIRControlDialog: MaterialDialog? = null

    private var mColorPaletteDialog: ColorPaletteDialogFragment? = null

    private val mLivePlyGLRenderer by lazy { LivePlyGLRenderer() }

    private var mProgressDialog: MaterialDialog? = null

    private var mAlertDialog: AlertDialogFragment? = null

    private var mToast: Toast? = null

    private var mDepthMeasureEnabled: Boolean = false

    private var mFocalLengthDialog: FocalLengthDialogFragment? = null

    private var mRegionAccuracyDialog: RegionAccuracyDialogFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera_main, container, false)
        mPresenter.attach()
        setHasOptionsMenu(false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewLogic()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!setUpGlEs()) loge("esp_dynamic_ply_render setUpGlEs have problem")
    }

    override fun onStart() {
        super.onStart()
        mPresenter.onStart()
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.decorView?.apply { systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE }
        mPresenter.onResume()
        applyRotationChanges()
    }

    override fun onPause() {
        super.onPause()
        mPresenter.onPause()
    }

    override fun onStop() {
        super.onStop()
        mPresenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter.unattach()
    }

    override fun showAboutDialog(cameraName: String) = roUI {
        val fragment = SoftwareVersionDialogFragment(cameraName)
        fragment.show((context as FragmentActivity).supportFragmentManager, "softwareVersion")
    }

    override fun enableDepthFilterButton(isEnable: Boolean) = roUI {
        depth_filter_button?.alpha = if (isEnable) ALPHA_ENABLED else ALPHA_DISABLED
        depth_filter_button?.isEnabled = isEnable
    }

    override fun showDepthFilterDialogFragment(
        tag: String,
        version: String,
        subSampleModeCallback: ((Int) -> Unit)?,
        subSampleFactorCallback: ((Int) -> Unit)?,
        holeFillLevelSeekCallback: ((Int) -> Unit)?,
        edgePreservingLevelCallback: ((Int) -> Unit)?,
        temporalLevelCallback: ((Float) -> Unit)?
    ) {
        mDepthFilterDialog = DepthFilterDialogFragment(mPresenter, version)
        mDepthFilterDialog!!.setListener(
            subSampleModeCallback,
            subSampleFactorCallback,
            holeFillLevelSeekCallback,
            edgePreservingLevelCallback,
            temporalLevelCallback
        )
        mDepthFilterDialog!!.showNow(fragmentManager!!, tag)
    }

    override fun updateDepthFilterWidgetState(
        enableFull: Boolean,
        enableMin: Boolean,
        enableSubSampleEnabler: Boolean,
        enableSubMode: Boolean,
        enableSubFactor: Boolean,
        enableHoleEnabler: Boolean,
        enableHoleHorizontal: Boolean,
        enableHoleLevel: Boolean,
        enableEdgeEnabler: Boolean,
        enableEdgeLevel: Boolean,
        enableTemporalEnabler: Boolean,
        enableTemporalLevel: Boolean,
        enableRemoveEnabler: Boolean
    ) {
        mDepthFilterDialog!!.updateWidgetState(
            enableFull,
            enableMin,
            enableSubSampleEnabler,
            enableSubMode,
            enableSubFactor,
            enableHoleEnabler,
            enableHoleHorizontal,
            enableHoleLevel,
            enableEdgeEnabler,
            enableEdgeLevel,
            enableTemporalEnabler,
            enableTemporalLevel,
            enableRemoveEnabler
        )
    }

    override fun updateDepthFilterEnablers(
        bDoDepthFilter: Boolean,
        bFullChoose: Boolean,
        bMinChoose: Boolean,
        bSubSample: Boolean,
        bEdgePreServingFilter: Boolean,
        bHoleFill: Boolean,
        bHoleFillHorizontal: Boolean,
        bTempleFilter: Boolean,
        bFlyingDepthCancellation: Boolean
    ) {
        mDepthFilterDialog!!.setEnablers(
            bDoDepthFilter,
            bFullChoose,
            bMinChoose,
            bSubSample,
            bEdgePreServingFilter,
            bHoleFill,
            bHoleFillHorizontal,
            bTempleFilter,
            bFlyingDepthCancellation
        )
    }

    override fun updateDepthFilterValues(
        subSampleMode: Int,
        subSampleFactor: Int,
        holeFillLevel: Int,
        edgePreservingLevel: Int,
        temporalFilterAlpha: Float
    ) {
        mDepthFilterDialog!!.setValues(
            subSampleMode,
            subSampleFactor,
            holeFillLevel,
            edgePreservingLevel,
            temporalFilterAlpha
        )
    }

    override fun enableIMUButton(enabled: Boolean) = roUI {
        ib_imu?.isEnabled = enabled
        ib_imu?.alpha = if (enabled) ALPHA_ENABLED else ALPHA_DISABLED
    }

    override fun enableFocalLengthButton(enabled: Boolean) = roUI {
        ib_focal_length_settings?.isEnabled = enabled
        ib_focal_length_settings?.alpha = if (enabled) ALPHA_ENABLED else ALPHA_DISABLED
    }

    override fun showFocalLengthDialogFragment(
        leftFx: Int,
        leftFy: Int,
        rightFx: Int,
        rightFy: Int,
        pixelUnit: Int,
        listener: FocalLengthDialogFragment.OnListener
    ) = roUI {
        mFocalLengthDialog = FocalLengthDialogFragment()
        mFocalLengthDialog!!.setConfiguration(listener, leftFx, leftFy, rightFx, rightFy, pixelUnit)
        mFocalLengthDialog!!.show(fragmentManager!!, tag)
    }

    override fun onFocalLength(
        leftFx: Int,
        leftFy: Int,
        rightFx: Int,
        rightFy: Int,
        pixelUnit: Int
    ) = roUI {
        mFocalLengthDialog?.onFocalLength(leftFx, leftFy, rightFx, rightFy, pixelUnit)
    }

    override fun showIMUDialogFragment(
        listener: IMUDialogFragment.OnListener,
        syncFrameCnt: Boolean,
        isSaveRawData: Boolean
    ) {
        mIMUDialog = IMUDialogFragment()
        mIMUDialog!!.setConfiguration(listener, syncFrameCnt, isSaveRawData)
        mIMUDialog!!.show(fragmentManager!!, tag)
    }

    override fun onIMUStatus(enabled: Boolean) = roUI {
        mIMUDialog?.onStatus(enabled)
    }

    override fun onIMUModuleName(name: String) = roUI {
        mIMUDialog?.onModuleName(name)
    }

    override fun onIMUFWVersion(version: String) = roUI {
        mIMUDialog?.onFWVersion(version)
    }

    override fun onIMUCalibration(isSuccess: Boolean) = roUI {
        mIMUDialog?.onCalibration(isSuccess)
    }

    override fun onIMUSaveRawData(isSave: Boolean) = roUI {
        mIMUDialog?.onSaveRawData(isSave)
    }

    override fun onIMUInfo(data: IMUData) = roUI {
        mIMUDialog?.onIMUInfo(data)
    }

    override fun enableSensorSettingsButton(enabled: Boolean) = roUI {
        ib_sensor_settings?.isEnabled = enabled
        ib_sensor_settings?.alpha = if (enabled) ALPHA_ENABLED else ALPHA_DISABLED
    }

    override fun showSensorDialogFragment(
        isAE: Boolean,
        currentExposure: Int,
        minExposure: Int,
        maxExposure: Int,
        isAWB: Boolean,
        currentWB: Int,
        minWB: Int,
        maxWB: Int,
        isLLCEnable: Boolean,
        isLLC: Boolean,
        listener: SensorDialogFragment.OnSensorListener
    ) {
        mSensorDialog = SensorDialogFragment()
        mSensorDialog!!.setConfiguration(
            isAE,
            currentExposure,
            minExposure,
            maxExposure,
            isAWB,
            currentWB,
            minWB,
            maxWB,
            isLLCEnable,
            isLLC,
            listener
        )
        mSensorDialog!!.show(fragmentManager!!, tag)
    }

    override fun onSensorCheckedChanged(tag: String, enabled: Boolean) {
        mSensorDialog?.setChecked(tag, enabled)
    }

    override fun enableAccuracySettingsButton(enabled: Boolean) = roUI {
        ib_accuracy_settings?.isEnabled = enabled
        ib_accuracy_settings?.alpha = if (enabled) ALPHA_ENABLED else ALPHA_DISABLED
    }

    override fun showRegionAccuracyDialogFragment(
        listener: RegionAccuracyDialogFragment.OnListener,
        roiList: List<String>,
        roiIndex: Int,
        enableGroundTruth: Boolean,
        groundTruth: Float
    ) = roUI {
        mRegionAccuracyDialog = RegionAccuracyDialogFragment()
        mRegionAccuracyDialog!!.setConfiguration(
            listener,
            roiList,
            roiIndex,
            enableGroundTruth,
            groundTruth
        )
        mRegionAccuracyDialog!!.show(fragmentManager!!, tag)
    }

    override fun onAccuracyInfo(distance: Float, fillRate: Float, zAccuracy: Float) = roUI {
        mRegionAccuracyDialog?.onAccuracyInfo(distance, fillRate, zAccuracy)
    }

    override fun onTemporalNoise(temporalNoise: Float) = roUI {
        mRegionAccuracyDialog?.onTemporalNoise(temporalNoise)
    }

    override fun onSpatialNoise(spatialNoise: Float, angle: Float, angleX: Float, angleY: Float) =
        roUI {
            mRegionAccuracyDialog?.onSpatialNoise(spatialNoise, angle, angleX, angleY)
        }

    override fun onGroundTruth(groundTruth: Float) = roUI {
        mRegionAccuracyDialog?.onGroundTruth(groundTruth)
    }

    override fun onAccuracyRegion(rect: Rect?) = roUI {
        if (rect == null) {
            view_accuracy_region.visibility = View.GONE
        } else {
            val layoutParams = RelativeLayout.LayoutParams(
                camera_view_depth.width,
                camera_view_depth.height
            )
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            view_accuracy_region.layoutParams = layoutParams
            view_accuracy_region.visibility = View.VISIBLE
            view_accuracy_region.setRect(rect.left, rect.top, rect.right, rect.bottom)
        }
    }

    override fun enableSettingsButton(enabled: Boolean) = roUI {
        settingsButton?.alpha = if (enabled) ALPHA_ENABLED else ALPHA_DISABLED
        settingsButton?.isEnabled = enabled
        settingsButton?.postInvalidate()
    }

    override fun enableIRButton(flag: Boolean) = roUI {
        ir?.alpha = if (flag) ALPHA_ENABLED else ALPHA_DISABLED
        ir?.isEnabled = flag
    }

    override fun showIRChangeDialog(
        iRmax: Int,
        irMin: Int,
        irCurrentValue: Int,
        extended: Boolean,
        positiveCallback: (Int, Boolean) -> Unit
    ) = roUI {
        mIRControlDialog = DialogGenerator.showIRChangeDialog(
            requireContext(),
            iRmax,
            irMin,
            irCurrentValue,
            extended,
            positiveCallback
        )
        mIRControlDialog?.setOnDismissListener { mIRControlDialog = null }
    }

    override fun updateIRChangeDialog(
        irMin: Int,
        irMax: Int,
        irCurrentValue: Int,
        irExtended: Boolean
    ) {
        val view = mIRControlDialog?.getCustomView() ?: return
        loge("[ir_ext] updateIRChangeDialog $irMin: $irMax $irCurrentValue $irExtended")
        val slider = view.findViewById<IndicatorSeekBar>(R.id.ir_value_seekbar)
        slider.min = irMin.toFloat()
        slider.max = irMax.toFloat()
        slider.tickCount = (slider.max - slider.min + 1).toInt()
        slider.setProgress(irCurrentValue.toFloat())
        val extendedView = view.findViewById<CheckBox>(R.id.ir_range_extended_checkbox)
        extendedView.isChecked = irExtended
        slider.postInvalidate()
    }

    override fun enableColorPaletteButton(isEnable: Boolean) = roUI {
        loge("esp_palette_enable $isEnable")
        colorPalette?.alpha = if (isEnable) ALPHA_ENABLED else ALPHA_DISABLED
        colorPalette?.isEnabled = isEnable
        colorPalette?.isClickable = isEnable
    }

    override fun showColorPaletteDialog(zNear: Int, zFar: Int) {
        loge("[esp_palette_dialog] showColorPaletteDialog: $zNear  $zFar")
        mColorPaletteDialog = ColorPaletteDialogFragment()
        mColorPaletteDialog?.setTitle(context?.getString(R.string.color_palette_title))
        mColorPaletteDialog?.setPositiveButton(context?.getString(R.string.btn_set))
        mColorPaletteDialog?.setNegativeButton(context?.getString(R.string.btn_reset))
        mColorPaletteDialog?.setListener(object :
            ColorPaletteDialogFragment.OnDistanceValueChangeListener {
            override fun onChange(min: Int, max: Int) {
                mPresenter.onColorPaletteDistanceChange(min, max)
            }

            override fun onReset() {
                mPresenter.onColorPaletteDistanceReset()
            }
        })
        mColorPaletteDialog?.showNow(fragmentManager!!, tag)
        mColorPaletteDialog?.setValues(zNear, zFar)
    }

    override fun enableDepthMeasure(flag: Boolean) = roUI {
        mDepthMeasureEnabled = flag
        if (mDepthMeasureEnabled) {
            depthMeasure?.alpha = ALPHA_ENABLED
            crosshair?.visibility = View.VISIBLE
        } else {
            crosshair?.visibility = View.GONE
            depthMeasure?.alpha = ALPHA_DISABLED
        }
    }

    override fun updateColorPaletteCurrentExtreme(zNear: Int, zFar: Int) {
        mLivePlyGLRenderer.updateCurrentZExtreme(zNear, zFar)
    }

    override fun enableLivePlyButton(isEnable: Boolean) = roUI {
        if (isEnable) live_ply?.alpha = ALPHA_ENABLED else live_ply?.alpha = ALPHA_DISABLED
    }

    override fun renderLivePly(colorArray: FloatArray, depthVertexArray: FloatArray) {
        mLivePlyGLRenderer.setData(depthVertexArray, colorArray, depthVertexArray.size / 3)
    }

    override fun showLivePlyView(isEnable: Boolean) {
        live_ply_surfaceView.visibility = if (isEnable) View.VISIBLE else View.INVISIBLE
        tv_ply_fps.visibility = if (isEnable) View.VISIBLE else View.INVISIBLE
        if (isEnable) live_ply_surfaceView.onResume()
        else live_ply_surfaceView.onPause()
    }

    override fun onPlyFps(fps: String) {
        if (fps.isNotEmpty()) {
            tv_ply_fps?.visibility = View.VISIBLE
            tv_ply_fps?.text = fps
        } else {
            tv_ply_fps?.visibility = View.GONE
        }
    }

    override fun enablePLYButton(isEnable: Boolean) = roUI {
        if (isEnable) savePly?.alpha = ALPHA_ENABLED else savePly?.alpha = ALPHA_DISABLED
    }

    override fun showPlyErrorDialog(
        isStatic: Boolean,
        listener: AlertDialogFragment.OnAlertListener,
        msg: String
    ) =
        roUI {
            mAlertDialog = AlertDialogFragment()
            mAlertDialog?.setTitle(
                if (isStatic) getString(R.string.static_ply_error_dialog_title) else getString(
                    R.string.dynamic_ply_error_dialog_title
                )
            )
            mAlertDialog?.setMessage(msg)
            mAlertDialog?.setPositiveButton(getString(R.string.error_handle_dialog_positive))
            mAlertDialog?.setListener(listener)
            mAlertDialog?.showNow(fragmentManager!!, AlertDialogFragment.TAG_ERROR_PLY)
        }

    override fun toast(text: String, length: Int) = roUI {
        if (mToast != null) {
            mToast!!.cancel()
        }
        mToast = Toast.makeText(requireContext(), "", length)
        mToast!!.duration = length
        mToast!!.setText(text)
        mToast!!.show()
    }

    override fun showSnack(text: String, length: Int, actionText: String, onClick: (() -> Unit)?) =
        roUI {
            if (view == null) return@roUI
            if (onClick != null) {
                Snackbar.make(view!!, text, length).setAction(actionText) {
                    onClick()
                }.show()
            } else {
                Snackbar.make(view!!, text, length).show()
            }
        }

    override fun getRGBTextureView(): UVCCameraTextureView {
        return camera_view_RGB
    }

    override fun getDepthTextureView(): UVCCameraTextureView {
        return camera_view_depth
    }

    override fun updateInfoTextRGB(fps: String) = roUI {
        if (fps.isNotEmpty()) {
            text_rgb_fps?.visibility = View.VISIBLE
            text_rgb_fps?.text = fps
        } else {
            text_rgb_fps?.visibility = View.GONE
        }
    }

    override fun updateInfoTextDepth(fps: String) = roUI {
        if (fps.isNotEmpty()) {
            text_depth_fps?.visibility = View.VISIBLE
            text_depth_fps?.text = fps
        } else {
            text_depth_fps?.visibility = View.GONE
        }
    }

    override fun updateFrameCntRGB(cnt: Int) = roUI {
        if (cnt == 0) {
            text_rgb_frame_cnt?.visibility = View.GONE
        } else {
            text_rgb_frame_cnt?.visibility = View.VISIBLE
            text_rgb_frame_cnt?.text = getString(R.string.camera_preview_sn, cnt)
        }
    }

    override fun updateFrameCntDepth(cnt: Int) = roUI {
        if (cnt == 0) {
            text_depth_frame_cnt?.visibility = View.GONE
        } else {
            text_depth_frame_cnt?.visibility = View.VISIBLE
            text_depth_frame_cnt?.text = getString(R.string.camera_preview_sn, cnt)
        }
    }

    override fun onSyncFrameCnt(cnt: Int) = roUI {
        text_rgb_frame_cnt.text = getString(R.string.camera_preview_sn, cnt)
        text_depth_frame_cnt.text = getString(R.string.camera_preview_sn, cnt)
        mIMUDialog?.onSyncFrameCnt(cnt)
    }

    override fun letCrosshairToCenter() {
        val drawable = this.resources.getDrawable(R.drawable.ic_crosshairs, null)
        crosshair.x = (container_depth.width - drawable.intrinsicWidth) / 2.0f
        crosshair.y = (container_depth.height - drawable.intrinsicHeight) / 2.0f
    }

    override fun showProgressDialog(
        flag: Boolean,
        enableTitleHint: Boolean,
        text: String,
        cancelable: Boolean,
        cancelCallback: (() -> Unit)?
    ) = roUI {
        if (flag) {
            mProgressDialog?.dismiss() // clear remaining dialogs
            mProgressDialog = null
            mProgressDialog = MaterialDialog(requireContext())
            mProgressDialog!!.customView(R.layout.progress_ply_saving)
            mProgressDialog!!.show {
                val titleString = if (enableTitleHint)
                    getString(R.string.progress_title) + getString(R.string.progress_title_hint)
                else
                    getString(R.string.progress_title)

                title(null, titleString)
                message(text = text + "\n\n") //styling
                if (cancelable) {
                    negativeButton(R.string.dlg_cancel) {
                        cancelCallback?.invoke()
                    }
                }
                noAutoDismiss()
                cancelOnTouchOutside(cancelable)
                cancelable(cancelable)
            }
        } else {
            mProgressDialog?.dismiss()
            mProgressDialog = null
        }
    }

    override fun showWarningDialog(listener: AlertDialogFragment.OnAlertListener) = roUI {
        mAlertDialog = AlertDialogFragment()
        mAlertDialog!!.setTitle(getString(R.string.warning))
        mAlertDialog!!.setMessage(getString(R.string.warning_high_performance))
        mAlertDialog!!.setPositiveButton(getString(R.string.dlg_ok))
        mAlertDialog!!.setNegativeButton(getString(R.string.dlg_cancel))
        mAlertDialog!!.setListener(listener)
        mAlertDialog!!.showNow(fragmentManager!!, AlertDialogFragment.TAG_HIGH_PERFORMANCE)
    }

    override fun showErrorHandleDialog(listener: AlertDialogFragment.OnAlertListener) = roUI {
        mAlertDialog = AlertDialogFragment()
        mAlertDialog?.setTitle(getString(R.string.error_handle_dialog_title))
        mAlertDialog?.setMessage(getString(R.string.error_handle_dialog_hint))
        mAlertDialog?.setPositiveButton(getString(R.string.error_handle_dialog_positive))
        mAlertDialog?.setListener(listener)
        mAlertDialog?.showNow(fragmentManager!!, AlertDialogFragment.TAG_ERROR_HANDLE)
        mAlertDialog?.setErrorCountDown()
    }

    override fun showQualityFailDialog(listener: AlertDialogFragment.OnAlertListener) = roUI {
        mAlertDialog = AlertDialogFragment()
        mAlertDialog?.setTitle(getString(R.string.warning))
        mAlertDialog?.setMessage(getString(R.string.warning_quality_register_fail))
        mAlertDialog?.setPositiveButton(getString(R.string.dlg_ok))
        mAlertDialog?.setListener(listener)
        mAlertDialog?.showNow(fragmentManager!!, AlertDialogFragment.TAG_QUALITY)
    }

    override fun hideDialogs() {
        mProgressDialog?.dismiss()
        mIRControlDialog?.dismiss()
        mAlertDialog?.dismiss()
        mDepthFilterDialog?.dismiss()
        mIMUDialog?.dismiss()
        mSensorDialog?.dismiss()
        mColorPaletteDialog?.dismiss()
    }

    private fun applyRotationChanges() {
        /**
         * Mirroring as per client request
         * setting the scale is an easy "mirror"
         * However it will break the depth measure
         * The depth x coordinate will be inverse
         * */
        if (AppSettings.getInstance(requireContext()).get(AppSettings.MIRROR_MODE, false)) {
            camera_view_depth.scaleX = -1f
            camera_view_RGB.scaleX = -1f
        } else {
            camera_view_depth.scaleX = 1f
            camera_view_RGB.scaleX = 1f
        }

        if (AppSettings.getInstance(requireContext()).get(AppSettings.LANDSCAPE_MODE, false)) {
            val scaleRGB = container_rgb.height.toFloat() / camera_view_RGB.width.toFloat()
            camera_view_RGB.rotation = LANDSCAPE_ROTATION
            camera_view_RGB.scaleX = if (camera_view_RGB.scaleX < 0) -scaleRGB else scaleRGB

            val scaleDepthX = container_depth.height.toFloat() / camera_view_RGB.width.toFloat()
            camera_view_depth.rotation = LANDSCAPE_ROTATION
            camera_view_depth.scaleX = if (isScaled()) -scaleDepthX else scaleDepthX

            settingsButton.rotation = LANDSCAPE_ROTATION
            ib_sensor_settings.rotation = LANDSCAPE_ROTATION
            ib_imu.rotation = LANDSCAPE_ROTATION
            ib_focal_length_settings.rotation = LANDSCAPE_ROTATION
            ib_accuracy_settings.rotation = LANDSCAPE_ROTATION
            depth_filter_button.rotation = LANDSCAPE_ROTATION
            aboutButton.rotation = LANDSCAPE_ROTATION
            savePly.rotation = LANDSCAPE_ROTATION
            depthMeasure.rotation = LANDSCAPE_ROTATION
            live_ply.rotation = LANDSCAPE_ROTATION
            colorPalette.rotation = LANDSCAPE_ROTATION
            ir.rotation = LANDSCAPE_ROTATION
        } else {
            camera_view_RGB.rotation = NORMAL_ROTATION
            camera_view_RGB.scaleX = if (camera_view_RGB.scaleX < 0) -1.0f else 1.0f
            camera_view_depth.rotation = NORMAL_ROTATION
            camera_view_depth.scaleX = if (camera_view_RGB.scaleX < 0) -1.0f else 1.0f

            settingsButton.rotation = NORMAL_ROTATION
            ib_sensor_settings.rotation = NORMAL_ROTATION
            ib_imu.rotation = NORMAL_ROTATION
            ib_focal_length_settings.rotation = NORMAL_ROTATION
            ib_accuracy_settings.rotation = NORMAL_ROTATION
            depth_filter_button.rotation = NORMAL_ROTATION
            aboutButton.rotation = NORMAL_ROTATION
            savePly.rotation = NORMAL_ROTATION
            depthMeasure.rotation = NORMAL_ROTATION
            live_ply.rotation = NORMAL_ROTATION
            colorPalette.rotation = NORMAL_ROTATION
            ir.rotation = NORMAL_ROTATION
        }
    }

    private fun setupViewLogic() {
        camera_view_RGB.setOnLongClickListener {
            if (BuildConfig.DEBUG) {
                mPresenter.rgbCameraToggle()
            }

            true
        }

        camera_view_depth.setOnLongClickListener {
            if (BuildConfig.DEBUG) {
                mPresenter.depthCameraToggle()
            }
            true
        }

        camera_view_depth.setOnLongClickListener {
            if (mDepthMeasureEnabled) return@setOnLongClickListener true //we dont want it to turn off the depth camera
            mPresenter.depthCameraToggle()
            true
        }
        camera_view_depth.setOnTouchListener { _, e ->
            if (!mDepthMeasureEnabled) {
                false
            } else {
                val eventX = e.x.coerceIn(0f..camera_view_depth.width.toFloat())
                val eventY = e.y.coerceIn(0f..camera_view_depth.height.toFloat())

                val pts = floatArrayOf(eventX, eventY)
                camera_view_depth.matrix.mapPoints(pts)
                var xCoord = pts.component1()
                var yCoord = pts.component2()

                if (camera_view_depth.rotation != 0f) {
                    val constraintHeightMin =
                        container_depth.height.toFloat() / 2f - (camera_view_depth.height.toFloat() / 2)
                    val constraintHeightMax =
                        container_depth.height.toFloat() / 2f + (camera_view_depth.height.toFloat() / 2) + camera_view_depth.top
                    xCoord = (xCoord - ((crosshair.width / 2) - camera_view_depth.left)).coerceIn(
                        constraintHeightMin,
                        constraintHeightMax
                    )
                    yCoord = (yCoord - ((crosshair.height / 2) - camera_view_depth.top)).coerceIn(
                        camera_view_depth.left.toFloat(),
                        ((container_depth.width - crosshair.width) + camera_view_depth.left).toFloat()
                    )
                    if (isScaled()) {
                        xCoord =
                            (camera_view_depth.width - (xCoord + (crosshair.width / 2))).coerceAtMost(
                                (camera_view_depth.width - crosshair.width).toFloat()
                            )
                    }
                } else {
                    yCoord = (eventY - ((crosshair.height / 2) - camera_view_depth.top)).coerceIn(
                        camera_view_depth.top.toFloat(),
                        ((camera_view_depth.height - crosshair.height) + camera_view_depth.top).toFloat()
                    )
                    if (isScaled()) {
                        xCoord =
                            (camera_view_depth.width - eventX - ((crosshair.width / 2) - camera_view_depth.left)).coerceIn(
                                camera_view_depth.left.toFloat(),
                                ((camera_view_depth.width - crosshair.width) + camera_view_depth.left).toFloat()
                            )
                    } else {
                        xCoord =
                            (eventX - ((crosshair.width / 2) - camera_view_depth.left)).coerceIn(
                                camera_view_depth.left.toFloat(),
                                ((camera_view_depth.width - crosshair.width) + camera_view_depth.left).toFloat()
                            )
                    }
                }
                when (e.action) {
                    ACTION_MOVE -> {
                        crosshair.x = xCoord
                        crosshair.y = yCoord
                    }
                    ACTION_UP -> {
                        crosshair.x = xCoord
                        crosshair.y = yCoord
                        val crossX = (crosshair.x + (crosshair.width / 2)) - camera_view_depth.left
                        val crossY = (crosshair.y + (crosshair.height / 2) - camera_view_depth.top)

                        if (camera_view_depth.rotation != 0f) {
                            val crossPts = floatArrayOf((crossX), crossY)
                            val mat = Matrix()
                            mat.setRotate(
                                -camera_view_depth.rotation,
                                camera_view_depth.pivotX,
                                camera_view_depth.pivotY
                            )
                            mat.mapPoints(crossPts)
                            mat.setScale(
                                abs(1f / camera_view_depth.scaleX),
                                camera_view_depth.scaleY,
                                camera_view_depth.pivotX,
                                camera_view_depth.pivotY
                            )
                            mat.mapPoints(crossPts)
                            mPresenter.updateDepthMeasurePosition(
                                crossPts.component1(),
                                crossPts.component2()
                            )
                        } else {
                            mPresenter.updateDepthMeasurePosition(crossX, crossY)
                        }
                    }
                }
                true
            }
        }

        aboutButton.setOnClickListener {
            showAboutDialog(mPresenter.getProductVersion() ?: "")
        }
        settingsButton.setOnClickListener {
            if (mPresenter.canNavigateFlag()) {
                SettingsMainActivity.startInstance(context!!)
            }
        }
        ib_sensor_settings.isEnabled = false
        ib_sensor_settings.setOnClickListener { mPresenter.onSensorSettingsClick() }
        depth_filter_button.setOnClickListener {
            mPresenter.onDepthFilterClick(DepthFilterDialogFragment.TAG_DEPTH_FILTER)
        }
        ib_imu.isEnabled = false
        ib_imu.setOnClickListener { mPresenter.onIMUClick() }
        ib_focal_length_settings.isEnabled = false
        ib_focal_length_settings.setOnClickListener {
            mPresenter.onFocalLengthClick()
        }
        ib_accuracy_settings.isEnabled = false
        ib_accuracy_settings.setOnClickListener {
            mPresenter.onAccuracySettingsClick()
        }
        savePly.setOnClickListener {
            mPresenter.savePLY()
        }
        depthMeasure.setOnClickListener {
            toast(getString(R.string.error_feature_not_available))
        }
        live_ply.setOnClickListener {
            mPresenter.onStartLivePly()
        }
        colorPalette.setOnClickListener {
            mPresenter.onPaletteClick()
        }
        crosshair.visibility = View.INVISIBLE
        depthMeasure.alpha = ALPHA_DISABLED
        depthMeasure.setOnClickListener {
            mPresenter.enabledDepthMeasure(depthMeasure.alpha == ALPHA_DISABLED)//intentionally opposite
        }
        ir.setOnClickListener {
            if (ir.isEnabled) mPresenter.onIRClick()
            else toast(getString(R.string.ir_not_supported))
        }
    }

    private fun isScaled() = (camera_view_depth.scaleX < 0)

    private fun setUpGlEs(): Boolean {
        val supportGlEs = AndroidApplication.eglMajorVersion
        when {
            supportGlEs >= 0x30000 -> live_ply_surfaceView.setEGLContextClientVersion(3)
            supportGlEs >= 0x20000 -> live_ply_surfaceView.setEGLContextClientVersion(2)
            else -> return false
        }
        live_ply_surfaceView.setRenderer(mLivePlyGLRenderer)
        return true
    }
}
