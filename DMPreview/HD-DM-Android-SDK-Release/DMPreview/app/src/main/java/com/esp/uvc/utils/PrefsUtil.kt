package com.esp.uvc.utils

import android.content.Context
import android.content.SharedPreferences
import com.esp.uvc.R

object PrefsUtil {
    private const val SKIP_SELECTION_KEY = "selection"
    private const val PREVIEW_KEY = "preview"

    private fun getSkipSelection(context: Context, id: String, default: Boolean = true): Boolean? {
        return getPrefs(context)?.getBoolean(id, default)
    }

    private fun setBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context)?.edit()?.putBoolean(key, value)?.apply()
    }

    private fun getPrefs(context: Context): SharedPreferences? {
        val prefsName = context.getString(R.string.preference_file)
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    fun setCameraSelectionToggle(ctx: Context, value: Boolean) {
        setBoolean(ctx, SKIP_SELECTION_KEY, value)
    }

    fun isCameraSelectionToggled(ctx: Context): Boolean {
        return getSkipSelection(ctx, SKIP_SELECTION_KEY) ?: true
    }

    fun isEnabledPreview(ctx: Context): Boolean {
        return getSkipSelection(ctx, PREVIEW_KEY) ?: false
    }

    fun togglePreviewView(ctx: Context): Boolean {
        val enabled = isEnabledPreview(ctx)
        setBoolean(ctx, PREVIEW_KEY, !enabled)
        return !enabled
    }
}

