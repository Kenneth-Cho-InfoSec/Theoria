/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */

package com.dot.gallery.cloud.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.cloudAlbumId
import com.dot.gallery.cloud.data.dao.CloudAlbumSyncDao
import com.dot.gallery.cloud.data.entity.CloudAlbumSyncEntity
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import com.dot.gallery.core.error.ErrorReporter
import com.dot.gallery.core.error.toAppError
import com.dot.gallery.core.error.userMessage
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class CloudMediaUiState(
    val isLoading: Boolean = false,
    val media: List<CloudMediaEntity> = emptyList(),
    val albums: List<CloudAlbum> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CloudMediaViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val albumSyncDao: CloudAlbumSyncDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudMediaUiState())
    val uiState: StateFlow<CloudMediaUiState> = _uiState.asStateFlow()

    val cachedMedia = repository.getCachedMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudTimelineMedia: StateFlow<List<Media.UriMedia>> = repository.getCachedMedia()
        .map { entities -> entities.filter { !it.trashed }.map { it.toUriMedia() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val cloudAlbums: StateFlow<List<Album>> = _uiState
        .map { state -> state.albums.map { it.toAlbum() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hasConfiguredProviders: Boolean
        get() = repository.hasConfiguredProviders

    private val albumsLoadMutex = Mutex()
    private var albumsLoaded = false
    private var remoteMediaJob: Job? = null
    private var remoteAlbumsJob: Job? = null
    private var albumMediaJob: Job? = null
    private var cloudAlbumMediaJob: Job? = null

    init {
        if (hasConfiguredProviders) {
            loadRemoteAlbums()
        }
    }

    private suspend fun ensureAlbumsLoaded() {
        albumsLoadMutex.withLock {
            if (!albumsLoaded && _uiState.value.albums.isEmpty()) {
                try {
                    val resource = repository.getAllRemoteAlbums().firstOrNull()
                    when (resource) {
                        is Resource.Success -> {
                            _uiState.value = _uiState.value.copy(
                                albums = resource.data ?: emptyList()
                            )
                            albumsLoaded = true
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                error = resource.error?.userMessage() ?: resource.message
                            )
                        }
                        null -> albumsLoaded = true
                    }
                } catch (e: Exception) {
                    val error = e.toAppError("load cloud albums")
                    ErrorReporter.report(error)
                    _uiState.value = _uiState.value.copy(error = error.userMessage())
                }
            }
        }
    }

    fun loadRemoteMedia(page: Int = 0, pageSize: Int = 100) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        remoteMediaJob?.cancel()
        remoteMediaJob = viewModelScope.launch {
            repository.getAllRemoteAssets(page, pageSize)
                .catch { throwable ->
                    val error = throwable.toAppError("load remote media")
                    ErrorReporter.report(error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.userMessage()
                    )
                }
                .collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                media = resource.data ?: emptyList()
                            )
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                media = resource.data ?: _uiState.value.media,
                                error = resource.error?.userMessage() ?: resource.message
                            )
                        }
                    }
                }
            }
        }

    fun loadRemoteAlbums() {
        remoteAlbumsJob?.cancel()
        remoteAlbumsJob = viewModelScope.launch {
            repository.getAllRemoteAlbums()
                .catch { throwable ->
                    val error = throwable.toAppError("load remote albums")
                    ErrorReporter.report(error)
                    _uiState.value = _uiState.value.copy(error = error.userMessage())
                }
                .collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            _uiState.value = _uiState.value.copy(
                                albums = resource.data ?: emptyList()
                            )
                            albumsLoaded = true
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                albums = resource.data ?: _uiState.value.albums,
                                error = resource.error?.userMessage() ?: resource.message
                            )
                        }
                    }
                }
            }
        }

    fun loadAlbumMedia(type: ProviderType, albumId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        _uiState.value = _uiState.value.copy(media = emptyList())
        albumMediaJob?.cancel()
        albumMediaJob = viewModelScope.launch {
            repository.getAlbumMedia(type, albumId)
                .catch { throwable ->
                    val error = throwable.toAppError("load cloud album media")
                    ErrorReporter.report(error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.userMessage()
                    )
                }
                .collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                media = resource.data ?: emptyList()
                            )
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = resource.error?.userMessage() ?: resource.message
                            )
                        }
                    }
                }
            }
        }

    private val _cloudAlbumMedia = MutableStateFlow<List<Media.UriMedia>>(emptyList())
    val cloudAlbumMedia: StateFlow<List<Media.UriMedia>> = _cloudAlbumMedia.asStateFlow()

    fun findCloudAlbumByComputedId(computedId: Long): CloudAlbum? {
        return _uiState.value.albums.find {
            cloudAlbumId(it.providerType, it.serverConfigId, it.remoteId) == computedId
        }
    }

    fun isCloudAlbumId(albumId: Long): Boolean {
        if (albumId >= 0) return false
        return findCloudAlbumByComputedId(albumId) != null
    }

    fun loadCloudAlbumMedia(computedAlbumId: Long) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        _cloudAlbumMedia.value = emptyList()
        cloudAlbumMediaJob?.cancel()
        cloudAlbumMediaJob = viewModelScope.launch {
            ensureAlbumsLoaded()
            val cloudAlbum = findCloudAlbumByComputedId(computedAlbumId)
            if (cloudAlbum == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    media = emptyList(),
                    error = "Cloud album not found"
                )
                return@launch
            }
            repository.getAlbumMedia(cloudAlbum.providerType, cloudAlbum.remoteId)
                .catch { throwable ->
                    val error = throwable.toAppError("load cloud album media")
                    ErrorReporter.report(error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.userMessage()
                    )
                }
                .collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val media = resource.data?.map { it.toUriMedia() } ?: emptyList()
                            _cloudAlbumMedia.value = media
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                        is Resource.Error -> {
                            _cloudAlbumMedia.value = emptyList()
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = resource.error?.userMessage() ?: resource.message
                            )
                        }
                    }
                }
            }
        }

    suspend fun search(query: String): List<CloudMediaEntity> {
        return repository.search(query).getOrDefault(emptyList())
    }

    val albumSyncPreferences: StateFlow<List<CloudAlbumSyncEntity>> = albumSyncDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleAlbumSync(album: CloudAlbum, enabled: Boolean) {
        viewModelScope.launch {
            albumSyncDao.upsert(
                CloudAlbumSyncEntity(
                    albumRemoteId = album.remoteId,
                    providerType = album.providerType,
                    serverConfigId = album.serverConfigId,
                    albumName = album.name,
                    syncEnabled = enabled
                )
            )
        }
    }
}
