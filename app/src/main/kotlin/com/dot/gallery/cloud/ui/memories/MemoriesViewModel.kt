/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */

package com.dot.gallery.cloud.ui.memories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.MemoryInfo
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoriesUiState(
    val memories: List<MemoryInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MemoriesViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val registry: ProviderRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoriesUiState())
    val uiState: StateFlow<MemoriesUiState> = _uiState.asStateFlow()

    init {
        loadMemories()
    }

    fun loadMemories() {
        val providers = registry.getMemoriesProviders().filter { it.isAvailable }
        if (providers.isEmpty()) {
            _uiState.value = MemoriesUiState(error = "No memories provider available")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true)
        val type = providers.first().providerType
        viewModelScope.launch {
            repository.getMemories(type)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to load memories"
                    )
                }
                .collect { resource ->
                when (resource) {
                    is Resource.Success -> _uiState.value = MemoriesUiState(
                        memories = resource.data ?: emptyList(),
                        isLoading = false
                    )
                    is Resource.Error -> _uiState.value = MemoriesUiState(
                        isLoading = false,
                        error = resource.message
                    )
                }
            }
        }
    }
}
