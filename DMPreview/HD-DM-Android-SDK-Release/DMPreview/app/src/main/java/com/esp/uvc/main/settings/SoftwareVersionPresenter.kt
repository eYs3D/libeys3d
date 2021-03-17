package com.esp.uvc.main.settings

import com.esp.uvc.BuildConfig

class SoftwareVersionPresenter : SoftwareVersionContract.Presenter {

    override lateinit var view: SoftwareVersionContract.View

    override fun unattach() {
    }

    override fun attach(view: SoftwareVersionContract.View) {
        this.view = view

        view.showAppVersion(BuildConfig.VERSION_NAME, BuildConfig.BUILD_NUMBER)
        view.showSdkVersion(com.esp.android.usb.camera.core.BuildConfig.VERSION_NAME)
        view.showLicense("Apache 2.0, MIT") //TODO: change the licenses list
        view.showCopyright(BuildConfig.BUILD_YEAR, "eYs3D Microelectronics")
    }
}