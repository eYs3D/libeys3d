package com.esp.uvc.main.settings.sensor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esp.uvc.BaseFragment
import com.esp.uvc.R
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.android.synthetic.main.activity_sensor_settings.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class SensorSettingsActivity : AppCompatActivity(), ISensor.View {

    companion object {
        fun startInstance(context: Context) {
            context.startActivity(Intent(context, SensorSettingsActivity::class.java))
        }
    }

    private val mPresenter: ISensor.Presenter by inject { parametersOf(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        BaseFragment.applyScreenRotation(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_settings)

        autoExposureCheckbox.setOnCheckedChangeListener(mOnCheckedListener)
        exposureTimeBar.onSeekChangeListener = mOnSeekChangeListener
        autoWhiteBalanceCheckbox.setOnCheckedChangeListener(mOnCheckedListener)
        manualWhiteBalanceBar.onSeekChangeListener = mOnSeekChangeListener
        lightSourceRadioGroup.setOnCheckedChangeListener(mOnRadioGroupCheckedChangeListener)
        lowLightCompensationCheckbox.setOnCheckedChangeListener(mOnCheckedListener)
        bt_reset.setOnClickListener(mOnClickListener)
    }

    override fun onResume() {
        super.onResume()
        mPresenter.getSensorConfiguration()
    }

    /**
     * Exposure
     */

    override fun onAutoExposure(error: String?, enabled: Boolean) {
        if (error == null) {
            autoExposureCheckbox.isEnabled = true
            autoExposureCheckbox.isChecked = enabled
            exposureTimeBar.isEnabled = !enabled
        } else {
            autoExposureCheckbox.isEnabled = false
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onUnsupportedAutoExposure() {
        autoExposureCheckbox.isEnabled = false
    }

    override fun onExposureTime(error: String?, value: Int) {
        if (error == null) {
            if (!autoExposureCheckbox.isChecked) {
                exposureTimeBar.isEnabled = true
            }
            exposureTimeBar.setProgress(value.toFloat())
        } else {
            exposureTimeBar.isEnabled = false
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onUnsupportedExposureTime() {
        exposureTimeBar.isEnabled = false
    }

    override fun onExposureTimeLimit(min: Int, max: Int) {
        exposureTimeBar.max = max.toFloat()
        exposureTimeBar.min = min.toFloat()
    }

    /**
     * White balance
     */

    override fun onAutoWhiteBalance(error: String?, enabled: Boolean) {
        if (error == null) {
            autoWhiteBalanceCheckbox.isEnabled = true
            autoWhiteBalanceCheckbox.isChecked = enabled
            manualWhiteBalanceBar.isEnabled = !enabled
        } else {
            autoWhiteBalanceCheckbox.isEnabled = false
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onUnsupportedAutoWhiteBalance() {
        autoWhiteBalanceCheckbox.isEnabled = false
    }

    override fun onWhiteBalance(error: String?, value: Int) {
        if (error == null) {
            if (!autoWhiteBalanceCheckbox.isChecked) {
                manualWhiteBalanceBar.isEnabled = true
            }
            manualWhiteBalanceBar.setProgress(value.toFloat())
        } else {
            manualWhiteBalanceBar.isEnabled = false
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onUnsupportedWhiteBalance() {
        manualWhiteBalanceBar.isEnabled = false
    }

    override fun onWhiteBalanceLimit(min: Int, max: Int) {
        manualWhiteBalanceBar.max = max.toFloat()
        manualWhiteBalanceBar.min = min.toFloat()
    }

    /**
     * Light source
     */

    override fun onLightSource(error: String?, value: Int) {
        if (error == null) {
            tvLightSource.isEnabled = true
            lightSourceRadioGroup.clearCheck()
            lightSourceRadioGroup.check(value)
        } else {
            lightSourceRadioGroup.removeAllViews()
            tvLightSource.isEnabled = false
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onUnsupportedLightSource() {
        tvLightSource.isEnabled = false
    }

    override fun onLightSourceLimit(min: Int, max: Int) {
        setupLightSourceRadio(min, max)
    }

    override fun onLowLightCompensation(error: String?, enabled: Boolean) {
        if (error == null) {
            lowLightCompensationCheckbox.isEnabled = true
            lowLightCompensationCheckbox.isChecked = enabled
        } else {
            lowLightCompensationCheckbox.isEnabled = false
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onUnsupportedLowLightCompensation() {
        lowLightCompensationCheckbox.isEnabled = false
    }

    private fun setupLightSourceRadio(min: Int, max: Int) {
        lightSourceRadioGroup.removeAllViews()
        for (i in min..max) {
            val radioButton = RadioButton(this)
            val msg = (50 + (i - 1) * 10).toString() + "Hz"
            val dim = resources.getDimension(R.dimen.base_margin_horizontal).toInt()
            radioButton.setPadding(dim, 0, dim, 0)
            radioButton.text = msg
            radioButton.id = i
            val layoutParams = RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(dim, 0, 0, 0)
            lightSourceRadioGroup.addView(radioButton, layoutParams)
        }
    }

    private val mOnCheckedListener = CompoundButton.OnCheckedChangeListener { view, isChecked ->
        when (view) {
            autoExposureCheckbox -> mPresenter.setAutoExposure(isChecked)
            autoWhiteBalanceCheckbox -> mPresenter.setAutoWhiteBalance(isChecked)
            lowLightCompensationCheckbox -> mPresenter.setLowLightCompensation(isChecked)
        }
    }

    private val mOnSeekChangeListener = object : OnSeekChangeListener {

        override fun onSeeking(seekParams: SeekParams?) {
        }

        override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {
            when (seekBar) {
                exposureTimeBar -> mPresenter.setExposureTime(seekBar?.progress ?: -1)
                manualWhiteBalanceBar -> mPresenter.setWhiteBalance(seekBar?.progress ?: -1)
            }
        }
    }

    private val mOnRadioGroupCheckedChangeListener =
        RadioGroup.OnCheckedChangeListener { parentView, resId ->
            if (resId >= 0) {
                mPresenter.setLightSource(resId)
            }
        }

    private val mOnClickListener = View.OnClickListener { view ->
        when (view) {
            bt_reset -> mPresenter.reset()
        }
    }
}