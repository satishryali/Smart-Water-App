package com.smartdispenser.repository

import com.smartdispenser.data.SettingsRepository
import com.smartdispenser.model.DispenseLimits
import com.smartdispenser.model.DispenseRequest
import com.smartdispenser.model.DispenserState
import com.smartdispenser.model.StatusResponse
import com.smartdispenser.network.DispenserApi
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DispenserRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient
) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getStatus(): Result<StatusResponse> {
        return try {
            val api = createApi()
            Result.success(api.getStatus())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkConnection(): Boolean {
        return getStatus().map { it.connected }.getOrDefault(false)
    }

    suspend fun dispense(durationSeconds: Int): Result<Unit> {
        val duration = durationSeconds.coerceIn(DispenseLimits.MIN_SECONDS, DispenseLimits.MAX_SECONDS)
        return try {
            val api = createApi()
            val response = api.dispense(DispenseRequest(duration))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Dispense failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stop(): Result<Unit> {
        return try {
            val api = createApi()
            val response = api.stop()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Stop failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun dispenserStateOf(status: StatusResponse): DispenserState =
        DispenserState.fromApi(status.state)

    private suspend fun createApi(): DispenserApi {
        val baseUrl = settingsRepository.esp32BaseUrl.first()
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        return retrofit.create(DispenserApi::class.java)
    }
}
