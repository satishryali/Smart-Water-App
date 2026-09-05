package com.smartdispenser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdispenser.data.SettingsRepository
import com.smartdispenser.model.DispenseLimits
import com.smartdispenser.repository.DispenserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dispenserRepository: DispenserRepository
) : ViewModel() {

    val esp32BaseUrl: StateFlow<String> = settingsRepository.esp32BaseUrl
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "http://192.168.1.100"
        )

    val secondsFor250Ml: StateFlow<Int> = settingsRepository.secondsFor250Ml
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DispenseLimits.DEFAULT_SECONDS_FOR_250ML
        )

    fun saveSettings(url: String, secondsFor250Ml: Int, onSaved: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setEsp32BaseUrl(url)
            settingsRepository.setSecondsFor250Ml(secondsFor250Ml)
            dispenserRepository.checkConnection()
            onSaved()
        }
    }
}
