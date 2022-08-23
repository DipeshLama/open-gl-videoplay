package com.example.openglvideoplaying

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.opengl.GLES11Ext
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class GlRenderer(
    private val context: Context,
    private val frameAvailableListener: SurfaceTexture.OnFrameAvailableListener,
) : GLSurfaceView.Renderer {

    private val TAG = "VideoRender"
    private val FLOAT_SIZE_BYTES = 4

    private var mProgramHandle = 0
    private var vPositionLoc = 0
    private var texCoordLoc = 0
    private var textureLoc = 0
    private var mvpMatrixLoc = 0
    private var mTexSamplerHandler = 0
    private var textureId = 0

    var mediaPlayer: MediaPlayer? = null

    private var vertexBuffer = arrayToBuffer(
        floatArrayOf(
            -1.0f, 1.0f, 0.0f,  // top left
            -1.0f, -1.0f, 0.0f,  // bottom left
            1.0f, -1.0f, 0.0f,  // bottom right
            1.0f, 1.0f, 0.0f  // top right
        ))


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

    var screenWidth: Int = 0
    var screenHeight: Int = 0

    var videoWidth = 0
    var videoHeight = 0

    var modelMatrix = FloatArray(16)

    //New variables
    private val cap = 9
    var verticals = FloatArray((180 / cap) * (360 / cap) * 6 * 3)
    private val uvTexVertex = FloatArray((180 / cap) * (360 / cap) * 6 * 2)

    private var verticalsBuffer: FloatBuffer
    private var mUvVertexBuffer: FloatBuffer

    val mProjectionMatrix = FloatArray(16)
    val mCameraMatrix = FloatArray(16)
    val mMvpMatrix = FloatArray(16)

    val mAngleX = 0f
    val mAngleY = 0f
    val mAngleZ = 1f
    val r = 6f

    init {
        val x = 0f
        val y = 0f
        val z = 0f

        var index = 0
        var index1 = 0

        val d: Double = cap * PI / 180

        var i = 0
        while (i < 180) {
            val d1 = i * Math.PI / 180
            var j = 0
            while (j < 360) {
                val d2 = j * Math.PI / 180
                verticals[index++] = (x + r * sin(d1 + d) * cos(d2 + d)).toFloat()
                verticals[index++] = (y + r * cos(d1 + d)).toFloat()
                verticals[index++] = (z + r * sin(d1 + d) * sin(d2 + d)).toFloat()

                uvTexVertex[index1++] = (j + cap) * 1f / 360
                uvTexVertex[index1++] = (i + cap) * 1f / 180

                verticals[index++] = (x + r * sin(d1) * cos(d2)).toFloat()
                verticals[index++] = (y + r * cos(d1)).toFloat()
                verticals[index++] = (z + r * sin(d1) * sin(d2)).toFloat()

                uvTexVertex[index1++] = j * 1f / 360
                uvTexVertex[index1++] = i * 1f / 180

                verticals[index++] = (x + r * sin(d1) * cos(d2 + d)).toFloat()
                verticals[index++] = (y + r * cos(d1)).toFloat()
                verticals[index++] = (z + r * sin(d1) * sin(d2 + d)).toFloat()

                uvTexVertex[index1++] = (j + cap) * 1f / 360
                uvTexVertex[index1++] = i * 1f / 180

                verticals[index++] = (x + r * sin(d1 + d) * cos(d2 + d)).toFloat()
                verticals[index++] = (y + r * cos(d1 + d)).toFloat()
                verticals[index++] = (z + r * sin(d1 + d) * sin(d2 + d)).toFloat()

                uvTexVertex[index1++] = (j + cap) * 1f / 360
                uvTexVertex[index1++] = (i + cap) * 1f / 180

                verticals[index++] = (x + r * sin(d1 + d) * cos(d2)).toFloat()
                verticals[index++] = (y + r * cos(d1 + d)).toFloat()
                verticals[index++] = (z + r * sin(d1 + d) * sin(d2)).toFloat()

                uvTexVertex[index1++] = j * 1f / 360
                uvTexVertex[index1++] = (i + cap) * 1f / 180

                verticals[index++] = (x + r * sin(d1) * cos(d2)).toFloat()
                verticals[index++] = (y + r * cos(d1)).toFloat()
                verticals[index++] = (z + r * sin(d1) * sin(d2)).toFloat()

                uvTexVertex[index1++] = j * 1f / 360
                uvTexVertex[index1++] = i * 1f / 180

                j += cap
            }
            i += cap
        }

        Log.d(TAG, "vertex: ${verticals.count()}")
        Log.d(TAG, "UvTEx: ${uvTexVertex.count()}")

        verticalsBuffer = arrayToBuffer(verticals)
        mUvVertexBuffer = arrayToBuffer(uvTexVertex)
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        mProgramHandle =
            createProgram(ShaderSourceCode.mVertexShader, ShaderSourceCode.mFragmentShader)

        vPositionLoc = glGetAttribLocation(mProgramHandle, "a_Position")
        texCoordLoc = glGetAttribLocation(mProgramHandle, "a_TexCoordinate")
        textureLoc = glGetUniformLocation(mProgramHandle, "u_Texture")
        mvpMatrixLoc = glGetUniformLocation(mProgramHandle, "mvpMatrix")

        textureId = createOESTextureId()
        Log.d(TAG, "textureId:$textureId")

        val surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture.setOnFrameAvailableListener(frameAvailableListener)
        mediaPlayer = MediaPlayer()

        mediaPlayer?.setAudioAttributes(AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())

        val surface = Surface(surfaceTexture)
        mediaPlayer?.setSurface(surface)
        startVideo()
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        if (width < height) {
            val ratio = height * 1f / width
            Matrix.frustumM(mProjectionMatrix, 0, -1f, 1f, -ratio, ratio, 1f, 1000f)
        } else {
            val ratio = width * 1f / height
            Matrix.perspectiveM(mProjectionMatrix, 0, 70f, ratio, 1F, 1000f)
        }
    }

    override fun onDrawFrame(p0: GL10?) {
        Matrix.setLookAtM(mCameraMatrix, 0, 0f, 0f, 0f, mAngleX, mAngleY, mAngleZ, 0f, 1f, 0f)

        Matrix.multiplyMM(mMvpMatrix, 0, mProjectionMatrix, 0, mCameraMatrix, 0)

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        glUseProgram(mProgramHandle)

        //set vertex data
        glEnableVertexAttribArray(vPositionLoc)
        glVertexAttribPointer(vPositionLoc, 3, GL_FLOAT, false, 12, verticalsBuffer)

        //set texture vertex data
        glEnableVertexAttribArray(texCoordLoc)
        glVertexAttribPointer(texCoordLoc, 2, GL_FLOAT, false, 0, mUvVertexBuffer)

        //set texture
//        glBindTexture(GL_TEXTURE_2D, textureId)

        glUniformMatrix4fv(mvpMatrixLoc, 1, false, mMvpMatrix, 0)
        glUniform1i(textureLoc, 0)

        glDrawArrays(GL_TRIANGLES, 0, (180 / cap) * (360 * cap) * 6)
        glDisableVertexAttribArray(vPositionLoc)
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        var shader = glCreateShader(shaderType)
        if (shader == 0) {
            return 0
        }
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
        return shader
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource)
        Log.d(TAG, "vertexShader: $vertexShader")
        if (vertexShader == 0) {
            return 0
        }

        val fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource)
        Log.d(TAG, "fragmentShader: $fragmentShader")

        if (fragmentShader == 0) {
            return 0
        }

        var program = glCreateProgram()

        if (program != 0) {
            glAttachShader(program, vertexShader)

            glAttachShader(program, fragmentShader)

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
        glActiveTexture(GL_TEXTURE0)

        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)

        glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL_TEXTURE_MIN_FILTER,
            GL_LINEAR.toFloat()
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
        val shortBuffer = ByteBuffer.allocateDirect(ar.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
        shortBuffer.put(ar).position(0)
        return shortBuffer
    }
}