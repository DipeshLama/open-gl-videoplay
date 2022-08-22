package com.example.openglvideoplaying

import android.opengl.GLES20.*
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

const val TAG = "VideoRender"

fun loadShader(shaderType: Int, source: String): Int {
    var shader = glCreateShader(shaderType)
    Log.d(TAG, "shader:$shader")

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

fun createAndLinkProgram(vertexShaderSource: String, fragmentShaderSource: String): Int {
    val vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderSource)
    Log.d(TAG, "vertexShader: $vertexShader")
    if (vertexShader == 0) {
        return 0
    }

    val fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderSource)
    Log.d(TAG, "fragmentShader: $fragmentShader")
    if (fragmentShader == 0) {
        return 0
    }

    // creating program
    var program = glCreateProgram()
    if (program != 0) {
        glAttachShader(program, vertexShader)
        glAttachShader(program, fragmentShader)

        //linking program
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

fun checkGlError (op :String){
    var error: Int
    while (glGetError().also { error = it } != GL_NO_ERROR) {
        Log.e(TAG, "$op: glError $error")
        throw RuntimeException("$op: glError $error")
    }
}

