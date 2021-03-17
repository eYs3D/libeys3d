package com.esp.uvc.application

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import com.esp.uvc.BuildConfig
import com.esp.uvc.di.appModule
import com.esp.uvc.usbcamera.AppSettings
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AndroidApplication : Application() {

    companion object {

        private var mInstance: AndroidApplication? = null

        var eglMajorVersion: Int = 0

        fun applicationContext(): Context {
            return mInstance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
        AppSettings.getInstance(applicationContext).clear()
        AppSettings.getInstance(applicationContext).init()
        startKoin {
            if (BuildConfig.DEBUG) {
                androidLogger()
            }
            androidContext(this@AndroidApplication)
            modules(appModule)
        }
        val config =
            (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).deviceConfigurationInfo
        eglMajorVersion = config.reqGlEsVersion
    }
}
