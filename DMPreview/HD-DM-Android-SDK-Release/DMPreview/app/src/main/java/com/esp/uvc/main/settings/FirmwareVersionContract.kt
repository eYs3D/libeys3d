package com.esp.uvc.main.settings

import android.widget.Toast
import com.esp.uvc.BasePresenter
import com.esp.uvc.BaseView
import com.google.android.material.snackbar.Snackbar

interface FirmwareVersionContract {

    interface Presenter : BasePresenter<View> {
        fun onStart()
        fun onResume()
        fun onPause()
        fun onStop()
        fun canNavigate(): Boolean
    }

    interface View : BaseView<Presenter> {
        fun displayFirmwareVersion(version: String)
        fun displayProductId(pid: String)
        fun displayVendorId(vid: String)
        fun displaySerialNumber(sn: String)
        fun toast(text: String, length: Int = Toast.LENGTH_SHORT)
        fun showSnack(text: String, length: Int = Snackbar.LENGTH_INDEFINITE, actionText: String = "Ok", onClick: (() -> Unit)? = null)
    }
}