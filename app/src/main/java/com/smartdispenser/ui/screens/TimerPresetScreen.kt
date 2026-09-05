package com.smartdispenser.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartdispenser.model.DispenseLimits
import com.smartdispenser.viewmodel.TimerPresetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerPresetScreen(
    categoryId: Long,
    presetId: Long?,
    onBack: () -> Unit,
    viewModel: TimerPresetViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var timerSeconds by remember { mutableIntStateOf(60) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var secondsError by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    val existingPreset by viewModel.existingPreset.collectAsStateWithLifecycle()
    val isEditing = presetId != null && presetId > 0

    LaunchedEffect(existingPreset) {
        val preset = existingPreset
        if (preset != null && !loaded) {
            name = preset.presetName
            timerSeconds = preset.timerInSeconds
            loaded = true
        }
    }

    fun validateSeconds(value: Int): String? {
        return when {
            value < DispenseLimits.MIN_SECONDS -> "Timer must be at least ${DispenseLimits.MIN_SECONDS} second"
            value > DispenseLimits.MAX_SECONDS -> "Timer cannot exceed ${DispenseLimits.MAX_SECONDS} seconds"
            else -> null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Timer Preset" else "New Timer Preset",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Timer Name") },
                placeholder = { Text("e.g., 500 ml cup") },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = if (timerSeconds > 0) timerSeconds.toString() else "",
                onValueChange = { input ->
                    timerSeconds = input.toIntOrNull() ?: 0
                    secondsError = validateSeconds(timerSeconds)
                },
                label = { Text("Timer Duration (seconds)") },
                placeholder = { Text("60") },
                singleLine = true,
                isError = secondsError != null,
                supportingText = {
                    Text(secondsError ?: "Max ${DispenseLimits.MAX_SECONDS} seconds (safety limit).")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Name is required"
                        return@Button
                    }
                    secondsError = validateSeconds(timerSeconds)
                    if (secondsError != null) return@Button

                    viewModel.savePreset(
                        name = name,
                        timerInSeconds = timerSeconds,
                        onSuccess = onBack
                    )
                },
                enabled = name.isNotBlank() && validateSeconds(timerSeconds) == null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Save Changes" else "Create Preset")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
