package com.smartdispenser.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    @SerialName("connected")
    val connected: Boolean = true,
    @SerialName("state")
    val state: String = "IDLE",
    @SerialName("remainingMs")
    val remainingMs: Int = 0
)
