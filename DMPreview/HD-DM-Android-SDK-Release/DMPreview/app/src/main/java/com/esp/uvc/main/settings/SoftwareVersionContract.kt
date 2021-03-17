package com.esp.uvc.main.settings

import com.esp.uvc.BasePresenter
import com.esp.uvc.BaseView

interface SoftwareVersionContract {

    interface Presenter : BasePresenter<View> {
    }

    interface View : BaseView<Presenter> {
        fun showAppVersion(version: String, build: String)
        fun showSdkVersion(version: String)
        fun showCopyright(year: Int, company: String)
        fun showLicense(license: String)
    }
}