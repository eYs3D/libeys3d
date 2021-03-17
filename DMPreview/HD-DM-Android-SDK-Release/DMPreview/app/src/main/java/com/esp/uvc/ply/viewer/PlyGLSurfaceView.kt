package com.esp.uvc.ply.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ScaleGestureDetector

class PlyGLSurfaceView : GLSurfaceView, ScaleGestureDetector.OnScaleGestureListener {
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    private var mPreviousX = 0f
    private var mPreviousY = 0f

    private val mScaleGestureDetector = ScaleGestureDetector(context, this)

    private var mRenderer: GLRenderer? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        if (event!!.pointerCount == 1) {
        val x = event!!.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPreviousX = x
                mPreviousY = y
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX: Float = (x - mPreviousX) / 2f
                val deltaY: Float = (y - mPreviousY) / 2f
                if (mRenderer != null) {
                    mRenderer!!.setDeltaXY(deltaX, deltaY)
                }
                mPreviousX = x
                mPreviousY = y
            }
        }
//        } else {
        mScaleGestureDetector.onTouchEvent(event)
//        }
        return true
    }

    override fun onScaleBegin(p0: ScaleGestureDetector?): Boolean {
        return true
    }

    override fun onScaleEnd(p0: ScaleGestureDetector?) {
    }

    override fun onScale(p0: ScaleGestureDetector?): Boolean {
        if (mRenderer != null) {
            mRenderer!!.setScale(p0!!.scaleFactor)
        }
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (0 != event!!.source and InputDevice.SOURCE_CLASS_POINTER && event.action == MotionEvent.ACTION_SCROLL) {
            if(event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0) {
                mRenderer!!.setScale(0.9f)
            } else {
                mRenderer!!.setScale(1.1f)
            }
            return true
        }
        return false
    }

    override fun setRenderer(renderer: Renderer?) {
        mRenderer = renderer as GLRenderer
        super.setRenderer(renderer)
    }
}