package com.esp.uvc.main.settings

import android.widget.Toast
import com.esp.uvc.BasePresenter
import com.esp.uvc.BaseView
import com.google.android.material.snackbar.Snackbar

interface FirmwareTableContract {

    interface View : BaseView<Presenter> {
        fun setIndexValues(values: Array<Int>)
        fun toast(text: String, length: Int = Toast.LENGTH_SHORT)
        fun showSnack(text: String, length: Int = Snackbar.LENGTH_INDEFINITE, actionText: String = "Ok", onClick: (() -> Unit)? = null)
        fun showDialog(text: String)
        fun showWaitingSpinner(visible: Boolean)
    }

    interface Presenter : BasePresenter<View> {
        fun onIndexSelected(index: Int)
        fun onRectifyLogReadClicked()
        fun onStart()
        fun onResume()
        fun onPause()
        fun onStop()
        fun canNavigate(): Boolean
    }
}