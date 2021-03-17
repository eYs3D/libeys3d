package com.esp.uvc.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager


object LocationUtils {

    fun locationCheck(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        return !manager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun requestGPSSimple(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ -> context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                .setNegativeButton("No") { dialog, _ -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }
}