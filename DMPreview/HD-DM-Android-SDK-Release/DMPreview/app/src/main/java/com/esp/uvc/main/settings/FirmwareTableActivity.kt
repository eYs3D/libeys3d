package com.esp.uvc.main.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.esp.uvc.BaseFragment
import com.esp.uvc.R
import com.esp.uvc.utils.roUI
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_firmware_table_settings.*
import org.koin.android.ext.android.inject

class FirmwareTableActivity : AppCompatActivity(), FirmwareTableContract.View {

    private lateinit var adapter: ArrayAdapter<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        BaseFragment.applyScreenRotation(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firmware_table_settings)
        initUI()
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

    private fun initUI() {
        adapter = ArrayAdapter<Int>(this, android.R.layout.simple_spinner_item, ArrayList<Int>())
        indexSpinner.adapter = adapter
        indexSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                //no-op
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                presenter.onIndexSelected(position)
            }
        }
        rectifyLogReadButton.setOnClickListener { presenter.onRectifyLogReadClicked() }
    }

    override fun showDialog(text: String) = roUI {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.rectify_log_read_title)
        builder.setMessage(text)
        builder.setPositiveButton(android.R.string.ok) { dialogInterface, _ -> dialogInterface.dismiss() }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.black))
        }

        dialog.show()
    }

    override fun setIndexValues(values: Array<Int>) = roUI {
        adapter.clear()
        adapter.addAll(values.asList())
    }

    override val presenter: FirmwareTableContract.Presenter by inject()

    override fun toast(text: String, length: Int) = roUI {
        Toast.makeText(this, text, length).show()
    }

    override fun showSnack(text: String, length: Int, actionText: String, onClick: (() -> Unit)?) = roUI {
        val view = findViewById<View>(android.R.id.content).rootView ?: return@roUI
        if (onClick != null) {
            Snackbar.make(view!!, text, length).setAction(actionText) {
                onClick()
            }.show()
        } else {
            Snackbar.make(view!!, text, length).show()
        }
    }

    override fun showWaitingSpinner(visible: Boolean) = roUI {
        waitingSpinner.visibility = if (visible) View.VISIBLE else View.GONE
    }

    companion object {
        fun startInstance(context: Context) {
            context.startActivity(Intent(context, FirmwareTableActivity::class.java))
        }
    }
}
