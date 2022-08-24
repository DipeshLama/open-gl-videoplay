package com.example.openglvideoplaying

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import com.example.openglvideoplaying.databinding.ActivityMainBinding
import com.google.vrtoolkit.cardboard.CardboardActivity

class MainActivity : CardboardActivity(), SurfaceTexture.OnFrameAvailableListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var renderer: VRVideoRenderer

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        renderer = VRVideoRenderer(this, this)
        binding.cardBoardView.apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        binding.cardBoardView.setOnTouchListener { _, event ->

            val posX = event.x
            val posY = event.y
            if (event.action == MotionEvent.ACTION_MOVE) {
                renderer.mCamera.setMotionPos(posX, posY)
            }
            renderer.mCamera.mLastX = posX
            renderer.mCamera.mLastY = posY

            return@setOnTouchListener true
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        binding.cardBoardView.queueEvent {
            surfaceTexture?.updateTexImage()
            binding.cardBoardView.requestRender()
        }
    }
}