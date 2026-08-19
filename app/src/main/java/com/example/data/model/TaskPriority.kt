package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class TaskPriority(
    val level: Int,
    val displayName: String,
    val shortName: String,
    val colorHex: String,
    val composeColor: Color,
    val bgAlphaColor: Color
) {
    P1_URGENT(
        level = 1,
        displayName = "P1 Urgent",
        shortName = "P1",
        colorHex = "#FF3B30",
        composeColor = Color(0xFFFF3B30),
        bgAlphaColor = Color(0x33FF3B30)
    ),
    P2_HIGH(
        level = 2,
        displayName = "P2 High",
        shortName = "P2",
        colorHex = "#FF9500",
        composeColor = Color(0xFFFF9500),
        bgAlphaColor = Color(0x33FF9500)
    ),
    P3_MEDIUM(
        level = 3,
        displayName = "P3 Medium",
        shortName = "P3",
        colorHex = "#0A84FF",
        composeColor = Color(0xFF0A84FF),
        bgAlphaColor = Color(0x330A84FF)
    ),
    P4_NORMAL(
        level = 4,
        displayName = "P4 Normal",
        shortName = "P4",
        colorHex = "#30D158",
        composeColor = Color(0xFF30D158),
        bgAlphaColor = Color(0x3330D158)
    );

    companion object {
        fun fromLevel(level: Int): TaskPriority {
            return entries.find { it.level == level } ?: P3_MEDIUM
        }
    }
}
