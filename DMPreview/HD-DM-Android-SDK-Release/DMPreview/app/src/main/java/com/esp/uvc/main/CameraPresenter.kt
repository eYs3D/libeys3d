package com.esp.uvc.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Rect
import android.hardware.SensorManager
import android.hardware.usb.UsbDevice
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import androidx.compose.ui.graphics.vectormath.Vector3
import com.esp.android.usb.camera.core.*
import com.esp.android.usb.camera.core.EtronCamera.*
import com.esp.android.usb.camera.core.UVCCamera.CAMERA_COLOR
import com.esp.uvc.BuildConfig
import com.esp.uvc.R
import com.esp.uvc.camera_modes.CameraMode
import com.esp.uvc.camera_modes.CameraModeManager
import com.esp.uvc.camera_modes.CameraState
import com.esp.uvc.camera_modes.DepthRange
import com.esp.uvc.main.common.*
import com.esp.uvc.main.common.AlertDialogFragment.Companion.TAG_ERROR_HANDLE
import com.esp.uvc.main.common.AlertDialogFragment.Companion.TAG_HIGH_PERFORMANCE
import com.esp.uvc.main.common.SensorDialogFragment.Companion.TAG_EXPOSURE
import com.esp.uvc.main.common.SensorDialogFragment.Companion.TAG_LOW_LIGHT_COMPENSATION
import com.esp.uvc.main.common.SensorDialogFragment.Companion.TAG_WHITE_BALANCE
import com.esp.uvc.manager.ExposureManager
import com.esp.uvc.manager.IRManager
import com.esp.uvc.manager.LightSourceManager
import com.esp.uvc.manager.WhiteBalanceManager
import com.esp.uvc.old.usbcamera.CameraDialog
import com.esp.uvc.old.usbcamera.FrameGrabber
import com.esp.uvc.roi_size.RoiSizeProvider
import com.esp.uvc.usbcamera.AppSettings.*
import com.esp.uvc.utils.*
import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

private const val FRAME_COUNT_INTERVAL = 33

@ExperimentalUnsignedTypes
class CameraPresenter(v: IMain.View, context: Context) : IMain.Presenter, KoinComponent {

    companion object {
        const val FPS_THROTTLE = 500L
        const val CAMERA_SETTINGS_TOGGLE = 8000L
        const val PLY_DATE_PATTERN = "yyyyMMdd_HHmmss"
    }

    private val mIView = v

    private val mContext = context

    private val roiSizeProivider: RoiSizeProvider by inject()
    private var roiSize = roiSizeProivider.getRoiSize()

    private var mProductVersion: String? = null
    private var mUsbType: String? = null

    /*USB & Camera*/
    private var usbMonitor: USBMonitor? = null
    private var etronCamera: EtronCamera? = null

    private var mCameraMode: CameraMode? = null

    private var irManager = IRManager(context)
    private var mColorPaletteModel = ColorPaletteModel()

    private var mStreamInfoIndexColor = 0
    private var mStreamInfoIndexDepth = 0

    /**
     * Display Variables
     * */
    private var rgbSurface: Surface? = null
    private var depthSurface: Surface? = null
    private var mEnableStreamColor = false
    private var mEnableStreamDepth = false

    /**
     * Frame grabber
     * */
    private val mFrameGrabber = FrameGrabber()

    /**
     * measure variables
     * */
    private var depthMeasureEnabled = false
    private var roiIndexes = ArrayList<Int>()
    private var mZDBuffer: IntArray? = null
    private var eY8038BaseLine: Int = DepthRange.DEPTH_RANGES[DepthRange.NEAR].distance

    /**Framerate*/
    private var monitorFramerate = true

    /**
     * zFar*/
    private var zFar: Int = 1000

    /**
     * Image fixes
     * */
    private var aspectRatioEnabled = true

    /**
     * Settings Overrides
     * */
    private var irOverride = true

    /**
     * Navigation flag to halt the user from navigating away from the activity while the camera is opening
     * */
    private var canNavigate = true

    private var mLivePlyStartTime: Long = 0
    private var mLivePlyFrameCount = 0
    private var mLivePlyEndTime: Long = 0

    private var mMonitorIMU = false
    private var mIsSaveIMUData = false
    private var mSyncFrameCnt = false

    // Avoid imu add the same cnt to mSyncMap (color / depth will not receive the same cnt from callback)
    private val mIMUCntMap = HashMap<Int, Boolean>()
    private val mSyncMap = HashMap<Int, Int>()

    private var mColorInfo: StreamInfo? = null
    private var mDepthInfo: StreamInfo? = null

    private var mMonitorAccuracy = false

    private val REGION_RATE = mapOf(0.2f to "20%", 0.4f to "40%", 0.6f to "60%", 0.8f to "80%")
    private var mDepthAccuracyRegionRatio = 0.8f
    private var mEnableGroundTruth = false
    private var mDepthAccuracyGroundTruthDistanceMM = 0f
    private var mDepthInvalidBandPixel = 0
    private var mDepthAccuracyDistanceMM = 0f
    private var mDepthAccuracyFillRate = 0f
    private var mDepthAccuracyZAccuracy = 0f
    private var mDepthSpatialNoise = 0f
    private var mDepthAngle = 0f
    private var mDepthAngleX = 0f
    private var mDepthAngleY = 0f
    private val TEMPORAL_NOISE_COUNT = 9
    private lateinit var mDepths: ArrayList<IntArray>
    private var mDepthTemporalNoise = 0f
    private lateinit var mRectifyLogData: RectifyLogData

    override fun attach() {
    }

    override fun onStart() {
        usbMonitor = USBMonitor(mContext, mOnDeviceConnectListener)
        usbMonitor?.register()
        etronCamera?.destroy()
        etronCamera = null
        depthMeasureEnabled = false
        mIView.enableDepthMeasure(false)
        mIView.updateInfoTextRGB()
        mIView.updateInfoTextDepth()
        mIView.updateFrameCntRGB(0)
        mIView.updateFrameCntDepth(0)
        mEnableStreamColor = false
        mEnableStreamDepth = false
        roiSize = roiSizeProivider.getRoiSize()
        eY8038BaseLine = getInstance(mContext).get(
            DEPTH_RANGE,
            DepthRange.DEPTH_RANGES[DepthRange.NEAR].distance
        )
        mIsSaveIMUData = false
        mSyncFrameCnt = false
        mIMUCntMap.clear()
        mSyncMap.clear()
        mDepthAccuracyRegionRatio = 0.8f
        mEnableGroundTruth = false
        mDepthAccuracyGroundTruthDistanceMM = 0f
        mDepthInvalidBandPixel = 0
        mDepthAccuracyDistanceMM = 0f
        mDepthAccuracyFillRate = 0f
        mDepthAccuracyZAccuracy = 0f
        mDepthSpatialNoise = 0f
        mDepthAngle = 0f
        mDepthAngleX = 0f
        mDepthAngleY = 0f
        mDepthTemporalNoise = 0f
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onStop() {
        savePreferenceState()
        irManager.setIrCurrentVal(etronCamera, 0)
        mIView.enableDepthFilterButton(false)
        mIView.enableColorPaletteButton(false)
        mIView.enablePLYButton(false)
        mIView.enableLivePlyButton(false)
        mIView.enableSensorSettingsButton(false)
        mIView.enableIMUButton(false)
        mIView.enableFocalLengthButton(false)
        mIView.enableAccuracySettingsButton(false)
        mZDBuffer = null
        etronCamera?.destroy()
        etronCamera = null
        usbMonitor?.unregister()
        usbMonitor?.destroy()
        usbMonitor = null
        mIView.updateInfoTextRGB()
        mIView.updateInfoTextDepth()
        mIView.hideDialogs()
        orientationListener?.disable()
        orientationListener = null
        mProductVersion = ""
        mUsbType = ""
        roiIndexes.clear()
    }

    override fun unattach() {
        etronCamera?.close()
        etronCamera?.destroy()
        etronCamera = null
        usbMonitor?.destroy()
        usbMonitor = null

        irManager.clear()
        mIView.updateInfoTextRGB()
        mIView.updateInfoTextDepth()

        orientationListener?.disable()
        orientationListener = null

        getInstance(mContext).clear()
        getInstance(mContext).init()
    }

    private var mDepthFilterModel = DepthFilterModel()

    override fun onDepthFilterClick(tag: String) {
        if (etronCamera == null) {
            mIView.toast(getString(R.string.camera_error_not_connected_abort))
            return
        }
        mIView.showDepthFilterDialogFragment(
            tag,
            mDepthFilterModel.getVersion(),
            subSampleModeCallback = mDepthFilterModel::subSampleModeCallback,
            subSampleFactorCallback = mDepthFilterModel::subSampleFactorCallback,
            holeFillLevelSeekCallback = mDepthFilterModel::holeFillLevelCallback,
            edgePreservingLevelCallback = mDepthFilterModel::edgePreservingLevelCallback,
            temporalLevelCallback = mDepthFilterModel::temporalFilterAlphaCallback
        )

        renderDepthFilterCurrentStateToWidgets()

        // The seekBar listener is onStopTrackingTouch will not call their self on view update.
        mDepthFilterModel.subSampleModeCallback(mDepthFilterModel.mSubSampleMode)
        mDepthFilterModel.subSampleFactorCallback(mDepthFilterModel.mSubSampleFactor)
        mDepthFilterModel.holeFillLevelCallback(mDepthFilterModel.mHoleFillLevel)
        mDepthFilterModel.edgePreservingLevelCallback(mDepthFilterModel.mEdgePreservingLevel)
        mDepthFilterModel.temporalFilterAlphaCallback(mDepthFilterModel.mTemporalAlpha)
    }

    override fun onDepthFilterMainEnablerChanged(isEnabled: Boolean) {
        mDepthFilterModel.setMainEnabler(isEnabled)
        if (mDepthFilterModel.mIsMainEnablerFilter && (isAnyPresetChose())) {
            enableOnlyPresetSection()
            return
        }
        enableByMainEnablerState()
    }

    override fun onDepthFilterFullChoose(isEnabled: Boolean) {
        mDepthFilterModel.setFullChoose(isEnabled)
        if (mDepthFilterModel.mIsEnableFull) {
            enableOnlyPresetSection()
            renderDepthFilterCurrentStateToWidgets()
        } else {
            enableByMainEnablerState()
        }
    }

    override fun onDepthFilterMinChoose(isEnabled: Boolean) {
        mDepthFilterModel.setMinChoose(isEnabled)
        if (mDepthFilterModel.mIsEnableMin) {
            enableOnlyPresetSection()
            renderDepthFilterCurrentStateToWidgets()
        } else {
            enableByMainEnablerState()
        }
    }

    override fun onDepthFilterHoleFillChanged(isEnabled: Boolean) {
        mDepthFilterModel.setHoleFillEnabler(isEnabled)
        enableByMainEnablerState()
        loge("esp_widget onDepthFilterHoleFillChanged $isEnabled")
    }

    override fun onDepthFilterHoleFillHorizontalChanged(isEnabled: Boolean) {
        loge("esp_widget onDepthFilterHoleFillHorizontalChanged $isEnabled")
        mDepthFilterModel.setHoleFillHorizontal(isEnabled)
    }

    override fun onDepthFilterEdgePreservingChanged(isEnabled: Boolean) {
        mDepthFilterModel.setEdgePreservingEnabler(isEnabled)
        enableByMainEnablerState()
        loge("esp_widget onDepthFilterEdgePreservingChanged $isEnabled")
    }

    override fun onDepthFilterTemporalFilterChanged(isEnabled: Boolean) {
        mDepthFilterModel.setTemporalFilterEnabler(isEnabled)
        enableByMainEnablerState()
        loge("esp_widget onDepthFilterTemporalFilterChanged $isEnabled")
    }

    override fun onDepthFilterRemoveCurveChanged(isEnabled: Boolean) {
        mDepthFilterModel.setRemoveCurveEnabler(isEnabled)
        enableByMainEnablerState()
        loge("esp_widget onDepthFilterRemoveCurveChanged $isEnabled")
    }

    override fun onDepthFilterSubsampleChanged(isEnabled: Boolean) {
        mDepthFilterModel.subSampleEnabler(isEnabled)
        enableByMainEnablerState()
        loge("esp_widget onDepthFilterSubsampleChanged $isEnabled")
    }

    override fun onAccuracySettingsClick() {
        mDepths = ArrayList()
        val size =
            mCameraMode!!.depthCameraState!!.resolution.width * mCameraMode!!.depthCameraState!!.resolution.height
        for (i in 0..TEMPORAL_NOISE_COUNT) {
            mDepths.add(IntArray(size))
        }
        adjustDepthInvalidBandPixel()
        var i = 0
        REGION_RATE.onEachIndexed { index, entry ->
            if (entry.key == mDepthAccuracyRegionRatio) i = index
        }
        mIView.showRegionAccuracyDialogFragment(
            mAccuracyListener,
            REGION_RATE.map { it.value },
            i,
            mEnableGroundTruth,
            mDepthAccuracyGroundTruthDistanceMM
        )
        mColorPaletteModel.enableRegionAccuracy(true)
        mMonitorAccuracy = true
        mIView.onAccuracyRegion(getAccuracyRegion(true))
        startAccuracyJob()
    }

    override fun onIMUClick() {
        if (etronCamera == null) {
            mIView.toast(getString(R.string.camera_error_not_connected_abort))
        } else {
            mIView.showIMUDialogFragment(
                mIMUListener,
                mSyncFrameCnt,
                mIsSaveIMUData
            )
            mMonitorIMU = true
        }
    }

    override fun onFocalLengthClick() {
        val focalLength = etronCamera!!.deviceFocalLength
        val focalLength2 =
            etronCamera!!.getFlashFocalLength(mDepthInfo!!.width, mDepthInfo!!.height)
        mIView.showFocalLengthDialogFragment(
            focalLength[0],
            focalLength[1],
            focalLength[2],
            focalLength[3],
            focalLength2[4],
            mFocalLengthListener
        )
    }

    override fun onSensorSettingsClick() {
        if (etronCamera == null) {
            mIView.toast(getString(R.string.camera_error_not_connected_abort))
        } else {
            val exposureInfo = getExposureInfo()
            val wbInfo = getWBInfo()
            mIView.showSensorDialogFragment(
                exposureInfo[0] == EXPOSURE_MODE_AUTO_APERTURE,
                exposureInfo[1],
                exposureInfo[2],
                exposureInfo[3],
                wbInfo[0] == AUTO_WHITE_BALANCE_ON,
                wbInfo[1],
                wbInfo[2],
                wbInfo[3],
                !getInstance(mContext).get(INTERLEAVE_FPS_CHOSEN, false),
                LightSourceManager.isLLC(etronCamera),
                mOnSensorListener
            )
        }
    }

    override fun onIRClick() {
        if (etronCamera == null) {
            mIView.toast(getString(R.string.camera_error_not_connected_abort))
            return
        }

        if (!irManager.validIR()) {
            loge("[ir_ext] err Invalid values for IR: pls chk")
            mIView.toast(getString(R.string.ir_invalid_values))
            return
        }

        mIView.showIRChangeDialog(
            irManager.irMax,
            irManager.irMin,
            irManager.irCurrentVal,
            irManager.irExtendedFlag,
            fun(newIrValue: Int, extended: Boolean) {
                val isExtSuccess = irManager.setIRExtension(etronCamera, extended)
                irManager.setIrCurrentVal(
                    etronCamera,
                    newIrValue.coerceIn(irManager.irMin..irManager.irMax)
                )

                if (isExtSuccess) {
                    dumpIR()
                    mIView.updateIRChangeDialog(
                        irManager.irMin,
                        irManager.irMax,
                        irManager.irCurrentVal,
                        irManager.irExtendedFlag
                    )
                }
            }
        )
    }

    override fun onPaletteClick() {
        loge("esp_palette_presenter onPaletteClick ${mColorPaletteModel.mCurrentZNearest} ${mColorPaletteModel.mCurrentZFarthest}")
        mIView.showColorPaletteDialog(
            mColorPaletteModel.mCurrentZNearest,
            mColorPaletteModel.mCurrentZFarthest
        )
    }

    override fun onColorPaletteDistanceChange(zNear: Int, zFar: Int) {
        loge("esp_palette_presenter onColorPaletteDistanceChange")
        mColorPaletteModel.setZDistance(zNear, zFar)
        mIView.updateColorPaletteCurrentExtreme(zNear, zFar)
    }

    override fun onColorPaletteDistanceReset() {
        loge("esp_palette_presenter onColorPaletteDistanceReset")
        mColorPaletteModel.resetZDistance()
    }

    override fun enabledDepthMeasure(enabled: Boolean) {
        if (etronCamera == null) {
            mIView.toast(getString(R.string.camera_error_not_connected_abort))
            return
        }
        if (!mEnableStreamDepth || mZDBuffer == null) {
            mIView.toast(getString(R.string.camera_error_no_depth_enabled))
            return
        }
        val instance = getInstance(mContext)
        if (instance.get(MIRROR_MODE, false) && instance.get(LANDSCAPE_MODE, false)) {
            mIView.toast(getString(R.string.error_feature_not_available))
            return
        }
        if (roiIndexes.isEmpty()) {
            mIView.letCrosshairToCenter()
            updateDepthMeasurePosition(
                mIView.getDepthTextureView().width / 2f,
                mIView.getDepthTextureView().height / 2f
            ) // so it centered at start
        }
        depthMeasureEnabled = enabled
        mIView.enableDepthMeasure(depthMeasureEnabled)
    }

    override fun onStartLivePly() {
        if (etronCamera == null) {
            mIView.toast(getString(R.string.camera_error_not_connected_abort))
            return
        }
        if (!mEnableStreamDepth) {
            mIView.toast(getString(R.string.ply_error_depth_not_enabled))
            return
        }
        if (!mEnableStreamColor) {
            mIView.toast(getString(R.string.ply_error_color_not_enabled))
            return
        }
        val errorMsg = if (isSupportOpenCL()) { // adb cmd check
            if (EtronCamera.isSupportOpenCL()) {  // init check
                val result = etronCamera?.onStartLivePly(mLivePlyPresenterCallback)
                if (eys_error.EYS_SUCCESS.ordinal == result) {
                    mIView.showLivePlyView(true)
                    return
                } else {
                    getString(R.string.ply_error_dialog_hint_null)
                }
            } else {
                getString(R.string.ply_error_dialog_hint_add_opencl_to_public_libraries)
            }
        } else {
            getString(R.string.ply_error_dialog_hint_not_support_opencl)
        }
        mIView.showPlyErrorDialog(
            false,
            mOnAlertListener,
            errorMsg
        )
    }

    override fun onStopLivePly() {
        etronCamera?.onStopLivePly()
        mIView.showLivePlyView(false)
        mLivePlyFrameCount = 0
    }


    @SuppressLint("SimpleDateFormat")
    override fun savePLY() {
        mIView.enablePLYButton(false)
        if (etronCamera == null) {
            mIView.toast(getString(R.string.camera_error_not_connected_abort))
            return
        }
        if (!mEnableStreamDepth) {
            mIView.toast(getString(R.string.ply_error_depth_not_enabled))
            return
        }
        if (!mEnableStreamColor) {
            mIView.toast(getString(R.string.ply_error_color_not_enabled))
            return
        }

        val timestampFilename = SimpleDateFormat(PLY_DATE_PATTERN).format(Date())
        val isUseFilter = getInstance(mContext).get(KEY_POST_PROCESS_PLY, POST_PROCESS_PLY_DEFAULT)
        mIView.showProgressDialog(
            true,
            isUseFilter,
            "${getString(R.string.ply_saving_start)}$timestampFilename"
        )
        GlobalScope.launch(Dispatchers.IO) {
            val result = etronCamera!!.saveStaticPlyWithFilter(timestampFilename, isUseFilter)
            mIView.showProgressDialog(false, isUseFilter, "")
            val errorMessage = when (result) {
                eys_error.EYS_SUCCESS.ordinal -> null
                eys_error.EYS_ERROR_OTHER.ordinal -> getString(R.string.ply_error_dialog_hint_other)
                eys_error.EYS_ERROR_INVALID_FILENAME.ordinal -> getString(R.string.ply_error_dialog_hint_invalid_file)
                eys_error.EYS_ERROR_INVALID_DIRECTORY.ordinal -> getString(R.string.ply_error_dialog_hint_mkdir)
                eys_error.EYS_ERROR_NULLITY.ordinal -> getString(R.string.ply_error_dialog_hint_null)
                eys_error.EYS_ERROR_IO.ordinal -> getString(R.string.ply_error_dialog_hint_io)
                eys_error.EYS_ERROR_FIRMWARE_IO.ordinal -> getString(R.string.ply_error_dialog_hint_io_firmware)
                else -> getString(R.string.ply_error_dialog_hint_other)
            }
            if (errorMessage != null) {
                mIView.showPlyErrorDialog(true, mOnAlertListener, errorMessage)
            }
            mIView.enablePLYButton(true)
        }
    }

    override fun canNavigateFlag(): Boolean {
        return canNavigate
    }

    override fun getProductVersion(): String? {
        return mProductVersion
    }

    override fun rgbCameraToggle() {
        if (etronCamera == null) {
            mIView.showSnack(getString(R.string.camera_error_not_connected)) {}
            return
        }
        if (!mEnableStreamColor) {
            mIView.toast(getString(R.string.camera_preview_toast_opening_rgb_camera))
            mEnableStreamColor = !mEnableStreamColor
            if (mEnableStreamColor && etronCamera!!.getStreamInfoList(INTERFACE_NUMBER_COLOR)
                    .isNotEmpty()
            ) {
                try {
                    mColorInfo =
                        etronCamera!!.getStreamInfoList(INTERFACE_NUMBER_COLOR)[mStreamInfoIndexColor]
                    logd(
                        "Setup Color Streaming for device : ${etronCamera!!.productVersion}" +
                                ", mCameraMode : ${mCameraMode!!.rgbCameraState!!}\n" +
                                "Real to set camera : (${mColorInfo!!.width}, ${mColorInfo!!.height})" +
                                ", isMJPEG : ${mColorInfo!!.bIsFormatMJPEG}, interfaceNumber : ${mColorInfo!!.interfaceNumber}"
                    )
                    etronCamera!!.setPreviewSize(mColorInfo, mCameraMode!!.rgbCameraState!!.fps)
                    //set texture for preview.
                    rgbSurface = Surface(mIView.getRGBTextureView().surfaceTexture)
                    etronCamera!!.setPreviewDisplay(rgbSurface, CAMERA_COLOR)
                    //callback function for processing.
                    etronCamera!!.setFrameCallback(
                        mColorIFrameCallback,
                        PIXEL_FORMAT_RGBX,
                        CAMERA_COLOR
                    )
                    etronCamera!!.startPreview(CAMERA_COLOR)
                } catch (e: Exception) {
                    loge("Failed to setup color stream from usb device : $e")
                    mIView.showSnack(getString(R.string.camera_error_rgb_failed)) {}
                    return
                }
            }
        } else {
            mEnableStreamColor = !mEnableStreamColor
            etronCamera?.stopPreview(CAMERA_COLOR)
            rgbSurface?.release()
            rgbSurface = null
            mIView.updateInfoTextRGB()
        }
    }

    override fun depthCameraToggle() {
        if (etronCamera == null) {
            mIView.showSnack(getString(R.string.camera_error_not_connected)) {}
            return
        }
        if (!mEnableStreamDepth) {
            mEnableStreamDepth = !mEnableStreamDepth
            mIView.toast(getString(R.string.camera_preview_toast_opening_depth_camera))
            if (mEnableStreamDepth && etronCamera!!.getStreamInfoList(INTERFACE_NUMBER_DEPTH)
                    .isNotEmpty()
            ) {
                try {
                    mDepthInfo =
                        etronCamera!!.getStreamInfoList(INTERFACE_NUMBER_DEPTH)[mStreamInfoIndexDepth]
                    logd(
                        "Setup Depth Streaming for device : ${etronCamera!!.productVersion}" +
                                ", mCameraMode : ${mCameraMode!!.depthCameraState!!}\n" +
                                "Real to set camera : (${mDepthInfo!!.width}, ${mDepthInfo!!.height})" +
                                ", isMJPEG : ${mDepthInfo!!.bIsFormatMJPEG}, interfaceNumber : ${mDepthInfo!!.interfaceNumber}"
                    )
                    etronCamera!!.setPreviewSize(mDepthInfo, mCameraMode!!.depthCameraState!!.fps)
                    mColorPaletteModel.init(etronCamera!!)
                    mColorPaletteModel.setZDistance(
                        mColorPaletteModel.mCurrentZNearest,
                        mColorPaletteModel.mCurrentZFarthest
                    )
                    mIView.updateColorPaletteCurrentExtreme(
                        mColorPaletteModel.mCurrentZNearest,
                        mColorPaletteModel.mCurrentZFarthest
                    )
                    mDepthFilterModel.init(etronCamera!!)
                    mIView.enableDepthFilterButton(true)
                    mIView.enableColorPaletteButton(true)
                    if (mEnableStreamColor && mEnableStreamDepth) {
                        mIView.enablePLYButton(true)
                        mIView.enableLivePlyButton(true)
                    }
                    depthSurface = Surface(mIView.getDepthTextureView().surfaceTexture)
                    etronCamera!!.setPreviewDisplay(depthSurface, CAMERA_DEPTH)
                    etronCamera!!.setFrameCallback(
                        mDepthIFrameCallback,
                        FRAME_FORMAT_YUYV,
                        CAMERA_DEPTH
                    )
                    etronCamera!!.setErrorCallback(
                        { mIView.showErrorHandleDialog(mOnAlertListener) },
                        CAMERA_DEPTH
                    )
                    etronCamera!!.startPreview(CAMERA_DEPTH)
                } catch (e: Exception) {
                    loge("Failed to setup depth stream from usb device : $e")
                    e.printStackTrace()
                    mIView.showSnack(getString(R.string.camera_error_depth_failed)) {}
                    return
                }
            }
        } else {
            mEnableStreamDepth = !mEnableStreamDepth
            etronCamera?.stopPreview(CAMERA_DEPTH)
            mIView.enableColorPaletteButton(false)
            mIView.enableDepthFilterButton(false)
            mIView.enablePLYButton(false)
            mIView.enableLivePlyButton(false)
            mIView.enableFocalLengthButton(false)
            mIView.enableAccuracySettingsButton(false)
            depthSurface?.release()
            depthSurface = null
            mIView.updateInfoTextDepth()
        }
    }

    override fun updateDepthMeasurePosition(
        xCoordinate: Float,
        yCoordinate: Float
    ) { // because the touch point need change to camera point (different resolution)
        val xCentral = if (mIView.getDepthTextureView().scaleX < 0) {
            ((mIView.getDepthTextureView().width - xCoordinate) * (mCameraMode!!.depthCameraState!!.resolution.width / mIView.getDepthTextureView().width.toFloat())).toInt()
        } else {
            (xCoordinate * (mCameraMode!!.depthCameraState!!.resolution.width / mIView.getDepthTextureView().width.toFloat())).toInt()
        }
        val realY =
            yCoordinate * (mCameraMode!!.depthCameraState!!.resolution.height / mIView.getDepthTextureView().height.toFloat())
        val yCentral = realY.toInt()
        roiIndexes.clear()

        val rangeX: IntRange
        val rangeY: IntRange
        if (roiSize.size % 2 != 0) {
            rangeX =
                (xCentral - roiSize.size / 2).coerceAtLeast(0)..(xCentral + roiSize.size / 2).coerceAtMost(
                    mCameraMode!!.depthCameraState!!.resolution.width - 1
                )
            rangeY =
                (yCentral - roiSize.size / 2).coerceAtLeast(0)..(yCentral + roiSize.size / 2).coerceAtMost(
                    mCameraMode!!.depthCameraState!!.resolution.height - 1
                )
        } else {
            rangeX =
                (xCentral - roiSize.size / 2).coerceAtLeast(0)..(xCentral + roiSize.size / 2 - 1).coerceAtMost(
                    mCameraMode!!.depthCameraState!!.resolution.width - 1
                )
            rangeY =
                (yCentral - roiSize.size / 2).coerceAtLeast(0)..(yCentral + roiSize.size / 2 - 1).coerceAtMost(
                    mCameraMode!!.depthCameraState!!.resolution.height - 1
                )
        }
        for (y in rangeY) {
            for (x in rangeX) {
                roiIndexes.add(x + (mCameraMode!!.depthCameraState!!.resolution.width * y))
            }
        }
    }

    private fun savePreferenceState() {
        val appSettings = getInstance(mContext)
        if (etronCamera == null) {
            appSettings.put(ENABLE_STREAM_COLOR, !mEnableStreamColor)
            appSettings.put(ENABLE_STREAM_DEPTH, !mEnableStreamDepth)
        } else {
            appSettings.put(ENABLE_STREAM_COLOR, mEnableStreamColor)
            appSettings.put(ENABLE_STREAM_DEPTH, mEnableStreamDepth)
        }

        appSettings.put(ETRON_INDEX, mStreamInfoIndexColor)
        appSettings.put(DEPTH_INDEX, mStreamInfoIndexDepth)
        appSettings.put(DEPTH_DATA_TYPE, mCameraMode?.videoMode ?: 1)
        appSettings.put(COLOR_FRAME_RATE, mCameraMode?.rgbCameraState?.fps ?: 30)
        appSettings.put(DEPTH_FRAME_RATE, mCameraMode?.depthCameraState?.fps ?: 30)
        appSettings.put(Z_FAR, zFar)
        appSettings.put(MONITOR_FRAMERATE, monitorFramerate)
        appSettings.put(CAMERA_VERSION, mProductVersion ?: "")
        appSettings.put(KEY_USB_TYPE, mUsbType ?: "")
        appSettings.put(IR_OVERRIDE, irOverride)

        ExposureManager.setupSharedPrefs(etronCamera)
        WhiteBalanceManager.setupSharedPrefs(etronCamera)
        LightSourceManager.setupSharedPrefs(etronCamera)

        irManager.saveSharedPref()

        appSettings.saveAll()
    }

    private fun renderDepthFilterCurrentStateToWidgets() {
        mIView.updateDepthFilterEnablers(
            bDoDepthFilter = mDepthFilterModel.mIsMainEnablerFilter,
            bFullChoose = mDepthFilterModel.mIsEnableFull,
            bMinChoose = mDepthFilterModel.mIsEnableMin,
            bHoleFill = mDepthFilterModel.mIsMainEnablerHoleFill,
            bHoleFillHorizontal = mDepthFilterModel.mIsHoleFillHorizontal,
            bEdgePreServingFilter = mDepthFilterModel.mIsMainEnablerEdgePreserving,
            bSubSample = mDepthFilterModel.mIsMainEnablerSubSample,
            bFlyingDepthCancellation = mDepthFilterModel.mIsMainEnablerRemoveCurve,
            bTempleFilter = mDepthFilterModel.mIsMainEnablerTemporalFilter
        )

        mIView.updateDepthFilterValues(
            subSampleMode = mDepthFilterModel.mSubSampleMode,
            subSampleFactor = mDepthFilterModel.mSubSampleFactor,
            holeFillLevel = mDepthFilterModel.mHoleFillLevel,
            edgePreservingLevel = mDepthFilterModel.mEdgePreservingLevel,
            temporalFilterAlpha = mDepthFilterModel.mTemporalAlpha
        )
    }

    private fun isAnyPresetChose() =
        mDepthFilterModel.mIsEnableFull || mDepthFilterModel.mIsEnableMin

    private fun enableByMainEnablerState() {
        if (!isAnyPresetChose()) {
            val main = mDepthFilterModel.mIsMainEnablerFilter
            mIView.updateDepthFilterWidgetState(
                main,
                main,
                enableSubSampleEnabler = main,
                enableSubMode = mDepthFilterModel.mIsMainEnablerSubSample && main,
                enableSubFactor = mDepthFilterModel.mIsMainEnablerSubSample && main,
                enableHoleEnabler = main,
                enableHoleLevel = mDepthFilterModel.mIsMainEnablerHoleFill && main,
                enableHoleHorizontal = mDepthFilterModel.mIsMainEnablerHoleFill && main,
                enableTemporalEnabler = main,
                enableTemporalLevel = mDepthFilterModel.mIsMainEnablerTemporalFilter && main,
                enableEdgeEnabler = main,
                enableEdgeLevel = mDepthFilterModel.mIsMainEnablerEdgePreserving && main,
                enableRemoveEnabler = main
            )
        }
    }

    private fun enableOnlyPresetSection() {
        mIView.updateDepthFilterWidgetState(
            enableFull = true,
            enableMin = true,
            enableSubSampleEnabler = false,
            enableSubMode = false,
            enableSubFactor = false,
            enableHoleEnabler = false,
            enableHoleLevel = false,
            enableHoleHorizontal = false,
            enableTemporalEnabler = false,
            enableTemporalLevel = false,
            enableEdgeEnabler = false,
            enableEdgeLevel = false,
            enableRemoveEnabler = false
        )
    }

    private fun getExposureInfo(): IntArray {
        val isAE = ExposureManager.getAE(etronCamera)
        // Avoid into SensorSettings and edit the current value but the auto is on then back to main.
        // (The current value is changed, but not set to camera.)
        var current = ExposureManager.getExposureAbsoluteTimeBySharedPrefs()
        if (current == DEVICE_FIND_FAIL) {
            current = ExposureManager.getExposureAbsoluteTime(etronCamera)
        }
        ExposureManager.setSharedPrefs(ExposureManager.INDEX_EXPOSURE_ABSOLUTE_TIME, current)
        val limit = ExposureManager.getExposureAbsoluteTimeLimit()
        return intArrayOf(isAE, current, limit[0], limit[1])
    }

    private fun getWBInfo(): IntArray {
        val isAWB = WhiteBalanceManager.getAWB(etronCamera)
        // Avoid into SensorSettings and edit the current value but the auto is on then back to main.
        // (The current value is changed, but not set to camera.)
        var current = WhiteBalanceManager.getCurrentWBBySharedPrefs()
        if (current == EYS_ERROR) {
            current = WhiteBalanceManager.getCurrentWB(etronCamera)
        }
        WhiteBalanceManager.setSharedPrefs(WhiteBalanceManager.INDEX_CURRENT_WHITE_BALANCE, current)
        var limit = WhiteBalanceManager.getWBLimitBySharedPrefs()
        if (limit[0] == EYS_ERROR) {
            limit = WhiteBalanceManager.getWBLimit(etronCamera)!!
        }
        return intArrayOf(isAWB, current, limit[0], limit[1])
    }

    private fun sensorCheckedChanged(tag: String, enabled: Boolean) {
        if (tag == TAG_EXPOSURE) {
            val ae = ExposureManager.isAE(etronCamera)
            if (ae == enabled) {
                // Avoid first setup the dialog and callback this.
                return
            }
            val result = ExposureManager.setAE(etronCamera, enabled)
            if (enabled) {
                if (result == 0) {
                    mIView.toast(getString(R.string.camera_ae_enabled))
                } else {
                    mIView.toast(getString(R.string.camera_ae_enabled_failed))
                    mIView.onSensorCheckedChanged(tag, !enabled)
                }
            } else {
                if (result == 0) {
                    mIView.toast(getString(R.string.camera_ae_disabled))
                    // Camera will not auto set to the past value, so force = true
                    ExposureManager.setExposureAbsoluteTimeBySharedPrefs(etronCamera, true)
                } else {
                    mIView.toast(getString(R.string.camera_ae_disabled_failed))
                    mIView.onSensorCheckedChanged(tag, !enabled)
                }
            }
        } else if (tag == TAG_WHITE_BALANCE) {
            val awb = WhiteBalanceManager.isAWB(etronCamera)
            if (awb == enabled) {
                // Avoid first setup the dialog and callback this.
                return
            }
            val result = WhiteBalanceManager.setAWB(etronCamera, enabled)
            if (enabled) {
                if (result == 0) {
                    mIView.toast(getString(R.string.camera_awb_enabled))
                } else {
                    mIView.toast(getString(R.string.camera_awb_enabled_failed))
                    mIView.onSensorCheckedChanged(tag, !enabled)
                }
            } else {
                if (result == 0) {
                    mIView.toast(getString(R.string.camera_awb_disabled))
                    // Camera will not auto set to the past value, so force = true
                    WhiteBalanceManager.setCurrentWBBySharedPrefs(etronCamera, true)
                } else {
                    mIView.toast(getString(R.string.camera_awb_disabled_failed))
                    mIView.onSensorCheckedChanged(tag, !enabled)
                }
            }
        } else if (tag == TAG_LOW_LIGHT_COMPENSATION) {
            val llc = LightSourceManager.isLLC(etronCamera)
            if (llc == enabled) {
                // Avoid first setup the dialog and callback this.
                return
            }
            val result = LightSourceManager.setLLC(etronCamera, enabled)
            if (enabled) {
                if (result == 0) {
                    mIView.toast(getString(R.string.camera_llc_enabled))
                } else {
                    mIView.toast(getString(R.string.camera_llc_enabled_failed))
                    mIView.onSensorCheckedChanged(tag, !enabled)
                }
            } else {
                if (result == 0) {
                    mIView.toast(getString(R.string.camera_llc_disabled))
                } else {
                    mIView.toast(getString(R.string.camera_llc_disabled_failed))
                    mIView.onSensorCheckedChanged(tag, !enabled)
                }
            }
        }
    }

    private fun sensorValueChanged(tag: String?, value: Int) {
        if (tag == TAG_EXPOSURE) {
            ExposureManager.setExposureAbsoluteTime(etronCamera, value)
            ExposureManager.setSharedPrefs(ExposureManager.INDEX_EXPOSURE_ABSOLUTE_TIME, value)
        } else if (tag == TAG_WHITE_BALANCE) {
            WhiteBalanceManager.setCurrentWB(etronCamera, value)
            WhiteBalanceManager.setSharedPrefs(
                WhiteBalanceManager.INDEX_CURRENT_WHITE_BALANCE,
                value
            )
        }
    }

    private fun dumpIR() {
        loge("[ir_ext] camerapresenter updateIRChangeDialog++ ${irManager.irMin} ${irManager.irMax} ${irManager.irCurrentVal} ${irManager.irExtendedFlag} ")
    }

    private fun checkForSupportedDevice(pid: Int, lowSwitch: Boolean = false): CameraMode? {
        val defaultMode = CameraModeManager.getDefaultMode(pid, etronCamera!!.isUSB3, lowSwitch)
        if (defaultMode != null) {
            mCameraMode = defaultMode
            logi("Defaults applied : $mCameraMode")
        }
        return defaultMode
    }

    private fun updatePreviewSizeSetting() {
        if (etronCamera == null) {
            return
        }
        val supportedSize = etronCamera!!.supportedSize
        logi("supportedSize:$supportedSize")
        if (supportedSize != null && supportedSize.isNotEmpty()) {
            val appSettings = getInstance(mContext)
            appSettings.put(SUPPORTED_SIZE, supportedSize)
            appSettings.saveAll()
        }
    }

    private fun startCameraViaDefaults(default: CameraMode?) {
        if (default == null) {
            mIView.showSnack(getString(R.string.camera_not_known_not_starting)) {}
            setCanNavigate(true)
            return
        }
        val appSettings = getInstance(mContext)
        appSettings.put(PRESET_NUMBER, -1)
        appSettings.saveAll()
        logd("Set video mode : ${mCameraMode!!.videoMode}")
        etronCamera?.videoMode = default.videoMode
        configureInterleaveMode(
            getInstance(mContext).get(
                INTERLEAVE_FPS_CHOSEN,
                false
            ) || default.videoMode > 9
        )
        if (default.rgbCameraState != null) {
            loge("Starting RGB")
            setupRGBSize(true)
            rgbCameraToggle()
        }
        if (default.depthCameraState != null) {
            loge("Starting DEPTH")
            if (etronCamera == null) {
                mIView.showSnack(getString(R.string.camera_error_depth_failed))
                return
            }
            setupDepthSize(true)
            depthCameraToggle()
        }

        updateUI() //the sizes are known at this point
    }

    private fun startCameraViaPreferences() {
        if (etronCamera == null) {
            mIView.toast(getString(R.string.camera_error_not_connected_abort))
            setCanNavigate(true)
            return
        }
        val appSettings = getInstance(mContext)

        val presetMode = appSettings.get(KEY_PRESET_MODE, "")
        val fps = appSettings.get(COLOR_FRAME_RATE, mCameraMode!!.rgbCameraState!!.fps)

        if (mProductVersion != null && ((mProductVersion!! == "YX8059" && presetMode == "5") ||
                    (mProductVersion!! == "EX8036_L" &&
                            (presetMode == "1" || presetMode == "2" || presetMode == "3" || presetMode == "8" ||
                                    presetMode == "12" || presetMode == "13" || presetMode == "14" || presetMode == "15" ||
                                    presetMode == "16" || presetMode == "18" || presetMode == "19" || presetMode == "28")) ||
                    (mProductVersion!! == "EX8036" &&
                            (presetMode == "1" || presetMode == "3" || presetMode == "5" || presetMode == "6" ||
                                    (presetMode == "7" && fps > 30) || presetMode == "8" || (presetMode == "10" && fps > 30) ||
                                    presetMode == "14")) ||
                    (mProductVersion!! == "YX8062" && (presetMode == "1" || presetMode == "2")))
        ) {
            mIView.showWarningDialog(mOnAlertListener)
        } else {
            startPreview()
        }
    }

    private fun startPreview() {
        val appSettings = getInstance(mContext)
        mEnableStreamColor = appSettings.get(ENABLE_STREAM_COLOR, mEnableStreamColor)
        mEnableStreamDepth = appSettings.get(ENABLE_STREAM_DEPTH, mEnableStreamDepth)
        mStreamInfoIndexColor = appSettings.get(ETRON_INDEX, mStreamInfoIndexColor)
        mStreamInfoIndexDepth = appSettings.get(DEPTH_INDEX, mStreamInfoIndexDepth)
        monitorFramerate = appSettings.get(MONITOR_FRAMERATE, monitorFramerate)
        zFar = appSettings.get(Z_FAR, zFar)

        mCameraMode!!.videoMode = appSettings.get(DEPTH_DATA_TYPE, mCameraMode!!.videoMode)
        logd("Set video mode : ${mCameraMode!!.videoMode}")
        etronCamera?.videoMode = mCameraMode!!.videoMode
        ExposureManager.setAEBySharedPrefs(etronCamera)
        WhiteBalanceManager.setAWBBySharedPrefs(etronCamera)
        LightSourceManager.setCurrentLSBySharedPrefs(etronCamera)
        LightSourceManager.setLLCBySharedPrefs(etronCamera)
        configureInterleaveMode(getInstance(mContext).get(INTERLEAVE_FPS_CHOSEN, false))
        if (mEnableStreamColor) {
            mEnableStreamColor = false
            setupRGBSize(false)
            rgbCameraToggle()
        } else {
            mCameraMode?.rgbCameraState?.resolution = Size(0, 0)
        }
        if (mEnableStreamDepth) {
            mEnableStreamDepth = false
            setupDepthSize(false)
            depthCameraToggle()
        } else {
            mCameraMode?.depthCameraState?.resolution = Size(0, 0)
        }

        updateUI() //the sizes are known at this point
    }

    private fun configureInterleaveMode(enabled: Boolean) {
        if (etronCamera == null) {
            return
        }
        if (CameraModeManager.isSupportedInterLeaveMode(
                mContext,
                etronCamera!!.pid,
                if (etronCamera!!.isUSB3) "3" else "2",
                mProductVersion!!.contains("_L")
            )
        ) {
            val res = etronCamera!!.setInterleaveMode(enabled)
            if (BuildConfig.DEBUG) {
                logi(
                    "[esp_interleave] INTERLEAVE_FPS_CHOSEN=${
                        getInstance(mContext).get(
                            INTERLEAVE_FPS_CHOSEN,
                            false
                        )
                    }"
                )
                logi("[esp_interleave] enable=$enabled res=$res")
            }
        }
    }

    private fun setupRGBSize(isDefaultMode: Boolean) {
        val streamInfoColorList = etronCamera!!.getStreamInfoList(INTERFACE_NUMBER_COLOR)
        if (streamInfoColorList != null && streamInfoColorList.isNotEmpty()) {
            if (isDefaultMode) {
                var rgbIndex = -1
                streamInfoColorList.forEachIndexed { i, info ->
                    if (info.height == mCameraMode!!.rgbCameraState!!.resolution.height &&
                        info.width == mCameraMode!!.rgbCameraState!!.resolution.width &&
                        info.bIsFormatMJPEG == mCameraMode!!.rgbCameraState!!.isMJPEG
                    ) {
                        rgbIndex = i
                    }
                }
                if (rgbIndex != -1) {
                    mStreamInfoIndexColor = rgbIndex
                } else {
                    loge("No matching resolution found for camera state & streaminfo, generating default")
                }
                mCameraMode!!.rgbCameraState!!.resolution = Size(
                    streamInfoColorList[mStreamInfoIndexColor].width,
                    streamInfoColorList[mStreamInfoIndexColor].height
                )
            } else {
                mCameraMode!!.rgbCameraState = CameraState(
                    mCameraMode!!.rgbCameraState!!.isMJPEG,
                    Size(
                        streamInfoColorList[mStreamInfoIndexColor].width,
                        streamInfoColorList[mStreamInfoIndexColor].height
                    ),
                    getInstance(mContext).get(COLOR_FRAME_RATE, mCameraMode!!.rgbCameraState!!.fps)
                )
            }
        }
    }

    private fun setupDepthSize(isDefaultMode: Boolean) {
        val streamInfoDepthList = etronCamera!!.getStreamInfoList(UVCCamera.INTERFACE_NUMBER_DEPTH)
        if (streamInfoDepthList != null && streamInfoDepthList.isNotEmpty()) {
            // Bit8 PIF's "width / 2" in csv / get from FW / set to camera, "real width" to calculate something ...
            val depthWidthModifier =
                if (mCameraMode!!.videoMode == VideoMode.RECTIFY_8_BITS || mCameraMode!!.videoMode == VideoMode.RAW_8_BITS ||
                    mCameraMode!!.videoMode == VideoMode.RECTIFY_8_BITS_INTERLEAVE_MODE || mCameraMode!!.videoMode == VideoMode.RAW_8_BITS_INTERLEAVE_MODE
                ) 2 else 1
            if (isDefaultMode) {
                var depthIndex = -1
                streamInfoDepthList.forEachIndexed { i, info ->
                    if (info.height == mCameraMode!!.depthCameraState!!.resolution.height && info.width == mCameraMode!!.depthCameraState!!.resolution.width) {
                        depthIndex = i
                    }
                }
                if (depthIndex != -1) {
                    mStreamInfoIndexDepth = depthIndex
                } else {
                    loge("No matching resolution found for camera state & streaminfo, generating default - depth")
                }
                mCameraMode!!.depthCameraState!!.resolution = Size(
                    streamInfoDepthList[mStreamInfoIndexDepth].width * depthWidthModifier,
                    streamInfoDepthList[mStreamInfoIndexDepth].height
                )
            } else {
                mCameraMode!!.depthCameraState = CameraState(
                    mCameraMode!!.depthCameraState!!.isMJPEG,
                    Size(
                        streamInfoDepthList[mStreamInfoIndexDepth].width * depthWidthModifier,
                        streamInfoDepthList[mStreamInfoIndexDepth].height
                    ),
                    getInstance(mContext).get(
                        DEPTH_FRAME_RATE,
                        mCameraMode!!.depthCameraState!!.fps
                    )
                )
            }
        }
    }

    private fun updateUI() {
        mIView.enableSensorSettingsButton(true)
        if (aspectRatioEnabled) {
            GlobalScope.launch(Dispatchers.Main) {
                mIView.getDepthTextureView()
                    .setAspectRatio(mCameraMode!!.depthCameraState?.getAspectRatio() ?: 0.0)
                mIView.getRGBTextureView()
                    .setAspectRatio(mCameraMode!!.rgbCameraState?.getAspectRatio() ?: 0.0)
            }
        }
        setCanNavigate(::canNavigate.toggleTimeout(CAMERA_SETTINGS_TOGGLE) {
            setCanNavigate(canNavigate)
        })
    }

    private fun configureIRSupport(isFirstLaunch: Boolean) {
        if (etronCamera == null) {
            return
        }
        if (irOverride) {
            mIView.enableIRButton(true)
            irManager.initIR(etronCamera!!, isFirstLaunch)
        } else {
            mIView.enableIRButton(false)
            irManager.setIrCurrentVal(etronCamera, 0)
        }
    }

    private fun getQualityCfg(product: String): List<String>? {
        val path = when {
            product.contains("8036_L") -> "QualityCfg/EX8036_DM_Quality_Register_Setting.cfg"
            else -> "QualityCfg/${product}_DM_Quality_Register_Setting.cfg"
        }
        return try {
            mContext.assets.open(path).bufferedReader().readLines()
        } catch (e: FileNotFoundException) {
            loge("getQualityCfg path : $path, FileNotFoundException")
            null
        }
    }

    private suspend fun setQualityRegister(lines: List<String>) = withContext(Dispatchers.IO) {
        val lineValue = IntArray(lines[0].length)
        for (line in lines) {
            for (i in line.indices) {
                when {
                    line[i] in '0'..'9' -> {
                        lineValue[i] = line[i] - '0'
                    }
                    line[i] in 'A'..'F' -> {
                        lineValue[i] = line[i] - 'A' + 10
                    }
                    line[i] in 'a'..'f' -> {
                        lineValue[i] = line[i] - 'a' + 10
                    }
                    else -> {
                        lineValue[i] = 0
                    }
                }
            }
            // ex : 0xF402,0xFF,0xEF
            if ((line[0] == '0') && (line[6] == ',') && (line[11] == ',') && (line[12] == '0')) {
                val regAddress =
                    lineValue[2].shl(12) + lineValue[3].shl(8) + lineValue[4].shl(4) + lineValue[5]
                val validDataRange = lineValue[9].shl(4) + lineValue[10]
                val notValidDataRange = validDataRange.toUShort().inv()
                val data = lineValue[14].shl(4) + lineValue[15]

                val tmp = arrayOfNulls<String>(1)
                if (etronCamera != null) {
                    etronCamera!!.getHWRegisterValue(tmp, regAddress)
                    var regValue = tmp[0]!!.toInt(16).toUShort()
                    regValue = regValue.and(notValidDataRange)
                    regValue = regValue.or(data.toUShort())
                    var retryCount = 5
                    while (etronCamera != null && etronCamera!!.setHWRegisterValue(
                            regAddress,
                            regValue.toInt()
                        ) < 0 && retryCount > 0
                    ) {
                        loge("setQualityRegister fail, retry")
                        retryCount--
                        delay(5)
                    }
                    if (retryCount <= 0) {
                        mIView.showQualityFailDialog(mOnAlertListener)
                        break
                    }
                    delay(5) // delay time, need fine tune in the future
                }
            }
        }
    }

    private fun setCanNavigate(flag: Boolean) {
        mIView.enableSettingsButton(flag)
        canNavigate = flag
    }

    /*Helpers*/
    private fun getString(id: Int): String {
        return mContext.getString(id)
    }

    @Synchronized
    private fun syncFrameCnt(cnt: Int) {
        if (mSyncMap[cnt] == null) {
            mSyncMap[cnt] = 1
        } else {
            mSyncMap[cnt] = mSyncMap[cnt]!!.toInt() + 1
            if (mSyncMap[cnt] == 3) {
                mIView.onSyncFrameCnt(cnt)
                mSyncMap.remove(cnt)
                mIMUCntMap[cnt] = false
            }
        }
    }

    private val mOnDeviceConnectListener: USBMonitor.OnDeviceConnectListener =
        object : USBMonitor.OnDeviceConnectListener {

            override fun onAttach(device: UsbDevice?) {
                logi("usb device attached: $device")
                val usbDevice: UsbDevice =
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
                    CameraDialog.showDialog(mIView.getActivity(), usbMonitor)
                } else {
                    usbMonitor!!.requestPermission(usbDevice)
                }
            }

            override fun onConnect(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                if (!createNew) {
                    return
                }
                if (etronCamera == null) {
                    etronCamera = EtronCamera()
                }
                GlobalScope.launch(Dispatchers.IO) {
                    if (ctrlBlock!!.isIMU) {
                        if (etronCamera!!.open(ctrlBlock) == EYS_OK) {
                            logd("IMU open success")
                            mIView.enableIMUButton(true)
                            etronCamera!!.enableIMUDataOutput(true)
                            etronCamera!!.readIMUData(mIMUDataCallback)
                        }
                    } else {
                        setCanNavigate(false)
                        //open camera
                        if (etronCamera!!.open(ctrlBlock) != EYS_OK) {
                            mIView.showSnack(getString(R.string.camera_error_open_failed_retry)) {}
                            etronCamera = null
                            setCanNavigate(true)
                            return@launch //jump out as we cannot continue no camera to work with
                        }

                        mProductVersion = etronCamera!!.productVersion
                        var defaultMode: CameraMode? = null
                        if (mProductVersion != null) {
                            if (mProductVersion!!.contains("8036")) {
                                val lowSwitch = IntArray(1)
                                etronCamera!!.getFWRegisterValue(lowSwitch, 0xE5)
                                if (lowSwitch[0] == 1) {
                                    mProductVersion += "_L"
                                }
                            }
                            defaultMode = checkForSupportedDevice(
                                etronCamera!!.pid,
                                mProductVersion!!.contains("_L")
                            )
                        }
                        mUsbType = if (etronCamera!!.isUSB3) VALUE_USB_TYPE_3 else VALUE_USB_TYPE_2
                        depthSurface?.release()
                        depthSurface = null
                        rgbSurface?.release()
                        rgbSurface = null

                        if (getInstance(context).get(IS_CLEARED, true) || getInstance(context).get(
                                CAMERA_VERSION,
                                ""
                            ) != mProductVersion || getInstance(context).get(
                                KEY_USB_TYPE,
                                ""
                            ) != mUsbType
                        ) {
                            loge("setup startCameraViaDefaults")
                            updatePreviewSizeSetting()
                            startCameraViaDefaults(defaultMode)
                            configureIRSupport(true)
                        } else {
                            loge("setup startCameraViaPreferences")
                            startCameraViaPreferences()
                            configureIRSupport(false)
                        }

                        // Before preview is better, but the register will error...
                        val qualityCfg = getQualityCfg(mProductVersion!!)
                        if (qualityCfg != null) {
                            setQualityRegister(qualityCfg)
                        }
                        if (mDepthInfo != null) {
                            if (mProductVersion != null && mProductVersion!!.contains("8062")) {
                                if (etronCamera!!.adjustFocalLength(
                                        mDepthInfo!!.width,
                                        mDepthInfo!!.height
                                    ) == eys_error.EYS_SUCCESS.ordinal
                                ) {
                                    mIView.enableFocalLengthButton(true)
                                }
                            }
                            val fileIndex = etronCamera!!.currentFileIndex
                            mRectifyLogData = etronCamera!!.getRectifyLogData(fileIndex)
                            mZDBuffer = etronCamera!!.getZDTableValue(fileIndex)
                            mIView.enableAccuracySettingsButton(true)
                        }
                        if (monitorFramerate) {
                            etronCamera?.setMonitorFrameRate(monitorFramerate, CAMERA_COLOR)
                            etronCamera?.setMonitorFrameRate(
                                monitorFramerate,
                                UVCCamera.CAMERA_DEPTH
                            )
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
                    etronCamera!!.destroy()
                    etronCamera = null
                }
                mIView.hideDialogs()
                mIView.enableIRButton(false)
                mIView.enableDepthFilterButton(true)
                mIView.enableColorPaletteButton(false)
                mIView.enablePLYButton(false)
                mIView.enableLivePlyButton(false)
                mIView.enableSensorSettingsButton(false)
                mIView.enableIMUButton(false)
                mIView.enableFocalLengthButton(false)
                mIView.enableAccuracySettingsButton(false)
                mIView.updateInfoTextRGB()
                mIView.updateInfoTextDepth()
                mEnableStreamColor = false
                mEnableStreamDepth = false
                depthMeasureEnabled = false
                mIView.enableDepthMeasure(false)
                roiIndexes.clear()
                setCanNavigate(true)
            }

            override fun onDetach(device: UsbDevice?) {
                mProductVersion = ""
                mUsbType = ""
                getInstance(context).put(CAMERA_VERSION, "")
                getInstance(context).put(KEY_USB_TYPE, "")
                getInstance(context).put(PRESET_NUMBER, -1)
                getInstance(context).saveAll()
                mIView.toast(getString(R.string.usb_device_detached))
            }

            override fun onCancel() = logi("usb device - onCancel")
        }

    private val mOnSensorListener = object : SensorDialogFragment.OnSensorListener {

        override fun onCheckedChanged(tag: String, enabled: Boolean) {
            sensorCheckedChanged(tag, enabled)
        }

        override fun onSeekBarChanged(tag: String, value: Int) {
            sensorValueChanged(tag, value)
        }
    }

    private val mLivePlyPresenterCallback = ILivePlyCallback { colorArray, depthVertex ->
        calculateLivePlyFpsShow(context)
        mIView.renderLivePly(colorArray, depthVertex)
    }

    private fun calculateLivePlyFpsShow(context: Context) {
        if (monitorFramerate) {
            when (mLivePlyFrameCount) {
                0 -> {
                    mLivePlyStartTime = System.nanoTime()
                    mLivePlyFrameCount++
                }
                FRAME_COUNT_INTERVAL -> {
                    mLivePlyEndTime = System.nanoTime()
                    val fps =
                        FRAME_COUNT_INTERVAL / ((mLivePlyEndTime - mLivePlyStartTime) / 1_000_000_000.0)
                    mIView.onPlyFps(context.getString(R.string.camera_preview_fps_dynamic_ply, fps))
                    mLivePlyFrameCount = 0
                }
                else -> {
                    mLivePlyFrameCount++
                }
            }
        } else {
            mIView.onPlyFps("")
        }
    }

    private val mOnAlertListener = object : AlertDialogFragment.OnAlertListener {

        override fun onClick(tag: String?, button: Int) {
            when (button) {
                DialogInterface.BUTTON_POSITIVE -> {
                    if (tag == TAG_HIGH_PERFORMANCE) {
                        GlobalScope.launch(Dispatchers.IO) {
                            startPreview()
                        }
                    } else if (tag == TAG_ERROR_HANDLE) {
                        Runtime.getRuntime().exit(0)
                    }
                }
            }
        }
    }

    /**
     * Callbacks & Frames
     * */
    private var rgbfpsThrottleJob: Job? = null
    private val mColorIFrameCallback = IFrameCallback { frame, frameCount ->
        if (mSyncFrameCnt) {
            syncFrameCnt(frameCount)
        } else {
            mIView.updateFrameCntRGB(frameCount)
        }
        if (rgbfpsThrottleJob == null && mEnableStreamColor && monitorFramerate) {
            rgbfpsThrottleJob = GlobalScope.launch(Dispatchers.IO) {
                val rgbFPS = etronCamera?.getCurrentFrameRate(CAMERA_COLOR)
                if (rgbFPS != null) {
                    if (BuildConfig.DEBUG) {
                        mIView.updateInfoTextRGB(
                            context.getString(
                                R.string.camera_preview_fps_measurement_debug,
                                rgbFPS.mFrameRatePreview,
                                rgbFPS.mFrameRateUvc
                            )
                        )
                    } else {
                        mIView.updateInfoTextRGB(
                            context.getString(
                                R.string.camera_preview_fps_measurement_release,
                                rgbFPS.mFrameRateUvc
                            )
                        )
                    }
                }
                delay(FPS_THROTTLE)
                rgbfpsThrottleJob = null
            }
        }
    }
    private var depthFpsThrottleJob: Job? = null

    private var count = 0
    private val depthCalculationRate = 3
    private val mDepthIFrameCallback = IFrameCallback { frame, frameCount ->
        if (mSyncFrameCnt) {
            syncFrameCnt(frameCount)
        } else {
            mIView.updateFrameCntDepth(frameCount)
        }
        if (depthFpsThrottleJob == null && mEnableStreamDepth && !depthMeasureEnabled && monitorFramerate) {
            depthFpsThrottleJob = GlobalScope.launch(Dispatchers.IO) {
                val depthFPS = etronCamera?.getCurrentFrameRate(CAMERA_DEPTH)
                if (depthFPS != null) {
                    if (BuildConfig.DEBUG) {
                        mIView.updateInfoTextDepth(
                            context.getString(
                                R.string.camera_preview_fps_measurement_debug,
                                depthFPS.mFrameRatePreview,
                                depthFPS.mFrameRateUvc
                            )
                        )
                    } else {
                        mIView.updateInfoTextDepth(
                            context.getString(
                                R.string.camera_preview_fps_measurement_release,
                                depthFPS.mFrameRateUvc
                            )
                        )
                    }
                }
                delay(FPS_THROTTLE)
                depthFpsThrottleJob = null
            }
        }

        if (depthMeasureEnabled && (count++ % depthCalculationRate == 0)) {

            val depthMeasurements = ArrayList<Pair<Int, Int>>()
            if (mProductVersion != null && mProductVersion!!.contains("8038")) {
                for (index in roiIndexes) {
                    depthMeasurements.add(
                        JavaCameraUtils.calculate8038Depth(
                            frame,
                            index,
                            mCameraMode!!.videoMode,
                            eY8038BaseLine
                        )
                    )
                }
            } else {
                for (index in roiIndexes) {
                    depthMeasurements.add(
                        JavaCameraUtils.calculateDepth(
                            frame,
                            index,
                            mCameraMode!!.videoMode,
                            mZDBuffer
                        )
                    )
                }
            }
            if (roiIndexes.size == 1) {
                mIView.updateInfoTextDepth(
                    context.getString(
                        R.string.camera_preview_depth_measure_result_with_d_value,
                        depthMeasurements[0].first,
                        depthMeasurements[0].second,
                        roiSize.size,
                        roiSize.size
                    )
                )
            } else {
                val nonZeros = depthMeasurements.filter { it.first != 0 }
                val avgDepth =
                    if (nonZeros.isEmpty()) 0 else nonZeros.sumBy { it.first } / nonZeros.size
                mIView.updateInfoTextDepth(
                    context.getString(
                        R.string.camera_preview_depth_measure_result,
                        avgDepth,
                        roiSize.size,
                        roiSize.size
                    )
                )
            }
        }
        if (mMonitorAccuracy) {
            mCurrentDepthBuffer = frame.duplicate()
        }
    }

    private val mIMUDataCallback = object : IIMUCallback {

        override fun onData(data: IMUData?) {
            if (mSyncFrameCnt) {
                if (mIMUCntMap[data!!.mFrameCount] == null) {
                    mIMUCntMap[data.mFrameCount] = true
                    syncFrameCnt(data.mFrameCount)
                } else if (mIMUCntMap[data.mFrameCount] == false) {
                    mIMUCntMap.remove(data.mFrameCount)
                }
            }
            if (mMonitorIMU) {
                mIView.onIMUInfo(data!!)
            }
        }

        override fun onCalibration(isSuccess: Boolean) {
            mIView.onIMUCalibration(isSuccess)
        }
    }

    private val mIMUListener = object : IMUDialogFragment.OnListener {

        override fun onDismiss() {
            mMonitorIMU = false
        }

        override fun onSync(sync: Boolean) {
            mSyncFrameCnt = sync
        }

        override fun onClicked(tag: String) {
            when (tag) {
                IMUDialogFragment.TAG_STATUS -> {
                    mIView.onIMUStatus(etronCamera!!.isIMUEnabled)
                }
                IMUDialogFragment.TAG_ENABLE_IMU -> {
                    etronCamera!!.enableIMUDataOutput(true)
                    etronCamera!!.readIMUData(mIMUDataCallback)
                }
                IMUDialogFragment.TAG_DISABLE_IMU -> {
                    etronCamera!!.enableIMUDataOutput(false)
                    etronCamera!!.stopReadIMUData()
                }
                IMUDialogFragment.TAG_GET_MODULE_NAME -> {
                    mIView.onIMUModuleName(etronCamera!!.imuModuleName)
                }
                IMUDialogFragment.TAG_GET_FW_VERSION -> {
                    mIView.onIMUFWVersion(etronCamera!!.imufwVersion)
                }
                IMUDialogFragment.TAG_CALIBRATION -> {
                    etronCamera!!.doIMUCalibration(mIMUDataCallback)
                }
                IMUDialogFragment.TAG_SAVE_RAW_DATA -> {
                    mIsSaveIMUData = !mIsSaveIMUData
                    if (mIsSaveIMUData) {
                        val timestampFilename = SimpleDateFormat(PLY_DATE_PATTERN).format(Date())
                        etronCamera!!.startIMULogData(timestampFilename)
                    } else {
                        etronCamera!!.stopIMULogData()
                    }
                    mIView.onIMUSaveRawData(mIsSaveIMUData)
                }
                IMUDialogFragment.TAG_RESET -> etronCamera!!.resetIMU()
            }
        }
    }

    private val mFocalLengthListener = object : FocalLengthDialogFragment.OnListener {

        override fun onFocalLength(value: Int) {
            if (etronCamera!!.adjustFocalLength(
                    mDepthInfo!!.width,
                    mDepthInfo!!.height,
                    value
                ) == eys_error.EYS_SUCCESS.ordinal
            ) {
                val focalLength = etronCamera!!.deviceFocalLength
                val focalLength2 =
                    etronCamera!!.getFlashFocalLength(mDepthInfo!!.width, mDepthInfo!!.height)
                mIView.onFocalLength(
                    focalLength[0],
                    focalLength[1],
                    focalLength[2],
                    focalLength[3],
                    focalLength2[4]
                )
            }
        }
    }

    /**
     * Rotation changes
     * */
    private var orientationListener: OrientationEventListener? = null

    private fun addOrientationListener() {
        var rotationIgnoreTimer: Long = 1
        orientationListener = object :
            OrientationEventListener(mIView.getActivity(), SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                var scale = 1f
                val orient = if (orientation in 315..360 || orientation in 0..45) {
                    0f
                } else if (orientation in 46..135) {
                    scale =
                        mIView.getDepthTextureView().height.toFloat() / mIView.getDepthTextureView().width.toFloat()
                    90f
                } else if (orientation in 136..225) {
                    180f
                } else {
                    scale =
                        mIView.getDepthTextureView().height.toFloat() / mIView.getDepthTextureView().width.toFloat()
                    270f
                }
                if ((System.currentTimeMillis() - rotationIgnoreTimer) < 1000) {
                    return
                }
                GlobalScope.launch(Dispatchers.Main) {
                    rotationIgnoreTimer = System.currentTimeMillis()
                    mIView.getDepthTextureView().rotation = orient
                    mIView.getDepthTextureView().scaleX =
                        if (mIView.getDepthTextureView().scaleX > 0) scale else -scale //keeping mirror option
                    mIView.getDepthTextureView().scaleY = scale
                    mIView.getRGBTextureView().rotation = orient
                    mIView.getRGBTextureView().scaleX =
                        if (mIView.getRGBTextureView().scaleX > 0) scale else -scale
                    mIView.getRGBTextureView().scaleY = scale
                }
            }
        }
        if (orientationListener?.canDetectOrientation()!!) orientationListener?.enable()
    }

    private val mAccuracyListener = object : RegionAccuracyDialogFragment.OnListener {

        override fun onDismiss() {
            mMonitorAccuracy = false
            mColorPaletteModel.enableRegionAccuracy(false)
            mIView.onAccuracyRegion(null)
            stopAccuracyJob()
        }

        override fun onROI(index: Int) {
            REGION_RATE.onEachIndexed { i, entry ->
                if (index == i) mDepthAccuracyRegionRatio = entry.key
            }
            mIView.onAccuracyRegion(getAccuracyRegion(true))
        }

        override fun onGroundTruth(enabled: Boolean) {
            mEnableGroundTruth = enabled
            adjustDepthInvalidBandPixel()
            mIView.onAccuracyRegion(getAccuracyRegion(true))
        }

        override fun onGroundTruthValue(value: Float) {
            mDepthAccuracyGroundTruthDistanceMM = value
            adjustDepthInvalidBandPixel()
            mIView.onAccuracyRegion(getAccuracyRegion(true))
        }

        override fun onGroundTruthValue(add: Boolean) {
            if (add)
                mDepthAccuracyGroundTruthDistanceMM += 1f
            else {
                if (mDepthAccuracyGroundTruthDistanceMM != 0f) mDepthAccuracyGroundTruthDistanceMM -= 1f
            }
            adjustDepthInvalidBandPixel()
            mIView.onGroundTruth(mDepthAccuracyGroundTruthDistanceMM)
            mIView.onAccuracyRegion(getAccuracyRegion(true))
        }
    }

    // Accuracy

    private var mAccuracyInfoJob: Job? = null
    private var mSpatialNoiseJob: Job? = null
    private var mTemporalNoiseJob: Job? = null

    private var mCurrentDepthBuffer: ByteBuffer? = null

    private fun getCurrentDepthBuffer(): ByteBuffer? {
        return mCurrentDepthBuffer
    }

    private fun startAccuracyJob() {
        if (mAccuracyInfoJob == null) {
            mAccuracyInfoJob = GlobalScope.launch(Dispatchers.IO) {
                try {
                    while (isActive) {
                        if (getCurrentDepthBuffer() != null) {
                            calculateDepthAccuracyInfo(getCurrentDepthBuffer()!!.duplicate())
                        }
                        delay(100)
                    }
                } finally {
                    // For detect Job cancel
                }
            }
        }
        if (mSpatialNoiseJob == null) {
            mSpatialNoiseJob = GlobalScope.launch(Dispatchers.IO) {
                try {
                    while (isActive) {
                        if (getCurrentDepthBuffer() != null) {
                            calculateDepthSpatialNoise(getCurrentDepthBuffer()!!.duplicate())
                        }
                        delay(100)
                    }
                } finally {
                }
            }
        }
        if (mTemporalNoiseJob == null) {
            mTemporalNoiseJob = GlobalScope.launch(Dispatchers.IO) {
                try {
                    while (isActive) {
                        if (getCurrentDepthBuffer() != null) {
                            calculateDepthTemporalNoise(getCurrentDepthBuffer()!!.duplicate())
                        }
                        delay(100)
                    }
                } finally {
                }
            }
        }
    }

    private fun stopAccuracyJob() {
        if (mAccuracyInfoJob != null) {
            mAccuracyInfoJob!!.cancel()
            mAccuracyInfoJob = null
        }
        if (mTemporalNoiseJob != null) {
            mTemporalNoiseJob!!.cancel()
            mTemporalNoiseJob = null
        }
        if (mSpatialNoiseJob != null) {
            mSpatialNoiseJob!!.cancel()
            mSpatialNoiseJob = null
        }
    }

    private fun getAccuracyRegion(isPaint: Boolean = false): Rect {
        var nHorizontalMargin =
            (((1 - mDepthAccuracyRegionRatio) * (mCameraMode!!.depthCameraState!!.resolution.width - mDepthInvalidBandPixel)) / 2).toInt()
        nHorizontalMargin =
            minOf(nHorizontalMargin, mCameraMode!!.depthCameraState!!.resolution.width - 1)
        var nVerticalMargin =
            (((1 - mDepthAccuracyRegionRatio) * mCameraMode!!.depthCameraState!!.resolution.height) / 2).toInt()
        nVerticalMargin =
            minOf(nVerticalMargin, mCameraMode!!.depthCameraState!!.resolution.height - 1)

        var left = nHorizontalMargin + mDepthInvalidBandPixel
        var right = mCameraMode!!.depthCameraState!!.resolution.width - nHorizontalMargin - 1
        var top = nVerticalMargin
        var bottom = mCameraMode!!.depthCameraState!!.resolution.height - nVerticalMargin - 1

        if (isPaint) {
            val hRation =
                mIView.getDepthTextureView().width / mCameraMode!!.depthCameraState!!.resolution.width.toFloat()
            val vRation =
                mIView.getDepthTextureView().height / mCameraMode!!.depthCameraState!!.resolution.height.toFloat()
            left = (left * hRation).toInt()
            right = (right * hRation).toInt()
            top = (top * vRation).toInt()
            bottom = (bottom * vRation).toInt()
        }
        return Rect(left, top, right, bottom)
    }

    // Get depth z of ROI by java layer (slow and the data will be inaccurate if depth filter on ...)
    private fun getDepthZOfROI(rect: Rect, frame: ByteBuffer): IntArray {
        val rangeX = rect.left..rect.right
        val rangeY = rect.top..rect.bottom
        val z = IntArray((rect.width() + 1) * (rect.height() + 1))
        for (y in rangeY) {
            for (x in rangeX) {
                val index = x + (mCameraMode!!.depthCameraState!!.resolution.width * y)
                z[(y - rect.top) * (rect.width() + 1) + (x - rect.left)] =
                    JavaCameraUtils.calculateDepth(
                        frame,
                        index,
                        mCameraMode!!.videoMode,
                        mZDBuffer
                    ).first
            }
        }
        return z
    }

    private fun sortZ(depthZOfROI: IntArray) {
        val tmp = depthZOfROI.clone()
        tmp.sort()
        val firstNotZero = tmp.indexOfFirst { it != 0 }
        if (firstNotZero == tmp.lastIndex) {
            return
        }
        val dblDeleteBoundaryRatio = 0.005
        val boundCount = ((tmp.lastIndex + 1 - firstNotZero) * dblDeleteBoundaryRatio).toInt()
        if (boundCount == 0) return
        val upperBound = tmp[tmp.lastIndex + 1 - boundCount]
        val lowerBound = tmp[firstNotZero + boundCount]
        depthZOfROI.forEachIndexed { index, value ->
            run {
                if (value == 0) return@forEachIndexed
                if (value > upperBound || upperBound < lowerBound) depthZOfROI[index] = 0
            }
        }
    }

    private fun calculateFittedPlane(depthZOfROI: IntArray, w: Int, h: Int): Array<Float> {
        var matrixXX = 0f
        var matrixYY = 0f
        var matrixXY = 0f
        var matrixX = 0f
        var matrixY = 0f
        var matrixN = 0f
        var matrixXZ = 0f
        var matrixYZ = 0f
        var matrixZ = 0f
        val matrixBase: Float
        var idx: Int

        for (y in 0 until h) {
            for (x in 0 until w) {
                idx = y * w + x
                if (depthZOfROI[idx] != 0) {
                    matrixXX += (x * x)
                    matrixYY += (y * y)
                    matrixXY += (x * y)
                    matrixX += x
                    matrixY += y
                    matrixN++
                    matrixXZ += (x * depthZOfROI[idx])
                    matrixYZ += (y * depthZOfROI[idx])
                    matrixZ += depthZOfROI[idx]
                }
            }
        }

        matrixBase = (matrixXX * matrixYY * matrixN) +
                (2 * matrixXY * matrixX * matrixY) -
                (matrixX * matrixX * matrixYY) -
                (matrixY * matrixY * matrixXX) -
                (matrixXY * matrixXY * matrixN)
        val a = ((matrixXZ * matrixYY * matrixN) +
                (matrixZ * matrixXY * matrixY) +
                (matrixYZ * matrixX * matrixY) -
                (matrixZ * matrixYY * matrixX) -
                (matrixXZ * matrixY * matrixY) -
                (matrixYZ * matrixXY * matrixN)) / matrixBase
        val b = ((matrixYZ * matrixXX * matrixN) +
                (matrixXZ * matrixX * matrixY) +
                (matrixZ * matrixXY * matrixX) -
                (matrixYZ * matrixX * matrixX) -
                (matrixZ * matrixXX * matrixY) -
                (matrixXZ * matrixXY * matrixN)) / matrixBase
        val d = ((matrixZ * matrixXX * matrixYY) +
                (matrixYZ * matrixXY * matrixX) +
                (matrixXZ * matrixXY * matrixY) -
                (matrixXZ * matrixYY * matrixX) -
                (matrixYZ * matrixXX * matrixY) -
                (matrixZ * matrixXY * matrixXY)) / matrixBase
        return arrayOf(a, b, d)
    }

    private fun calculateZAccuracy(
        w: Int,
        h: Int,
        vecBefore: Vector3,
        vecAfter: Vector3,
        depthZOfROI: IntArray
    ): Double {
        if (!mEnableGroundTruth || mDepthAccuracyGroundTruthDistanceMM == 0f) return 0.0

        val roiPointNum = w * h
        val centerX = w / 2
        val centerY = h / 2

        val dotResult = androidx.compose.ui.graphics.vectormath.dot(vecBefore, vecAfter)
        val before = androidx.compose.ui.graphics.vectormath.length2(vecBefore)
        val after = androidx.compose.ui.graphics.vectormath.length2(vecAfter)
        val rotationAngle = acos((dotResult / (before * after)).toDouble())

        var u = androidx.compose.ui.graphics.vectormath.cross(vecBefore, vecAfter)
        val norm = androidx.compose.ui.graphics.vectormath.length2(u)
        u /= norm

        val rotationMatrix = arrayOf(
            arrayOf(0.0, 0.0, 0.0), arrayOf(0.0, 0.0, 0.0), arrayOf(0.0, 0.0, 0.0)
        )
        rotationMatrix[0][0] = cos(rotationAngle) + u.x * u.x * (1 - cos(rotationAngle))
        rotationMatrix[0][1] = u.x * u.y * (1 - cos(rotationAngle) - u.z * sin(rotationAngle))
        rotationMatrix[0][2] = u.y * sin(rotationAngle) + u.x * u.z * (1 - cos(rotationAngle))

        rotationMatrix[1][0] = u.z * sin(rotationAngle) + u.x * u.y * (1 - cos(rotationAngle))
        rotationMatrix[1][1] = cos(rotationAngle) + u.y * u.y * (1 - cos(rotationAngle));
        rotationMatrix[1][2] = -u.x * sin(rotationAngle) + u.y * u.z * (1 - cos(rotationAngle))

        rotationMatrix[2][0] = -u.y * sin(rotationAngle) + u.x * u.z * (1 - cos(rotationAngle))
        rotationMatrix[2][1] = u.x * sin(rotationAngle) + u.y * u.z * (1 - cos(rotationAngle))
        rotationMatrix[2][2] = cos(rotationAngle) + u.z * u.z * (1 - cos(rotationAngle))

        val index = centerY * w + centerX
        val transZ =
            rotationMatrix[2][0] * centerX + rotationMatrix[2][1] * centerY + rotationMatrix[2][2] * depthZOfROI[index] - mDepthAccuracyDistanceMM

        val vecZError = DoubleArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val z = depthZOfROI[i]
                if (z != 0) {
                    vecZError[i] =
                        (rotationMatrix[2][0] * x + rotationMatrix[2][1] * y + rotationMatrix[2][2] * z - mDepthAccuracyGroundTruthDistanceMM - transZ)
                }
            }
        }
        vecZError.sort()
        val midError = vecZError[roiPointNum / 2]
        return midError / mDepthAccuracyGroundTruthDistanceMM
    }

    private fun calculateDepthAccuracyInfo(frame: ByteBuffer) {
        val rect = getAccuracyRegion()
        val width = rect.width() + 1
        val height = rect.height() + 1

        val bytes = ByteArray(frame.remaining())
        frame.get(bytes, 0, bytes.size)
        val depthZOfROI =
            etronCamera!!.getDepthZOfROI(bytes, rect.left, rect.top, rect.right, rect.bottom)

        var fillCount = 0
        for (z in depthZOfROI) {
            if (z != 0) {
                ++fillCount
            }
        }
        if (fillCount == 0) {
            return
        }

        sortZ(depthZOfROI)
        val fittedPlane = calculateFittedPlane(depthZOfROI, width, height)

        val centerX = width / 2
        val centerY = height / 2

        val vecBefore = Vector3(-fittedPlane[0], -fittedPlane[1], 1f)
        val vecAfter = Vector3(0f, 0f, 1f)

        mDepthAccuracyDistanceMM =
            (fittedPlane[0] * centerX) + (fittedPlane[1] * centerY) + fittedPlane[2]
        mDepthAccuracyFillRate = fillCount / depthZOfROI.size.toFloat()
        mDepthAccuracyZAccuracy =
            calculateZAccuracy(width, height, vecBefore, vecAfter, depthZOfROI).toFloat()
        mIView.onAccuracyInfo(
            mDepthAccuracyDistanceMM,
            mDepthAccuracyFillRate * 100f,
            mDepthAccuracyZAccuracy * 100f
        )
    }

    private fun calculateDepthSpatialNoise(frame: ByteBuffer) {
        val rect = getAccuracyRegion()
        val width = rect.width() + 1
        val height = rect.height() + 1

        val bytes = ByteArray(frame.remaining())
        frame.get(bytes, 0, bytes.size)
        val depthZOfROI =
            etronCamera!!.getDepthZOfROI(bytes, rect.left, rect.top, rect.right, rect.bottom)

        sortZ(depthZOfROI)
        val fittedPlane = calculateFittedPlane(depthZOfROI, width, height)

        var depthZSum = 0.0
        var count = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                if (depthZOfROI[i] != 0) {
                    count++
                    depthZSum += (depthZOfROI[i].toDouble() - (fittedPlane[0] * x + fittedPlane[1] * y + fittedPlane[2])).pow(
                        2.0
                    )
                }
            }
        }
        mDepthSpatialNoise = if (count > 0) sqrt(depthZSum / count).toFloat() else 0f
        if (mEnableGroundTruth && mDepthAccuracyGroundTruthDistanceMM > 0.0f) {
            mDepthSpatialNoise /= mDepthAccuracyGroundTruthDistanceMM
        } else {
            mDepthSpatialNoise = 0.0f
        }

        mDepthAngle =
            (acos(1.0f / sqrt(fittedPlane[0] * fittedPlane[0] + fittedPlane[1] * fittedPlane[1] + 1)) * 180.0f / Math.PI).toFloat()
        mDepthAngleX =
            (acos(1.0f / sqrt(fittedPlane[0] * fittedPlane[0] + 1)) * 180.0f / Math.PI).toFloat()
        if (fittedPlane[0] < 0) mDepthAngleX *= -1.0f
        mDepthAngleY =
            (acos(1.0f / sqrt(fittedPlane[1] * fittedPlane[1] + 1)) * 180.0f / Math.PI).toFloat()
        if (fittedPlane[1] > 0) mDepthAngleY *= -1.0f
        mIView.onSpatialNoise(mDepthSpatialNoise * 100f, mDepthAngle, mDepthAngleX, mDepthAngleY)
    }

    private fun calculateDepthTemporalNoise(frame: ByteBuffer) {
        val rect = getAccuracyRegion()
        val left = rect.left
        val top = rect.top
        val right = rect.right
        val bottom = rect.bottom

        val depthZ = mDepths.last()
        val STD =
            DoubleArray(mCameraMode!!.depthCameraState!!.resolution.width * mCameraMode!!.depthCameraState!!.resolution.height)

        var stdCnt = 0

        for (y in top until bottom) {
            for (x in left until right) {
                val index = x + (mCameraMode!!.depthCameraState!!.resolution.width * y)
                depthZ[index] = JavaCameraUtils.calculateDepth(
                    frame,
                    index,
                    mCameraMode!!.videoMode,
                    mZDBuffer
                ).first
            }
        }
        for (y in top until bottom) {
            for (x in left until right) {
                val index = x + (mCameraMode!!.depthCameraState!!.resolution.width * y)
                var depthZSum = 0.0
                var count = 0
                var avgDepth: Double
                mDepths.forEach {
                    if (it[index] != 0) count++
                    depthZSum += it[index]
                }
                if (count != 0) {
                    avgDepth = depthZSum / count
                    depthZSum = 0.0
                    mDepths.forEach {
                        if (it[index] != 0) depthZSum += (it[index] - avgDepth).pow(2.0)
                    }
                    STD[stdCnt++] = sqrt(depthZSum / count)
                }
            }
        }
        if (stdCnt > 0) {
            val tmp = STD.toMutableList()
            val tmp2 = tmp.subList(0, stdCnt)
            tmp2.sort()
            mDepthTemporalNoise = tmp2[stdCnt / 2].toFloat()
            if (mEnableGroundTruth && mDepthAccuracyGroundTruthDistanceMM > 0f) {
                mDepthTemporalNoise /= mDepthAccuracyGroundTruthDistanceMM
            } else {
                mDepthTemporalNoise = 0f
            }
            mIView.onTemporalNoise(mDepthTemporalNoise * 100f)
        }
        val tmp = mDepths.removeAt(0)
        mDepths.add(tmp)
    }

    private fun adjustDepthInvalidBandPixel() {
        if (!mEnableGroundTruth || mDepthAccuracyGroundTruthDistanceMM == 0f) {
            mDepthInvalidBandPixel = 0
        } else {
            val focalLength: Float
            val baseline: Float
            val mDblBaselineDist = 0.0 // always 0 ?!
            val mDblCamFocus = 0.0 // always 0 ?!
            if (mDblBaselineDist != 0.0 && mDblCamFocus != 0.0) {
                focalLength = mDblCamFocus.toFloat()
                baseline = mDblBaselineDist.toFloat()
            } else if (mRectifyLogData.ReProjectMat[14] == 0f) {
                mDepthInvalidBandPixel = 0
                return
            } else {
                val ratioMat =
                    mCameraMode!!.depthCameraState!!.resolution.height.toFloat() / mRectifyLogData.OutImgHeight
                focalLength = mRectifyLogData.ReProjectMat[11] * ratioMat
                baseline = 1.0f / mRectifyLogData.ReProjectMat[14]
            }
            val depthInvalidBandPixel =
                (baseline * focalLength / mDepthAccuracyGroundTruthDistanceMM).toInt()
            mDepthInvalidBandPixel =
                minOf(depthInvalidBandPixel, mCameraMode!!.depthCameraState!!.resolution.width)
        }
    }
}
