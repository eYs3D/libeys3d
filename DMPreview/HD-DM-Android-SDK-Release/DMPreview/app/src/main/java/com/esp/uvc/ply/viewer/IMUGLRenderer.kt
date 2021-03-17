package com.esp.uvc.ply.viewer

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.esp.uvc.ply.IMUMesh
import com.esp.uvc.utils.gdx.Quaternion
import com.esp.uvc.utils.logd
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import org.koin.core.KoinComponent
import org.koin.core.inject

// Reference GLRenderer
class IMUGLRenderer : GLSurfaceView.Renderer, KoinComponent {

    private val mMesh: IMUMesh by inject()

    private val mViewMatrix = FloatArray(16)
    private val mProjectionMatrix = FloatArray(16)
    private val mModelMatrix = FloatArray(16)
    private val mMVPMatrix = FloatArray(16)

    private var mQuaternion: Quaternion? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        logd("onSurfaceCreated")
        GLES30.glClearColor(1f, 1f, 1f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        mMesh.prepareProgram()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        logd("onSurfaceChanged")
        GLES30.glViewport(0, 0, width, height)
        Matrix.setIdentityM(mProjectionMatrix, 0)
        val ratio = width.toFloat() / height
        val left = -ratio
        val bottom = -1.0f
        val top = 1.0f
        val near = 1.0f
        val far = 10.0f
        Matrix.frustumM(mProjectionMatrix, 0, left, ratio, bottom, top, near, far)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.rotateM(mModelMatrix, 0, -90f, 1f, 0f, 0f)
        if (mQuaternion != null) {
            val tmp = FloatArray(16)
            mQuaternion!!.toMatrix(tmp)
            Matrix.multiplyMM(mModelMatrix, 0, mModelMatrix, 0, tmp, 0)
        }

        val axisModelMatrix = mModelMatrix.clone()
        Matrix.scaleM(axisModelMatrix, 0, 1.25f, 0.75f, 0.75f)
        val axisMVPMatrix = mMVPMatrix.clone()
        Matrix.multiplyMM(axisMVPMatrix, 0, mViewMatrix, 0, axisModelMatrix, 0)
        Matrix.multiplyMM(axisMVPMatrix, 0, mProjectionMatrix, 0, axisMVPMatrix, 0)

        Matrix.scaleM(mModelMatrix, 0, 3.5f, 1.25f, 1.25f)
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)

        mMesh.draw(mMVPMatrix, axisMVPMatrix)
    }

    fun setData(
        boxVertex: FloatArray,
        boxColor: FloatArray,
        boxVertexCount: Int,
        axisVertex: FloatArray,
        axisColor: Array<FloatArray>,
        axisVertexCount: Int
    ) {
        mMesh.setData(boxVertex, boxColor, boxVertexCount, axisVertex, axisColor, axisVertexCount)
    }

    fun setQuaternion(q: Quaternion) {
        mQuaternion = q
    }
}