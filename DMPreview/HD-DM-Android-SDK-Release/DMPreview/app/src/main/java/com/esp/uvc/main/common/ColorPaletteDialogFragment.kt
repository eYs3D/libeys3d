package com.esp.uvc.main.common

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_POSITIVE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import com.esp.uvc.R
import com.esp.uvc.utils.loge
import java.lang.RuntimeException

class ColorPaletteDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    private var mZNear: Int = 0
    private var mZFar: Int = 1000
    private var mTitle: String? = null
    private var mMsg: String? = null
    private var mPositive: String? = null
    private var mNegative: String? = null

    private var mListener: OnDistanceValueChangeListener? = null

    private var mView: View? = null

    interface OnDistanceValueChangeListener {
        fun onChange(min: Int, max: Int)
        fun onReset()
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setView(R.layout.dialog_palette)
        setupUI(builder)
        isCancelable = true
        loge("esp_palette_dialog onCreateDialog 01")
        return builder.create()
    }

    fun setTitle(title: String?) {
        mTitle = title
    }

    fun setMessage(msg: String?) {
        mMsg = msg
    }

    fun setPositiveButton(text: String?) {
        mPositive = text
    }

    fun setValues(zNearCurrent: Int, zFarCurrent: Int) {
        loge("esp_palette_dialog 02 setValues $zNearCurrent $zFarCurrent")
        mZNear = zNearCurrent
        mZFar = zFarCurrent
        val editTextZNear = dialog?.findViewById(R.id.edit_text_z_near) as EditText?
        if (editTextZNear == null) {
            loge("esp_palette_dialog 03 null dialog ${dialog == null}")
        }
        val editTextZFar = dialog?.findViewById(R.id.edit_text_z_far) as EditText?
        if (editTextZNear == null) {
            loge("esp_palette_dialog 04 null")
        }
        editTextZNear?.setText(mZNear.toString())
        editTextZFar?.setText(mZFar.toString())
    }

    fun setNegativeButton(text: String?) {
        mNegative = text
    }

    fun setListener(listener: OnDistanceValueChangeListener?) {
        mListener = listener
    }

    private fun setupUI(builder: AlertDialog.Builder) {
        if (mTitle != null) {
            builder.setTitle(mTitle)
        }
        if (mMsg != null) {
            builder.setMessage(mMsg)
        }
        if (mPositive != null) {
            builder.setPositiveButton(mPositive, this)
        }
        if (mNegative != null) {
            builder.setNegativeButton(mNegative, this)
        }
    }

    override fun onClick(dialogInterface: DialogInterface?, which: Int) {
        when (which) {
            BUTTON_POSITIVE -> {
                val editTextZNear = dialog?.findViewById(R.id.edit_text_z_near) as EditText?
                val editTextZFar = dialog?.findViewById(R.id.edit_text_z_far) as EditText?
                mZNear = editTextZNear?.text.toString().toInt()
                mZFar = editTextZFar?.text.toString().toInt()
                mListener?.onChange(mZNear, mZFar)
            }
            BUTTON_NEGATIVE -> mListener?.onReset()
        }
    }
}