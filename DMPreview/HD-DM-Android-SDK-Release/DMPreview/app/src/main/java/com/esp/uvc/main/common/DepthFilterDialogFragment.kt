package com.esp.uvc.main.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.esp.uvc.R
import com.esp.uvc.main.IMain
import com.esp.uvc.utils.roUI
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.android.synthetic.main.dialog_depth_filter.view.*

class DepthFilterDialogFragment(presenter: IMain.Presenter, version: String) : DialogFragment() {

    private var mPresenter = presenter
    private val mVersion = version

    companion object {
        const val TAG_DEPTH_FILTER = "TAG_DEPTH_FILTER"
    }

    private lateinit var mView: View

    private var mSubSampleModeCallback: ((Int) -> Unit)? = null
    private var mSubSampleFactorCallback: ((Int) -> Unit)? = null
    private var mHoleFillLevelSeekCallback: ((Int) -> Unit)? = null
    private var mEdgePreservingLevelCallback: ((Int) -> Unit)? = null
    private var mTemporalLevelCallback: ((Float) -> Unit)? = null

    fun setListener(
        subSampleModeCallback: ((Int) -> Unit)?,
        subSampleFactorCallback: ((Int) -> Unit)?,
        holeFillLevelSeekCallback: ((Int) -> Unit)?,
        edgePreservingLevelCallback: ((Int) -> Unit)?,
        temporalLevelCallback: ((Float) -> Unit)?
    ) {
        mSubSampleModeCallback = subSampleModeCallback
        mSubSampleFactorCallback = subSampleFactorCallback
        mHoleFillLevelSeekCallback = holeFillLevelSeekCallback
        mEdgePreservingLevelCallback = edgePreservingLevelCallback
        mTemporalLevelCallback = temporalLevelCallback
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mView = activity!!.layoutInflater.inflate(R.layout.dialog_depth_filter, null)
        val builder = AlertDialog.Builder(activity!!)
        builder.setView(mView)
        setupUI()
        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val window = dialog.window
            if (window != null) {
                window.attributes?.gravity = Gravity.FILL
                window.attributes?.dimAmount = 0f
                window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
    }

    fun setEnablers(
        bDoDepthFilter: Boolean, bFullChoose: Boolean, bMinChoose: Boolean,
        bSubSample: Boolean, bEdgePreServingFilter: Boolean, bHoleFill: Boolean,
        bHoleFillEnableHorizontal: Boolean, bTempleFilter: Boolean,
        bFlyingDepthCancellation: Boolean
    ) {

        mView.depth_filter_enabler_enable?.isChecked = bDoDepthFilter
        mView.depth_filter_enabler_full?.isChecked = bFullChoose
        mView.depth_filter_enabler_min?.isChecked = bMinChoose
        mView.depth_filter_sub_sample_main_enabler?.isChecked = bSubSample
        mView.depth_filter_hole_fill_main_enabler?.isChecked = bHoleFill
        mView.depth_filter_hole_fill_horizontal_enabler?.isChecked = bHoleFillEnableHorizontal
        mView.depth_filter_edge_preserve_main_enabler?.isChecked = bEdgePreServingFilter
        mView.depth_filter_temporal_filter_main_enabler?.isChecked = bTempleFilter
        mView.depth_filter_remove_curve_main_enabler?.isChecked = bFlyingDepthCancellation
    }

    fun setValues(
        subSampleMode: Int, subSampleFactor: Int,
        holeFillLevel: Int,
        edgePreservingLevel: Int,
        temporalFilterAlpha: Float
    ) {
        mView.depth_filter_sub_sample_mode_seek?.setProgress(subSampleMode.toFloat())
        mView.depth_filter_sub_sample_factor_seek?.setProgress(subSampleFactor.toFloat())
        mView.depth_filter_hole_fill_seekbar?.setProgress(holeFillLevel.toFloat())
        mView.depth_filter_edge_preserve_seekbar?.setProgress(edgePreservingLevel.toFloat())
        mView.depth_filter_temporal_filter_seekbar?.setProgress(temporalFilterAlpha)
    }

    private val mRadioCheckedListener = RadioGroup.OnCheckedChangeListener { group, checkedId ->
        when (checkedId) {
            R.id.depth_filter_enabler_full ->
                mPresenter.onDepthFilterFullChoose(mView.depth_filter_enabler_full.isChecked)
            R.id.depth_filter_enabler_min ->
                mPresenter.onDepthFilterMinChoose(mView.depth_filter_enabler_min.isChecked)
        }
    }

    private val mOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { buttonView,
                                                                                    isChecked ->
        when (buttonView.id) {
            R.id.depth_filter_enabler_enable ->
                mPresenter.onDepthFilterMainEnablerChanged(isChecked)
            R.id.depth_filter_sub_sample_main_enabler ->
                mPresenter.onDepthFilterSubsampleChanged(isChecked)
            R.id.depth_filter_hole_fill_main_enabler ->
                mPresenter.onDepthFilterHoleFillChanged(isChecked)
            R.id.depth_filter_hole_fill_horizontal_enabler ->
                mPresenter.onDepthFilterHoleFillHorizontalChanged(isChecked)
            R.id.depth_filter_edge_preserve_main_enabler ->
                mPresenter.onDepthFilterEdgePreservingChanged(isChecked)
            R.id.depth_filter_temporal_filter_main_enabler ->
                mPresenter.onDepthFilterTemporalFilterChanged(isChecked)
            R.id.depth_filter_remove_curve_main_enabler ->
                mPresenter.onDepthFilterRemoveCurveChanged(isChecked)
        }
    }

    private val mOnSeekChangeListener = object : OnSeekChangeListener {
        override fun onSeeking(seekParams: SeekParams?) {}
        override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}
        override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {
            when (seekBar?.id) {
                R.id.depth_filter_hole_fill_seekbar ->
                    mHoleFillLevelSeekCallback?.invoke(seekBar.progress)
                R.id.depth_filter_edge_preserve_seekbar ->
                    mEdgePreservingLevelCallback?.invoke(seekBar.progress)
                R.id.depth_filter_temporal_filter_seekbar ->
                    mTemporalLevelCallback?.invoke(seekBar.progressFloat)
            }
        }
    }

    private fun setupUI() {
        mView.tv_version.text = resources.getString(R.string.depth_filters_version, mVersion)
        mView.depth_filter_enabler_enable.setOnCheckedChangeListener(mOnCheckedChangeListener)
        mView.depth_filter_sub_sample_main_enabler.setOnCheckedChangeListener(
            mOnCheckedChangeListener
        )
        mView.depth_filter_hole_fill_horizontal_enabler.setOnCheckedChangeListener(
            mOnCheckedChangeListener
        )
        mView.depth_filter_hole_fill_main_enabler.setOnCheckedChangeListener(
            mOnCheckedChangeListener
        )
        mView.depth_filter_edge_preserve_main_enabler.setOnCheckedChangeListener(
            mOnCheckedChangeListener
        )
        mView.depth_filter_temporal_filter_main_enabler.setOnCheckedChangeListener(
            mOnCheckedChangeListener
        )
        mView.depth_filter_remove_curve_main_enabler.setOnCheckedChangeListener(
            mOnCheckedChangeListener
        )
        mView.depth_filter_radio_enabler.setOnCheckedChangeListener(mRadioCheckedListener)

        // Avoid "IndicatorSeekBar" progress not stopping on the ticks (2 ticks count)
        mView.depth_filter_sub_sample_mode_seek.setOnTouchListener(mOnTouchListener)
        mView.depth_filter_sub_sample_factor_seek.setOnTouchListener(mOnTouchListener)

        mView.depth_filter_hole_fill_seekbar.onSeekChangeListener = mOnSeekChangeListener
        mView.depth_filter_edge_preserve_seekbar.onSeekChangeListener = mOnSeekChangeListener
        mView.depth_filter_temporal_filter_seekbar.onSeekChangeListener = mOnSeekChangeListener
    }

    fun updateWidgetState(
        enableFull: Boolean, enableMin: Boolean,
        enableSubModeEnabler: Boolean, enableSubMode: Boolean, enableSubFactor: Boolean,
        enableHoleEnabler: Boolean, enableHoleHorizontal: Boolean, enableHoleLevel: Boolean,
        enableEdgeEnabler: Boolean, enableEdgeLevel: Boolean, enableTemporal: Boolean,
        enableTemporalLevel: Boolean, enableRemoveEnabler: Boolean
    ) = roUI {

        mView.depth_filter_enabler_full?.isEnabled = enableFull
        mView.depth_filter_enabler_min?.isEnabled = enableMin

        mView.depth_filter_sub_sample_main_enabler?.isEnabled = enableSubModeEnabler
        mView.depth_filter_sub_sample_mode_seek?.isEnabled = enableSubMode
        mView.depth_filter_sub_sample_factor_seek?.isEnabled = enableSubFactor

        mView.depth_filter_hole_fill_main_enabler?.isEnabled = enableHoleEnabler
        mView.depth_filter_hole_fill_horizontal_enabler?.isEnabled = enableHoleHorizontal
        mView.depth_filter_hole_fill_seekbar?.isEnabled = enableHoleLevel

        mView.depth_filter_edge_preserve_main_enabler?.isEnabled = enableEdgeEnabler
        mView.depth_filter_edge_preserve_seekbar?.isEnabled = enableEdgeLevel

        mView.depth_filter_temporal_filter_main_enabler?.isEnabled = enableTemporal
        mView.depth_filter_temporal_filter_seekbar?.isEnabled = enableTemporalLevel

        mView.depth_filter_remove_curve_main_enabler?.isEnabled = enableRemoveEnabler
    }

    @SuppressLint("ClickableViewAccessibility")
    private val mOnTouchListener = View.OnTouchListener { v, event ->
        when (v!!.id) {
            R.id.depth_filter_sub_sample_mode_seek -> {
                val view = mView.depth_filter_sub_sample_mode_seek
                val view2 = mView.depth_filter_sub_sample_factor_seek
                if (event!!.action == MotionEvent.ACTION_DOWN) {
                    if (view.progress.toFloat() == view.min) {
                        view.setProgress(view.max)
                    } else {
                        view.setProgress(view.min)
                    }
                    mSubSampleModeCallback?.invoke(view.progress)
                    if (view.progress.toFloat() == view.max) {
                        view2.max = 5f
                        view2.min = 4f
                        view2.setProgress(4f)
                    } else {
                        view2.min = 2f
                        view2.max = 3f
                        view2.setProgress(3f)
                    }
                    mSubSampleFactorCallback?.invoke(view2.progress)
                }
            }
            R.id.depth_filter_sub_sample_factor_seek -> {
                val view = mView.depth_filter_sub_sample_factor_seek
                if (event!!.action == MotionEvent.ACTION_DOWN) {
                    if (view.progress.toFloat() == view.min) {
                        view.setProgress(view.max)
                    } else {
                        view.setProgress(view.min)
                    }
                    mSubSampleFactorCallback?.invoke(view.progress)
                }
            }
        }
        true
    }
}
