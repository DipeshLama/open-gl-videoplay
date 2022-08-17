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
    private var storagePermissionLauncher: ActivityResultLauncher<String>? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermission()
        videoSurfaceView = VideoSurfaceView(this)
        videoSurfaceView.setEGLContextClientVersion(2)
        videoSurfaceView.setRenderer(GlRenderer(this, this))
        videoSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermission() {
        storagePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { result ->
            if (result) {

            } else
                respondOnUserPermissionAct()
        }
        storagePermissionLauncher?.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun respondOnUserPermissionAct() {
        when {
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED -> {

            }

            shouldShowRequestPermissionRationale
                (Manifest.permission.WRITE_EXTERNAL_STORAGE)
            -> displayPermissionRationale()

            else -> respondOnUserPermissionAct()
        }
    }

    private fun displayPermissionRationale() {
        AlertDialog.Builder(this)
            .setCancelable(false)
            .setMessage("We need permission in order to fetch music from your device")
            .setPositiveButton("Grant") { _, _ ->
                storagePermissionLauncher?.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            .setNegativeButton("Dismiss") { _, _ ->
            }
            .show()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        videoSurfaceView.queueEvent{
            surfaceTexture?.updateTexImage()
            videoSurfaceView.requestRender()
        }
    }
}