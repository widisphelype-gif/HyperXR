package com.hyperxr.pcvr

import android.app.Activity
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var address: EditText
    private lateinit var discoverButton: Button
    private lateinit var connectButton: Button
    @Volatile private var discoveredHost: String? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF101827.toInt()
        window.navigationBarColor = 0xFF101827.toInt()
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(0xFF101827.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            text = "HyperXR PCVR"
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
        }
        val subtitle = TextView(this).apply {
            text = "Conecte seu Android ao host HyperXR no Windows"
            textSize = 15f
            setTextColor(0xFFB8C5D8.toInt())
            setPadding(0, 8, 0, 24)
        }
        status = TextView(this).apply {
            text = "Status: desconectado"
            textSize = 16f
            setTextColor(0xFF80CBC4.toInt())
            setPadding(0, 16, 0, 20)
        }
        val section = TextView(this).apply {
            text = "1. Localizar o PC por Wi-Fi"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
        }
        discoverButton = makeButton("Procurar PC na rede") { discoverHost() }
        val manual = TextView(this).apply {
            text = "2. Ou informe o IP do PC (Wi-Fi ou USB tethering)"
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 24, 0, 8)
        }
        address = EditText(this).apply {
            hint = "Ex.: 192.168.1.100"
            setSingleLine(true)
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF8795A8.toInt())
            setPadding(14, 10, 14, 10)
            setBackgroundColor(0xFF263449.toInt())
        }
        connectButton = makeButton("Conectar ao PC") { connectToHost() }
        val note = TextView(this).apply {
            text = "Esta versão testa descoberta e handshake de rede. A transmissão de vídeo VR e a integração SteamVR ainda estão em desenvolvimento."
            textSize = 13f
            setTextColor(0xFFB8C5D8.toInt())
            setPadding(0, 28, 0, 8)
        }
        content.addView(title)
        content.addView(subtitle)
        content.addView(status)
        content.addView(section)
        content.addView(discoverButton, matchParams())
        content.addView(manual)
        content.addView(address, matchParams(56))
        content.addView(connectButton, matchParams())
        content.addView(note)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun matchParams(height: Int = 52) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply { topMargin = 8 }

    private fun makeButton(label: String, action: () -> Unit) =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setOnClickListener { action() }
        }

    private fun setStatus(message: String) {
        runOnUiThread { status.text = message }
    }

    private fun discoverHost() {
        discoverButton.isEnabled = false
        setStatus("Status: procurando anúncios HyperXR na porta UDP 9965…")
        thread(name = "pcvr-discovery") {
            var socket: DatagramSocket? = null
            try {
                val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                multicastLock = wifi.createMulticastLock("HyperXR-PCVR").apply {
                    setReferenceCounted(false)
                    acquire()
                }
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(9965))
                    soTimeout = 6500
                }
                val buffer = ByteArray(2048)
                val packet = DatagramPacket(buffer, buffer.size)
                while (true) {
                    try {
                        socket.receive(packet)
                        val json = JSONObject(String(packet.data, packet.offset, packet.length, Charsets.UTF_8))
                        if (json.optString("service") == "hyperxr-pcvr" && json.optInt("version") == 1) {
                            val host = packet.address.hostAddress ?: continue
                            discoveredHost = host
                            runOnUiThread { address.setText(host) }
                            setStatus("PC encontrado: $host — toque em Conectar ao PC.")
                            return@thread
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        break
                    }
                }
                setStatus("Nenhum PC encontrado. Confira se o host está aberto e tente informar o IP manualmente.")
            } catch (e: Exception) {
                setStatus("Falha na busca: ${e.message ?: "erro de rede"}. Tente o IP manual.")
            } finally {
                socket?.close()
                try { multicastLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) { }
                multicastLock = null
                runOnUiThread { discoverButton.isEnabled = true }
            }
        }
    }

    private fun connectToHost() {
        val host = address.text.toString().trim().ifBlank { discoveredHost.orEmpty() }
        if (host.isBlank()) {
            setStatus("Informe o IP do PC ou procure na rede primeiro.")
            return
        }
        connectButton.isEnabled = false
        setStatus("Status: conectando a $host:9966…")
        thread(name = "pcvr-handshake") {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, 9966), 4000)
                    socket.soTimeout = 5000
                    val request = JSONObject()
                        .put("service", "hyperxr-pcvr")
                        .put("version", 1)
                    socket.getOutputStream().write((request.toString() + "\n").toByteArray(Charsets.UTF_8))
                    socket.getOutputStream().flush()
                    val response = socket.getInputStream().bufferedReader(Charsets.UTF_8).readLine()
                        ?: error("O PC encerrou a conexão sem resposta")
                    val json = JSONObject(response)
                    if (!json.optBoolean("ok")) error(json.optString("error", "protocolo incompatível"))
                    val encoding = json.optJSONObject("encoding")
                    val ffmpeg = encoding?.optBoolean("ffmpeg", false) == true
                    val nvenc = encoding?.optBoolean("h264_nvenc", false) == true
                    setStatus(
                        "Conectado ao PC: ${json.optString("host", host)}\n" +
                        "Transporte: handshake confirmado\n" +
                        "FFmpeg: ${if (ffmpeg) "detectado" else "não detectado"} · NVIDIA NVENC: ${if (nvenc) "disponível" else "não detectado"}\n" +
                        "Vídeo VR: ainda não implementado."
                    )
                }
            } catch (e: Exception) {
                setStatus("Não foi possível conectar: ${e.message ?: "verifique o IP, o host e o firewall do Windows"}.")
            } finally {
                runOnUiThread { connectButton.isEnabled = true }
            }
        }
    }

    override fun onDestroy() {
        try { multicastLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) { }
        super.onDestroy()
    }
}
