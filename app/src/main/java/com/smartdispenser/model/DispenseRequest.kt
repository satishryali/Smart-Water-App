package com.smartdispenser.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DispenseRequest(
    @SerialName("duration")
    val duration: Int
)