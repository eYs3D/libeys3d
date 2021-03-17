package com.esp.uvc.main.settings.sensor

import com.esp.uvc.BasePresenter
import com.esp.uvc.BaseView

interface ISensor {

    interface Model {

        /**
         * Exposure
         */

        fun isAutoExposure()

        fun setAutoExposure(enabled: Boolean)

        fun getExposureTime()

        fun getExposureTimeLimit()

        fun setExposureTime(value: Int)

        /**
         * White balance
         */

        fun isAutoWhiteBalance()

        fun setAutoWhiteBalance(enabled: Boolean)

        fun getWhiteBalance()

        fun getWhiteBalanceLimit()

        fun setWhiteBalance(value: Int)

        /**
         * Light source
         */

        fun getLightSource()

        fun getLightSourceLimit()

        fun setLightSource(value: Int)

        fun getLowLightCompensation()

        fun setLowLightCompensation(enabled: Boolean)

        /**
         * Reset
         */

        fun reset()
    }

    interface View {

        /**
         * Exposure
         */

        fun onAutoExposure(error: String?, enabled: Boolean)

        fun onUnsupportedAutoExposure()

        fun onExposureTime(error: String?, value: Int)

        fun onUnsupportedExposureTime()

        fun onExposureTimeLimit(min: Int, max: Int)

        /**
         * White balance
         */

        fun onAutoWhiteBalance(error: String?, enabled: Boolean)

        fun onUnsupportedAutoWhiteBalance()

        fun onWhiteBalance(error: String?, value: Int)

        fun onUnsupportedWhiteBalance()

        fun onWhiteBalanceLimit(min: Int, max: Int)

        /**
         * Light source
         */

        fun onLightSource(error: String?, value: Int)

        fun onUnsupportedLightSource()

        fun onLightSourceLimit(min: Int, max: Int)

        fun onLowLightCompensation(error: String?, enabled: Boolean)

        fun onUnsupportedLowLightCompensation()
    }

    interface Presenter {

        fun getSensorConfiguration()

        /**
         * Exposure
         */

        fun setAutoExposure(enabled: Boolean)

        fun onAutoExposure(error: String? = null, enabled: Boolean)

        fun onUnsupportedAutoExposure()

        fun setExposureTime(value: Int)

        fun onExposureTime(error: String? = null, value: Int)

        fun onUnsupportedExposureTime()

        fun onExposureTimeLimit(min: Int, max: Int)

        /**
         * White balance
         */

        fun setAutoWhiteBalance(enabled: Boolean)

        fun onAutoWhiteBalance(error: String? = null, enabled: Boolean)

        fun onUnsupportedAutoWhiteBalance()

        fun setWhiteBalance(value: Int)

        fun onWhiteBalance(error: String? = null, value: Int)

        fun onUnsupportedWhiteBalance()

        fun onWhiteBalanceLimit(min: Int, max: Int)

        /**
         * Light source
         */

        fun setLightSource(value: Int)

        fun onLightSource(error: String? = null, value: Int)

        fun onUnsupportedLightSource()

        fun onLightSourceLimit(min: Int, max: Int)

        fun setLowLightCompensation(enabled: Boolean)

        fun onLowLightCompensation(error: String? = null, enabled: Boolean)

        fun onUnsupportedLowLightCompensation()

        /**
         * Reset
         */

        fun reset()
    }
}