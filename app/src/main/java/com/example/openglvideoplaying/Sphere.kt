package com.example.openglvideoplaying

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

class Sphere(
    private val nSlices: Int,
    x: Float,
    y: Float,
    z: Float,
    r: Float,
    numIndexBuffer: Int,
) {
    val FLOAT_SIZE = 4
    val SHORT_SIZE = 2

    private var mVertices: FloatBuffer
    private val mIndices: Array<ShortBuffer?>

    private var mNumIndices: IntArray
    private var mTotalIndices = 0

    init {
        val iMax = nSlices + 1
        val nVertices = iMax * iMax
        if (nVertices > Short.MAX_VALUE) {
            // this cannot be handled in one vertices / indices pair
            throw RuntimeException("nSlices $nSlices too big for vertex")
        }

        mTotalIndices = nSlices * nSlices * 6

        val angleStepI = Math.PI.toFloat() / nSlices
        val angleStepJ = 2.0f * Math.PI.toFloat() / nSlices

        // 3 vertex coords + 2 texture coords
        mVertices = ByteBuffer.allocateDirect(nVertices * 5 * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mIndices = arrayOfNulls(numIndexBuffer)

        mNumIndices = IntArray(numIndexBuffer)

        // first evenly distribute to n-1 buffers, then put remaining ones to the last one.
        val noIndicesPerBuffer: Int = mTotalIndices / numIndexBuffer / 6 * 6
        for (i in 0 until numIndexBuffer - 1) {
            mNumIndices[i] = noIndicesPerBuffer
        }
        mNumIndices[numIndexBuffer - 1] = mTotalIndices - noIndicesPerBuffer *
                (numIndexBuffer - 1)
        for (i in 0 until numIndexBuffer) {
            mIndices[i] = ByteBuffer.allocateDirect(mNumIndices[i] * SHORT_SIZE)
                .order(ByteOrder.nativeOrder()).asShortBuffer()
        }
        // calling put for each float took too much CPU time, so put by line instead
        val vLineBuffer = FloatArray(iMax * 5)
        for (i in 0 until iMax) {
            for (j in 0 until iMax) {
                val vertexBase = j * 5
                val sini = sin((angleStepI * i).toDouble()).toFloat()
                val sinj = sin((angleStepJ * j).toDouble()).toFloat()
                val cosi = cos((angleStepI * i).toDouble()).toFloat()
                val cosj = cos((angleStepJ * j).toDouble()).toFloat()
                // vertex x,y,z
                vLineBuffer[vertexBase + 0] = x + r * sini * sinj
                vLineBuffer[vertexBase + 1] = y + r * sini * cosj
                vLineBuffer[vertexBase + 2] = z + r * cosi
                // texture s,t
                vLineBuffer[vertexBase + 3] = j.toFloat() / nSlices.toFloat()
                vLineBuffer[vertexBase + 4] = (1.0f - i) / nSlices.toFloat()
            }
            mVertices.put(vLineBuffer, 0, vLineBuffer.size)
        }
        val indexBuffer = ShortArray(max(mNumIndices))
        var index = 0
        var bufferNum = 0
        for (i in 0 until nSlices) {
            for (j in 0 until nSlices) {
                val i1 = i + 1
                val j1 = j + 1
                if (index >= mNumIndices[bufferNum]) {
                    // buffer ready for moving to target
                    mIndices[bufferNum]?.put(indexBuffer, 0, mNumIndices[bufferNum])
                    // move to the next one
                    index = 0
                    bufferNum++
                }
                indexBuffer[index++] = (i * iMax + j).toShort()
                indexBuffer[index++] = (i1 * iMax + j).toShort()
                indexBuffer[index++] = (i1 * iMax + j1).toShort()
                indexBuffer[index++] = (i * iMax + j).toShort()
                indexBuffer[index++] = (i1 * iMax + j1).toShort()
                indexBuffer[index++] = (i * iMax + j1).toShort()
            }
        }
        mIndices[bufferNum]?.put(indexBuffer, 0, mNumIndices[bufferNum])
        mVertices.position(0)
        for (i in 0 until numIndexBuffer) {
            mIndices[i]?.position(0)
        }
    }

    fun getVertices(): FloatBuffer {
        return mVertices
    }

    fun getVerticesStride(): Int {
        return 5 * FLOAT_SIZE
    }

    fun getIndices(): Array<ShortBuffer?> {
        return mIndices
    }

    fun getNumIndices(): IntArray {
        return mNumIndices
    }

    fun getTotalIndices(): Int {
        return mTotalIndices
    }

    private fun max(array: IntArray): Int {
        var max = array[0]
        for (i in 1 until array.size) {
            if (array[i] > max) max = array[i]
        }
        return max
    }
}