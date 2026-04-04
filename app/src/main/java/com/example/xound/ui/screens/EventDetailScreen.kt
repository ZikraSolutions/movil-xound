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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import com.example.xound.data.model.EventResponse
import com.example.xound.data.network.CoverArtService
import com.example.xound.data.model.SetlistSongResponse
import com.example.xound.data.model.SongResponse
import com.example.xound.ui.theme.LocalXoundColors
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow
import com.example.xound.ui.viewmodel.EventViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val instrumentColors = listOf(
    Color(0xFF7B2D8E),
    Color(0xFF2D5F8E),
    Color(0xFF8E2D2D),
    Color(0xFF2D8E5F),
    Color(0xFF8E6B2D),
    Color(0xFF4A2D8E)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    event: EventResponse,
    onBack: () -> Unit = {},
    onAddSongToSetlist: () -> Unit = {},
    onViewSong: (SongResponse) -> Unit = {},
    isAdmin: Boolean = true,
    eventViewModel: EventViewModel
) {
    val colors = LocalXoundColors.current
    val setlistSongs by eventViewModel.setlistSongs.collectAsState()
    val setlistLoading by eventViewModel.setlistLoading.collectAsState()

    var songToRemove by remember { mutableStateOf<SetlistSongResponse?>(null) }
    val publishDone by eventViewModel.publishDone.collectAsState()

    LaunchedEffect(event.id) {
        eventViewModel.fetchSetlist(event.id)
    }

    LaunchedEffect(publishDone) {
        if (publishDone) {
            eventViewModel.resetPublishDone()
            onBack()
        }
    }

    val formattedDate = formatDetailDate(event.eventDate)

    // Remove confirmation dialog
    if (songToRemove != null) {
        AlertDialog(
            onDismissRequest = { songToRemove = null },
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
                    text = "Quitar del setlist",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    "¿Quitar \"${songToRemove?.song?.title ?: "esta canción"}\" del setlist?",
                    color = colors.textPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        songToRemove?.let {
                            eventViewModel.removeSongFromSetlist(event.id, it.songId)
                        }
                        songToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Quitar", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { songToRemove = null }) {
                    Text("Cancelar", color = colors.textPrimary)
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-12).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = colors.textPrimary
                        )
                    }

                    Text(
                        text = event.title,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = XoundYellow
                    )
                }

                // Add song button (admin only)
                if (isAdmin) {
                    IconButton(
                        onClick = onAddSongToSetlist,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(40.dp)
                            .background(XoundYellow, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar canción al setlist",
                            tint = XoundNavy,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Venue + Date
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!event.venue.isNullOrBlank()) {
                    Text(
                        text = event.venue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = XoundYellow
                    )
                    Text(
                        text = "  ",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                }
                Text(
                    text = formattedDate,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${setlistSongs.size} canciones",
                fontSize = 13.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Publicar + Compartir buttons (admin only)
            if (isAdmin) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { eventViewModel.togglePublishFromDetail(event.id) },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = XoundYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (event.published) "Despublicar" else "Publicar",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (event.shareCode != null) {
                        OutlinedButton(
                            onClick = { },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = XoundYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Compartir",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Setlist
        if (setlistLoading) {
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
                items(setlistSongs, key = { it.id }) { setlistItem ->
                    SwipeableSetlistCard(
                        setlistItem = setlistItem,
                        colorIndex = (setlistItem.songId % instrumentColors.size).toInt(),
                        onRemove = { songToRemove = setlistItem },
                        onClick = { setlistItem.song?.let { onViewSong(it) } },
                        swipeEnabled = isAdmin
                    )
                }

                if (setlistSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay canciones en el setlist",
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
private fun SwipeableSetlistCard(
    setlistItem: SetlistSongResponse,
    colorIndex: Int,
    onRemove: () -> Unit,
    onClick: () -> Unit = {},
    swipeEnabled: Boolean = true
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
        // Background - only left swipe (delete)
        if (animatedOffset < -20f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
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
                .pointerInput(swipeEnabled) {
                    if (!swipeEnabled) return@pointerInput
                    cardWidth = size.width.toFloat()
                    detectHorizontalDragGestures(
                        onDragStart = {
                            actionTriggered = false
                        },
                        onDragEnd = {
                            val ratio = abs(offsetX) / cardWidth
                            if (ratio >= threshold && offsetX < 0 && !actionTriggered) {
                                actionTriggered = true
                                onRemove()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = offsetX + dragAmount
                            offsetX = newOffset.coerceIn(-cardWidth * threshold, 0f)
                        }
                    )
                }
        ) {
            SetlistSongCard(
                setlistItem = setlistItem,
                colorIndex = colorIndex,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun SetlistSongCard(setlistItem: SetlistSongResponse, colorIndex: Int, onClick: () -> Unit = {}) {
    val colors = LocalXoundColors.current
    val song = setlistItem.song
    var coverUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(setlistItem.songId) {
        coverUrl = CoverArtService.getCoverUrl(song?.artist, song?.title ?: "")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.navyCardDark),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover art / fallback icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(instrumentColors[colorIndex]),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = song?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song?.title ?: "Canción ${setlistItem.songId}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!song?.tone.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .background(XoundYellow, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = song!!.tone!!,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = XoundNavy
                            )
                        }
                    }
                    if (song?.bpm != null) {
                        Text(
                            text = "${song.bpm} BPM",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// Dialog to pick songs to add to setlist
@Composable
fun AddSongToSetlistDialog(
    songs: List<SongResponse>,
    setlistSongIds: Set<Long>,
    onAdd: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalXoundColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogBackground,
        title = {
            Text(
                text = "Agregar al setlist",
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            val available = songs.filter { it.id !in setlistSongIds }
            if (available.isEmpty()) {
                Text("No hay canciones disponibles para agregar", color = colors.textPrimary)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(available, key = { it.id }) { song ->
                        Card(
                            onClick = { onAdd(song.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = XoundNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!song.artist.isNullOrBlank()) {
                                        Text(
                                            text = song.artist,
                                            fontSize = 12.sp,
                                            color = colors.textSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (!song.tone.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .background(XoundYellow, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = song.tone,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = XoundNavy
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = XoundYellow)
            }
        }
    )
}

private fun formatDetailDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    return try {
        val date = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm 'h'", Locale("es", "MX"))
        date.format(formatter).uppercase()
    } catch (_: Exception) {
        dateStr
    }
}
