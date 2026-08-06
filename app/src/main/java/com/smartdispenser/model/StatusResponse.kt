package com.smartdispenser.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    @SerialName("connected")
    val connected: Boolean
)