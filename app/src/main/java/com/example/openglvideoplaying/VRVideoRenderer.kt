package com.example.openglvideoplaying

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.opengl.GLES11Ext
import android.opengl.GLES20
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

class VRVideoRenderer(
    private val context: Context,
    private val frameAvailableListener: SurfaceTexture.OnFrameAvailableListener,
) : CardboardView.StereoRenderer {
    private val TAG = "VideoRender"

    private val FLOAT_SIZE_BYTES = 4
    private val SHORT_SIZE_BYTES = 2
    private val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 3 * FLOAT_SIZE_BYTES
    private val TEXTURE_VERTICES_DATA_STRIDE_BYTES = 2 * FLOAT_SIZE_BYTES
    private val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
    private val TRIANGLE_VERTICES_DATA_UV_OFFSET = 0

    private var mProgramHandle = 0
    private var vPositionLoc = 0
    private var texCoordLoc = 0
    private var mvpMatrixLoc = 0
    private var textureId = 0
    private var uTextureMatrixLocation = 0

    private var mediaPlayer: MediaPlayer? = null

    private lateinit var mIndices: ShortArray

    private var mSphereVertices: FloatBuffer
    private var mSphereTextures: FloatBuffer
    private val mIndexBuffer: ShortBuffer

    private val mSTMatrix = FloatArray(16)
    private val mMVPMatrix = FloatArray(16)
    private val mMMatrix = FloatArray(16)
    private val mProjMatrix = FloatArray(16)

    private lateinit var surfaceTexture: SurfaceTexture

    var mCamera = Camera()

    private var mRatio = 0f
    private val mFOV = 60.0f
    private val mNear = 0.1f
    private val mFar = 10.0f

    private val GL_TEXTURE_EXTERNAL_OES = 0x8D65

    init {
        createSphereBuffer(1.0f, 40, 40)
        mSphereVertices = arrayToBuffer(Sphere.mVertices)
        mSphereTextures = arrayToBuffer(Sphere.mUV)
        mIndexBuffer = shortArrayToBuffer(mIndices)
        Matrix.setIdentityM(mSTMatrix, 0)
    }

    private fun createSphereBuffer(fRadius: Float, iRings: Int, iSectors: Int) {
        var r: Int
        var s: Int
        var size_index_indices = 0
        mIndices = ShortArray((iRings - 1) * (iSectors - 1) * 6)
        r = 0
        while (r < iRings - 1) {
            s = 0
            while (s < iSectors - 1) {
                mIndices[size_index_indices++] = (r * iSectors + s).toShort() //(a)
                mIndices[size_index_indices++] = (r * iSectors + (s + 1)).toShort() //(b)
                mIndices[size_index_indices++] = ((r + 1) * iSectors + (s + 1)).toShort() // (c)
                mIndices[size_index_indices++] = (r * iSectors + s).toShort() //(a)
                mIndices[size_index_indices++] = ((r + 1) * iSectors + (s + 1)).toShort() // (c)
                mIndices[size_index_indices++] = ((r + 1) * iSectors + s).toShort() //(d)
                s++
            }
            r++
        }
    }

    override fun onNewFrame(headTransform: HeadTransform?) {

    }

    override fun onDrawEye(eye: Eye?) {
        // clear buffer
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(mSTMatrix)

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LESS)

        GLES20.glUseProgram(mProgramHandle)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId)

        mSphereVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(vPositionLoc,
            3,
            GLES20.GL_FLOAT,
            false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            mSphereVertices)

        GLES20.glEnableVertexAttribArray(vPositionLoc)
        mSphereTextures.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)

        GLES20.glVertexAttribPointer(texCoordLoc, 2, GLES20.GL_FLOAT,
            false, TEXTURE_VERTICES_DATA_STRIDE_BYTES, mSphereTextures)
        GLES20.glEnableVertexAttribArray(texCoordLoc)

        Matrix.setIdentityM(mMVPMatrix, 0)
        Matrix.setIdentityM(mMMatrix, 0)

        mCamera.computeLookAtMatrix()

        Matrix.multiplyMM(mMVPMatrix, 0, mCamera.getViewMatrix(), 0, mMMatrix, 0)
        Matrix.multiplyMM(mMMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0)

        GLES20.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mMVPMatrix, 0)
        GLES20.glUniformMatrix4fv(uTextureMatrixLocation, 1, false, mSTMatrix, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
            mIndexBuffer.limit(),
            GLES20.GL_UNSIGNED_SHORT,
            mIndexBuffer)

        GLES20.glFinish()
    }

    override fun onFinishFrame(p0: Viewport?) {
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        mRatio = width.toFloat() / height

        GLES20.glViewport(0, 0, width, height)

        Matrix.perspectiveM(mProjMatrix, 0, mFOV, mRatio, mNear, mFar)
    }

    override fun onSurfaceCreated(p0: EGLConfig?) {
        mProgramHandle =
            createProgram(ShaderSourceCode.mVertexShader, ShaderSourceCode.mFragmentShader)
        vPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "a_Position")
        texCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate")
        mvpMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "mvpMatrix")
        uTextureMatrixLocation = GLES20.glGetUniformLocation(mProgramHandle, "uTextureMatrix")

        textureId = createOESTextureId()
        Log.d(TAG, "textureId:$textureId")

        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture.setOnFrameAvailableListener(frameAvailableListener)
        mediaPlayer = MediaPlayer()

        mediaPlayer?.setAudioAttributes(AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())

        val surface = Surface(surfaceTexture)
        mediaPlayer?.setSurface(surface)
        surface.release()

        startVideo()
    }

    override fun onRendererShutdown() {
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        var shader = GLES20.glCreateShader(shaderType)
        if (shader == 0) {
            return 0
        }
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)

        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader $shaderType:")
            Log.e(TAG, GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            shader = 0
        }
        return shader
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        Log.d(TAG, "vertexShader: $vertexShader")

        if (vertexShader == 0) {
            return 0
        }

        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        Log.d(TAG, "fragmentShader: $fragmentShader")

        if (fragmentShader == 0) {
            return 0
        }

        var program = GLES20.glCreateProgram()

        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader)

            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)

            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ")
                Log.e(TAG, GLES20.glGetProgramInfoLog(program))
                GLES20.glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }

    private fun createOESTextureId(): Int {
        val textures = IntArray(1)
        val texture = textures[0]
        GLES20.glGenTextures(1, textures, 0)

        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture)

        GLES20.glTexParameterf(
            GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
            GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        return texture
    }

    private fun startVideo() {
        try {
            mediaPlayer?.reset()
            val fd = context.assets.openFd("videos/sample360.mp4")
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
        val shortBuffer = ByteBuffer.allocateDirect(ar.size * SHORT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
        shortBuffer.put(ar).position(0)
        return shortBuffer
    }
}