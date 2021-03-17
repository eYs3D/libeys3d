package com.esp.uvc.main.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esp.uvc.BaseFragment
import com.esp.uvc.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_firmware_register.*
import org.koin.android.ext.android.inject

class FirmwareRegisterActivity : AppCompatActivity(), FirmwareRegisterContract.View {
    override fun showRegisterValue(value: Int) {
        valueTextInput.setNumber(value)
    }

    override fun enableAddressLengthSelection(enabled: Boolean) {
        address_2_bytes_checkbox.isEnabled = enabled
    }

    override fun enableValueLengthSelection(enabled: Boolean) {
        value_2_bytes_checkbox.isEnabled = enabled
    }

    override fun enableSlaveAddressInput(enabled: Boolean) {
        i2cSlaveLayout.isEnabled = enabled

        if (!enabled) {
            i2cSlaveTextInput.setText("")
        }

        if (i2cSlaveTextInput.hasFocus() && !enabled) {
            i2cSlaveTextInput.rootView.requestFocus()
        }
    }

    override fun checkAddress2Bytes(checked: Boolean) {
        address_2_bytes_checkbox.isChecked = checked
    }

    override fun checkValue2Bytes(checked: Boolean) {
        value_2_bytes_checkbox.isChecked = checked
    }

    override val presenter: FirmwareRegisterContract.Presenter by inject()

    companion object {
        fun startInstance(context: Context) {
            context.startActivity(Intent(context, FirmwareRegisterActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        BaseFragment.applyScreenRotation(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firmware_register)

        title = getString(R.string.firmware_register_title)
        initViews()
        presenter.attach(this)
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.unattach()
    }

    override fun onBackPressed() {
        if (presenter.canNavigate()) {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                if (presenter.canNavigate()) {
                    super.onOptionsItemSelected(item)
                } else {
                    true
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initViews() {
        fwRegisterRadioButton.setOnClickListener { presenter.onFwRegisterSelected() }
        asicRegisterRadioButton.setOnClickListener { presenter.onAsicRegisterSelected() }
        i2cRegisterRadioButton.setOnClickListener { presenter.onI2cRegisterSelected() }
        getButton.setOnClickListener { presenter.onGetClicked(addressTextInput.getNumber(), i2cSlaveTextInput.getNumber()) }
        setButton.setOnClickListener { presenter.onSetClicked(addressTextInput.getNumber(), valueTextInput.getNumber(), i2cSlaveTextInput.getNumber()) }
        address_2_bytes_checkbox.setOnCheckedChangeListener { _, b -> presenter.onAddress2BytesChecked(b) }
        value_2_bytes_checkbox.setOnCheckedChangeListener { _, b -> presenter.onValue2BytesChecked(b) }
    }

    override fun toast(text: String, length: Int) {
        Toast.makeText(this, text, length).show()
    }

    override fun showSnack(text: String, length: Int, actionText: String, onClick: (() -> Unit)?) {
        val view = findViewById<View>(android.R.id.content).rootView ?: return
        if (onClick != null) {
            Snackbar.make(view!!, text, length).setAction(actionText) {
                onClick()
            }.show()
        } else {
            Snackbar.make(view!!, text, length).show()
        }
    }
}