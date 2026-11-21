package com.hyperxr.pcvr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import kotlin.math.abs

/** Local-only experimental visual-flow estimate; not an IMU or 6DoF tracker. */
class CameraMotionTracker(private val context: Context, private val onMotion: (Float, Float, String) -> Unit) {
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    @Volatile private var running = false
    private var previous: ByteArray? = null
    private var frame = 0

    fun start() {
        if (running) return
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            onMotion(0f, 0f, "Permissão da câmera necessária"); return
        }
        try {
            val id = manager.cameraIdList.firstOrNull {
                manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: run { onMotion(0f, 0f, "Câmera traseira não encontrada"); return }
            running = true
            previous = null
            frame = 0
            worker = HandlerThread("HyperXR-VisualFlow").also { it.start() }
            handler = Handler(worker!!.looper)
            reader = ImageReader.newInstance(320, 240, ImageFormat.YUV_420_888, 2).also { r ->
                r.setOnImageAvailableListener({ source ->
                    val image = try { source.acquireLatestImage() } catch (_: Exception) { null }
                    image?.use { process(it) }
                }, handler)
            }
            onMotion(0f, 0f, "Abrindo câmera traseira…")
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    if (!running) { camera.close(); return }
                    device = camera
                    val surface = reader?.surface ?: return
                    camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(config: CameraCaptureSession) {
                            if (!running) { config.close(); return }
                            session = config
                            try {
                                val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                    addTarget(surface)
                                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                }.build()
                                config.setRepeatingRequest(request, null, handler)
                                onMotion(0f, 0f, "Câmera ativa · analisando movimento visual")
                            } catch (e: Exception) { onMotion(0f, 0f, "Falha ao iniciar câmera: ${e.message}") }
                        }
                        override fun onConfigureFailed(config: CameraCaptureSession) {
                            onMotion(0f, 0f, "Falha ao configurar câmera")
                        }
                    }, handler)
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close(); device = null; onMotion(0f, 0f, "Câmera desconectada")
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close(); device = null; onMotion(0f, 0f, "Erro da câmera ($error)")
                }
            }, handler)
        } catch (e: Exception) {
            onMotion(0f, 0f, "Câmera indisponível: ${e.message}")
            stop()
        }
    }

    private fun process(image: Image) {
        if (!running || image.planes.isEmpty()) return
        if (++frame % 2 != 0) return
        val p = image.planes[0]; val b = p.buffer
        val w = image.width; val h = image.height
        val current = ByteArray(80 * 60)
        for (y in 0 until 60) for (x in 0 until 80) {
            val index = (y * h / 60) * p.rowStride + (x * w / 80) * p.pixelStride
            current[y * 80 + x] = if (index < b.limit()) b.get(index) else 0
        }
        val old = previous; previous = current
        if (old == null) return
        val (dx, dy, confidence) = shift(old, current)
        if (confidence < 2f || (abs(dx) < 1 && abs(dy) < 1)) return
        onMotion((-dx * 0.55f).coerceIn(-4f, 4f), (dy * 0.42f).coerceIn(-3f, 3f),
            "Rastreamento visual ativo · estimativa experimental")
    }

    private fun shift(a: ByteArray, b: ByteArray): Triple<Int, Int, Float> {
        var bx = 0; var by = 0; var best = Long.MAX_VALUE; var second = Long.MAX_VALUE
        for (dy in -5..5) for (dx in -5..5) {
            var cost = 0L; var n = 0
            for (y in 8 until 52 step 4) for (x in 8 until 72 step 4) {
                cost += abs((a[y * 80 + x].toInt() and 255) - (b[(y + dy) * 80 + x + dx].toInt() and 255)); n++
            }
            val score = cost / n
            if (score < best) { second = best; best = score; bx = dx; by = dy }
            else if (score < second) second = score
        }
        return Triple(bx, by, (second - best).toFloat())
    }

    fun stop() {
        running = false
        try { session?.stopRepeating() } catch (_: Exception) {}
        try { session?.close() } catch (_: Exception) {}
        try { device?.close() } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        session = null; device = null; reader = null; previous = null
        worker?.quitSafely(); worker = null; handler = null
    }
}
