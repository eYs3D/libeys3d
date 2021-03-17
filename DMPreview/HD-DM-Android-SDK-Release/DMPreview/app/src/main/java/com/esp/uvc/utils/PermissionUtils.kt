package com.esp.uvc.utils

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    const val REQUEST_CODE = 100

    fun checkPermission(activity: Activity, permissionName: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permissionName
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkPermissions(activity: Activity, permissionNames: Array<String>): Boolean {
        permissionNames.forEach {
            if (!checkPermission(activity, it))
                return false
        }
        return true
    }

    fun requestPermissions(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int = REQUEST_CODE
    ) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    fun checkSettingsSystemPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Settings.System.canWrite(activity)
        } else {
            true
        }
    }

    fun requestSettingsSystemPermission(activity: Activity, requestCode: Int = REQUEST_CODE) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:${activity.packageName}")
        activity.startActivityForResult(intent, requestCode)
    }

    fun setSettingsSystem(contentResolver: ContentResolver, name: String, value: Int) {
        Settings.System.putInt(contentResolver, name, value)
    }
}