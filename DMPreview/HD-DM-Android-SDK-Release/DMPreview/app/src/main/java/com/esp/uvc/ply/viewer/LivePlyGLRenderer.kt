package com.esp.uvc.ply.viewer

import android.opengl.GLES30
import android.opengl.Matrix
import android.view.MotionEvent
import androidx.compose.ui.graphics.vectormath.Vector2
import com.esp.android.usb.camera.core.glrender.ArcBall
import com.esp.uvc.utils.logd
import com.esp.uvc.utils.loge
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import org.koin.core.KoinComponent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val DEFAULT_SCALE = 1f

class LivePlyGLRenderer : GLRenderer(), KoinComponent {
    private var mZNear = 0
    private var mZFar = 0
    private var mViewWidth = 0.0f
    private var mViewHeight = 0.0f
    var mLivePlyMesh: LivePlyMesh = LivePlyMesh()

    fun updateCurrentZExtreme(zMin: Int, zMax: Int) {
        mZNear = zMin
        mZFar = zMax
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        logd("onSurfaceCreated")
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        mScale = 1f
        mLivePlyMesh.prepareProgramAndObjects()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        logd("onSurfaceChanged")
        GLES30.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        mViewWidth = width.toFloat()
        mViewHeight = height.toFloat()
        Matrix.perspectiveM(mProjectionMatrix,0,120.0f, ratio, 0.1f, 16384.0f)
        mLivePlyMesh.resize(mViewWidth, mViewHeight)
        reset()
    }

    override fun onDrawFrame(gl: GL10?) {
        Matrix.setIdentityM(mViewMatrix, 0)
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, 0f, 0f, mScale)
        Matrix.translateM(mModelMatrix, 0, 0f, 0f, -(mZFar-mZNear)/2.0f)
        Matrix.multiplyMM(mModelMatrix, 0, mModelMatrix, 0, mLivePlyMesh.getArcBallTransformationMatrix(), 0)
        Matrix.translateM(mModelMatrix, 0, 0f, 0f, +(mZFar-mZNear)/2.0f)
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        mLivePlyMesh.draw(mMVPMatrix)
        mLivePlyMesh.drawAxis(mViewWidth, mViewHeight)
    }

    override fun setData(vertexBuffer: FloatArray, colorBuffer: FloatArray, vertexCount: Int) {
        mLivePlyMesh.setPointCloudData(vertexBuffer, colorBuffer, vertexCount)
    }
    fun setArcBallMouseDown(x:Float, y:Float){
        mLivePlyMesh.setArcBallDownPos(x, y)
    }

    fun setArcBallMove(x: Float, y: Float) {
        mLivePlyMesh.setArcBallMovePos(x, y)
    }

    fun setArcBallReset() {
        mLivePlyMesh.setArcBallReset()
    }

    override fun setScale(scale: Float) {
        if (scale < 1.0f) mScale -= 10.0f else mScale += 10.0f
    }

    override fun reset() {
        mScale = DEFAULT_SCALE
        setArcBallReset()
    }

    class LivePlyMesh {

        private var mProgramId = -100
        private var mPositionHandle = -100
        private var mColorHandle = -100
        private var mMVPMatrixHandle = 0

        private val COORDINATES_PER_VERTEX = 3
        private val COORDINATES_PER_COLOR = 3
        private val BYTES_PER_FLOAT = 4

        private var mVertexBuffer: FloatBuffer? = null
        private var mColorBuffer: FloatBuffer? = null
        private var mVertexCount: Int = -1
        private var mSphereVertexBuffer: FloatBuffer? = null
        private var mSphereColorBuffer: FloatBuffer? = null
        private var mSphereVertexCount: Int = -1

        private val UNIFORM_MATRIX = "uMVPMatrix"
        private val VERTEX_POSITION = "vPosition"
        private val VERTEX_COLOR = "vColor"
        private val FRAGMENT_COLOR = "fColor"
        private val mLock = Any()

        private val LIVE_PLY_VERTEX_SHADER =
                "uniform mat4 $UNIFORM_MATRIX;\n" +
                "attribute vec3 $VERTEX_POSITION;\n" +
                "attribute vec3 $VERTEX_COLOR;\n" +
                "varying vec3 $FRAGMENT_COLOR;\n" +
                "void main() {\n" +
                "  gl_Position = $UNIFORM_MATRIX * vec4($VERTEX_POSITION, 1);\n" +
                "  $FRAGMENT_COLOR = $VERTEX_COLOR;\n" +
                "  gl_PointSize = 3.0;" +
                "}\n"

        /** Move alpha into shader*/
        private val LIVE_PLY_FRAGMENT_SHADER =
                "precision mediump float;\n" +
                "varying vec3 $FRAGMENT_COLOR;\n" +
                "void main() {\n" +
                "  gl_FragColor = vec4($FRAGMENT_COLOR, 1f);\n" +
                "}\n"

        fun prepareProgramAndObjects() {
            mProgramId = createProgram(LIVE_PLY_VERTEX_SHADER, LIVE_PLY_FRAGMENT_SHADER)
            mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgramId, UNIFORM_MATRIX)
            mPositionHandle = GLES30.glGetAttribLocation(mProgramId, VERTEX_POSITION)
            mColorHandle = GLES30.glGetAttribLocation(mProgramId, VERTEX_COLOR)
            GLES30.glUseProgram(mProgramId)
            GLES30.glEnableVertexAttribArray(mPositionHandle)
            GLES30.glEnableVertexAttribArray(mColorHandle)
            prepareCircleData()
        }

        fun mat4ToString(matrix: FloatArray): String? {
            if (matrix.size < 16) {
                return "not a 4x4 matrix"
            }
            val str = StringBuilder()
            for (col in 0..3) {
                for (row in 0..3) {
                    str.append(String.format("%.5f", matrix[4 * row + col]))
                    str.append(',')
                }
                str.deleteCharAt(str.length - 1)
                str.append('\n')
            }
            return str.toString()
        }

        fun setPointCloudData(vertexBuffer: FloatArray, colorBuffer: FloatArray, vertexCount: Int) {
            synchronized(mLock) {
                if (mVertexBuffer == null || mColorBuffer == null || mVertexCount != vertexCount) { // Allocate once to prevent GC
                    mVertexBuffer = ByteBuffer.allocateDirect(vertexBuffer.size * BYTES_PER_FLOAT)
                            .order(ByteOrder.nativeOrder()).asFloatBuffer()
                    mColorBuffer = ByteBuffer.allocateDirect(colorBuffer.size * BYTES_PER_FLOAT)
                            .order(ByteOrder.nativeOrder()).asFloatBuffer()
                }

                mVertexBuffer!!.put(vertexBuffer).position(0)
                mColorBuffer!!.put(colorBuffer).position(0)
                mVertexCount = vertexCount
            }
        }

        private fun setSphereData(vertexBuffer: FloatArray, colorBuffer: FloatArray, vertexCount: Int) {
            if (mSphereVertexBuffer == null || mSphereColorBuffer == null) { // Set once
                synchronized(mLock) {
                    mSphereVertexBuffer = ByteBuffer.allocateDirect(vertexBuffer.size * BYTES_PER_FLOAT)
                            .order(ByteOrder.nativeOrder()).asFloatBuffer()
                    mSphereVertexBuffer!!.put(vertexBuffer).position(0)
                    mSphereColorBuffer =
                            ByteBuffer.allocateDirect(colorBuffer.size * BYTES_PER_FLOAT)
                                    .order(ByteOrder.nativeOrder())
                                    .asFloatBuffer()
                    mSphereColorBuffer!!.put(colorBuffer).position(0)
                    mSphereVertexCount = vertexCount
                }
            }
        }
        private var mCircleVertices = FloatArray(4500)
        private var mCircleColors = FloatArray(4500)  // RGBA
        val M_PI = PI.toFloat()
        val nTotalCirclePoint = 500
        val V_DIMENSION = 3
        val C_DIMENSION = 3
        fun prepareCircleData() {
            val fRadio = 0.5f
            val fOffset = (2 * M_PI) / (nTotalCirclePoint)
            val nTotalVerticesValueCount = nTotalCirclePoint * V_DIMENSION
            val nTotalVerticesColorCount = nTotalCirclePoint * C_DIMENSION
            var r = 0.0f
            var i = 0
            while (i < nTotalCirclePoint) {
                mCircleVertices[i * V_DIMENSION    ] = cos(r) * fRadio
                mCircleVertices[i * V_DIMENSION + 1] = sin(r) * fRadio
                mCircleVertices[i * V_DIMENSION + 2] = 0.0f
                mCircleColors[i * C_DIMENSION    ] = 163f / 255f
                mCircleColors[i * C_DIMENSION + 1] = 163f / 255f
                mCircleColors[i * C_DIMENSION + 2] = 234f / 255f

                mCircleVertices[i * V_DIMENSION +     nTotalVerticesValueCount] = 0f
                mCircleVertices[i * V_DIMENSION + 1 + nTotalVerticesValueCount] = cos(r) * fRadio
                mCircleVertices[i * V_DIMENSION + 2 + nTotalVerticesValueCount] = sin(r) * fRadio
                mCircleColors[i * C_DIMENSION +     nTotalVerticesColorCount] = 230F / 255f
                mCircleColors[i * C_DIMENSION + 1 + nTotalVerticesColorCount] = 161f / 255f
                mCircleColors[i * C_DIMENSION + 2 + nTotalVerticesColorCount] = 161f / 255f

                mCircleVertices[i * V_DIMENSION +     (nTotalVerticesValueCount * 2)] = sin(r) * fRadio
                mCircleVertices[i * V_DIMENSION + 1 + (nTotalVerticesValueCount * 2)] = 0f
                mCircleVertices[i * V_DIMENSION + 2 + (nTotalVerticesValueCount * 2)] = cos(r) * fRadio
                mCircleColors[i * C_DIMENSION +     (nTotalVerticesColorCount * 2)] = 179f / 255f
                mCircleColors[i * C_DIMENSION + 1 + (nTotalVerticesColorCount * 2)] = 244f / 255f
                mCircleColors[i * C_DIMENSION + 2 + (nTotalVerticesColorCount * 2)] = 185f / 255f
                ++i
                r += fOffset
            }
        }
        /** These matrix only for drawing sphere */
        private var mCircleModelMatrix = FloatArray(16)
        private val mCircleOrthoMatrix = FloatArray(16)
        private var m_arcBall = ArcBall()

        fun drawAxis(width: Float, height: Float) {
            synchronized(mLock){
                val nScale = if (width < height) width else height
                val circleMvpMatrix = FloatArray(16)
                val sphereVertexCount = mCircleVertices.size / 3
                val oneCircleCount = sphereVertexCount / 3

                Matrix.setIdentityM(mCircleModelMatrix, 0)
                Matrix.scaleM(mCircleModelMatrix, 0, nScale, nScale, nScale)

                Matrix.multiplyMM(circleMvpMatrix, 0, mCircleModelMatrix, 0,
                        m_arcBall.GetTransformation(), 0)
                Matrix.multiplyMM(circleMvpMatrix, 0, mCircleOrthoMatrix, 0,
                        circleMvpMatrix, 0)
                GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, circleMvpMatrix, 0)

                setSphereData(mCircleVertices, mCircleColors, oneCircleCount)
                GLES30.glVertexAttribPointer(mPositionHandle, COORDINATES_PER_VERTEX, GLES30.GL_FLOAT,
                        false, 0, mSphereVertexBuffer)
                GLES30.glVertexAttribPointer(mColorHandle, COORDINATES_PER_COLOR, GLES30.GL_FLOAT,
                        false, 0, mSphereColorBuffer)

                GLES30.glLineWidth(1.0f)
                GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, oneCircleCount)
                GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 1 * oneCircleCount, oneCircleCount)
                GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 2 * oneCircleCount, oneCircleCount)
            }
        }

        fun resize(width: Float, height: Float){
            Matrix.setIdentityM(mCircleOrthoMatrix, 0)
            var nZPlane = if (width < height) width else height
            Matrix.orthoM(mCircleOrthoMatrix, 0, -width/2, width/2, -height/2, height/2,
                    -nZPlane/2, nZPlane/2)
            m_arcBall.Resize(width, height)
        }

        fun getArcBallTransformationMatrix(): FloatArray{
            return m_arcBall.GetTransformation()
        }

        fun setArcBallDownPos(x: Float, y: Float) {
            m_arcBall.OnMouseDown(Vector2(x,y))
        }

        fun setArcBallMovePos(x: Float, y: Float) {
            m_arcBall.OnMouseMove(Vector2(x,y), MotionEvent.ACTION_MOVE)
        }

        fun setArcBallReset() {
            m_arcBall.Reset()
        }

        fun draw(mvpMatrix: FloatArray) {
            if (mProgramId < 0 || mVertexBuffer == null || mColorBuffer == null) {
                loge("Program || VertexBuffer || ColorBuffer not ready, return")
                return
            }
            synchronized(mLock){
                GLES30.glVertexAttribPointer(mPositionHandle, COORDINATES_PER_VERTEX,
                        GLES30.GL_FLOAT, false, 0, mVertexBuffer)
                GLES30.glVertexAttribPointer(mColorHandle, COORDINATES_PER_COLOR, GLES30.GL_FLOAT,
                        true, 0, mColorBuffer)
                GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
                GLES30.glDrawArrays(GLES30.GL_POINTS, 0, mVertexCount)
            }
        }

        private fun createShader(type: Int, source: String): Int {
            val shaderId = GLES30.glCreateShader(type)
            if (shaderId > 0) {
                GLES30.glShaderSource(shaderId, source)
                GLES30.glCompileShader(shaderId)
                val status = IntArray(1)
                GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, status, 0)
                if (status[0] <= 0) {
                    loge("glCompileShader failed : ${GLES30.glGetShaderInfoLog(shaderId)}")
                    GLES30.glDeleteShader(shaderId)
                }
            }
            return shaderId
        }

        private fun createProgram(vertexSource: String, fragmentSource: String): Int {
            val vertexShader = createShader(GLES30.GL_VERTEX_SHADER, vertexSource)
            val fragmentShader = createShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
            val mProgramId = GLES30.glCreateProgram()
            if (mProgramId > 0) {
                GLES30.glAttachShader(mProgramId, vertexShader)
                GLES30.glAttachShader(mProgramId, fragmentShader)
                GLES30.glBindAttribLocation(mProgramId, 0, VERTEX_POSITION)
                GLES30.glBindAttribLocation(mProgramId, 1, VERTEX_COLOR)
                GLES30.glLinkProgram(mProgramId)
                val status = IntArray(1)
                GLES30.glGetProgramiv(mProgramId, GLES30.GL_LINK_STATUS, status, 0)
                if (status[0] <= 0) {
                    loge("glLinkProgram failed : ${GLES30.glGetProgramInfoLog(mProgramId)}")
                    GLES30.glDeleteProgram(mProgramId)
                }
            }
            return mProgramId
        }
    }
}