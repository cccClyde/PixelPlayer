package com.theveloper.pixelplay.presentation.netease.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.database.NeteasePlaylistEntity
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.netease.NeteaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NeteaseDashboardViewModel @Inject constructor(
    private val repository: NeteaseRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val playlists: StateFlow<List<NeteasePlaylistEntity>> = repository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _selectedPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    val selectedPlaylistSongs: StateFlow<List<Song>> = _selectedPlaylistSongs.asStateFlow()

    val userNickname: String? get() = repository.userNickname
    val userAvatar: String? get() = repository.userAvatar
    
    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedInFlow

    init {
        // Auto-sync playlists when the dashboard opens
        syncPlaylists()
    }

    private val resources get() = appContext.resources

    private fun syncFailedMessage(reason: String?): String {
        return if (reason.isNullOrBlank()) {
            resources.getString(R.string.sync_failed_generic)
        } else {
            resources.getString(R.string.sync_failed_with_reason, reason)
        }
    }

    private fun playlistCountText(count: Int): String =
        resources.getQuantityString(R.plurals.playlist_count, count, count)

    private fun songCountText(count: Int): String =
        resources.getQuantityString(R.plurals.song_count, count, count)

    fun syncAllPlaylistsAndSongs() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = resources.getString(R.string.sync_message_all_playlists_and_songs)
            val result = repository.syncAllPlaylistsAndSongs()
            result.fold(
                onSuccess = { summary ->
                    _syncMessage.value = if (summary.failedPlaylistCount == 0) {
                        resources.getString(
                            R.string.sync_summary_playlists_songs,
                            playlistCountText(summary.playlistCount),
                            songCountText(summary.syncedSongCount)
                        )
                    } else {
                        resources.getString(
                            R.string.sync_summary_playlists_songs_failed,
                            playlistCountText(summary.playlistCount),
                            songCountText(summary.syncedSongCount),
                            summary.failedPlaylistCount
                        )
                    }
                },
                onFailure = { _syncMessage.value = syncFailedMessage(it.message) }
            )
            _isSyncing.value = false
        }
    }

    fun syncPlaylists() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = resources.getString(R.string.sync_message_playlists)
            val result = repository.syncUserPlaylists()
            result.fold(
                onSuccess = {
                    _syncMessage.value = resources.getString(
                        R.string.sync_summary_playlists,
                        playlistCountText(it.size)
                    )
                },
                onFailure = { _syncMessage.value = syncFailedMessage(it.message) }
            )
            _isSyncing.value = false
        }
    }

    fun syncPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = resources.getString(R.string.sync_message_songs)
            val result = repository.syncPlaylistSongs(playlistId)
            result.fold(
                onSuccess = {
                    _syncMessage.value = resources.getString(
                        R.string.sync_summary_songs,
                        songCountText(it)
                    )
                },
                onFailure = { _syncMessage.value = syncFailedMessage(it.message) }
            )
            _isSyncing.value = false
        }
    }

    fun loadPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            repository.getPlaylistSongs(playlistId).collect { songs ->
                _selectedPlaylistSongs.value = songs
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
