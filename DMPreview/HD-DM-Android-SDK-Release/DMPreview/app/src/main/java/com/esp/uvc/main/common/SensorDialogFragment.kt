package com.esp.uvc.main.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.esp.uvc.R
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.android.synthetic.main.dialog_sensor.view.*

class SensorDialogFragment : DialogFragment() {

    companion object {
        const val TAG_EXPOSURE = "TAG_EXPOSURE"
        const val TAG_WHITE_BALANCE = "TAG_WHITE_BALANCE"
        const val TAG_LOW_LIGHT_COMPENSATION = "TAG_LOW_LIGHT_COMPENSATION"
    }

    private lateinit var mView: View

    private var mIsAE = false
    private var mCurrentExposure = 0
    private var mMinExposure = 0
    private var mMaxExposure = 0

    private var mIsAWB = false
    private var mCurrentWB = 0
    private var mMinWB = 0
    private var mMaxWB = 0

    private var mIsLLC = false
    private var mLLCEnable = true

    private var mTag = TAG_EXPOSURE

    private var mListener: OnSensorListener? = null

    interface OnSensorListener {

        fun onCheckedChanged(tag: String, enabled: Boolean)

        fun onSeekBarChanged(tag: String, value: Int)
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mView = activity!!.layoutInflater.inflate(R.layout.dialog_sensor, null)
        val builder = AlertDialog.Builder(activity!!)
        builder.setView(mView)
        setupUI()
        return builder.create()
    }

    // Full screen and bg no dim
    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val window = dialog.window
            if (window != null) {
                window.attributes?.gravity = Gravity.BOTTOM
                window.attributes?.dimAmount = 0f
                window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }
    }

    fun setConfiguration(
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
        listener: OnSensorListener
    ) {
        mIsAE = isAE
        mCurrentExposure = currentExposure
        mMinExposure = minExposure
        mMaxExposure = maxExposure
        mIsAWB = isAWB
        mCurrentWB = currentWB
        mMinWB = minWB
        mMaxWB = maxWB
        mLLCEnable = isLLCEnable
        mIsLLC = isLLC
        mListener = listener
    }

    // For sensor set fail then update the UI.
    fun setChecked(tag: String, enabled: Boolean) {
        when (tag) {
            TAG_EXPOSURE -> {
                if (enabled == mView.cb_ae.isChecked) {
                    return
                }
                mView.cb_ae.isChecked = enabled
            }

            TAG_WHITE_BALANCE -> {
                if (enabled == mView.cb_awb.isChecked) {
                    return
                }
                mView.cb_awb.isChecked = enabled
            }
            TAG_LOW_LIGHT_COMPENSATION -> {
                if (enabled == mView.cb_llc.isChecked) {
                    return
                }
                mView.cb_llc.isChecked = enabled
            }
        }
    }

    private fun setupUI() {
        mView.cb_ae.isChecked = mIsAE
        mView.cb_ae.setOnCheckedChangeListener(mOnCheckedListener)
        mView.seekBar_exposure.isEnabled = !mIsAE
        mView.seekBar_exposure.max = mMaxExposure.toFloat()
        mView.seekBar_exposure.min = mMinExposure.toFloat()
        mView.seekBar_exposure.setProgress(mCurrentExposure.toFloat())
        mView.seekBar_exposure.onSeekChangeListener = mOnSeekChangeListener

        mView.cb_awb.isChecked = mIsAWB
        mView.cb_awb.setOnCheckedChangeListener(mOnCheckedListener)
        mView.seekBar_wb.isEnabled = !mIsAWB
        mView.seekBar_wb.max = mMaxWB.toFloat()
        mView.seekBar_wb.min = mMinWB.toFloat()
        mView.seekBar_wb.setProgress(mCurrentWB.toFloat())
        mView.seekBar_wb.onSeekChangeListener = mOnSeekChangeListener

        mView.cb_llc.isEnabled = mLLCEnable
        mView.cb_llc.isChecked = mIsLLC
        mView.cb_llc.setOnCheckedChangeListener(mOnCheckedListener)
    }

    private val mOnCheckedListener = CompoundButton.OnCheckedChangeListener { view, isChecked ->
        when (view) {
            mView.cb_ae -> {
                mView.seekBar_exposure?.isEnabled = !isChecked
                mTag = TAG_EXPOSURE
            }
            mView.cb_awb -> {
                mView.seekBar_wb?.isEnabled = !isChecked
                mTag = TAG_WHITE_BALANCE
            }
            mView.cb_llc -> mTag = TAG_LOW_LIGHT_COMPENSATION
        }
        mListener?.onCheckedChanged(mTag, isChecked)
    }

    private val mOnSeekChangeListener = object : OnSeekChangeListener {

        override fun onSeeking(seekParams: SeekParams?) {
        }

        override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {
            when (seekBar) {
                mView.seekBar_exposure -> mTag = TAG_EXPOSURE
                mView.seekBar_wb -> mTag = TAG_WHITE_BALANCE
            }
            mListener?.onSeekBarChanged(mTag, seekBar?.progress ?: -1)
        }
    }
}