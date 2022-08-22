package com.example.openglvideoplaying

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.openglvideoplaying.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity(), SurfaceTexture.OnFrameAvailableListener {

    private lateinit var videoSurfaceView: VideoSurfaceView
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.glSurfaceView.setEGLContextClientVersion(2)
        binding.glSurfaceView.setRenderer(GlRenderer(this, this))
        binding.glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        binding.glSurfaceView.queueEvent{
            surfaceTexture?.updateTexImage()
            binding.glSurfaceView.requestRender()
        }
    }

//    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
//        videoSurfaceView.queueEvent{
//            surfaceTexture?.updateTexImage()
//            videoSurfaceView.requestRender()
//        }
//    }
}