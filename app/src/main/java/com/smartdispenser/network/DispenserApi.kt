package com.smartdispenser.network

import com.smartdispenser.model.DispenseRequest
import com.smartdispenser.model.StatusResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DispenserApi {

    @GET("/status")
    suspend fun getStatus(): StatusResponse

    @POST("/dispense")
    suspend fun dispense(@Body request: DispenseRequest): retrofit2.Response<Unit>

    @POST("/stop")
    suspend fun stop(): retrofit2.Response<Unit>
}
