package com.esp.uvc.ply.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ScaleGestureDetector

class LivePlyGLSurfaceView : GLSurfaceView, ScaleGestureDetector.OnScaleGestureListener {
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    private val mSimpleOnGestureListener = object : SimpleOnGestureListener() {

        override fun onLongPress(e: MotionEvent) {
            if (mRenderer != null) {
                mRenderer!!.reset()
            }
        }
    }

    private val mScaleGestureDetector = ScaleGestureDetector(context, this)
    private val mGestureDetector = GestureDetector(mSimpleOnGestureListener)
    private var mRenderer: LivePlyGLRenderer? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = event!!.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN ->
                mRenderer?.setArcBallMouseDown(x, y)
            MotionEvent.ACTION_MOVE ->
                mRenderer?.setArcBallMove(x, y)
        }
        var retVal = mScaleGestureDetector.onTouchEvent(event)
        retVal = mGestureDetector.onTouchEvent(event) || retVal
        return retVal || super.onTouchEvent(event)
    }

    override fun onScaleBegin(p0: ScaleGestureDetector?): Boolean {
        return true
    }

    override fun onScaleEnd(p0: ScaleGestureDetector?) {
    }

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        if (mRenderer != null) {
            mRenderer!!.setScale(detector!!.scaleFactor)
        }
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (0 != event!!.source and InputDevice.SOURCE_CLASS_POINTER && event.action == MotionEvent.ACTION_SCROLL) {
            if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0) {
                mRenderer!!.setScale(0.9f)
            } else {
                mRenderer!!.setScale(1.1f)
            }
            return true
        }
        return false
    }

    override fun setRenderer(renderer: Renderer?) {
        mRenderer = renderer as LivePlyGLRenderer
        super.setRenderer(renderer)
    }
}