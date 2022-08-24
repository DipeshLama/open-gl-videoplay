package com.example.openglvideoplaying

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.openglvideoplaying.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), SurfaceTexture.OnFrameAvailableListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var renderer : GlRenderer

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        renderer = GlRenderer(this, this)
        binding.glSurfaceView.apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        binding.glSurfaceView.setOnTouchListener{_ , event ->

            val posX = event.x
            val posY = event.y
            if(event.action == MotionEvent.ACTION_MOVE){
                renderer.mCamera.setMotionPos(posX, posY)
            }
            renderer.mCamera.mLastX =posX
            renderer.mCamera.mLastY = posY

            return@setOnTouchListener true

        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        binding.glSurfaceView.queueEvent{
            surfaceTexture?.updateTexImage()
            binding.glSurfaceView.requestRender()
        }
    }
}