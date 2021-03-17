package com.esp.uvc.ply

import android.opengl.GLES30
import com.esp.uvc.utils.loge
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private const val UNIFORM_MATRIX = "uMVPMatrix"
private const val VERTEX_POSITION = "vPosition"
private const val VERTEX_COLOR = "vColor"
private const val FRAGMENT_COLOR = "fColor"

private const val VERTEX_SHADER =
    "uniform mat4 $UNIFORM_MATRIX;\n" +
            "attribute vec3 $VERTEX_POSITION;\n" +
            "attribute vec4 $VERTEX_COLOR;\n" +
            "varying vec4 $FRAGMENT_COLOR;\n" +
            "void main() {\n" +
            "  gl_Position = $UNIFORM_MATRIX * vec4($VERTEX_POSITION, 1);\n" +
            "  $FRAGMENT_COLOR = $VERTEX_COLOR;\n" +
            "}\n"

private const val FRAGMENT_SHADER =
    "precision mediump float;\n" +
            "varying vec4 $FRAGMENT_COLOR;\n" +
            "void main() {\n" +
            "  gl_FragColor = $FRAGMENT_COLOR;\n" +
            "}\n"

private const val BYTES_PER_FLOAT = 4

private const val COORDINATES_PER_VERTEX = 3
private const val COORDINATES_PER_COLOR = 4

class Mesh {

    private var mProgramId = -100
    private var mPositionHandle = -100
    private var mColorHandle = -100

    private var mVertexBuffer: FloatBuffer? = null
    private var mColorBuffer: FloatBuffer? = null
    private var mVertexCount: Int = -1

    private var mMVPMatrixHandle = 0

    private val mLock = Any()

    fun prepareProgram() {
        mProgramId = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgramId, UNIFORM_MATRIX)
        mPositionHandle = GLES30.glGetAttribLocation(mProgramId, VERTEX_POSITION)
        mColorHandle = GLES30.glGetAttribLocation(mProgramId, VERTEX_COLOR)
        GLES30.glUseProgram(mProgramId)
        GLES30.glEnableVertexAttribArray(mPositionHandle)
        GLES30.glEnableVertexAttribArray(mColorHandle)
    }

    fun setData(vertexBuffer: FloatArray, colorBuffer: FloatArray, vertexCount: Int) {
        synchronized(mLock) {
            mVertexBuffer = ByteBuffer.allocateDirect(vertexBuffer.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            mVertexBuffer!!.put(vertexBuffer).position(0)
            mColorBuffer =
                ByteBuffer.allocateDirect(colorBuffer.size * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
            mColorBuffer!!.put(colorBuffer).position(0)
            mVertexCount = vertexCount
        }
    }

    fun draw(mvpMatrix: FloatArray) {
        if (mProgramId < 0 || mVertexBuffer == null || mColorBuffer == null) {
            loge("Program || VertexBuffer || ColorBuffer not ready, return")
            return
        }
        synchronized(mLock) {
            GLES30.glVertexAttribPointer(
                mPositionHandle,
                COORDINATES_PER_VERTEX,
                GLES30.GL_FLOAT,
                false,
                0,
                mVertexBuffer
            )
            GLES30.glVertexAttribPointer(
                mColorHandle,
                COORDINATES_PER_COLOR,
                GLES30.GL_FLOAT,
                false,
                0,
                mColorBuffer
            )
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