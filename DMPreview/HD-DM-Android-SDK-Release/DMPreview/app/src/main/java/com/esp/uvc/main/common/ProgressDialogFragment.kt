package com.esp.uvc.main.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.esp.uvc.R
import kotlinx.android.synthetic.main.dialog_progress.view.*

// todo : more useful
class ProgressDialogFragment : DialogFragment() {

    private var mLoadingMsg = "Loading ..."
    private var mProgressInfo: String? = null

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = activity!!.layoutInflater.inflate(R.layout.dialog_progress, null)
        view.tv_loading_msg.text = mLoadingMsg
        if (mProgressInfo != null) {
            view.tv_progress_info.text = mProgressInfo
            view.tv_progress_info.visibility = View.VISIBLE
        }
        val builder = AlertDialog.Builder(activity!!)
        builder.setView(view)
        isCancelable = false
        return builder.create()
    }

    fun setLoadingMsg(msg: String) {
        mLoadingMsg = msg
    }

    fun setProgressInfo(info: String) {
        mProgressInfo = info
    }
}