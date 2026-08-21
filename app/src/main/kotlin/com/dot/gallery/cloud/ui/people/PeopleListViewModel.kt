/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */

package com.dot.gallery.cloud.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

data class PeopleListUiState(
    val people: List<PersonInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PeopleListViewModel @Inject constructor(
    private val repository: CloudRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeopleListUiState())
    val uiState: StateFlow<PeopleListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadPeople()
        viewModelScope.launch {
            repository.peopleInvalidation.collect {
                loadPeople()
            }
        }
    }

    fun loadPeople() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getAllPeople()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to load people"
                    )
                }
                .collect { resource ->
                when (resource) {
                    is Resource.Success -> _uiState.value = _uiState.value.copy(
                        people = resource.data ?: emptyList(),
                        isLoading = false,
                        error = null
                    )
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = resource.message
                    )
                }
            }
        }
    }
}
