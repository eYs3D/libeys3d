package com.esp.uvc.utils

import android.view.View

abstract class DoubleClickListener : View.OnClickListener {

    private var lastClickTime: Long = 0

    override fun onClick(v: View) {
        val clickTime = System.currentTimeMillis()
        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
            onDoubleClick(v)
            lastClickTime = 0
        } else {
            onSingleClick(v)
        }
        lastClickTime = clickTime
    }

    abstract fun onSingleClick(v: View)
    abstract fun onDoubleClick(v: View)

    companion object {
        private val DOUBLE_CLICK_TIME_DELTA: Long = 300//milliseconds
    }
}