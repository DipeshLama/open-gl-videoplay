package com.example.openglvideoplaying

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.openglvideoplaying.databinding.ActivityMainBinding
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity(), SurfaceTexture.OnFrameAvailableListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var renderer: GlRenderer

    var startRawX = 0f
    var startRawY = 0f

    var xFlingAngle = 0.0
    var xFlingAngleTemp = 0.0

    var yFlingAngle = 0.0
    var yFlingAngleTemp = 0.0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        renderer = GlRenderer(this, this)


        binding.glSurfaceView.setEGLContextClientVersion(2)
        binding.glSurfaceView.setRenderer(renderer)
        binding.glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        binding.glSurfaceView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                startRawX = event.rawX
                startRawY = event.rawY
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                var distanceX = startRawX - event.rawX
                var distanceY = startRawY - event.rawY
                distanceY = 0.1f * (distanceY) / windowManager.defaultDisplay.height

                yFlingAngleTemp = distanceY * 180 / (Math.PI * 3);

                if (yFlingAngleTemp + yFlingAngle > Math.PI / 2) {
                    yFlingAngleTemp = Math.PI / 2 - yFlingAngle
                }
                if (yFlingAngleTemp + yFlingAngle < -Math.PI / 2) {
                    yFlingAngleTemp = -Math.PI / 2 - yFlingAngle
                }

                distanceX = 0.1f * (-distanceX) / windowManager.defaultDisplay.width
                xFlingAngleTemp = distanceX * 180 / (Math.PI * 3)
                renderer.mAngleX = ((cos(yFlingAngle + yFlingAngleTemp) * sin(xFlingAngle + xFlingAngleTemp)).toFloat())

            } else if (event.action == MotionEvent.ACTION_UP) {
                xFlingAngle += xFlingAngleTemp
                yFlingAngle += yFlingAngleTemp
            }
            return@setOnTouchListener true
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        binding.glSurfaceView.queueEvent {
            surfaceTexture?.updateTexImage()
            binding.glSurfaceView.requestRender()
        }
    }
}