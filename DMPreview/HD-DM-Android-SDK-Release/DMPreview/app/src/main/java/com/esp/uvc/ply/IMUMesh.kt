package com.esp.uvc.ply

import android.opengl.GLES30
import android.opengl.Matrix
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

// Reference Mesh.kt
open class IMUMesh {

    private var mProgramId = -100
    private var mPositionHandle = -100
    private var mColorHandle = -100

    private var mBoxVertex: FloatBuffer? = null
    private var mBoxColor: FloatBuffer? = null
    private var mBoxVertexCount: Int = -1

    private var mAxisVertex: FloatBuffer? = null
    private var mAxisColor: Array<FloatBuffer>? = null
    private var mAxisVertexCount: Int = -1

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

    fun setData(
        boxVertex: FloatArray,
        boxColor: FloatArray,
        boxVertexCount: Int,
        axisVertex: FloatArray,
        axisColor: Array<FloatArray>,
        axisVertexCount: Int
    ) {
        synchronized(mLock) {
            mBoxVertex = ByteBuffer.allocateDirect(boxVertex.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            mBoxVertex!!.put(boxVertex).position(0)
            mBoxColor =
                ByteBuffer.allocateDirect(boxColor.size * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
            mBoxColor!!.put(boxColor).position(0)
            mBoxVertexCount = boxVertexCount

            mAxisVertex = ByteBuffer.allocateDirect(axisVertex.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            mAxisVertex!!.put(axisVertex).position(0)
            val tmpAxisColor = ByteBuffer.allocateDirect(axisColor[0].size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            val tmpAxisColor1 = ByteBuffer.allocateDirect(axisColor[1].size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            val tmpAxisColor2 = ByteBuffer.allocateDirect(axisColor[2].size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            mAxisColor = arrayOf(tmpAxisColor, tmpAxisColor1, tmpAxisColor2)
            for(i in axisColor.indices) {
                mAxisColor!![i].put(axisColor[i]).position(0)
            }

            mAxisVertexCount = axisVertexCount
        }
    }

    fun draw(boxMvpMatrix: FloatArray, axisMvpMatrix: FloatArray) {
        if (mProgramId < 0 || mBoxVertex == null || mBoxColor == null || mAxisVertex == null ) {
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
                mBoxVertex
            )
            GLES30.glVertexAttribPointer(
                mColorHandle,
                COORDINATES_PER_COLOR,
                GLES30.GL_FLOAT,
                false,
                0,
                mBoxColor
            )

            GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, boxMvpMatrix, 0)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, mBoxVertexCount)

            GLES30.glVertexAttribPointer(
                mPositionHandle,
                COORDINATES_PER_VERTEX,
                GLES30.GL_FLOAT,
                false,
                0,
                mAxisVertex
            )
            for(i in mAxisColor!!.indices) {
                GLES30.glVertexAttribPointer(
                    mColorHandle,
                    COORDINATES_PER_COLOR,
                    GLES30.GL_FLOAT,
                    false,
                    0,
                    mAxisColor!![i]
                )

                when(i) {
                    1 -> Matrix.rotateM(axisMvpMatrix, 0, 90f, 0f, 0f, 1f)
                    2-> Matrix.rotateM(axisMvpMatrix, 0, -90f, 0f, 1f, 0f)
                }

                GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, axisMvpMatrix, 0)
                GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, mAxisVertexCount)
            }
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