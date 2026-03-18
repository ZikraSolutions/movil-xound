package com.example.xound.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.xound.data.network.CoverArtService
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xound.data.model.SongResponse
import com.example.xound.ui.theme.LocalXoundColors
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow
import com.example.xound.ui.viewmodel.SongViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

private val thumbnailColors = listOf(
    Color(0xFF7B2D8E),
    Color(0xFF2D5F8E),
    Color(0xFF8E2D2D),
    Color(0xFF2D8E5F),
    Color(0xFF8E6B2D),
    Color(0xFF4A2D8E)
)

@Composable
fun LibraryScreen(
    onBack: () -> Unit = {},
    onAddSong: () -> Unit = {},
    onEditSong: (SongResponse) -> Unit = {},
    onViewSong: (SongResponse) -> Unit = {},
    showAddButton: Boolean = true,
    songViewModel: SongViewModel = viewModel()
) {
    val colors = LocalXoundColors.current
    val songs by songViewModel.songs.collectAsState()
    val favorites by songViewModel.favorites.collectAsState()
    val isLoading by songViewModel.isLoading.collectAsState()
    val deleteError by songViewModel.deleteError.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todas") }
    val filters = listOf("Todas", "Favoritos", "Recientes", "Tonalidad")

    // Delete confirmation dialog
    var songToDelete by remember { mutableStateOf<SongResponse?>(null) }

    LaunchedEffect(Unit) {
        songViewModel.fetchSongs()
        songViewModel.fetchFavorites()
    }

    val filteredSongs = remember(songs, favorites, selectedFilter) {
        when (selectedFilter) {
            "Favoritos" -> songs.filter { favorites.contains(it.id) }
            "Recientes" -> songs.sortedByDescending { it.createdAt }
            "Tonalidad" -> songs.sortedBy { it.tone ?: "ZZZ" }
            else -> songs
        }
    }

    // Delete confirmation alert
    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            containerColor = colors.dialogBackground,
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Eliminar cancion",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    "¿Estás seguro de que quieres eliminar la cancion \"${songToDelete?.title}\"?",
                    color = colors.textPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        songToDelete?.let { songViewModel.deleteSong(it.id) }
                        songToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Elimar", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { songToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete error dialog
    if (deleteError != null) {
        AlertDialog(
            onDismissRequest = { songViewModel.clearDeleteError() },
            containerColor = colors.dialogBackground,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE5A100),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "No se pudo eliminar",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    deleteError ?: "",
                    color = colors.textPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { songViewModel.clearDeleteError() },
                    colors = ButtonDefaults.buttonColors(containerColor = XoundNavy)
                ) {
                    Text("Entendido", color = Color.White)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .padding(top = 48.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.offset(x = (-12).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = XoundNavy
                    )
                }
                Text(
                    text = "Biblioteca",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = XoundYellow
                )
                Text(
                    text = "${songs.size} canciones",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            if (showAddButton) {
                IconButton(
                    onClick = onAddSong,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(40.dp)
                        .background(XoundYellow, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar canción",
                        tint = XoundNavy,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                songViewModel.searchSongs(it)
            },
            placeholder = { Text("Search", color = colors.textHint) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.textHint,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colors.searchBorder,
                focusedBorderColor = XoundNavy,
                unfocusedContainerColor = colors.searchBackground,
                focusedContainerColor = colors.searchBackground,
                unfocusedTextColor = colors.textPrimary,
                focusedTextColor = colors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = XoundNavy,
                        selectedLabelColor = Color.White,
                        containerColor = colors.chipUnselectedBg,
                        labelColor = colors.chipUnselectedText
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = XoundNavy,
                        selectedBorderColor = XoundNavy,
                        enabled = true,
                        selected = isSelected
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Song list
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = XoundNavy,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSongs, key = { it.id }) { song ->
                    SwipeableSongCard(
                        song = song,
                        colorIndex = (song.id % thumbnailColors.size).toInt(),
                        isFavorite = favorites.contains(song.id),
                        onToggleFavorite = { songViewModel.toggleFavorite(song.id) },
                        onDelete = { songToDelete = song },
                        onEdit = { onEditSong(song) },
                        onClick = { onViewSong(song) }
                    )
                }

                if (filteredSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedFilter == "Favoritos") "Sin favoritos aún"
                                else "No hay canciones",
                                color = colors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableSongCard(
    song: SongResponse,
    colorIndex: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit = {}
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var cardWidth by remember { mutableFloatStateOf(1f) }
    val threshold = 0.35f
    var actionTriggered by remember { mutableStateOf(false) }

    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = if (offsetX == 0f) 300 else 0),
        label = "swipeOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        // Background
        val bgColor = when {
            animatedOffset < -20f -> Color(0xFFE53935)
            animatedOffset > 20f -> Color(0xFF2196F3)
            else -> Color.Transparent
        }
        val bgIcon = when {
            animatedOffset < -20f -> Icons.Default.Delete
            animatedOffset > 20f -> Icons.Default.Edit
            else -> null
        }
        val bgAlignment = when {
            animatedOffset < 0f -> Alignment.CenterEnd
            animatedOffset > 0f -> Alignment.CenterStart
            else -> Alignment.Center
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(bgColor)
                .padding(horizontal = 24.dp),
            contentAlignment = bgAlignment
        ) {
            if (bgIcon != null) {
                Icon(
                    imageVector = bgIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Foreground card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    cardWidth = size.width.toFloat()
                    detectHorizontalDragGestures(
                        onDragStart = {
                            actionTriggered = false
                        },
                        onDragEnd = {
                            val ratio = abs(offsetX) / cardWidth
                            if (ratio >= threshold && !actionTriggered) {
                                actionTriggered = true
                                if (offsetX < 0) onDelete() else onEdit()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = offsetX + dragAmount
                            // Limit to half the card width
                            offsetX = newOffset.coerceIn(-cardWidth * threshold, cardWidth * threshold)
                        }
                    )
                }
        ) {
            SongCard(
                song = song,
                colorIndex = colorIndex,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun SongCard(song: SongResponse, colorIndex: Int, isFavorite: Boolean = false, onToggleFavorite: () -> Unit = {}, onClick: () -> Unit = {}) {
    val colors = LocalXoundColors.current
    var coverUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(song.id) {
        coverUrl = CoverArtService.getCoverUrl(song.artist, song.title)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(thumbnailColors[colorIndex]),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!song.tone.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .background(XoundYellow, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = song.tone,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = XoundNavy
                            )
                        }
                    }
                    if (!song.artist.isNullOrBlank()) {
                        Text(
                            text = song.artist,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Favorite button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                    tint = if (isFavorite) Color(0xFFE53935) else Color(0xFFBBBBBB),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
