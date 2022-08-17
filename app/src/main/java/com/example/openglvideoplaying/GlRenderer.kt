package com.example.openglvideoplaying

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GlRenderer(val context: Context, val uri: Uri) : GLSurfaceView.Renderer,
    SurfaceTexture.OnFrameAvailableListener {

    private val TAG = "VideoRender"
    private val FLOAT_SIZE_BYTES = 4
    private val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 3 * FLOAT_SIZE_BYTES
    private val TEXTURE_VERTICES_DATA_STRIDE_BYTES = 2 * FLOAT_SIZE_BYTES
    private val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
    private val TRIANGLE_VERTICES_DATA_UV_OFFSET = 0

    private val mTriangleVerticesData = floatArrayOf(-1.0f, -1.0f, 0f, 1.0f,
        -1.0f, 0f, -1.0f, 1.0f, 0f, 1.0f, 1.0f, 0f)

    private val mTextureVerticesData = floatArrayOf(0f, 0.0f, 1.0f, 0f,
        0.0f, 1f, 1.0f, 1.0f)

    private var mTriangleVertices: FloatBuffer

    private var mTextureVertices: FloatBuffer

    private val mMVPMatrix = FloatArray(16)
    private val mSTMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    private var mProgram: Int? = null
    private var mTextureId: Int? = null
    private var muMVPMatrixHandle: Int? = null
    private var muSTMatrixHandle: Int? = null
    private var maPositionHandle: Int? = null
    private var maTextureHandle: Int? = null

    private var mSurface: SurfaceTexture? = null
    private var updateSurface = false

    private var GL_TEXTURE_EXTERNAL_OES = 0x8D65

    private var mMediaPlayer: MediaPlayer? = null

    init {
        mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mTriangleVertices.put(mTriangleVerticesData).position(0)

        mTextureVertices = ByteBuffer.allocateDirect(mTextureVerticesData.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mTextureVertices.put(mTextureVerticesData).position(0)

        Matrix.setIdentityM(mSTMatrix, 0)
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
    }

    fun createProgram (){

    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        Matrix.frustumM(projectionMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f,
            1.0f, 10.0f)
    }

    override fun onDrawFrame(p0: GL10?) {
        synchronized(this) {
            if (updateSurface) {
                mSurface?.updateTexImage()
                mSurface?.getTransformMatrix(mSTMatrix)
                updateSurface = false
            } else {
                return
            }
        }

        glClearColor(225.0f, 225.0f, 225.0f, 1.0f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)
        glUseProgram(mProgram ?: 0)
        checkGlError("glUseProgram")

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureId ?: 0)

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        glVertexAttribPointer(maPositionHandle ?: 0,
            3,
            GL_FLOAT,
            false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            mTriangleVertices)

        checkGlError("glVertexAttribPointer maPosition")
        glEnableVertexAttribArray(maPositionHandle ?: 0)
        checkGlError("glEnableVertexAttribArray maPositionHandle")

        mTextureVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        glVertexAttribPointer(maTextureHandle ?: 0,
            2,
            GL_FLOAT,
            false,
            TEXTURE_VERTICES_DATA_STRIDE_BYTES,
            mTextureVertices)

        checkGlError("glVertexAttribPointer maTextureHandle")
        glEnableVertexAttribArray(maTextureHandle ?: 0)
        checkGlError("glEnableVertexAttribArray maTextureHandle")

        Matrix.setIdentityM(mMVPMatrix, 0)
        glUniformMatrix4fv(muMVPMatrixHandle ?: 0, 1, false, mMVPMatrix,
            0)
        glUniformMatrix4fv(muSTMatrixHandle ?: 0, 1, false, mSTMatrix, 0)

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
        glFinish()
    }

    override fun onFrameAvailable(surface: SurfaceTexture?) {
        updateSurface = true
    }

    private fun checkGlError(op: String) {
        var error: Int
        while (glGetError().also { error = it } != GL_NO_ERROR) {
            Log.e(TAG, "$op: glError $error")
            throw RuntimeException("$op: glError $error")
        }
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        var shader = glCreateShader(shaderType)
        if (shader != 0) {
            glShaderSource(shader, source)
            glCompileShader(shader)
            val compiled = IntArray(1)
            glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader $shaderType:")
                Log.e(TAG, glGetShaderInfoLog(shader))
                glDeleteShader(shader)
                return 0
            }
        }
        return shader
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            return 0
        }

        val fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) {
            return 0
        }

        var program = glCreateProgram()

        if (program != 0) {
            glAttachShader(program, vertexShader)
            checkGlError("glAttachShader")
            glAttachShader(program, fragmentShader)
            checkGlError("glAttachShader")
            glLinkProgram(program)
            val linkStatus = IntArray(1)

            glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GL_TRUE) {
                Log.e(TAG, "Could not link program: ")
                Log.e(TAG, glGetProgramInfoLog(program))
                glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }
}