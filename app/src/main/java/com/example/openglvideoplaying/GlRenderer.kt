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

class GlRenderer(
    private val context: Context,
    private val frameAvailableListener: SurfaceTexture.OnFrameAvailableListener,
) : GLSurfaceView.Renderer {

    private val TAG = "VideoRender"
    private val FLOAT_SIZE_BYTES = 4

    val SPHERE_SLICES = 180
    private val SPHERE_INDICES_PER_VERTEX = 1
    private val SPHERE_RADIUS = 500.0f

    private var mProgramHandle = 0
    private var vPositionLoc = 0
    private var texCoordLoc = 0
    private var textureLoc = 0
    private var mvpMatrixLoc = 0
    private var textureId = 0
    private var uTextureMatrixLocation = 0

    private var mediaPlayer: MediaPlayer? = null

    private var vertexBuffer = arrayToBuffer(
        floatArrayOf(
            -1.0f, 1.0f, 0.0f,  // top left
            -1.0f, -1.0f, 0.0f,  // bottom left
            1.0f, -1.0f, 0.0f,  // bottom right
            1.0f, 1.0f, 0.0f  // top right
        ))

    // For rotating in oppositeDirection
//    private var vertexBuffer = arrayToBuffer(floatArrayOf(
//        1.0f, 1.0f, 0.0f,  // top left
//        1.0f, -1.0f, 0.0f,  // bottom left
//        -1.0f, -1.0f, 0.0f,  // bottom right
//        -1.0f, 1.0f, 0.0f  // top right
//    ))

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
    var textureMatrix = FloatArray(16)

    private lateinit var sphere: Sphere

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        glClearColor(0f, 0f, 0f, 1f)
        mProgramHandle =
            createProgram(ShaderSourceCode.mVertexShader, ShaderSourceCode.mFragmentShader)
        vPositionLoc = glGetAttribLocation(mProgramHandle, "a_Position")
        texCoordLoc = glGetAttribLocation(mProgramHandle, "a_TexCoordinate")
        textureLoc = glGetUniformLocation(mProgramHandle, "u_Texture")
        mvpMatrixLoc = glGetUniformLocation(mProgramHandle, "mvpMatrix")
        uTextureMatrixLocation = glGetUniformLocation(mProgramHandle, "uTextureMatrix")

        sphere = Sphere(SPHERE_SLICES, 0.0f, 0.0f, 0.0f, SPHERE_RADIUS, SPHERE_INDICES_PER_VERTEX)

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

//        mediaPlayer?.setOnVideoSizeChangedListener { _, width, height ->
//            run {
//                videoWidth = width
//                Log.d(TAG, "videoWidth:$videoWidth")
//
//                videoHeight = height
//                Log.d(TAG, "videoHeight:$videoHeight")
//
//                if (screenWidth > 0 && screenHeight > 0) {
//                    computeMatrix()
//                }
//            }
//        }
    }

    private fun computeMatrix() {
        val videoRatio = videoWidth / videoHeight.toFloat()
        Log.d(TAG, "videoRatio:$videoRatio ")
        val screenRatio = screenWidth / screenHeight.toFloat()
        Log.d(TAG, "screenRatio:$screenRatio ")

        Matrix.setIdentityM(modelMatrix, 0)
        if (videoRatio > screenRatio) {
            Matrix.scaleM(modelMatrix, 0, 1f, 1 - ((videoRatio - screenRatio) / 2), 1f)
        } else if (videoRatio < screenRatio) {
            Matrix.scaleM(modelMatrix, 0, 1 - ((screenRatio - videoRatio) / 2), 1f, 1f)
        }
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
//        screenWidth = width
//        Log.d(TAG, "screenWidth:$screenWidth")
//        screenHeight = height
//        Log.d(TAG, "screenHeight:$screenHeight")
//
//        if (videoWidth > 0 && videoHeight > 0) {
//            computeMatrix()
//        }
    }

    override fun onDrawFrame(p0: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        glUseProgram(mProgramHandle)

        //set vertex data
        vertexBuffer.position(0)
        glEnableVertexAttribArray(vPositionLoc)
        glVertexAttribPointer(vPositionLoc,
            3,
            GL_FLOAT,
            false,
            sphere.getVerticesStride(),
            sphere.getVertices())

        //set texture vertex data
        texBuffer.position(0)
        glEnableVertexAttribArray(texCoordLoc)
        glVertexAttribPointer(texCoordLoc,
            2,
            GL_FLOAT,
            false,
            sphere.getVerticesStride(),
            sphere.getVertices().duplicate().position(3))

        //set texture
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureId)
        glUniform1i(textureLoc, 0)

        glUniformMatrix4fv(uTextureMatrixLocation, 1, false, textureMatrix, 0)
        glUniformMatrix4fv(mvpMatrixLoc, 1, false, modelMatrix, 0)

        for (i in 0 until sphere.getNumIndices().size) {
            glDrawElements(GL_TRIANGLES,
                sphere.getNumIndices()[i],
                GL_UNSIGNED_SHORT,
                sphere.getIndices()[i])
        }
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