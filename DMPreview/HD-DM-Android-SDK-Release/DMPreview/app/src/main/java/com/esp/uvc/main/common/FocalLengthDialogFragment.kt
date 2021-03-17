package com.esp.uvc.main.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.esp.uvc.R
import kotlinx.android.synthetic.main.dialog_focal_length.view.*

class FocalLengthDialogFragment : DialogFragment() {

    private lateinit var mView: View

    private var mCurrentLeftFx = 0
    private var mCurrentLeftFy = 0
    private var mCurrentRightFx = 0
    private var mCurrentRightFy = 0
    private var mCurrentPixelUnit = 0

    private var mListener: OnListener? = null

    interface OnListener {
        fun onFocalLength(value: Int)
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mView = activity!!.layoutInflater.inflate(R.layout.dialog_focal_length, null)
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
                window.attributes?.gravity = Gravity.TOP
                window.attributes?.dimAmount = 0f
                window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
    }

    fun setConfiguration(
        listener: OnListener,
        leftFx: Int,
        leftFy: Int,
        rightFx: Int,
        rightFy: Int,
        pixelUnit: Int
    ) {
        mListener = listener
        mCurrentLeftFx = leftFx
        mCurrentLeftFy = leftFy
        mCurrentRightFx = rightFx
        mCurrentRightFy = rightFy
        mCurrentPixelUnit = pixelUnit
    }

    fun onFocalLength(
        leftFx: Int,
        leftFy: Int,
        rightFx: Int,
        rightFy: Int,
        pixelUnit: Int
    ) {
        mView.tv_left_x_value.text = leftFx.toString()
        mView.tv_left_y_value.text = leftFy.toString()
        mView.tv_right_x_value.text = rightFx.toString()
        mView.tv_right_y_value.text = rightFy.toString()
        mView.tv_pixel_unit_value.text = pixelUnit.toString()
    }

    private fun setupUI() {
        mView.button.setOnClickListener(mOnClickListener)
        mView.tv_left_x_value.text = mCurrentLeftFx.toString()
        mView.tv_left_y_value.text = mCurrentLeftFy.toString()
        mView.tv_right_x_value.text = mCurrentRightFx.toString()
        mView.tv_right_y_value.text = mCurrentRightFy.toString()
        mView.tv_pixel_unit_value.text = mCurrentPixelUnit.toString()
        mView.seekBar.setProgress(mCurrentPixelUnit.toFloat())
    }

    private val mOnClickListener =
        View.OnClickListener { mListener?.onFocalLength(mView.seekBar.progress) }
}