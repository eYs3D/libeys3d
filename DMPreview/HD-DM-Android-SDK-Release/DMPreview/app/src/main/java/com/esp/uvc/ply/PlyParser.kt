package com.esp.uvc.ply

import com.esp.uvc.utils.loge
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.abs

private const val HEADER_FILE_PLY = "ply"
private const val HEADER_FORMAT_ASCII = "ascii"
private const val HEADER_COMMON = "common"
private const val HEADER_ELEMENT = "element"
private const val HEADER_ELEMENT_VERTEX = "vertex"
private const val HEADER_PROPERTY = "property"
private const val HEADER_PROPERTY_RED = "red"
private const val HEADER_PROPERTY_GREEN = "green"
private const val HEADER_PROPERTY_BLUE = "blue"
private const val HEADER_PROPERTY_ALPHA = "alpha"
private const val HEADER_PROPERTY_X = "x"
private const val HEADER_PROPERTY_Y = "y"
private const val HEADER_PROPERTY_Z = "z"
private const val HEADER_END = "end_header"

private const val INDEX_X = 0
private const val INDEX_Y = 1
private const val INDEX_Z = 2

private const val NO_INDEX = 100

class PlyParser(plyFile: InputStream) {

    private var mBufferedReader: BufferedReader = BufferedReader(InputStreamReader(plyFile))

    // -------- HEADER --------
    private var mInHeader = true

    private var mVertexCount = 0 // total vertices

    private var mElementCount = 0 // x, y, z ,r, g, b, a

    private var mVertexIndex = NO_INDEX // vertex start index
    private var mVertexSize = 0 // x, y, z

    private var mColorIndex = NO_INDEX // color start index
    private var mColorSize = 0 // r, g, b, a

    // -------- DATA --------
    private var mCurrentElement = 0

    private var mVertices: FloatArray? = null
    private var mVertexMaxZ = -10000f
    private var mVertexMaxAbsZ = 0f
    private var mVertexMaxAbsXY = 0f

    private var mColors: FloatArray? = null
    private var mColorMax = 0

    // -------- Old PLY file --------
    private var mOldFile = false

    fun parsePly(): Boolean {
        var line: String? = mBufferedReader.readLine()
        if (line != HEADER_FILE_PLY) {
            loge("File is not a PLY!")
            return false
        }

        line = mBufferedReader.readLine()
        val words = line.split(" ").toTypedArray()
        if (words[1] != HEADER_FORMAT_ASCII) {
            loge("File is not ASCII format! Cannot read.")
            return false
        }

        line = mBufferedReader.readLine()
        while (line != null && mInHeader) {
            parseHeader(line)
            line = mBufferedReader.readLine()
        }

        if (mColorSize != 4) {
            loge("Incorrect count of colors! Expected 4.")
            return false
        }

        if (mVertexSize != 3) {
            loge("Incorrect count of vertices! Expected 3.")
            return false
        }

        mVertices = FloatArray(mVertexCount * mVertexSize)
        mColors = FloatArray(mVertexCount * mColorSize)
        while (line != null) {
            parseData(line)
            line = mBufferedReader.readLine() ?: break
        }
        scaleData()
        return true
    }

    fun getVertexCount(): Int {
        return mVertexCount
    }

    fun getVertices(): FloatArray? {
        return mVertices
    }

    fun getColors(): FloatArray? {
        return mColors
    }

    private fun parseHeader(line: String) {
        val words = line.split(" ").toTypedArray()
        when (words[0]) {
            HEADER_COMMON -> return
            HEADER_ELEMENT -> {
                if (words[1] == HEADER_ELEMENT_VERTEX) {
                    mVertexCount = words[2].toInt()
                }
            }
            HEADER_PROPERTY -> {
                if (words[2] == HEADER_PROPERTY_X || words[2] == HEADER_PROPERTY_Y || words[2] == HEADER_PROPERTY_Z) {
                    if (mVertexIndex > mElementCount) {
                        mVertexIndex = mElementCount
                    }
                    mVertexSize++
                } else if (words[2] == HEADER_PROPERTY_RED || words[2] == HEADER_PROPERTY_GREEN || words[2] == HEADER_PROPERTY_BLUE || words[2] == HEADER_PROPERTY_ALPHA) {
                    if (mColorIndex > mElementCount) {
                        mColorIndex = mElementCount
                    }
                    mColorSize++
                }
                mElementCount++
            }
            HEADER_END -> {
                mInHeader = false
                if (mColorSize == 3) { // for old ply file
                    mOldFile = true
                    mColorSize++
                    mElementCount++
                } else {
                    mOldFile = false
                }
                return
            }
        }
    }

    private fun parseData(line: String) {
        val words = line.split(" ").toTypedArray()
        if (mCurrentElement < mVertexCount) {
            for (i in 0 until mVertexSize) {
                mVertices!![mCurrentElement * mVertexSize + i] = words[mVertexIndex + i].toFloat()
                if ((i == INDEX_X || i == INDEX_Y) && mVertexMaxAbsXY < abs(mVertices!![mCurrentElement * mVertexSize + i])) {
                    mVertexMaxAbsXY = abs(mVertices!![mCurrentElement * mVertexSize + i])
                } else if (i == INDEX_Z) {
                    if (mVertexMaxZ < mVertices!![mCurrentElement * mVertexSize + i])
                        mVertexMaxZ = mVertices!![mCurrentElement * mVertexSize + i]
                    else if (mVertexMaxAbsZ < abs(mVertices!![mCurrentElement * mVertexSize + i])) {
                        mVertexMaxAbsZ = abs(mVertices!![mCurrentElement * mVertexSize + i])
                    }
                }
            }
            for (i in 0 until mColorSize) {
                if (i == 3 && mOldFile) { // alpha and old
                    mColors!![mCurrentElement * mColorSize + i] = 255f
                } else {
                    mColors!![mCurrentElement * mColorSize + i] = words[mColorIndex + i].toFloat()
                }
                if (mColorMax < mColors!![mCurrentElement * mColorSize + i]) {
                    mColorMax = mColors!![mCurrentElement * mColorSize + i].toInt()
                }
            }
            mCurrentElement++
        }
    }

    private fun scaleData() {
        for (i in 0 until mVertexCount * mVertexSize) {
            if ((i + 1) % 3 == 0) { // z
                mVertices!![i] /= mVertexMaxAbsXY
                mVertices!![i] += (mVertexMaxAbsZ / mVertexMaxAbsXY + abs(mVertexMaxZ / mVertexMaxAbsXY)) / 2
            } else { // x, y
                mVertices!![i] /= mVertexMaxAbsXY
            }
        }
        for (i in 0 until mVertexCount * mColorSize) {
            mColors!![i] = mColors!![i] / mColorMax
        }
    }
}