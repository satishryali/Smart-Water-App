package com.smartdispenser.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdispenser.model.DispenseLimits
import com.smartdispenser.model.TimerPreset
import com.smartdispenser.repository.TimerPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerPresetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val timerPresetRepository: TimerPresetRepository
) : ViewModel() {

    private val categoryId: Long = savedStateHandle["categoryId"] ?: -1L
    private val presetId: Long = savedStateHandle["presetId"] ?: -1L

    val existingPreset: StateFlow<TimerPreset?> =
        if (presetId > 0) {
            timerPresetRepository.observePresetById(presetId)
        } else {
            flowOf(null)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun savePreset(name: String, timerInSeconds: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val duration = timerInSeconds.coerceIn(DispenseLimits.MIN_SECONDS, DispenseLimits.MAX_SECONDS)
            if (presetId > 0) {
                val existing = timerPresetRepository.getPresetById(presetId)
                if (existing != null) {
                    timerPresetRepository.updatePreset(
                        existing.copy(
                            presetName = name.trim(),
                            timerInSeconds = duration
                        )
                    )
                }
            } else {
                timerPresetRepository.insertPreset(
                    TimerPreset(
                        categoryId = categoryId,
                        presetName = name.trim(),
                        timerInSeconds = duration
                    )
                )
            }
            onSuccess()
        }
    }
}
