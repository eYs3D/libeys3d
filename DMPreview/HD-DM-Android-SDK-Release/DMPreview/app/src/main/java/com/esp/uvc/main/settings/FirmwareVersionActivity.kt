package com.esp.uvc.main.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esp.uvc.BaseFragment
import com.esp.uvc.R
import com.esp.uvc.utils.roUI
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_firmware_version.*
import org.koin.android.ext.android.inject

class FirmwareVersionActivity : AppCompatActivity(), FirmwareVersionContract.View {

    override fun toast(text: String, length: Int) {
        Toast.makeText(this, text, length).show()
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

    override fun displayFirmwareVersion(version: String) = roUI { firmwareVersionText.text = version }

    override fun displayProductId(pid: String) = roUI { productIdText.text = pid }

    override fun displayVendorId(vid: String) = roUI { vendorIdText.text = vid }

    override fun displaySerialNumber(sn: String) = roUI { serialNumberText.text = sn }

    override val presenter: FirmwareVersionContract.Presenter by inject()

    companion object {
        fun startInstance(context: Context) {
            context.startActivity(Intent(context, FirmwareVersionActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        BaseFragment.applyScreenRotation(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firmware_version)
        title = getString(R.string.firmware_version_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
}