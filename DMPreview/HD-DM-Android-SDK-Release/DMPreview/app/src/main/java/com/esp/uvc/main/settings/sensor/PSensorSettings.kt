package com.esp.uvc.main.settings.sensor

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

class PSensorSettings(v: ISensor.View) : ISensor.Presenter, KoinComponent {

    private val mIView = v

    private val mModel: ISensor.Model by inject { parametersOf(this) }

    override fun getSensorConfiguration() {
        mModel.isAutoExposure()
        mModel.getExposureTime()
        mModel.isAutoWhiteBalance()
        mModel.getWhiteBalance()
        mModel.getLightSource()
        mModel.getLowLightCompensation()
    }

    /**
     * Exposure
     */

    override fun setAutoExposure(enabled: Boolean) {
        mModel.setAutoExposure(enabled)
    }

    override fun onAutoExposure(error: String?, enabled: Boolean) {
        mIView.onAutoExposure(error, enabled)
    }

    override fun onUnsupportedAutoExposure() {
        mIView.onUnsupportedAutoExposure()
    }

    override fun setExposureTime(value: Int) {
        mModel.setExposureTime(value)
    }

    override fun onExposureTime(error: String?, value: Int) {
        if (error == null) {
            mModel.getExposureTimeLimit()
        }
        mIView.onExposureTime(error, value)
    }

    override fun onExposureTimeLimit(min: Int, max: Int) {
        mIView.onExposureTimeLimit(min, max)
    }

    override fun onUnsupportedExposureTime() {
        mIView.onUnsupportedExposureTime()
    }

    /**
     * White balance
     */

    override fun setAutoWhiteBalance(enabled: Boolean) {
        mModel.setAutoWhiteBalance(enabled)
    }

    override fun onAutoWhiteBalance(error: String?, enabled: Boolean) {
        mIView.onAutoWhiteBalance(error, enabled)
    }

    override fun onUnsupportedAutoWhiteBalance() {
        mIView.onUnsupportedAutoWhiteBalance()
    }

    override fun setWhiteBalance(value: Int) {
        mModel.setWhiteBalance(value)
    }

    override fun onWhiteBalance(error: String?, value: Int) {
        if (error == null) {
            mModel.getWhiteBalanceLimit()
        }
        mIView.onWhiteBalance(error, value)
    }

    override fun onUnsupportedWhiteBalance() {
        mIView.onUnsupportedAutoWhiteBalance()
    }

    override fun onWhiteBalanceLimit(min: Int, max: Int) {
        mIView.onWhiteBalanceLimit(min, max)
    }

    /**
     * Light source
     */

    override fun setLightSource(value: Int) {
        mModel.setLightSource(value)
    }

    override fun onLightSource(error: String?, value: Int) {
        if (error == null) {
            mModel.getLightSourceLimit()
        }
        mIView.onLightSource(error, value)
    }

    override fun onUnsupportedLightSource() {
        mIView.onUnsupportedLightSource()
    }

    override fun onLightSourceLimit(min: Int, max: Int) {
        mIView.onLightSourceLimit(min, max)
    }

    override fun setLowLightCompensation(enabled: Boolean) {
        mModel.setLowLightCompensation(enabled)
    }

    override fun onLowLightCompensation(error: String?, enabled: Boolean) {
        mIView.onLowLightCompensation(error, enabled)
    }

    override fun onUnsupportedLowLightCompensation() {
        mIView.onUnsupportedLowLightCompensation()
    }

    /**
     * Reset
     */

    override fun reset() {
        mModel.reset()
    }
}