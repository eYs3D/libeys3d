package com.esp.uvc.main.common

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.DialogFragment
import com.esp.uvc.R
import com.esp.uvc.utils.loge

class AlertDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    companion object {
        const val TAG_HIGH_PERFORMANCE = "TAG_HIGH_PERFORMANCE"
        const val TAG_ERROR_HANDLE = "TAG_ERROR_HANDLE"
        const val TAG_QUALITY = "TAG_QUALITY"

        const val TAG_ERROR_PLY = "TAG_ERROR_PLY"
        const val ERROR_COUNT_INTERNVAL = 1000L
        const val ERROR_COUNT_LIMIT = 4000L
    }

    private var mDialog: AlertDialog? = null

    private var mCurrentCount = ERROR_COUNT_LIMIT
    private var mTitle: String? = null
    private var mMsg: String? = null
    private var mPositive: String? = null
    private var mNegative: String? = null

    private var mListener: OnAlertListener? = null

    interface OnAlertListener {
        fun onClick(tag: String?, button: Int)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        setupUI(builder)
        isCancelable = false
        mDialog = builder.create()
        mCurrentCount = ERROR_COUNT_LIMIT
        return mDialog!!
    }

    fun setTitle(title: String) {
        mTitle = title
    }

    fun setMessage(msg: String) {
        mMsg = msg
    }

    fun setPositiveButton(text: String) {
        mPositive = text
    }

    fun setNegativeButton(text: String) {
        mNegative = text
    }

    fun setListener(listener: OnAlertListener) {
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

    fun setErrorCountDown(){
        if (tag == TAG_ERROR_HANDLE) {
            val timer = object : CountDownTimer(ERROR_COUNT_LIMIT, ERROR_COUNT_INTERNVAL) {
                override fun onTick(millisUntilFinished: Long) {
                    mCurrentCount -= ERROR_COUNT_INTERNVAL
                    val button = mDialog?.getButton(DialogInterface.BUTTON_POSITIVE)
                    button?.isEnabled = false
                    button?.text = (mCurrentCount / ERROR_COUNT_INTERNVAL).toString()
                }
                override fun onFinish() {
                    val button = mDialog?.getButton(DialogInterface.BUTTON_POSITIVE)
                    button?.text = getString(R.string.error_handle_dialog_positive)
                    button?.isEnabled = true
                }
            }
            timer.start()
        } else {
            loge("esp_countdown is $tag")
        }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        mListener?.onClick(tag, which)
    }
}
