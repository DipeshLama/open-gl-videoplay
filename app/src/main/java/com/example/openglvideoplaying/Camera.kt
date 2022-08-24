package com.example.openglvideoplaying

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.sin

class Camera {

    private val mVMatrix = FloatArray(16)
    private val mProjectionMatrix = FloatArray(16)

    private val mPosition = FloatArray(3)
    private val mDirection = FloatArray(3)
    private val mRight = FloatArray(3)
    private val mUp = FloatArray(3)

    var mHorizontalAngle = 3.14f
    var mVerticalAngle = 0.0f

    var mLastX = 0.0f
    var mLastY = 0.0f

    init {
        // camera position
        mPosition[0] = 0.0f
        mPosition[1] = 0.0f
        mPosition[2] = 0.0f

        // direction vector
        mDirection[0] =
            (cos(mVerticalAngle.toDouble()) * sin(mHorizontalAngle.toDouble())).toFloat()
        mDirection[1] = sin(mVerticalAngle.toDouble()).toFloat()
        mDirection[2] =
            (cos(mVerticalAngle.toDouble()) * cos(mHorizontalAngle.toDouble())).toFloat()

        // up vector
        mUp[0] = 0.0f
        mUp[1] = 1.0f
        mUp[2] = 0.0f

        // look at matrix initialized
        Matrix.setLookAtM(mVMatrix, 0,
            mPosition[0], mPosition[1], mPosition[2],
            mDirection[0], mDirection[1], mDirection[2],
            mUp[0], mUp[1], mUp[2])
    }

    // cross calculation
    fun cross(p1: FloatArray, p2: FloatArray, result: FloatArray) {
        result[0] = p1[1] * p2[2] - p2[1] * p1[2]
        result[1] = p1[2] * p2[0] - p2[2] * p1[0]
        result[2] = p1[0] * p2[1] - p2[0] * p1[1]
    }

    fun setMotionPos(posx: Float, posy: Float) {

        // compute delta angles
        mHorizontalAngle += (posx - mLastX) * 0.001f
        mVerticalAngle += (posy - mLastY) * 0.001f
    }

    // compute lookat matrix
    fun computeLookAtMatrix() {

        // make direction vector
        mDirection[0] =
            (cos(mVerticalAngle.toDouble()) * sin(mHorizontalAngle.toDouble())).toFloat()
        mDirection[1] = sin(mVerticalAngle.toDouble()).toFloat()
        mDirection[2] =
            (cos(mVerticalAngle.toDouble()) * cos(mHorizontalAngle.toDouble())).toFloat()

        // make right vector
        mRight[0] = sin((mHorizontalAngle - 3.14f / 2.0f).toDouble()).toFloat()
        mRight[1] = 0.0f
        mRight[2] = cos((mHorizontalAngle - 3.14f / 2.0f).toDouble()).toFloat()

        // make up vector
        cross(mRight, mDirection, mUp)

        // make look at matrix
        Matrix.setLookAtM(mVMatrix, 0,
            mPosition[0], mPosition[1], mPosition[2],
            mDirection[0], mDirection[1], mDirection[2],
            mUp[0], mUp[1], mUp[2])
    }

    fun getViewMatrix(): FloatArray? {
        return mVMatrix
    }

    fun setProjectionMatrix(p: FloatArray) {
        System.arraycopy(p, 0, mProjectionMatrix, 0, p.size)
    }

    fun getProjectionMatrix(): FloatArray? {
        return mProjectionMatrix
    }}