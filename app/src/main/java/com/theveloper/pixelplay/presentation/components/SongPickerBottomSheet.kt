package com.theveloper.pixelplay.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import coil.size.Size
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import kotlinx.coroutines.flow.map
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongPickerBottomSheet(
    initiallySelectedSongIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedSongIds = remember {
        mutableStateMapOf<String, Boolean>().apply {
            initiallySelectedSongIds.forEach { put(it, true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxSize()
    ) {
        SongPickerContent(
            selectedSongIds = selectedSongIds,
            onConfirm = onConfirm,
            playerViewModel = playerViewModel
        )
    }
}

@Composable
fun SongPickerContent(
    selectedSongIds: MutableMap<String, Boolean>,
    onConfirm: (Set<String>) -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Add Songs",
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = GoogleSansRounded
                    )
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    modifier = Modifier.padding(bottom = 18.dp, end = 8.dp),
                    shape = CircleShape,
                    onClick = { onConfirm(selectedSongIds.filterValues { it }.keys) },
                    icon = { Icon(Icons.Rounded.Check, "Add songs") },
                    text = { Text("Add") },
                )
            }
        ) { innerPadding ->
            SongPickerSelectionPane(
                selectedSongIds = selectedSongIds,
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
                playerViewModel = playerViewModel
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(30.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )
    }
}

@Composable
fun SongPickerSelectionPane(
    selectedSongIds: MutableMap<String, Boolean>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 100.dp, top = 20.dp),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val pagedSongs = playerViewModel.playlistPickerSongs.collectAsLazyPagingItems()
    val searchResultsInitialValue: List<Song>? = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else null
    }
    val searchResults by remember(searchQuery, playerViewModel) {
        playerViewModel.searchSongs(searchQuery)
            .map<List<Song>, List<Song>?> { it }
    }.collectAsStateWithLifecycle(initialValue = searchResultsInitialValue)

    val animatedAlbumCornerRadius = 60.dp
    val albumShape = remember(animatedAlbumCornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = animatedAlbumCornerRadius,
            smoothnessAsPercentTR = 60,
            cornerRadiusTR = animatedAlbumCornerRadius,
            smoothnessAsPercentBR = 60,
            cornerRadiusBL = animatedAlbumCornerRadius,
            smoothnessAsPercentBL = 60,
            cornerRadiusBR = animatedAlbumCornerRadius,
            smoothnessAsPercentTL = 60
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        SongPickerSearchField(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it }
        )

        if (searchQuery.isBlank()) {
            SongPickerPagingList(
                pagedSongs = pagedSongs,
                selectedSongIds = selectedSongIds,
                albumShape = albumShape,
                modifier = Modifier.weight(1f),
                contentPadding = contentPadding
            )
        } else {
            SongPickerList(
                filteredSongs = searchResults ?: emptyList(),
                isLoading = searchResults == null,
                selectedSongIds = selectedSongIds,
                albumShape = albumShape,
                modifier = Modifier.weight(1f),
                contentPadding = contentPadding
            )
        }
    }
}

@Composable
private fun SongPickerSearchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedTrailingIconColor = Color.Transparent,
            focusedSupportingTextColor = Color.Transparent,
        ),
        onValueChange = onSearchQueryChange,
        label = { Text("Search for songs...") },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = CircleShape,
        singleLine = true,
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Filled.Clear, null)
                }
            }
        }
    )
}

@Composable
fun SongPickerPagingList(
    pagedSongs: LazyPagingItems<Song>,
    selectedSongIds: MutableMap<String, Boolean>,
    albumShape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 100.dp, top = 20.dp)
) {
    when {
        pagedSongs.loadState.refresh is LoadState.Loading && pagedSongs.itemCount == 0 -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        pagedSongs.loadState.refresh is LoadState.Error && pagedSongs.itemCount == 0 -> {
            val error = (pagedSongs.loadState.refresh as LoadState.Error).error
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = error.localizedMessage ?: "Failed to load songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { pagedSongs.retry() }) {
                        Text("Retry")
                    }
                }
            }
        }

        else -> {
            val listState = rememberLazyListState()
            Box(
                modifier = modifier
                    .padding(horizontal = 14.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = contentPadding.calculateBottomPadding(),
                        top = contentPadding.calculateTopPadding(),
                        start = contentPadding.calculateLeftPadding(LayoutDirection.Ltr),
                        end = if (listState.canScrollForward || listState.canScrollBackward) 12.dp else 0.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = pagedSongs.itemCount,
                        key = { index -> pagedSongs.peek(index)?.id ?: "song_picker_paged_$index" },
                        contentType = pagedSongs.itemContentType { "song_picker_song" }
                    ) { index ->
                        val song = pagedSongs[index]
                        if (song != null) {
                            SongPickerRow(
                                song = song,
                                selectedSongIds = selectedSongIds,
                                albumShape = albumShape
                            )
                        } else {
                            SongPickerPlaceholderRow()
                        }
                    }

                    if (pagedSongs.loadState.append is LoadState.Loading) {
                        item(key = "song_picker_append_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    if (pagedSongs.loadState.append is LoadState.Error) {
                        item(key = "song_picker_append_error") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(onClick = { pagedSongs.retry() }) {
                                    Text("Load more")
                                }
                            }
                        }
                    }
                }

                ExpressiveScrollBar(
                    listState = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(
                            bottom = contentPadding.calculateBottomPadding(),
                            top = contentPadding.calculateTopPadding() + 10.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun SongPickerRow(
    song: Song,
    selectedSongIds: MutableMap<String, Boolean>,
    albumShape: androidx.compose.ui.graphics.Shape
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .clickable {
                val currentSelection = selectedSongIds[song.id] ?: false
                selectedSongIds[song.id] = !currentSelection
            }
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selectedSongIds[song.id] ?: false,
            onCheckedChange = { isChecked ->
                selectedSongIds[song.id] = isChecked
            }
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                )
        ) {
            SmartImage(
                model = song.albumArtUriString,
                contentDescription = song.title,
                shape = albumShape,
                targetSize = Size(168, 168),
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                song.displayArtist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SongPickerPlaceholderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
        )
        Spacer(Modifier.width(18.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                )
        )
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .width(132.dp)
                    .height(14.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun SongPickerList(
    filteredSongs: List<Song>,
    isLoading: Boolean,
    selectedSongIds: MutableMap<String, Boolean>,
    albumShape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 100.dp, top = 20.dp)
) {
    if (isLoading) {
        Box(
            modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        val listState = rememberLazyListState()
        Box(
            modifier = modifier
                .padding(horizontal = 14.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = contentPadding.calculateBottomPadding(),
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateLeftPadding(LayoutDirection.Ltr),
                    end = if (listState.canScrollForward || listState.canScrollBackward) 12.dp else 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSongs, key = { it.id }) { song ->
                    SongPickerRow(
                        song = song,
                        selectedSongIds = selectedSongIds,
                        albumShape = albumShape
                    )
                }
            }

            ExpressiveScrollBar(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(
                        bottom = contentPadding.calculateBottomPadding(),
                        top = contentPadding.calculateTopPadding() + 10.dp
                    )
            )
        }
    }
}
