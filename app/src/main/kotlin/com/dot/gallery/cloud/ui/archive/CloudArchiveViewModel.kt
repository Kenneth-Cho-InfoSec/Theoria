/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */

package com.dot.gallery.cloud.ui.archive

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudArchiveViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val registry: ProviderRegistry
) : ViewModel() {

    private val _mediaState = MutableStateFlow(MediaState<Media.UriMedia>())
    val mediaState: StateFlow<MediaState<Media.UriMedia>> = _mediaState.asStateFlow()

    init {
        loadArchived()
    }

    fun loadArchived() {
        _mediaState.value = _mediaState.value.copy(isLoading = true, error = "")
        viewModelScope.launch {
            try {
                val cached = repository.getCachedArchivedAsync()
                if (cached.isNotEmpty()) {
                    val media = cached.map { it.toUriMedia() }
                    _mediaState.value = MediaState(
                        media = media,
                        isLoading = false
                    )
                    return@launch
                }
                val providers = registry.getRemoteProviders().filter { it.isAvailable }
                if (providers.isEmpty()) {
                    _mediaState.value = MediaState(
                        isLoading = false,
                        error = "No remote media provider available"
                    )
                    return@launch
                }
                val archived = LinkedHashMap<String, Media.UriMedia>()
                var lastError: String? = null
                for (provider in providers) {
                    provider.getRemoteArchived().collect { resource ->
                        when (resource) {
                            is Resource.Success -> resource.data.orEmpty().forEach { entity ->
                                archived["${entity.serverConfigId}:${entity.remoteId}"] = entity.toUriMedia()
                            }
                            is Resource.Error -> lastError = resource.message
                        }
                    }
                }
                _mediaState.value = MediaState(
                    media = archived.values.toList(),
                    isLoading = false,
                    error = if (archived.isEmpty()) lastError ?: "" else ""
                )
            } catch (e: Exception) {
                _mediaState.value = MediaState(
                    isLoading = false,
                    error = e.message ?: ""
                )
            }
        }
    }

    fun unarchive(remoteId: String) {
        val target = _mediaState.value.media.firstOrNull { media ->
            Uri.decode(media.uri.path.orEmpty().trimStart('/')) == remoteId
        }
        val authority = target?.uri?.authority ?: return
        val providerType = runCatching {
            ProviderType.parse(authority)
        }.getOrNull() ?: return
        val configId = target.uri.getQueryParameter("cfg")?.toLongOrNull() ?: -1L
        val provider = if (configId > 0L) {
            registry.getByConfigId(configId)
        } else {
            registry.get(providerType)
        } as? RemoteMediaProvider ?: return
        viewModelScope.launch {
            provider.toggleArchive(remoteId, false)
                .onSuccess {
                    _mediaState.value = _mediaState.value.copy(
                        media = _mediaState.value.media.filter {
                            val cloudUri = it.uri.toString()
                            !cloudUri.contains(remoteId)
                        }
                    )
                }
                .onFailure { error ->
                    _mediaState.value = _mediaState.value.copy(
                        error = error.message ?: "Unable to unarchive media"
                    )
                }
        }
    }
}
