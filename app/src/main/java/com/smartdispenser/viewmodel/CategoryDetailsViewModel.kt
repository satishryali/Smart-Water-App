package com.smartdispenser.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdispenser.model.ConnectionStatus
import com.smartdispenser.model.TimerPreset
import com.smartdispenser.repository.DispenserRepository
import com.smartdispenser.repository.TimerPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val timerPresetRepository: TimerPresetRepository,
    private val dispenserRepository: DispenserRepository
) : ViewModel() {

    private val categoryId: Long = savedStateHandle["categoryId"] ?: -1L

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.CHECKING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

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

    fun checkConnection() {
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.CHECKING
            val connected = dispenserRepository.checkConnection()
            _connectionStatus.value = if (connected) {
                ConnectionStatus.CONNECTED
            } else {
                ConnectionStatus.OFFLINE
            }
        }
    }

    fun dispense(preset: TimerPreset) {
        viewModelScope.launch {
            _dispensingPresetId.value = preset.id
            _dispenseResult.value = null
            val result = dispenserRepository.dispense(preset.timerInSeconds)
            result.fold(
                onSuccess = {
                    _dispenseResult.value = "Completed"
                },
                onFailure = { e ->
                    _dispenseResult.value = e.message ?: "Unknown error"
                }
            )
            _dispensingPresetId.value = null
        }
    }

    fun clearDispenseResult() {
        _dispenseResult.value = null
    }
}

data class CategoryDetailsUiState(
    val timerPresets: List<TimerPreset> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.CHECKING,
    val dispensingPresetId: Long? = null,
    val dispenseResult: String? = null
)