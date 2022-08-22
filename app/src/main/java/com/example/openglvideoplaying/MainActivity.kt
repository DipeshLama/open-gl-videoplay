package com.example.openglvideoplaying

import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.openglvideoplaying.databinding.ActivityMainBinding
import com.google.vrtoolkit.cardboard.CardboardActivity

class MainActivity : CardboardActivity(), SurfaceTexture.OnFrameAvailableListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardboardView.setEGLContextClientVersion(2)
        binding.cardboardView.setRenderer(VrVideoRenderer(this, this))
        binding.cardboardView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        cardboardView = binding.cardboardView
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        binding.cardboardView.queueEvent {
            surfaceTexture?.updateTexImage()
            binding.cardboardView.requestRender()
        }
    }
}