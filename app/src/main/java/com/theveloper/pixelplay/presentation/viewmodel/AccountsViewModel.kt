package com.theveloper.pixelplay.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theveloper.pixelplay.data.gdrive.GDriveRepository
import com.theveloper.pixelplay.data.jellyfin.JellyfinRepository
import com.theveloper.pixelplay.data.navidrome.NavidromeRepository
import com.theveloper.pixelplay.data.netease.NeteaseRepository
import com.theveloper.pixelplay.data.qqmusic.QqMusicRepository
import com.theveloper.pixelplay.data.repository.MusicRepository
import com.theveloper.pixelplay.data.telegram.TelegramRepository
import com.theveloper.pixelplay.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

enum class ExternalServiceAccount {
    TELEGRAM,
    GOOGLE_DRIVE,
    NETEASE,
    QQ_MUSIC,
    NAVIDROME,
    JELLYFIN
}

data class ExternalAccountUiModel(
    val service: ExternalServiceAccount,
    val title: String,
    val accountLabel: String,
    val syncedContentLabel: String,
    val isLoggingOut: Boolean
)

data class AccountsUiState(
    val connectedAccounts: List<ExternalAccountUiModel> = emptyList(),
    val disconnectedServices: List<ExternalServiceAccount> = emptyList()
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val telegramRepository: TelegramRepository,
    private val musicRepository: MusicRepository,
    private val gDriveRepository: GDriveRepository,
    private val neteaseRepository: NeteaseRepository,
    private val qqMusicRepository: QqMusicRepository,
    private val navidromeRepository: NavidromeRepository,
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {

    private val loggingOutServices = MutableStateFlow<Set<ExternalServiceAccount>>(emptySet())

    private val telegramStateFlow = combine(
        telegramRepository.authorizationState
            .map { it is TdApi.AuthorizationStateReady }
            .distinctUntilChanged(),
        musicRepository.getAllTelegramChannels().map { it.size }
    ) { connected, channelCount ->
        connected to channelCount
    }

    private val gDriveStateFlow = combine(
        gDriveRepository.isLoggedInFlow,
        gDriveRepository.getFolders().map { it.size }
    ) { connected, folderCount ->
        connected to folderCount
    }

    private val neteaseStateFlow = combine(
        neteaseRepository.isLoggedInFlow,
        neteaseRepository.getPlaylists().map { it.size }
    ) { connected, playlistCount ->
        connected to playlistCount
    }

    private val qqMusicStateFlow = combine(
        qqMusicRepository.isLoggedInFlow,
        qqMusicRepository.getPlaylists().map { it.size }
    ) { connected, playlistCount ->
        connected to playlistCount
    }

    private val navidromeStateFlow = combine(
        navidromeRepository.isLoggedInFlow,
        navidromeRepository.getPlaylists().map { it.size }
    ) { connected, playlistCount ->
        connected to playlistCount
    }

    private val jellyfinStateFlow = combine(
        jellyfinRepository.isLoggedInFlow,
        jellyfinRepository.getPlaylists().map { it.size }
    ) { connected, playlistCount ->
        connected to playlistCount
    }

    val uiState: StateFlow<AccountsUiState> = combine(
        combine(
            listOf(
                telegramStateFlow,
                gDriveStateFlow,
                neteaseStateFlow,
                qqMusicStateFlow,
                navidromeStateFlow,
                jellyfinStateFlow
            )
        ) { it.toList() },
        loggingOutServices
    ) { states, activeLogouts ->
        val (telegramConnected, telegramChannelCount) = states[0] as Pair<Boolean, Int>
        val (gDriveConnected, gDriveFolderCount) = states[1] as Pair<Boolean, Int>
        val (neteaseConnected, neteasePlaylistCount) = states[2] as Pair<Boolean, Int>
        val (qqConnected, qqPlaylistCount) = states[3] as Pair<Boolean, Int>
        val (navidromeConnected, navidromePlaylistCount) = states[4] as Pair<Boolean, Int>
        val (jellyfinConnected, jellyfinPlaylistCount) = states[5] as Pair<Boolean, Int>

        val connectedAccounts = buildList {
            if (telegramConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.TELEGRAM,
                        title = serviceTitle(ExternalServiceAccount.TELEGRAM),
                        accountLabel = appContext.getString(R.string.accounts_telegram_active_session),
                        syncedContentLabel = formatCount(telegramChannelCount, R.plurals.accounts_synced_channel_count),
                        isLoggingOut = ExternalServiceAccount.TELEGRAM in activeLogouts
                    )
                )
            }
            if (gDriveConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.GOOGLE_DRIVE,
                        title = serviceTitle(ExternalServiceAccount.GOOGLE_DRIVE),
                        accountLabel = gDriveRepository.userDisplayName
                            ?.takeIf { it.isNotBlank() }
                            ?: gDriveRepository.userEmail
                                ?.takeIf { it.isNotBlank() }
                            ?: appContext.getString(R.string.accounts_google_connected),
                        syncedContentLabel = formatCount(gDriveFolderCount, R.plurals.accounts_synced_folder_count),
                        isLoggingOut = ExternalServiceAccount.GOOGLE_DRIVE in activeLogouts
                    )
                )
            }
            if (neteaseConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.NETEASE,
                        title = serviceTitle(ExternalServiceAccount.NETEASE),
                        accountLabel = neteaseRepository.userNickname
                            ?.takeIf { it.isNotBlank() }
                            ?: appContext.getString(R.string.accounts_netease_connected),
                        syncedContentLabel = formatCount(neteasePlaylistCount, R.plurals.accounts_synced_playlist_count),
                        isLoggingOut = ExternalServiceAccount.NETEASE in activeLogouts
                    )
                )
            }
            if (qqConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.QQ_MUSIC,
                        title = serviceTitle(ExternalServiceAccount.QQ_MUSIC),
                        accountLabel = qqMusicRepository.userNickname
                            ?.takeIf { it.isNotBlank() }
                            ?: appContext.getString(R.string.accounts_qq_music_connected),
                        syncedContentLabel = formatCount(qqPlaylistCount, R.plurals.accounts_synced_playlist_count),
                        isLoggingOut = ExternalServiceAccount.QQ_MUSIC in activeLogouts
                    )
                )
            }
            if (navidromeConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.NAVIDROME,
                        title = serviceTitle(ExternalServiceAccount.NAVIDROME),
                        accountLabel = navidromeRepository.username
                            ?.takeIf { it.isNotBlank() }
                            ?: appContext.getString(R.string.accounts_subsonic_connected),
                        syncedContentLabel = formatCount(navidromePlaylistCount, R.plurals.accounts_synced_playlist_count),
                        isLoggingOut = ExternalServiceAccount.NAVIDROME in activeLogouts
                    )
                )
            }
            if (jellyfinConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.JELLYFIN,
                        title = serviceTitle(ExternalServiceAccount.JELLYFIN),
                        accountLabel = jellyfinRepository.username
                            ?.takeIf { it.isNotBlank() }
                            ?: appContext.getString(R.string.accounts_jellyfin_connected),
                        syncedContentLabel = formatCount(jellyfinPlaylistCount, R.plurals.accounts_synced_playlist_count),
                        isLoggingOut = ExternalServiceAccount.JELLYFIN in activeLogouts
                    )
                )
            }
        }

        val disconnectedServices = buildList {
            if (!telegramConnected) add(ExternalServiceAccount.TELEGRAM)
            if (!gDriveConnected) add(ExternalServiceAccount.GOOGLE_DRIVE)
            if (!neteaseConnected) add(ExternalServiceAccount.NETEASE)
            if (!qqConnected) add(ExternalServiceAccount.QQ_MUSIC)
            if (!navidromeConnected) add(ExternalServiceAccount.NAVIDROME)
            if (!jellyfinConnected) add(ExternalServiceAccount.JELLYFIN)
        }

        AccountsUiState(
            connectedAccounts = connectedAccounts,
            disconnectedServices = disconnectedServices
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun logout(service: ExternalServiceAccount) {
        if (service in loggingOutServices.value) return

        viewModelScope.launch {
            loggingOutServices.update { it + service }
            try {
                runCatching {
                    when (service) {
                        ExternalServiceAccount.TELEGRAM -> {
                            telegramRepository.logout()
                            telegramRepository.clearMemoryCache()
                            musicRepository.clearTelegramData()
                        }
                        ExternalServiceAccount.GOOGLE_DRIVE -> gDriveRepository.logout()
                        ExternalServiceAccount.NETEASE -> neteaseRepository.logout()
                        ExternalServiceAccount.QQ_MUSIC -> qqMusicRepository.logout()
                        ExternalServiceAccount.NAVIDROME -> navidromeRepository.logout()
                        ExternalServiceAccount.JELLYFIN -> jellyfinRepository.logout()
                    }
                }
            } finally {
                loggingOutServices.update { it - service }
            }
        }
    }

    private fun serviceTitle(service: ExternalServiceAccount): String {
        val resId = when (service) {
            ExternalServiceAccount.TELEGRAM -> R.string.accounts_service_telegram
            ExternalServiceAccount.GOOGLE_DRIVE -> R.string.accounts_service_google_drive
            ExternalServiceAccount.NETEASE -> R.string.accounts_service_netease
            ExternalServiceAccount.QQ_MUSIC -> R.string.accounts_service_qq_music
            ExternalServiceAccount.NAVIDROME -> R.string.accounts_service_subsonic
            ExternalServiceAccount.JELLYFIN -> R.string.accounts_service_jellyfin
        }
        return appContext.getString(resId)
    }

    private fun formatCount(count: Int, pluralsRes: Int): String {
        return appContext.resources.getQuantityString(pluralsRes, count, count)
    }
}
