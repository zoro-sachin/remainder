package com.example.sync

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.EncryptedSyncPacket
import com.example.data.model.PeerServerStatus
import com.example.data.model.SyncPayloadData
import com.example.data.repository.TaskRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Locale

class P2PSyncServer(
    private val context: Context,
    private val repository: TaskRepository,
    val port: Int = 8989
) {
    companion object {
        private const val TAG = "P2PSyncServer"
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val packetAdapter = moshi.adapter(EncryptedSyncPacket::class.java)
    private val payloadAdapter = moshi.adapter(SyncPayloadData::class.java)
    private val statusAdapter = moshi.adapter(PeerServerStatus::class.java)

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning = _isServerRunning.asStateFlow()

    private val _serverIp = MutableStateFlow("127.0.0.1")
    val serverIp = _serverIp.asStateFlow()

    private val _lastSyncLog = MutableStateFlow<String>("Server ready to start")
    val lastSyncLog = _lastSyncLog.asStateFlow()

    @Volatile
    var syncPassphrase: String = "ChronoTaskSecureSync2026"

    fun startServer() {
        if (_isServerRunning.value) return

        val ip = getLocalIpAddress()
        _serverIp.value = ip

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                _isServerRunning.value = true
                _lastSyncLog.value = "P2P Server listening on http://$ip:$port"
                Log.d(TAG, "P2P Server listening on http://$ip:$port")

                while (isActive && _isServerRunning.value) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        scope.launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!_isServerRunning.value) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server: ${e.message}")
                _lastSyncLog.value = "Server error: ${e.message}"
            } finally {
                _isServerRunning.value = false
            }
        }
    }

    fun stopServer() {
        _isServerRunning.value = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        serverJob?.cancel()
        serverJob = null
        _lastSyncLog.value = "Server stopped"
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out: OutputStream = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0].uppercase(Locale.ROOT)
            val path = parts[1]

            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                if (line!!.lowercase(Locale.ROOT).startsWith("content-length:")) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            when {
                path.startsWith("/api/v1/ping") -> {
                    val status = PeerServerStatus(
                        deviceName = android.os.Build.MODEL,
                        serverTime = System.currentTimeMillis(),
                        requiresPassphrase = true
                    )
                    val json = statusAdapter.toJson(status)
                    sendResponse(out, 200, "OK", "application/json", json)
                }

                path.startsWith("/api/v1/sync") && method == "POST" -> {
                    val bodyChars = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val count = reader.read(bodyChars, read, contentLength - read)
                        if (count == -1) break
                        read += count
                    }
                    val body = String(bodyChars, 0, read)

                    try {
                        val encryptedPacket = packetAdapter.fromJson(body)
                        if (encryptedPacket == null) {
                            sendResponse(out, 400, "Bad Request", "text/plain", "Invalid packet")
                            return@withContext
                        }

                        // Decrypt payload with user passphrase
                        val decryptedJson = CryptoEngine.decrypt(encryptedPacket, syncPassphrase)
                        val remotePayload = payloadAdapter.fromJson(decryptedJson)
                        if (remotePayload == null) {
                            sendResponse(out, 400, "Bad Request", "text/plain", "Decryption failed or invalid JSON")
                            return@withContext
                        }

                        // Merge remote data into Room database
                        val merged = repository.mergeSyncPayload(remotePayload)
                        _lastSyncLog.value = "Merged $merged items from peer ${remotePayload.deviceName}"

                        // Export our current data to send back
                        val localData = repository.exportSyncPayload(android.os.Build.MODEL)
                        val localDataJson = payloadAdapter.toJson(localData)
                        val responseEncryptedPacket = CryptoEngine.encrypt(localDataJson, syncPassphrase, android.os.Build.MODEL)
                        val responseJson = packetAdapter.toJson(responseEncryptedPacket)

                        sendResponse(out, 200, "OK", "application/json", responseJson)
                    } catch (e: Exception) {
                        Log.e(TAG, "Decryption or sync error: ${e.message}")
                        _lastSyncLog.value = "Sync error: ${e.message}"
                        sendResponse(out, 403, "Forbidden", "text/plain", "Sync error: Authentication/Decryption failed")
                    }
                }

                else -> {
                    sendResponse(out, 404, "Not Found", "text/plain", "Not Found")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client handle error: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun sendResponse(
        out: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: String
    ) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP: ${e.message}")
        }
        return "127.0.0.1"
    }
}
