package com.example.openglvideoplaying

import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.openglvideoplaying.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), SurfaceTexture.OnFrameAvailableListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.glSurfaceView.setEGLContextClientVersion(2)
        binding.glSurfaceView.setRenderer(GlRenderer(this, this))
        binding.glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        binding.glSurfaceView.queueEvent{
            surfaceTexture?.updateTexImage()
            binding.glSurfaceView.requestRender()
        }
    }
}