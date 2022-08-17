package com.example.openglvideoplaying

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.opengl.GLES11Ext
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GlRenderer(
    val context: Context,
    val frameAvailableListener: SurfaceTexture.OnFrameAvailableListener,
) : GLSurfaceView.Renderer {

    private val TAG = "VideoRender"
    private val FLOAT_SIZE_BYTES = 4

    //New
    private var mProgramHandle = 0
    private var vPositionLoc = 0
    private var texCoordLoc = 0
    private var textureLoc = 0
    private var textureId = 0

    private var mediaPlayer: MediaPlayer? = null

    private var vertexBuffer = arrayToBuffer(
        floatArrayOf(
            -1.0f, 1.0f, 0.0f,  // top left
            -1.0f, -1.0f, 0.0f,  // bottom left
            1.0f, -1.0f, 0.0f,  // bottom right
            1.0f, 1.0f, 0.0f  // top right
        )
    )
    private var texBuffer = arrayToBuffer(
        floatArrayOf(
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
        )
    )
    private var index = shortArrayOf(3, 2, 0, 0, 1, 2)
    private val indexBuffer = shortArrayToBuffer(index)

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        mProgramHandle =
            createProgram(ShaderSourceCode.mVertexShader, ShaderSourceCode.mFragmentShader)
        vPositionLoc = glGetAttribLocation(mProgramHandle, "a_Position")
        texCoordLoc = glGetAttribLocation(mProgramHandle , "a_TexCoordinate")
        textureLoc = glGetUniformLocation(mProgramHandle, "u_Texture")

        textureId = createOESTextureId()

        val surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture.setOnFrameAvailableListener(frameAvailableListener)
        mediaPlayer = MediaPlayer()
//        mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)

        mediaPlayer?.setAudioAttributes(AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())

        val surface = Surface(surfaceTexture)
        mediaPlayer?.setSurface(surface)
        startVideo()
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
//        glViewport(0, 0, width, height)
//        Matrix.frustumM(projectionMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f,
//            1.0f, 10.0f)
    }

    override fun onDrawFrame(p0: GL10?) {
        glUseProgram(mProgramHandle)

        //set vertex data
        glEnableVertexAttribArray(vPositionLoc)
        glVertexAttribPointer(vPositionLoc, 3, GL_FLOAT, false, 0, vertexBuffer)

        //set texture vertex data
        texBuffer.position(0)
        glEnableVertexAttribArray(texCoordLoc)
        glVertexAttribPointer(texCoordLoc, 2, GL_FLOAT, false, 0, texBuffer)

        //set texture
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureId)
        glUniform1i(textureLoc, 0)

        glDrawElements(GL_TRIANGLES, index.size, GL_UNSIGNED_SHORT, indexBuffer)
    }

//    private fun checkGlError(op: String) {
//        var error: Int
//        while (glGetError().also { error = it } != GL_NO_ERROR) {
//            Log.e(TAG, "$op: glError $error")
//            throw RuntimeException("$op: glError $error")
//        }
//    }

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
                shader = 0
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
//            checkGlError("glAttachShader")
            glAttachShader(program, fragmentShader)
//            checkGlError("glAttachShader")
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

    private fun createOESTextureId(): Int {
        val textures = IntArray(1)
        val texture = textures[0]
        glGenTextures(1, textures, 0)
//        checkGlError("texture generate")
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
//        checkGlError("texture bind")

        glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL_TEXTURE_MIN_FILTER,
            GL_NEAREST.toFloat()
        )
        glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL_TEXTURE_MAG_FILTER,
            GL_LINEAR.toFloat()
        )
        glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL_TEXTURE_WRAP_S,
            GL_CLAMP_TO_EDGE
        )
        glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL_TEXTURE_WRAP_T,
            GL_CLAMP_TO_EDGE
        )

        return texture
    }

    private fun startVideo() {
        try {
            mediaPlayer?.reset()
            val fd = context.assets.openFd("videos/video.mp4")
            mediaPlayer?.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.d(TAG, "startVideo: $e")
        }
    }

    private fun arrayToBuffer(ar: FloatArray): FloatBuffer {
        val floatBuffer = ByteBuffer.allocateDirect(ar.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        floatBuffer.put(ar).position(0)
        return floatBuffer
    }

    private fun shortArrayToBuffer(ar: ShortArray): ShortBuffer {
        val shortBuffer = ByteBuffer.allocateDirect(ar.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
        shortBuffer.put(ar).position(0)
        return shortBuffer
    }
}