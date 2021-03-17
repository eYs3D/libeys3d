package com.esp.uvc.about

import android.content.Context
import android.view.Gravity
import android.view.WindowManager
import android.widget.CheckBox
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.esp.uvc.R
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams

/**
 * This class generates the about page shown via the dialogs
 * */
object DialogGenerator {

    fun showIRChangeDialog(context: Context, irMax: Int,irMin: Int, irCurrentValue: Int, extended: Boolean, irCallback: (Int, Boolean) -> Unit) : MaterialDialog {
        val dialog = MaterialDialog(context).customView(R.layout.layout_ir)
        val view = dialog.getCustomView()
        val slider = view.findViewById<IndicatorSeekBar>(R.id.ir_value_seekbar)

        slider.max = irMax.toFloat()
        slider.min = irMin.toFloat()
        slider.tickCount = (slider.max - slider.min + 1).toInt()
        slider.setProgress(irCurrentValue.toFloat())

        val extendedView = view.findViewById<CheckBox>(R.id.ir_range_extended_checkbox)
        extendedView.isChecked = extended
        extendedView.setOnCheckedChangeListener { _, isChecked ->
            val value = slider.progress
            irCallback(value, isChecked)
        }

        var progressValue: Int = irCurrentValue
        slider.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams?) {}
            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {
                val needExtend = extendedView.isChecked
                progressValue = seekBar?.progress ?: progressValue
                irCallback(progressValue, needExtend)}
        }

        val window = dialog.window
        val wlp = window.attributes
        wlp.gravity = Gravity.TOP
        wlp.flags = wlp.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
        window.attributes = wlp

        //show dialog here
        dialog.show {
            title(R.string.ir_title)
            positiveButton {}
        }

        return dialog
    }

    fun showMeasureDialog(context: Context, measureCallback: () -> Unit) {
        val dialog = MaterialDialog(context).customView(R.layout.layout_depth_measure)
        val view = dialog.getCustomView()

        val window = dialog.window
        val wlp = window.attributes
        wlp.gravity = Gravity.TOP
        wlp.flags = wlp.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
        window.attributes = wlp

        dialog.show {
            title(R.string.ir_title)
            positiveButton {
                measureCallback()
            }
            negativeButton { }
        }
    }
}