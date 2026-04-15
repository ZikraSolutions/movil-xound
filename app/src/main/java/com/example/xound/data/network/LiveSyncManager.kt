// app/src/main/java/com/example/xound/data/network/LiveSyncManager.kt
package com.example.xound.data.network

import com.example.xound.data.local.SessionManager
import com.example.xound.data.model.LiveEvent
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

object LiveSyncManager {

    private const val WS_URL = "wss://xound.duckdns.org/ws"

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentBandId: Long = -1L
    private var connected = false

    private val _liveEvent = MutableStateFlow<LiveEvent?>(null)
    val liveEvent: StateFlow<LiveEvent?> = _liveEvent.asStateFlow()

    /** Conectar al WebSocket y suscribirse al topic de la banda */
    fun connect(bandId: Long) {
        if (connected && currentBandId == bandId) return
        disconnect()
        currentBandId = bandId
        val token = SessionManager.getToken() ?: return
        val request = Request.Builder().url(WS_URL).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                connected = true
                ws.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer $token\n\n\u0000")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                val frame = parseFrame(text)
                when (frame.command) {
                    "CONNECTED" -> {
                        ws.send("SUBSCRIBE\nid:sub-0\ndestination:/topic/band/$bandId/live\n\n\u0000")
                    }
                    "MESSAGE" -> {
                        runCatching {
                            val event = gson.fromJson(frame.body, LiveEvent::class.java)
                            _liveEvent.value = event
                        }
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                connected = false
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                connected = false
            }
        })
    }

    /** Admin: enviar inicio de sesión en vivo */
    fun sendStart(bandId: Long, eventId: Long) {
        sendFrame(
            destination = "/app/band/$bandId/live",
            body = """{"type":"LIVE_START","bandId":$bandId,"eventId":$eventId}"""
        )
    }

    /** Admin: enviar estado actual (canción, línea, play/pause) */
    fun sendState(bandId: Long, eventId: Long, songIndex: Int, lineIndex: Int, isPlaying: Boolean) {
        sendFrame(
            destination = "/app/band/$bandId/live",
            body = """{"type":"LIVE_STATE","bandId":$bandId,"eventId":$eventId,"songIndex":$songIndex,"lineIndex":$lineIndex,"isPlaying":$isPlaying}"""
        )
    }

    /** Admin: enviar fin de sesión en vivo */
    fun sendEnd(bandId: Long) {
        sendFrame(
            destination = "/app/band/$bandId/live",
            body = """{"type":"LIVE_END","bandId":$bandId}"""
        )
        _liveEvent.value = null
    }

    /** Limpiar el último evento sin desconectar */
    fun clearEvent() {
        _liveEvent.value = null
    }

    fun disconnect() {
        webSocket?.send("DISCONNECT\n\n\u0000")
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        connected = false
        currentBandId = -1L
    }

    private fun sendFrame(destination: String, body: String) {
        webSocket?.send(
            "SEND\ndestination:$destination\ncontent-type:application/json\n\n$body\u0000"
        )
    }

    private fun parseFrame(text: String): StompFrame {
        // Remove trailing null byte
        val nullIdx = text.indexOf('\u0000')
        val content = if (nullIdx >= 0) text.substring(0, nullIdx) else text

        val bodyIdx = content.indexOf("\n\n")
        val headerPart = if (bodyIdx >= 0) content.substring(0, bodyIdx) else content
        val body = if (bodyIdx >= 0) content.substring(bodyIdx + 2) else ""

        val lines = headerPart.split("\n")
        val command = lines.firstOrNull()?.trim() ?: ""
        val headers = lines.drop(1)
            .filter { it.contains(":") }
            .associate { line ->
                val colonIdx = line.indexOf(":")
                line.substring(0, colonIdx).trim() to line.substring(colonIdx + 1).trim()
            }
        return StompFrame(command, headers, body)
    }

    private data class StompFrame(
        val command: String,
        val headers: Map<String, String>,
        val body: String
    )
}
