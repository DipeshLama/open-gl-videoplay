package com.example.openglvideoplaying

import android.content.Context
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import com.google.vrtoolkit.cardboard.CardboardView
import com.google.vrtoolkit.cardboard.Eye
import com.google.vrtoolkit.cardboard.HeadTransform
import com.google.vrtoolkit.cardboard.Viewport
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig

class VrVideoRenderer(
    private val context: Context,
    private val frameAvailableListener: SurfaceTexture.OnFrameAvailableListener,
) : CardboardView.StereoRenderer {
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
            -1.0f, 1.0f, -1.0f,  // top left
            -1.0f, -1.0f, -1.0f,  // bottom left
            1.0f, -1.0f, -1.0f,  // bottom right
            1.0f, 1.0f, -1.0f  // top right
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

    private val Z_NEAR = 0.1f
    private val Z_FAR = 100.0f
    private val CAMERA_Z = 0.01f

    private val camera = FloatArray(16)
    private val view = FloatArray(16)
    private val modelViewProjection = FloatArray(16)

    private val mTransform = FloatArray(16)
    private val mView = FloatArray(16)

    private var triMVPMatrixParam = 0

    override fun onNewFrame(p0: HeadTransform?) {
        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
    }

    override fun onDrawEye(eye: Eye?) {
        glEnable(GL_DEPTH_TEST)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Apply the eye transformation to the camera
        Matrix.multiplyMM(view, 0, eye?.eyeView, 0, camera, 0)

        // Get the perspective transformation
        val perspective = eye?.getPerspective(Z_NEAR, Z_FAR)


        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, view, 0)

        // use surface and draw frame
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

        glUniformMatrix4fv(triMVPMatrixParam, 1, false,
            modelViewProjection, 0)

        glDrawElements(GL_TRIANGLES, index.size, GL_UNSIGNED_SHORT, indexBuffer)
    }

    override fun onFinishFrame(p0: Viewport?) {
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(p0: EGLConfig?) {
        mProgramHandle =
            createAndLinkProgram(ShaderSourceCode.mVertexShader, ShaderSourceCode.mFragmentShader)
        vPositionLoc = glGetAttribLocation(mProgramHandle, "a_Position")
        texCoordLoc = glGetAttribLocation(mProgramHandle, "a_TexCoordinate")
        textureLoc = glGetUniformLocation(mProgramHandle, "u_Texture")
        triMVPMatrixParam = glGetUniformLocation(mProgramHandle, "u_MVP")

        textureId = createOESTextureId()
        Log.d(TAG, "textureId: $textureId")

        val surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture.setOnFrameAvailableListener(frameAvailableListener)
        mediaPlayer = MediaPlayer()

        mediaPlayer?.setAudioAttributes(AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())

        val surface = Surface(surfaceTexture)
        mediaPlayer?.setSurface(surface)

        startVideo()
    }

    override fun onRendererShutdown() {

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