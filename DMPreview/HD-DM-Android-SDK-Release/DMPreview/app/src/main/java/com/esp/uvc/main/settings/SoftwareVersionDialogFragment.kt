package com.esp.uvc.main.settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.esp.uvc.R
import com.esp.uvc.utils.logi
import org.koin.android.ext.android.inject

class SoftwareVersionDialogFragment(private val cameraName: String? = null): DialogFragment(), SoftwareVersionContract.View {

    private lateinit var appVersion: TextView
    private lateinit var sdkVersion: TextView
    private lateinit var licenses: TextView
    private lateinit var copyright: TextView

    override fun showAppVersion(version: String, build: String) {
        appVersion.text = resources.getString(R.string.software_version_dialog_app_version, version, build)
    }

    override fun showSdkVersion(version: String) {
        sdkVersion.text = resources.getString(R.string.software_version_dialog_sdk_version, version)
    }

    override fun showCopyright(year: Int, company: String) {
        copyright.text = resources.getString(R.string.software_version_dialog_copyright, year, company)
    }

    override fun showLicense(license: String) {
        licenses.text = resources.getString(R.string.software_version_dialog_licenses, license)
    }

    override val presenter: SoftwareVersionContract.Presenter by inject()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        logi("onCreateDialog: $context")

        var inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var view = inflater.inflate(R.layout.dialog_software_version, null, false)

        sdkVersion = view.findViewById(R.id.sdkVersion)
        appVersion = view.findViewById(R.id.appVersion)
        licenses = view.findViewById(R.id.license)
        copyright = view.findViewById(R.id.copyright)

        var connectedCamera = view.findViewById(R.id.connectedCamera) as TextView
        if (cameraName != null && cameraName.isNotEmpty()) {
            connectedCamera.text = resources.getString(R.string.software_version_dialog_connected_camera, cameraName)
        } else if (cameraName != null){
            connectedCamera.text = resources.getString(R.string.software_version_dialog_camera_not_connected)
        } else {
            connectedCamera.visibility = View.GONE
        }

        var builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.software_version_dialog_title)
        builder.setView(view)
        builder.setPositiveButton("OK") { _, _ ->  dismiss()}
        var dialog = builder.create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.black))
        }

        presenter.attach(this)

        return dialog
    }
}