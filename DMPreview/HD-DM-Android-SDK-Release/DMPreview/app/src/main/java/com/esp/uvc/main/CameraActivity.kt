package com.esp.uvc.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.esp.uvc.R
import com.esp.uvc.roi_size.RoiSizeProvider
import com.esp.uvc.utils.PermissionUtils
import com.esp.uvc.utils.replaceFragmentInActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_camera_main.*
import kotlinx.android.synthetic.main.fragment_camera_main.*
import org.koin.android.ext.android.inject


private val REQUEST_PERMISSIONS = arrayOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
)

class CameraActivity : AppCompatActivity() {

    private val mCameraFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.contentFrame) as CameraFragment?
            ?: CameraFragment.newInstance()
    }

    private val roiSizeProvider: RoiSizeProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_camera_main)
        roiSizeProvider.clearRoiSize()
        getPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PermissionUtils.REQUEST_CODE -> {
                if ((grantResults.isNotEmpty())) {
                    for (i in grantResults) {
                        if (i != PackageManager.PERMISSION_GRANTED) {
                            Snackbar.make(
                                contentFrame,
                                R.string.request_permissions,
                                Snackbar.LENGTH_INDEFINITE
                            ).setAction(R.string.dlg_ok) { getPermissions() }.show()
                            return
                        }
                    }
                    setAccelerometerRotation()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionUtils.REQUEST_CODE) {
            if (PermissionUtils.checkSettingsSystemPermission(this)) {
                setAccelerometerRotation()
            } else {
                Snackbar.make(
                    contentFrame,
                    R.string.request_permissions,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.dlg_ok) { setAccelerometerRotation() }.show()
            }
        }
    }

    private fun getPermissions() {
        if (PermissionUtils.checkPermissions(this, REQUEST_PERMISSIONS)) {
            setAccelerometerRotation()
        } else {
            PermissionUtils.requestPermissions(this, REQUEST_PERMISSIONS)
        }
    }

    private fun setAccelerometerRotation() {
        if (PermissionUtils.checkSettingsSystemPermission(this)) {
            PermissionUtils.setSettingsSystem(
                contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                1
            )
            intoFragment()
        } else {
            PermissionUtils.requestSettingsSystemPermission(this)
        }
    }

    private fun intoFragment() {
        replaceFragmentInActivity(mCameraFragment, R.id.contentFrame)
    }

    override fun onBackPressed() {
        if (live_ply_surfaceView?.visibility == android.view.View.VISIBLE) {
            mCameraFragment.mPresenter.onStopLivePly()
        } else {
            super.onBackPressed()
        }
    }
}