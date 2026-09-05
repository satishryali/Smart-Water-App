package com.smartdispenser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdispenser.model.Category
import com.smartdispenser.model.ConnectionStatus
import com.smartdispenser.repository.CategoryRepository
import com.smartdispenser.repository.DispenserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val dispenserRepository: DispenserRepository
) : ViewModel() {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CHECKING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.observeAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<HomeUiState> = combine(
        categories,
        connectionStatus
    ) { categories, status ->
        HomeUiState(
            categories = categories,
            connectionStatus = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        viewModelScope.launch {
            var first = true
            while (isActive) {
                refreshConnection(showChecking = first)
                first = false
                delay(5_000)
            }
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            refreshConnection(showChecking = true)
        }
    }

    private suspend fun refreshConnection(showChecking: Boolean) {
        if (showChecking) {
            _connectionStatus.value = ConnectionStatus.CHECKING
        }
        val connected = dispenserRepository.checkConnection()
        _connectionStatus.value = if (connected) {
            ConnectionStatus.CONNECTED
        } else {
            ConnectionStatus.OFFLINE
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.insertCategory(name.trim())
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category.copy(name = category.name.trim()))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}

data class HomeUiState(
    val categories: List<Category> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.CHECKING
)
