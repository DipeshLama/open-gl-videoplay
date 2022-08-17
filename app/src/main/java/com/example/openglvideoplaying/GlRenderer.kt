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

class GlRenderer(private val context: Context, private val uri: Uri) : GLSurfaceView.Renderer,
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

    private var mProgram = 0
    private var mTextureId = 0
    private var muMVPMatrixHandle = 0
    private var muSTMatrixHandle = 0
    private var maPositionHandle = 0
    private var maTextureHandle = 0

    private var mSurface: SurfaceTexture? = null
    private var updateSurface = false

    private var GL_TEXTURE_EXTERNAL_OES = 0x8D65

    private var mMediaPlayer: MediaPlayer? = null

    init {
        mTriangleVertices =
            ByteBuffer.allocateDirect(mTriangleVerticesData.count() * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mTriangleVertices.put(mTriangleVerticesData).position(0)

        mTextureVertices =
            ByteBuffer.allocateDirect(mTextureVerticesData.count() * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mTextureVertices.put(mTextureVerticesData).position(0)

        Matrix.setIdentityM(mSTMatrix, 0)
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        mProgram = createProgram(ShaderSourceCode.mVertexShader, ShaderSourceCode.mFragmentShader)
        if (mProgram == 0) {
            return
        }
        maPositionHandle = glGetAttribLocation(mProgram, "aPosition")
        checkGlError("glGetAttribLocation aPosition")
        if (maPositionHandle == -1) {
            throw RuntimeException(
                "Could not get attrib location for aPosition")
        }
        maTextureHandle = glGetAttribLocation(mProgram, "aTextureCoord")
        checkGlError("glGetAttribLocation aTextureCoord")
        if (maTextureHandle == -1) {
            throw RuntimeException("Could not get attrib location for aTextureCoord")
        }
        muMVPMatrixHandle = glGetUniformLocation(mProgram, "uMvpMatrix")
        checkGlError("glGetUniformLocation uMVPMatrix")
        if (muMVPMatrixHandle == -1) {
            throw RuntimeException(
                "Could not get attrib location for uMVPMatrix")
        }

        muSTMatrixHandle = glGetUniformLocation(mProgram, "uSTMatrix")
        checkGlError("glGetUniformLocation uSTMatrix")
        if (muSTMatrixHandle == -1) {
            throw RuntimeException("Could not get attrib location for uSTMatrix")
        }

        val textures = IntArray(1)
        glGenTextures(1, textures, 0)

        mTextureId = textures[0]
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureId)
        checkGlError("glBindTexture mTextureID")

        glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_NEAREST.toFloat())
        glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR.toFloat())

        mSurface = SurfaceTexture(mTextureId)
        mSurface?.setOnFrameAvailableListener(this)

        val surface = Surface(mSurface)

        mMediaPlayer = MediaPlayer()

        if (uri != null) {
            try {
                mMediaPlayer?.setDataSource(context, uri)
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        mMediaPlayer?.setSurface(surface)
        surface.release()

        try {
            mMediaPlayer?.prepare()

        } catch (e: IOException) {
            Log.e(TAG, "media player prepare failed")
        }

        synchronized(this) {
            updateSurface = false
        }
        mMediaPlayer?.start()
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
        glUseProgram(mProgram)
        checkGlError("glUseProgram")

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureId)

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        glVertexAttribPointer(maPositionHandle,
            3,
            GL_FLOAT,
            false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            mTriangleVertices)

        checkGlError("glVertexAttribPointer maPosition")
        glEnableVertexAttribArray(maPositionHandle)
        checkGlError("glEnableVertexAttribArray maPositionHandle")

        mTextureVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        glVertexAttribPointer(maTextureHandle,
            2,
            GL_FLOAT,
            false,
            TEXTURE_VERTICES_DATA_STRIDE_BYTES,
            mTextureVertices)

        checkGlError("glVertexAttribPointer maTextureHandle")
        glEnableVertexAttribArray(maTextureHandle)
        checkGlError("glEnableVertexAttribArray maTextureHandle")

        Matrix.setIdentityM(mMVPMatrix, 0)
        glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix,
            0)
        glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0)

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
        val shader = glCreateShader(shaderType)
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
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Could not link program: ")
                Log.e(TAG, glGetProgramInfoLog(program))
                glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }
}