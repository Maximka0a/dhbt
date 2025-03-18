package com.example.dhbt.domain.model

data class PomodoroSession(
    val id: String,
    val taskId: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Int,
    val type: PomodoroSessionType,
    val isCompleted: Boolean = false,
    val notes: String? = null
)

enum class PomodoroSessionType(val value: Int) {
    WORK(0),
    SHORT_BREAK(1),
    LONG_BREAK(2);

    companion object {
        fun fromInt(value: Int): PomodoroSessionType = values().firstOrNull { it.value == value } ?: WORK
    }
}