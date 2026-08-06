package com.smartdispenser.repository

import com.smartdispenser.data.SettingsRepository
import com.smartdispenser.model.DispenseRequest
import com.smartdispenser.model.StatusResponse
import com.smartdispenser.network.DispenserApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
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

    val isConnected: Flow<Boolean> = settingsRepository.esp32BaseUrl
        .map { baseUrl ->
            try {
                createApi(baseUrl).getStatus().connected
            } catch (e: Exception) {
                false
            }
        }

    suspend fun checkConnection(): Boolean {
        return try {
            val baseUrl = settingsRepository.esp32BaseUrl.first()
            val api = createApi(baseUrl)
            api.getStatus().connected
        } catch (e: Exception) {
            false
        }
    }

    suspend fun dispense(duration: Int): Result<Unit> {
        return try {
            val baseUrl = settingsRepository.esp32BaseUrl.first()
            val api = createApi(baseUrl)
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

    private fun createApi(baseUrl: String): DispenserApi {
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        return retrofit.create(DispenserApi::class.java)
    }
}