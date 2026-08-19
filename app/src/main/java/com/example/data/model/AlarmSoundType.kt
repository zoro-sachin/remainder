package com.example.data.model

enum class AlarmSoundType(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val iconName: String,
    val frequencies: List<Double>,
    val noteDurationsMs: List<Int>
) {
    ZEN_CHIME(
        id = "zen_chime",
        displayName = "Zen Chime",
        subtitle = "Soft peaceful harmonic chimes",
        iconName = "Spa",
        frequencies = listOf(528.0, 660.0, 792.0, 1056.0),
        noteDurationsMs = listOf(350, 350, 450, 700)
    ),
    CYBER_BEACON(
        id = "cyber_beacon",
        displayName = "Cyber Beacon",
        subtitle = "Futuristic ascending electronic pulse",
        iconName = "Bolt",
        frequencies = listOf(440.0, 880.0, 1320.0, 1760.0),
        noteDurationsMs = listOf(150, 150, 150, 350)
    ),
    URGENT_RADAR(
        id = "urgent_radar",
        displayName = "Urgent Radar",
        subtitle = "High-priority rhythmic alert tone",
        iconName = "Warning",
        frequencies = listOf(900.0, 1200.0, 900.0, 1200.0, 1500.0),
        noteDurationsMs = listOf(120, 120, 120, 120, 350)
    ),
    CLASSIC_BELL(
        id = "classic_bell",
        displayName = "Classic Bell",
        subtitle = "Traditional twin-bell alarm",
        iconName = "NotificationsActive",
        frequencies = listOf(659.25, 783.99, 659.25, 783.99),
        noteDurationsMs = listOf(200, 200, 200, 400)
    ),
    DAWN_HARMONY(
        id = "dawn_harmony",
        displayName = "Dawn Harmony",
        subtitle = "Gentle major triad chord wake-up",
        iconName = "WbSunny",
        frequencies = listOf(440.0, 554.37, 659.25, 880.0),
        noteDurationsMs = listOf(250, 250, 250, 500)
    ),
    CRYSTAL_ECHO(
        id = "crystal_echo",
        displayName = "Crystal Echo",
        subtitle = "Sparkling crystalline reverberation",
        iconName = "AutoAwesome",
        frequencies = listOf(1046.5, 1318.5, 1567.98, 2093.0),
        noteDurationsMs = listOf(180, 180, 180, 600)
    );

    companion object {
        fun fromId(id: String?): AlarmSoundType {
            return entries.find { it.id == id } ?: ZEN_CHIME
        }
    }
}
