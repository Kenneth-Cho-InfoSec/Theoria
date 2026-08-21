/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */

package com.dot.gallery.cloud.ui.people

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.util.mapMediaToItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonDetailUiState(
    val person: PersonInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CloudRepository,
    private val registry: ProviderRegistry
) : ViewModel() {

    private val personId: String = savedStateHandle["personId"] ?: ""

    private val _uiState = MutableStateFlow(PersonDetailUiState())
    val uiState: StateFlow<PersonDetailUiState> = _uiState.asStateFlow()

    private val _mediaState = MutableStateFlow(MediaState<Media.UriMedia>())
    val mediaState: StateFlow<MediaState<Media.UriMedia>> = _mediaState.asStateFlow()

    /** Raw media of this person, used to pick a new cover face. */
    private val _personMedia = MutableStateFlow<List<Media.UriMedia>>(emptyList())
    val personMedia: StateFlow<List<Media.UriMedia>> = _personMedia.asStateFlow()

    // On-device (ML) person management was removed. These stubs keep the UI compiling;
    // they are never surfaced because isLocalPerson is always false.
    val blurProgress: StateFlow<Pair<Int, Int>?> = MutableStateFlow(null)
    val mergeCandidates: StateFlow<List<PersonInfo>> = MutableStateFlow(emptyList())
    val isLocalPerson: Boolean = false

    fun blurEverywhere(useMosaic: Boolean) = Unit

    fun mergeInto(targetPersonId: String) = Unit

    fun setCover(media: Media.UriMedia) {
        val id = _uiState.value.person?.id ?: return
        val uri = media.getUri().toString()
        _uiState.value = _uiState.value.copy(
            person = _uiState.value.person?.copy(thumbnailUrl = uri)
        )
    }

    fun hidePerson(onDone: () -> Unit) {
        onDone()
    }

    init {
        loadPerson()
    }

    private fun loadPerson() {
        if (personId.isBlank()) return
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            repository.getAllPeople().collect { resource ->
                if (resource is Resource.Success) {
                    val person = resource.data?.find { it.id == personId }
                    _uiState.value = _uiState.value.copy(person = person)
                }
            }
        }

        // Route to the provider that actually owns this person, instead of assuming the
        // first available people provider.
        viewModelScope.launch {
            val allPeople = repository.getAllPeople()
                .mapNotNull { (it as? Resource.Success)?.data }
                .firstOrNull()
            if (allPeople == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unable to load people"
                )
                return@launch
            }
            val ownerType = allPeople.find { it.id == personId }?.providerType
            val providers = registry.getPeopleProviders().filter { it.isAvailable }
            if (providers.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No people provider is available"
                )
                return@launch
            }
            val provider = providers.firstOrNull { it.providerType == ownerType }
                ?: providers.first()

            provider.getPersonMedia(personId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val mediaList = resource.data?.filterIsInstance<Media.UriMedia>() ?: emptyList()
                        _personMedia.value = mediaList
                        val mapped = mapMediaToItem(
                            data = mediaList,
                            error = "",
                            albumId = -1L,
                            withMonthHeader = false,
                            groupSimilarMedia = false,
                            defaultDateFormat = Constants.DEFAULT_DATE_FORMAT,
                            extendedDateFormat = Constants.EXTENDED_DATE_FORMAT,
                            weeklyDateFormat = Constants.WEEKLY_DATE_FORMAT
                        )
                        _mediaState.value = mapped
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = resource.message
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        if (personId.isBlank()) return
        val providers = registry.getPeopleProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        val type = _uiState.value.person?.providerType ?: providers.first().providerType
        viewModelScope.launch {
            repository.updatePersonName(type, personId, name).onSuccess {
                _uiState.value = _uiState.value.copy(
                    person = _uiState.value.person?.copy(name = name)
                )
            }
        }
    }

    fun updateBirthDate(birthDate: String) {
        if (personId.isBlank()) return
        val providers = registry.getPeopleProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        val type = _uiState.value.person?.providerType ?: providers.first().providerType
        viewModelScope.launch {
            repository.updatePersonBirthDate(type, personId, birthDate)
        }
    }
}
