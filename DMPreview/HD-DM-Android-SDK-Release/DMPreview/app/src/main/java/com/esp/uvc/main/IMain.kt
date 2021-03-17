package com.esp.uvc.main

import android.graphics.Rect
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.esp.android.usb.camera.core.IMUData
import com.esp.uvc.R
import com.esp.uvc.main.common.*
import com.esp.uvc.widget.UVCCameraTextureView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.dialog_region_accuracy.view.*

interface IMain {

    interface View {
        // --- about button ---
        fun showAboutDialog(cameraName: String = "")
        // --------------------

        // --- depth filter button ---
        fun enableDepthFilterButton(isEnable: Boolean)

        fun showDepthFilterDialogFragment(
            tag: String,
            version: String,
            subSampleModeCallback: ((Int) -> Unit)? = null,
            subSampleFactorCallback: ((Int) -> Unit)? = null,
            holeFillLevelSeekCallback: ((Int) -> Unit)? = null,
            edgePreservingLevelCallback: ((Int) -> Unit)? = null,
            temporalLevelCallback: ((Float) -> Unit)? = null
        )

        fun updateDepthFilterEnablers(
            bDoDepthFilter: Boolean,
            bFullChoose: Boolean,
            bMinChoose: Boolean,
            bSubSample: Boolean,
            bEdgePreServingFilter: Boolean,
            bHoleFill: Boolean,
            bHoleFillHorizontal: Boolean,
            bTempleFilter: Boolean,
            bFlyingDepthCancellation: Boolean
        )

        fun updateDepthFilterWidgetState(
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
        )

        fun updateDepthFilterValues(
            subSampleMode: Int,
            subSampleFactor: Int,
            holeFillLevel: Int,
            edgePreservingLevel: Int,
            temporalFilterAlpha: Float
        )
        // ---------------------------

        // --- IMU button ---
        fun enableIMUButton(enabled: Boolean)

        fun showIMUDialogFragment(
            listener: IMUDialogFragment.OnListener,
            syncFrameCnt: Boolean,
            isSaveRawData: Boolean
        )

        fun onIMUStatus(enabled: Boolean)

        fun onIMUModuleName(name: String)

        fun onIMUFWVersion(version: String)

        fun onIMUCalibration(isSuccess: Boolean)

        fun onIMUSaveRawData(isSave: Boolean)

        fun onIMUInfo(data: IMUData)
        // ---------------------------

        // --- Focal length ---
        fun enableFocalLengthButton(enabled: Boolean)

        fun showFocalLengthDialogFragment(
            leftFx: Int,
            leftFy: Int,
            rightFx: Int,
            rightFy: Int,
            pixelUnit: Int,
            listener: FocalLengthDialogFragment.OnListener
        )

        fun onFocalLength(
            leftFx: Int,
            leftFy: Int,
            rightFx: Int,
            rightFy: Int,
            pixelUnit: Int,
        )

        // --- sensor settings button ---
        fun enableSensorSettingsButton(enabled: Boolean)

        fun showSensorDialogFragment(
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
        )

        fun onSensorCheckedChanged(tag: String, enabled: Boolean)
        // ------------------------------

        // --- accuracy settings button ---
        fun enableAccuracySettingsButton(enabled: Boolean)

        fun showRegionAccuracyDialogFragment(
            listener: RegionAccuracyDialogFragment.OnListener,
            roiList: List<String>,
            roiIndex: Int,
            enableGroundTruth: Boolean,
            groundTruth: Float
        )

        fun onAccuracyInfo(distance: Float, fillRate: Float, zAccuracy: Float)

        fun onTemporalNoise(temporalNoise: Float)

        fun onSpatialNoise(spatialNoise: Float, angle: Float, angleX: Float, angleY: Float)

        fun onGroundTruth(groundTruth: Float)

        fun onAccuracyRegion(rect: Rect?)
        // ------------------------------

        // --- settings button ---
        fun enableSettingsButton(enabled: Boolean)
        // -----------------------

        // --- IR button ---
        fun enableIRButton(flag: Boolean = false)

        fun showIRChangeDialog(
            iRmax: Int,
            irMin: Int,
            irCurrentValue: Int,
            extended: Boolean,
            positiveCallback: (Int, Boolean) -> Unit
        )

        fun updateIRChangeDialog(irMin: Int, irMax: Int, irCurrentValue: Int, irExtended: Boolean)
        // -----------------

        // --- color palette button ---
        fun enableColorPaletteButton(isEnable: Boolean)
        fun showColorPaletteDialog(zNear: Int, zFar: Int)
        // ----------------------------

        // --- depth measure button ---
        fun enableDepthMeasure(flag: Boolean)
        // ----------------------------

        // --- dynamic ply button ---
        fun updateColorPaletteCurrentExtreme(zNear: Int, zFar: Int)
        fun enableLivePlyButton(isEnable: Boolean)
        fun renderLivePly(colorArray: FloatArray, depthVertexArray: FloatArray)
        fun showLivePlyView(isEnable: Boolean)
        fun onPlyFps(fps: String = "")
        // ----------------------------

        // --- save ply button ---
        fun enablePLYButton(isEnable: Boolean)

        fun showPlyErrorDialog(
            isStatic: Boolean,
            listener: AlertDialogFragment.OnAlertListener,
            msg: String
        )
        // -----------------------

        fun getActivity(): FragmentActivity?

        fun toast(text: String, length: Int = Toast.LENGTH_SHORT)

        fun showSnack(
            text: String,
            length: Int = Snackbar.LENGTH_INDEFINITE,
            actionText: String = "Ok",
            onClick: (() -> Unit)? = null
        )

        fun getRGBTextureView(): UVCCameraTextureView

        fun getDepthTextureView(): UVCCameraTextureView

        fun updateInfoTextRGB(fps: String = "")

        fun updateInfoTextDepth(fps: String = "")

        fun updateFrameCntRGB(cnt: Int)

        fun updateFrameCntDepth(cnt: Int)

        fun onSyncFrameCnt(cnt: Int)

        fun letCrosshairToCenter()

        // --- common dialog ---
        fun showProgressDialog(
            flag: Boolean,
            enableTitleHint: Boolean,
            text: String = "",
            cancelable: Boolean = false,
            cancelCallback: (() -> Unit)? = null
        )

        fun showWarningDialog(listener: AlertDialogFragment.OnAlertListener)

        fun showErrorHandleDialog(listener: AlertDialogFragment.OnAlertListener)

        fun showQualityFailDialog(listener: AlertDialogFragment.OnAlertListener)

        fun hideDialogs()

        // ---------------------
    }

    interface Presenter {

        fun attach()

        fun onStart()
        fun onResume()
        fun onPause()
        fun onStop()

        fun unattach()

        fun onDepthFilterClick(tag: String)
        fun onDepthFilterMainEnablerChanged(isEnabled: Boolean)
        fun onDepthFilterFullChoose(isEnabled: Boolean)
        fun onDepthFilterMinChoose(isEnabled: Boolean)
        fun onDepthFilterHoleFillChanged(isEnabled: Boolean)
        fun onDepthFilterHoleFillHorizontalChanged(isEnabled: Boolean)
        fun onDepthFilterEdgePreservingChanged(isEnabled: Boolean)
        fun onDepthFilterTemporalFilterChanged(isEnabled: Boolean)
        fun onDepthFilterRemoveCurveChanged(isEnabled: Boolean)
        fun onDepthFilterSubsampleChanged(isEnabled: Boolean)

        fun onAccuracySettingsClick()

        fun onSensorSettingsClick()

        fun onIMUClick()

        fun onFocalLengthClick()

        fun onIRClick()

        fun onPaletteClick()
        fun onColorPaletteDistanceChange(zNear: Int, zFar: Int)
        fun onColorPaletteDistanceReset()

        fun enabledDepthMeasure(enabled: Boolean)

        fun onStartLivePly()
        fun onStopLivePly()

        fun savePLY()

        fun canNavigateFlag(): Boolean

        fun getProductVersion(): String?

        fun rgbCameraToggle()

        fun depthCameraToggle()

        //expects the exact midpoint position of the crosshair i.e. middle of the crosshair picture position
        fun updateDepthMeasurePosition(xCoordinate: Float, yCoordinate: Float)
    }
}

