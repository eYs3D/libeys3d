package com.esp.uvc.ply.viewer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class IMUGLSurfaceView : GLSurfaceView {
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)
}