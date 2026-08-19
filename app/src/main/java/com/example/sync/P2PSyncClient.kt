package com.example.sync

import android.os.Build
import android.util.Log
import com.example.data.model.EncryptedSyncPacket
import com.example.data.model.PeerServerStatus
import com.example.data.model.SyncPayloadData
import com.example.data.repository.TaskRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class P2PSyncClient(
    private val repository: TaskRepository
) {
    companion object {
        private const val TAG = "P2PSyncClient"
    }

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val packetAdapter = moshi.adapter(EncryptedSyncPacket::class.java)
    private val payloadAdapter = moshi.adapter(SyncPayloadData::class.java)
    private val statusAdapter = moshi.adapter(PeerServerStatus::class.java)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun pingPeer(peerHost: String, port: Int = 8989): Result<PeerServerStatus> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$peerHost:$port/api/v1/ping"
            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Peer responded with HTTP ${response.code}"))
            }

            val status = statusAdapter.fromJson(body)
                ?: return@withContext Result.failure(Exception("Invalid peer status response"))

            Result.success(status)
        } catch (e: Exception) {
            Log.e(TAG, "Ping error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncWithPeer(
        peerHost: String,
        port: Int = 8989,
        passphrase: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 1. Export local data
            val localPayload = repository.exportSyncPayload(Build.MODEL)
            val localJson = payloadAdapter.toJson(localPayload)

            // 2. Encrypt with E2E passphrase
            val encryptedPacket = CryptoEngine.encrypt(localJson, passphrase, Build.MODEL)
            val packetJson = packetAdapter.toJson(encryptedPacket)

            // 3. Send to peer
            val url = "http://$peerHost:$port/api/v1/sync"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(url)
                .post(packetJson.toRequestBody(mediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Sync failed (HTTP ${response.code}): $responseBody")
                )
            }

            // 4. Decrypt peer's returned changes
            val responsePacket = packetAdapter.fromJson(responseBody)
                ?: return@withContext Result.failure(Exception("Invalid encrypted packet returned from peer"))

            val decryptedJson = CryptoEngine.decrypt(responsePacket, passphrase)
            val remotePayload = payloadAdapter.fromJson(decryptedJson)
                ?: return@withContext Result.failure(Exception("Could not parse decrypted remote sync payload"))

            // 5. Merge into local database
            val mergedCount = repository.mergeSyncPayload(remotePayload)
            Result.success(mergedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}")
            Result.failure(e)
        }
    }

    // Manual Offline Encrypted Code (Base64) for multi-device / iOS / clipboard sync
    suspend fun exportEncryptedCode(passphrase: String): String = withContext(Dispatchers.IO) {
        val payload = repository.exportSyncPayload(Build.MODEL)
        val json = payloadAdapter.toJson(payload)
        val packet = CryptoEngine.encrypt(json, passphrase, Build.MODEL)
        packetAdapter.toJson(packet)
    }

    suspend fun importEncryptedCode(encryptedPacketJson: String, passphrase: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val packet = packetAdapter.fromJson(encryptedPacketJson)
                ?: return@withContext Result.failure(Exception("Invalid encrypted sync package format"))
            val decryptedJson = CryptoEngine.decrypt(packet, passphrase)
            val payload = payloadAdapter.fromJson(decryptedJson)
                ?: return@withContext Result.failure(Exception("Decryption failed. Passphrase might be incorrect."))
            val count = repository.mergeSyncPayload(payload)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
