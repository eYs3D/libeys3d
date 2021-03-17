package com.esp.uvc.ply.viewer

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.esp.uvc.ply.Mesh
import com.esp.uvc.utils.logd
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import org.koin.core.KoinComponent
import org.koin.core.inject

private const val DEFAULT_SCALE = 1f

open class GLRenderer : GLSurfaceView.Renderer, KoinComponent {

    private val mMesh: Mesh by inject()

    val mViewMatrix = FloatArray(16)
    val mProjectionMatrix = FloatArray(16)
    val mModelMatrix = FloatArray(16)
    val mMVPMatrix = FloatArray(16)

    private var mDeltaX = 0f
    private var mDeltaY = 0f
    var mScale = DEFAULT_SCALE

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        logd("onSurfaceCreated")
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        mDeltaX = 0f
        mDeltaY = 0f
        mScale = 1f
        mMesh.prepareProgram()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        logd("onSurfaceChanged")
        GLES30.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        val left = -ratio
        val bottom = -1.0f
        val top = 1.0f
        val near = 1.0f
        val far = 10.0f
        Matrix.frustumM(mProjectionMatrix, 0, left, ratio, bottom, top, near, far)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 2f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.rotateM(mModelMatrix, 0, mDeltaX, 0f, 1f, 0f)
        Matrix.rotateM(mModelMatrix, 0, mDeltaY, 1f, 0f, 0f)
        Matrix.scaleM(mModelMatrix, 0, mScale, mScale, mScale)
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)

        mMesh.draw(mMVPMatrix)
    }

    open fun setData(vertexBuffer: FloatArray, colorBuffer: FloatArray, vertexCount: Int) {
        mMesh.setData(vertexBuffer, colorBuffer, vertexCount)
    }

    fun setDeltaXY(deltaX: Float, deltaY: Float) {
        mDeltaX += deltaX
        mDeltaY += deltaY
    }

    open fun setScale(scale: Float) {
        mScale *= scale
    }

    open fun reset() {
        mDeltaX = 0f
        mDeltaY = 0f
        mScale = DEFAULT_SCALE
    }
}