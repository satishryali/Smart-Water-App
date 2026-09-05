package com.smartdispenser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartdispenser.model.DispenserState
import com.smartdispenser.model.TimerPreset
import com.smartdispenser.ui.components.ConfirmDialog
import com.smartdispenser.ui.components.ConnectionStatusIndicator
import com.smartdispenser.viewmodel.CategoryDetailsViewModel
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScreen(
    categoryId: Long,
    onBack: () -> Unit,
    onAddTimer: () -> Unit,
    onEditTimer: (Long) -> Unit,
    viewModel: CategoryDetailsViewModel = hiltViewModel()
) {
    val timerPresets by viewModel.timerPresets.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val dispensingPresetId by viewModel.dispensingPresetId.collectAsStateWithLifecycle()
    val dispenseResult by viewModel.dispenseResult.collectAsStateWithLifecycle()
    val deviceState by viewModel.deviceState.collectAsStateWithLifecycle()
    val remainingMs by viewModel.remainingMs.collectAsStateWithLifecycle()
    val secondsFor250Ml by viewModel.secondsFor250Ml.collectAsStateWithLifecycle()
    var presetToDelete by remember { mutableStateOf<TimerPreset?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isDispensing = deviceState == DispenserState.DISPENSING

    LaunchedEffect(dispenseResult) {
        dispenseResult?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearDispenseResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Timer Presets",
                            fontWeight = FontWeight.Bold
                        )
                        ConnectionStatusIndicator(status = connectionStatus)
                    }
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTimer,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Timer",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                QuantityCard(
                    secondsFor250Ml = secondsFor250Ml,
                    isDispensing = isDispensing,
                    remainingMs = remainingMs,
                    deviceState = deviceState,
                    onDispenseMl = { viewModel.dispenseVolumeMl(it) },
                    onStop = { viewModel.stopDispense() }
                )
            }
            if (timerPresets.isEmpty()) {
                item {
                    Text(
                        text = "No timer presets yet. Tap + to add one, or use 250 ml / 500 ml / 1 L above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                items(
                    items = timerPresets,
                    key = { it.id }
                ) { preset ->
                    TimerPresetCard(
                        preset = preset,
                        isBusy = isDispensing,
                        isThisPreset = dispensingPresetId == preset.id,
                        onClick = { viewModel.dispense(preset) },
                        onEdit = { onEditTimer(preset.id) },
                        onDelete = { presetToDelete = preset }
                    )
                }
            }
        }
    }

    if (presetToDelete != null) {
        ConfirmDialog(
            title = "Delete Preset?",
            message = "Are you sure you want to delete \"${presetToDelete?.presetName}\"?",
            confirmText = "Delete",
            onConfirm = {
                presetToDelete?.let { viewModel.deletePreset(it) }
                presetToDelete = null
            },
            onDismiss = { presetToDelete = null }
        )
    }
}

@Composable
private fun QuantityCard(
    secondsFor250Ml: Int,
    isDispensing: Boolean,
    remainingMs: Int,
    deviceState: DispenserState,
    onDispenseMl: (Int) -> Unit,
    onStop: () -> Unit
) {
    val remainingSeconds = ceil(remainingMs / 1000.0).toInt()
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quantity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Uses time from settings ($secondsFor250Ml s per 250 ml). Calibrate after measuring a cup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isDispensing) {
                    "Dispensing · ${remainingSeconds}s left · $deviceState"
                } else {
                    "Status: $deviceState"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(250, 500, 1000).forEach { ml ->
                    val label = if (ml == 1000) "1 L" else "$ml ml"
                    OutlinedButton(
                        onClick = { onDispenseMl(ml) },
                        enabled = !isDispensing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label)
                    }
                }
            }
            if (isDispensing) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            }
        }
    }
}

@Composable
fun TimerPresetCard(
    preset: TimerPreset,
    isBusy: Boolean,
    isThisPreset: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.presetName,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${preset.timerInSeconds} seconds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onClick,
                enabled = !isBusy
            ) {
                if (isThisPreset) {
                    Text("Dispensing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dispense")
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
