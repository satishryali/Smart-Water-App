package com.smartdispenser.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdispenser.model.TimerPreset
import com.smartdispenser.repository.TimerPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerPresetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val timerPresetRepository: TimerPresetRepository
) : ViewModel() {

    private val categoryId: Long = savedStateHandle["categoryId"] ?: -1L
    private val presetId: Long? = savedStateHandle["presetId"]

    fun savePreset(name: String, timerInSeconds: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (presetId != null) {
                val existingPreset = timerPresetRepository.observePresetById(presetId)
                val preset = existingPreset.value?.firstOrNull()
                if (preset != null) {
                    timerPresetRepository.updatePreset(
                        preset.copy(
                            presetName = name.trim(),
                            timerInSeconds = timerInSeconds
                        )
                    )
                }
            } else {
                timerPresetRepository.insertPreset(
                    TimerPreset(
                        categoryId = categoryId,
                        presetName = name.trim(),
                        timerInSeconds = timerInSeconds
                    )
                )
            }
            onSuccess()
        }
    }

    fun deletePreset(preset: TimerPreset) {
        viewModelScope.launch {
            timerPresetRepository.deletePreset(preset)
        }
    }
}