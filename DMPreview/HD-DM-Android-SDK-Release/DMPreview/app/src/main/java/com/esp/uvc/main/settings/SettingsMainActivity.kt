package com.esp.uvc.main.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.esp.uvc.BaseFragment
import com.esp.uvc.R
import com.esp.uvc.main.settings.module_sync.ModuleSyncActivity
import com.esp.uvc.main.settings.sensor.SensorSettingsActivity
import kotlinx.android.synthetic.main.activity_settings_main.*

class SettingsMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        BaseFragment.applyScreenRotation(this)
        super.onCreate(savedInstanceState)
        title = getString(R.string.settings_main_title)
        setContentView(R.layout.activity_settings_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initViews()
    }

    private fun initViews() {
        layout_preview_image.setOnClickListener {
            PreviewImageActivity.startInstance(this)
        }
        layout_sensor_settings.setOnClickListener {
            SensorSettingsActivity.startInstance(this)
        }
        layout_ply_viewer.setOnClickListener {
            PlyViewerActivity.startInstance(this)
        }
        layout_firmware_version.setOnClickListener {
            showFirmwareVersion()
        }
        layout_firmware_register.setOnClickListener {
            FirmwareRegisterActivity.startInstance(this)
        }
        layout_firmware_table.setOnClickListener {
            FirmwareTableActivity.startInstance(this)
        }
        layout_software_version.setOnClickListener {
            showSoftwareVersion()
        }
        layout_module_sync.setOnClickListener {
            ModuleSyncActivity.startInstance(this)
        }
    }

    private fun showFirmwareVersion() {
        FirmwareVersionActivity.startInstance(this)
    }

    private fun showSoftwareVersion() {
        val fragment = SoftwareVersionDialogFragment()
        fragment.show(supportFragmentManager, "softwareVersion")
    }

    companion object {
        fun startInstance(context: Context) {
            context.startActivity(Intent(context, SettingsMainActivity::class.java))
        }
    }
}