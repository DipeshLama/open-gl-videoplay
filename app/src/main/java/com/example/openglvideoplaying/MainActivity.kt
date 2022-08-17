package com.example.openglvideoplaying

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
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

class MainActivity : AppCompatActivity() {

    private lateinit var videoSurfaceView: VideoSurfaceView
    private lateinit var binding: ActivityMainBinding
    private var storagePermissionLauncher: ActivityResultLauncher<String>? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermission()

        val params = ConstraintLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
//        videoSurfaceView =
//            VideoSurfaceView(this, File("/Internal storage/SHAREit/videos/TaudahaVideo.mp4"))
        videoSurfaceView =
            VideoSurfaceView(this,
                Uri.parse("android.resource://com.example.openglvideoplaying" + "R.raw.video"))

        binding.constraintLayout.addView(videoSurfaceView, params)
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
}