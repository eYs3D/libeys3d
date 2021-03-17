package com.esp.uvc.main.common

import androidx.annotation.NonNull
import com.esp.android.usb.camera.core.EtronCamera

class DepthFilterModel {
    var mIsMainEnablerFilter = true
    var mIsEnableFull = false
    var mIsEnableMin = true

    var mIsMainEnablerSubSample = true
    var mSubSampleMode = 0
    var mSubSampleFactor = 3

    var mIsMainEnablerHoleFill = true
    var mHoleFillLevel = 1
    var mIsHoleFillHorizontal = true

    var mIsMainEnablerEdgePreserving = true
    var mEdgePreservingLevel = 1

    var mIsMainEnablerTemporalFilter = true
    var mTemporalAlpha = 0.4f

    var mIsMainEnablerRemoveCurve = false

    var mCamera: EtronCamera? = null

    fun init(@NonNull etronCamera: EtronCamera) {
        mCamera = etronCamera
        minConfigs()
        updateCameraConfigs()
    }

    private fun updateCameraConfigs(){
        mCamera?.setDepthFilters(mIsMainEnablerFilter, mIsMainEnablerSubSample, mIsMainEnablerEdgePreserving,
                mIsMainEnablerHoleFill, mIsMainEnablerTemporalFilter, mIsMainEnablerRemoveCurve)
        mCamera?.setDepthFilterSubSampleParams(mSubSampleMode, mSubSampleFactor)
        mCamera?.setDepthFilterHoleFillingParams(mHoleFillLevel, mIsHoleFillHorizontal)
        mCamera?.setDepthFilterEdgePreservingParams(mEdgePreservingLevel)
        mCamera?.setDepthFilterTemporalParams(mTemporalAlpha)
    }

    fun setMainEnabler(enabled: Boolean) {
        mIsMainEnablerFilter = enabled
        mCamera?.setDepthFilters(mIsMainEnablerFilter, mIsMainEnablerSubSample, mIsMainEnablerEdgePreserving,
                mIsMainEnablerHoleFill, mIsMainEnablerTemporalFilter, mIsMainEnablerRemoveCurve)
    }

    private fun minConfigs() {
        mIsEnableFull = false
        mIsEnableMin = true

        mIsMainEnablerSubSample = true
        mSubSampleMode = 0
        mSubSampleFactor = 3

        mIsMainEnablerHoleFill = true
        mHoleFillLevel = 1
        mIsHoleFillHorizontal = true

        mIsMainEnablerEdgePreserving = true
        mEdgePreservingLevel = 1

        mIsMainEnablerTemporalFilter = true
        mTemporalAlpha = 0.4f

        mIsMainEnablerRemoveCurve = false
    }

    private fun fullConfigs() {
        mIsEnableFull = true
        mIsEnableMin = false

        mIsMainEnablerSubSample = true
        mSubSampleMode = 0
        mSubSampleFactor = 3

        mIsMainEnablerHoleFill = true
        mHoleFillLevel = 1
        mIsHoleFillHorizontal = true

        mIsMainEnablerEdgePreserving = true
        mEdgePreservingLevel = 1

        mIsMainEnablerTemporalFilter = true
        mTemporalAlpha = 0.4f

        mIsMainEnablerRemoveCurve = true
    }

    fun getVersion():String {
        return mCamera?.depthFilterVersion ?: ""
    }

    fun setFullChoose(enabled: Boolean) {
        mIsEnableFull = enabled
        if (mIsEnableFull) {
            mIsEnableMin = !enabled
            fullConfigs()
            updateCameraConfigs()
        }
    }

    fun setMinChoose(enabled: Boolean) {
        mIsEnableMin = enabled
        if (mIsEnableMin){
            mIsEnableFull = !enabled
            minConfigs()
            updateCameraConfigs()
        }
    }

    fun subSampleEnabler(enabled: Boolean) {
        mIsMainEnablerSubSample = enabled
        mCamera?.setDepthFilterByType(EtronCamera.SUBSAMPLE, mIsMainEnablerSubSample)
    }

    fun subSampleModeCallback(value: Int) {
        mSubSampleMode = value
        mCamera?.setDepthFilterSubSampleParams(mSubSampleMode, mSubSampleFactor)
        if (value == 1) {
            subSampleFactorCallback(4)
        } else {
            subSampleFactorCallback(3)
        }
    }

    fun subSampleFactorCallback(value: Int) {
        mSubSampleFactor = value
        mCamera?.setDepthFilterSubSampleParams(mSubSampleMode, mSubSampleFactor)
    }

    fun setHoleFillEnabler(enabled: Boolean) {
        mIsMainEnablerHoleFill = enabled
        mCamera?.setDepthFilterByType(EtronCamera.HOLE_FILL, mIsMainEnablerHoleFill)
    }

    fun setHoleFillHorizontal(enabled: Boolean) {
        mIsHoleFillHorizontal = enabled
        mCamera?.setDepthFilterHoleFillingParams(mHoleFillLevel, mIsHoleFillHorizontal)
    }

    fun holeFillLevelCallback(value: Int){
        mHoleFillLevel = value
        mCamera?.setDepthFilterHoleFillingParams(mHoleFillLevel, mIsHoleFillHorizontal)

    }

    fun setEdgePreservingEnabler(enabled: Boolean) {
        mIsMainEnablerEdgePreserving = enabled
        mCamera?.setDepthFilterByType(EtronCamera.EDGE_PRESERVING_FILTER, mIsMainEnablerEdgePreserving)
    }

    fun edgePreservingLevelCallback(value: Int) {
        mEdgePreservingLevel = value
        mCamera?.setDepthFilterEdgePreservingParams(mEdgePreservingLevel)
    }

    fun setTemporalFilterEnabler(enabled: Boolean) {
        mIsMainEnablerTemporalFilter = enabled
        mCamera?.setDepthFilterByType(EtronCamera.TEMPORAL_FILTER, mIsMainEnablerTemporalFilter)

    }

    fun temporalFilterAlphaCallback(value: Float){
        mTemporalAlpha = value
        mCamera?.setDepthFilterTemporalParams(mTemporalAlpha)
    }

    fun setRemoveCurveEnabler(enabled: Boolean) {
        mIsMainEnablerRemoveCurve = enabled
        mCamera?.setDepthFilterByType(EtronCamera.FLYING_POINT_FILTER, mIsMainEnablerRemoveCurve)
    }
}
