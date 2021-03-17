package com.esp.uvc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.fragment.app.Fragment
import com.esp.uvc.usbcamera.AppSettings

/**
 * A Base Fragment that is aware of all the possible state of a particular screen
 * */
abstract class BaseFragment : Fragment() {

    override fun onResume() {
        super.onResume()
        activity?.let {
            applyScreenRotation(it)
        }
    }

    companion object {
        /**
         * Helper method that forces a screen rotation for an activity
         * */
        @SuppressLint("SourceLockedOrientationActivity")
        fun applyScreenRotation(activity: Activity) {
            val appSettings = AppSettings.getInstance(activity.applicationContext)
            val mUpsideDownMode = appSettings.get(AppSettings.UPSIDEDOWN_MODE, true)
            if (mUpsideDownMode) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
}