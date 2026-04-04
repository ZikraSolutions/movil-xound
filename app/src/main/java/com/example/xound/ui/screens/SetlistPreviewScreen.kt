package com.example.xound.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.xound.data.model.EventResponse
import com.example.xound.data.model.SetlistSongResponse
import com.example.xound.data.network.CoverArtService
import com.example.xound.ui.theme.LocalXoundColors
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow
import com.example.xound.ui.viewmodel.EventViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val previewColors = listOf(
    Color(0xFF7B2D8E),
    Color(0xFF2D5F8E),
    Color(0xFF8E2D2D),
    Color(0xFF2D8E5F),
    Color(0xFF8E6B2D),
    Color(0xFF4A2D8E)
)

@Composable
fun SetlistPreviewScreen(
    event: EventResponse,
    onBack: () -> Unit = {},
    onStartLive: () -> Unit = {},
    eventViewModel: EventViewModel
) {
    val colors = LocalXoundColors.current
    val setlistSongs by eventViewModel.setlistSongs.collectAsState()
    val setlistLoading by eventViewModel.setlistLoading.collectAsState()

    LaunchedEffect(event.id) {
        eventViewModel.fetchSetlist(event.id)
    }

    val formattedDate = formatPreviewDate(event.eventDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .padding(top = 48.dp)
    ) {
        // Header
        Column(
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
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
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = XoundYellow
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!event.venue.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = event.venue,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "  •  ",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
                Text(
                    text = formattedDate,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${setlistSongs.size} canciones en el setlist",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Song list
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
        } else if (setlistSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No hay canciones en el setlist",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(setlistSongs, key = { _, item -> item.id }) { index, setlistItem ->
                    PreviewSongCard(
                        setlistItem = setlistItem,
                        index = index + 1,
                        colorIndex = (setlistItem.songId % previewColors.size).toInt()
                    )
                }
            }
        }

        // Start live mode button
        if (setlistSongs.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.screenBackground,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = onStartLive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = XoundYellow),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = XoundNavy,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Iniciar Modo en Vivo",
                            color = XoundNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSongCard(
    setlistItem: SetlistSongResponse,
    index: Int,
    colorIndex: Int
) {
    val colors = LocalXoundColors.current
    val song = setlistItem.song
    var coverUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(setlistItem.songId) {
        coverUrl = CoverArtService.getCoverUrl(song?.artist, song?.title ?: "")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position number
            Text(
                text = "$index",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textHint,
                modifier = Modifier.width(24.dp)
            )

            // Cover art
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(previewColors[colorIndex]),
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

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song?.title ?: "Canción",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!song?.artist.isNullOrBlank()) {
                        Text(
                            text = song!!.artist!!,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Song details badges
            Column(horizontalAlignment = Alignment.End) {
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${song.bpm} BPM",
                        fontSize = 10.sp,
                        color = colors.textHint
                    )
                }
            }
        }
    }
}

private fun formatPreviewDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "Sin fecha"
    return try {
        val date = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "MX"))
        date.format(formatter)
    } catch (_: Exception) {
        dateStr
    }
}
