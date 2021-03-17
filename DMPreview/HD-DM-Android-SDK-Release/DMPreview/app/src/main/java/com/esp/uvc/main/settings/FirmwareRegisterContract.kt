package com.esp.uvc.main.settings

import android.widget.Toast
import com.esp.uvc.BasePresenter
import com.esp.uvc.BaseView
import com.google.android.material.snackbar.Snackbar

interface FirmwareRegisterContract {

    interface Presenter : BasePresenter<View> {
        fun onStart()
        fun onResume()
        fun onPause()
        fun onStop()
        fun onFwRegisterSelected()
        fun onAsicRegisterSelected()
        fun onI2cRegisterSelected()
        fun onGetClicked(addr: Int, slave: Int)
        fun onSetClicked(addr: Int, value: Int, slave: Int)
        fun onAddress2BytesChecked(checked: Boolean)
        fun onValue2BytesChecked(checked: Boolean)
        fun canNavigate(): Boolean
    }

    interface View : BaseView<Presenter> {
        fun enableAddressLengthSelection(enabled: Boolean)
        fun enableValueLengthSelection(enabled: Boolean)
        fun enableSlaveAddressInput(enabled: Boolean)
        fun checkAddress2Bytes(checked: Boolean)
        fun checkValue2Bytes(checked: Boolean)

        fun showRegisterValue(value: Int)

        fun toast(text: String, length: Int = Toast.LENGTH_SHORT)
        fun showSnack(text: String, length: Int = Snackbar.LENGTH_INDEFINITE, actionText: String = "Ok", onClick: (() -> Unit)? = null)
    }
}