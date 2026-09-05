package com.smartdispenser.model

enum class DispenserState {
    IDLE,
    DISPENSING,
    STOPPED,
    COMPLETED,
    ERROR;

    companion object {
        fun fromApi(value: String?): DispenserState {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: IDLE
        }
    }
}
