package com.esp.uvc.main.settings

import android.widget.Toast
import com.esp.uvc.BasePresenter
import com.esp.uvc.BaseView
import com.google.android.material.snackbar.Snackbar

interface PreviewImageContract {

    interface Presenter : BasePresenter<View> {
        fun onStart()
        fun onResume()
        fun onPause()
        fun onStop()

        fun usePreset(usePreset: Boolean)
        fun onPresetSelected(index: Int)
        fun onColorFpsSelected(index: Int)
        fun onDepthFpsSelected(index: Int)
        fun onDepthDataTypeSelected(index: Int)
        fun canNavigate(): Boolean
        fun onExtendedSelectionChanged(selected: Boolean)
        fun onIrValueChanged(value: Int)
        fun onPostProcessChanged(enabled: Boolean)
    }

    interface View : BaseView<Presenter> {
        fun toast(text: String, length: Int = Toast.LENGTH_SHORT)
        fun showSnack(text: String, length: Int = Snackbar.LENGTH_INDEFINITE, actionText: String = "Ok", onClick: (() -> Unit)? = null)
        fun updateUILists(colorEntries: ArrayList<CharSequence>, depthEntries: ArrayList<CharSequence>, depthDataTypeEntries: ArrayList<CharSequence>)
        fun updateDepthDataTypeList(depthDataTypeEntries: ArrayList<CharSequence>)
        fun updateUISelection(colorStreamEnabled: Boolean, depthStreamEnabled: Boolean, cameraIndex: Int, depthCameraIndex: Int, depthDataTypeIndex: Int, colorFrameRate: Int, depthFrameRate: Int, postProcessingEnabled: Boolean, monitorFrameRate: Boolean, mZFar: Int, irOverride: Boolean, mirrorEnabled: Boolean, landscapeEnabled: Boolean, upsideDownEnabled: Boolean, roiSize: Int)
        fun getViewValues(): ArrayList<Any?>
        fun getRoiSizeValue(): Int
        fun updatePresetList(presets: ArrayList<String>, selection: Int)
        fun enablePresetMode(enabled: Boolean)
        fun switchPresetMode(preset: Boolean)
        fun enablePresetSelection(enabled: Boolean)
        fun enableColorStreamCheckbox(enabled: Boolean)
        fun enableDepthStreamCheckbox(enabled: Boolean)
        fun enableManualColorFpsInput(enabled: Boolean)
        fun enableManualDepthFpsInput(enabled: Boolean)
        fun enableColorResolutionSelection(enabled: Boolean)
        fun enableDepthResolutionSelection(enabled: Boolean)
        fun enableDepthFrameRateInputSelection(enabled: Boolean)
        fun updateUsingPresetSelection(isUsingPreset: Boolean)
        fun updateRoiValues(values: Array<String>)
        fun updateDepthRangeValues(values: Array<String>, selection: Int)
        fun setDepthStreamSelection(on: Boolean)
        fun updateFpsOptions(colorFpsOptions: ArrayList<String>?, depthFpsOptions: ArrayList<String>?)
        fun updateFpsSelection(colorFpsIndex: Int, depthFpsIndex: Int) //selection in dropdown menus
        fun updateFpsValues(colorFps: Int, depthFps: Int) //arbitrary value in manual mode
        fun enableIRSection(enabled: Boolean)
        fun showExtendedCheckbox(visible: Boolean)
        fun updateExtendedSelection(selected: Boolean)
        fun updateIrRange(min: Int, max: Int)
        fun updateIrValue(value: Int)
    }
}
