package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TaskSyncDto(
    val syncUuid: String,
    val title: String,
    val description: String,
    val dueTimestamp: Long,
    val dueDateString: String,
    val dueTimeString: String,
    val priorityLevel: Int,
    val projectName: String,
    val projectColorHex: String,
    val isAlarmEnabled: Boolean,
    val alarmSound: String,
    val isCompleted: Boolean,
    val recurrenceName: String,
    val customIntervalDays: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)

@JsonClass(generateAdapter = true)
data class ProjectSyncDto(
    val syncUuid: String,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val isDefault: Boolean,
    val updatedAt: Long,
    val isDeleted: Boolean
)

@JsonClass(generateAdapter = true)
data class SyncPayloadData(
    val deviceName: String,
    val timestamp: Long,
    val tasks: List<TaskSyncDto>,
    val projects: List<ProjectSyncDto>
)

@JsonClass(generateAdapter = true)
data class EncryptedSyncPacket(
    val version: Int = 1,
    val saltHex: String,
    val ivHex: String,
    val cipherTextBase64: String,
    val authTagHex: String = "",
    val deviceId: String
)

@JsonClass(generateAdapter = true)
data class PeerServerStatus(
    val appName: String = "ChronoTask P2P Sync Server",
    val version: String = "1.0",
    val deviceName: String,
    val serverTime: Long,
    val requiresPassphrase: Boolean = true
)
