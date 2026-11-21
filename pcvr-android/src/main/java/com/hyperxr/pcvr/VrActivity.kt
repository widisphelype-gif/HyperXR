package com.hyperxr.pcvr

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class VrActivity : Activity() {
    private lateinit var vrView: VrPlaceholderView
    private var tracker: CameraMotionTracker? = null
    private var permissionRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        vrView = VrPlaceholderView(this)
        setContentView(vrView)
        ensureCameraPermission()
    }

    private fun ensureCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startTracking()
        else if (!permissionRequested) {
            permissionRequested = true
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 41)
            vrView.setStatus("Permita a câmera para testar o rastreamento visual")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 41) {
            if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) startTracking()
            else vrView.setStatus("Câmera negada · ajuste manual por toque disponível")
        }
    }

    private fun startTracking() {
        tracker?.stop()
        tracker = CameraMotionTracker(this) { dyaw, dpitch, status ->
            runOnUiThread { vrView.motion(dyaw, dpitch, status) }
        }.also { it.start() }
    }

    override fun onResume() {
        super.onResume()
        if (::vrView.isInitialized && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startTracking()
    }

    override fun onPause() {
        tracker?.stop(); tracker = null
        super.onPause()
    }

    override fun onDestroy() {
        tracker?.stop(); tracker = null
        super.onDestroy()
    }
}

private class VrPlaceholderView(context: android.content.Context) : View(context) {
    private val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF111E32.toInt() }
    private val main = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEAF2FF.toInt(); textAlign = Paint.Align.CENTER; textSize = 32f }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9FB1C9.toInt(); textAlign = Paint.Align.CENTER; textSize = 18f }
    private var lastX = 0f; private var lastY = 0f
    private var yaw = 0f; private var pitch = 0f
    private var status = "Aguardando permissão da câmera"

    fun setStatus(value: String) { status = value; invalidate() }
    fun motion(dx: Float, dy: Float, value: String) {
        yaw = (yaw + dx).coerceIn(-180f, 180f)
        pitch = (pitch + dy).coerceIn(-85f, 85f)
        status = value; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val half = width / 2f
        canvas.drawColor(0xFF050912.toInt())
        canvas.drawRoundRect(RectF(8f, 8f, half - 5f, height - 8f), 22f, 22f, panel)
        canvas.drawRoundRect(RectF(half + 5f, 8f, width - 8f, height - 8f), 22f, 22f, panel)
        canvas.drawLine(half, 0f, half, height.toFloat(), panel)
        val cy = height * 0.39f
        canvas.drawText("HYPERXR", half * 0.5f, cy, main)
        canvas.drawText("HYPERXR", half + half * 0.5f, cy, main)
        canvas.drawText("Vídeo do PC ainda não conectado", half * 0.5f, cy + 34f, small)
        canvas.drawText("Vídeo do PC ainda não conectado", half + half * 0.5f, cy + 34f, small)
        canvas.drawText("Câmera: $status", half, height - 66f, small)
        canvas.drawText("Visão estimada: ${yaw.toInt()}° / ${pitch.toInt()}°", half, height - 38f, small)
        canvas.drawText("Arraste para ajuste manual · Voltar para sair", half, 30f, small)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX = event.x; lastY = event.y; return true }
            MotionEvent.ACTION_MOVE -> {
                yaw = (yaw + (lastX - event.x) * 0.35f).coerceIn(-180f, 180f)
                pitch = (pitch + (lastY - event.y) * 0.25f).coerceIn(-85f, 85f)
                lastX = event.x; lastY = event.y; invalidate(); return true
            }
        }
        return true
    }
}
