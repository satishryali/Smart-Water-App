package com.smartdispenser.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdispenser.data.SettingsRepository
import com.smartdispenser.model.ConnectionStatus
import com.smartdispenser.model.DispenserState
import com.smartdispenser.model.TimerPreset
import com.smartdispenser.repository.DispenserRepository
import com.smartdispenser.repository.TimerPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val timerPresetRepository: TimerPresetRepository,
    private val dispenserRepository: DispenserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val categoryId: Long = savedStateHandle["categoryId"] ?: -1L

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CHECKING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _deviceState = MutableStateFlow(DispenserState.IDLE)
    val deviceState: StateFlow<DispenserState> = _deviceState.asStateFlow()

    private val _remainingMs = MutableStateFlow(0)
    val remainingMs: StateFlow<Int> = _remainingMs.asStateFlow()

    private val _dispensingPresetId = MutableStateFlow<Long?>(null)
    val dispensingPresetId: StateFlow<Long?> = _dispensingPresetId.asStateFlow()

    private val _dispenseResult = MutableStateFlow<String?>(null)
    val dispenseResult: StateFlow<String?> = _dispenseResult.asStateFlow()

    val timerPresets: StateFlow<List<TimerPreset>> = timerPresetRepository.observePresetsByCategory(categoryId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val secondsFor250Ml: StateFlow<Int> = settingsRepository.secondsFor250Ml
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10
        )

    init {
        viewModelScope.launch {
            var first = true
            while (isActive) {
                refreshStatus(showChecking = first)
                first = false
                val polling = if (_deviceState.value == DispenserState.DISPENSING) 500L else 2_000L
                delay(polling)
            }
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            refreshStatus(showChecking = true)
        }
    }

    fun dispense(preset: TimerPreset) {
        startDispense(preset.timerInSeconds, preset.id)
    }

    fun dispenseVolumeMl(volumeMl: Int) {
        val seconds = settingsRepository.secondsForVolumeMl(secondsFor250Ml.value, volumeMl)
        startDispense(seconds, (-volumeMl).toLong())
    }

    fun stopDispense() {
        viewModelScope.launch {
            val result = dispenserRepository.stop()
            result.fold(
                onSuccess = {
                    _deviceState.value = DispenserState.STOPPED
                    _remainingMs.value = 0
                    _dispensingPresetId.value = null
                    _dispenseResult.value = "Stopped"
                },
                onFailure = { e ->
                    _dispenseResult.value = e.message ?: "Stop failed"
                }
            )
            refreshStatus(showChecking = false)
        }
    }

    fun deletePreset(preset: TimerPreset) {
        viewModelScope.launch {
            timerPresetRepository.deletePreset(preset)
        }
    }

    fun clearDispenseResult() {
        _dispenseResult.value = null
    }

    private fun startDispense(durationSeconds: Int, sourceId: Long) {
        if (_deviceState.value == DispenserState.DISPENSING) return
        viewModelScope.launch {
            _dispensingPresetId.value = sourceId
            _dispenseResult.value = null
            val result = dispenserRepository.dispense(durationSeconds)
            result.fold(
                onSuccess = {
                    _deviceState.value = DispenserState.DISPENSING
                },
                onFailure = { e ->
                    _dispensingPresetId.value = null
                    _deviceState.value = DispenserState.ERROR
                    _dispenseResult.value = e.message ?: "Unknown error"
                    refreshStatus(showChecking = false)
                }
            )
        }
    }

    private suspend fun refreshStatus(showChecking: Boolean) {
        if (showChecking) {
            _connectionStatus.value = ConnectionStatus.CHECKING
        }
        dispenserRepository.getStatus().fold(
            onSuccess = { status ->
                _connectionStatus.value = if (status.connected) {
                    ConnectionStatus.CONNECTED
                } else {
                    ConnectionStatus.OFFLINE
                }
                val state = dispenserRepository.dispenserStateOf(status)
                val previous = _deviceState.value
                _deviceState.value = state
                _remainingMs.value = status.remainingMs.coerceAtLeast(0)
                if (state != DispenserState.DISPENSING) {
                    _dispensingPresetId.value = null
                }
                if (previous == DispenserState.DISPENSING && state == DispenserState.COMPLETED) {
                    _dispenseResult.value = "Completed"
                }
                if (previous == DispenserState.DISPENSING && state == DispenserState.ERROR) {
                    _dispenseResult.value = "Dispenser error"
                }
            },
            onFailure = {
                _connectionStatus.value = ConnectionStatus.OFFLINE
            }
        )
    }
}
