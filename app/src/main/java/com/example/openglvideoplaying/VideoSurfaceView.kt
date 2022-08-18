package com.example.openglvideoplaying

import android.content.Context
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLSurfaceView
import java.io.File

class VideoSurfaceView(context: Context, uri: File) :
    GLSurfaceView(context) {
    private var mRenderer: GlRenderer
    private var mMediaPlayer: MediaPlayer? = null

    init {
        setEGLContextClientVersion(3)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        mRenderer = GlRenderer(context, uri)
        setRenderer(mRenderer)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mMediaPlayer != null) {
            mMediaPlayer?.stop()
            mMediaPlayer?.release()
        }
    }
}
