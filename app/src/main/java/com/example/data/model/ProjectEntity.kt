package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncUuid: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#6366F1",
    val iconName: String = "Folder",
    val isDefault: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
