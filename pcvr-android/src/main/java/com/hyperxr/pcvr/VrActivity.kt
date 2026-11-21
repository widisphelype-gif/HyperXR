package com.hyperxr.pcvr

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.max

class VrActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(VrPlaceholderView(this))
    }
}

private class VrPlaceholderView(context: Context) : View(context) {
    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF080D18.toInt() }
    private val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF111E32.toInt() }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEAF2FF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 34f
    }
    private val secondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9FB1C9.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 21f
    }
    private var touchStartX = 0f
    private var yaw = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val half = width / 2f
        canvas.drawColor(0xFF050912.toInt())
        canvas.drawRoundRect(RectF(8f, 8f, half - 5f, height - 8f), 22f, 22f, panel)
        canvas.drawRoundRect(RectF(half + 5f, 8f, width - 8f, height - 8f), 22f, 22f, panel)
        canvas.drawLine(half, 0f, half, height.toFloat(), background)
        val centerY = height * 0.46f
        canvas.drawText("HYPERXR", half * 0.5f, centerY - 22f, text)
        canvas.drawText("HYPERXR", half + half * 0.5f, centerY - 22f, text)
        canvas.drawText("Aguardando vídeo do PC", half * 0.5f, centerY + 22f, secondary)
        canvas.drawText("Aguardando vídeo do PC", half + half * 0.5f, centerY + 22f, secondary)
        val yawLabel = "Visão manual: ${yaw.toInt()}°"
        canvas.drawText(yawLabel, half, height - 30f, secondary)
        canvas.drawText("Arraste a tela para ajustar a visão · Voltar para sair", half, 34f, secondary)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { touchStartX = event.x; return true }
            MotionEvent.ACTION_MOVE -> {
                yaw = (yaw + (touchStartX - event.x) * 0.35f).coerceIn(-180f, 180f)
                touchStartX = event.x
                invalidate()
                return true
            }
        }
        return true
    }
}
