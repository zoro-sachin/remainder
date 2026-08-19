package com.example.data.model

import java.util.Calendar

enum class RecurrenceType(val displayName: String) {
    NONE("Does not repeat"),
    DAILY("Daily"),
    WEEKDAYS("Weekdays (Mon-Fri)"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom interval");

    fun calculateNextTimestamp(currentTimestamp: Long, customDays: Int = 1): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTimestamp
        }
        when (this) {
            NONE -> return currentTimestamp
            DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            WEEKDAYS -> {
                do {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                } while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            }
            WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            MONTHLY -> cal.add(Calendar.MONTH, 1)
            CUSTOM -> cal.add(Calendar.DAY_OF_YEAR, if (customDays > 0) customDays else 1)
        }
        return cal.timeInMillis
    }
}
