package com.example.xound.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import com.example.xound.data.model.EventResponse
import com.example.xound.data.model.SongResponse
import com.example.xound.data.network.LiveSyncManager
import com.example.xound.ui.theme.LocalXoundColors
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow
import com.example.xound.ui.viewmodel.EventViewModel
import kotlinx.coroutines.delay

private val LiveChordColor = Color(0xFFE5A100)

@Composable
fun LiveModeScreen(
    event: EventResponse,
    onBack: () -> Unit = {},
    eventViewModel: EventViewModel,
    isAdmin: Boolean = false,
    bandId: Long = -1L
) {
    val colors = LocalXoundColors.current
    val setlistSongs by eventViewModel.setlistSongs.collectAsState()
    val setlistLoading by eventViewModel.setlistLoading.collectAsState()

    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var highlightLine by remember { mutableIntStateOf(-1) }
    var seekToLine by remember { mutableIntStateOf(-1) }

    // Comentarios en vivo
    var showCommentDialog by remember { mutableStateOf(false) }
    var commentInput by remember { mutableStateOf("") }
    var visibleComment by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    LaunchedEffect(event.id) {
        eventViewModel.fetchSetlist(event.id)
    }

    // Declarar siempre fuera de cualquier if — regla de Compose
    val remoteLiveEvent by LiveSyncManager.liveEvent.collectAsState()

    // LIVE_START: se envía cuando bandId esté disponible (no en Unit para evitar bandId=-1)
    LaunchedEffect(bandId) {
        if (isAdmin && bandId > 0) {
            LiveSyncManager.sendStart(bandId, event.id)
        }
    }

    // LIVE_END al salir: captura el bandId actual en el momento del dispose
    val latestBandId by rememberUpdatedState(bandId)
    DisposableEffect(Unit) {
        onDispose {
            if (isAdmin && latestBandId > 0) {
                LiveSyncManager.sendEnd(latestBandId)
            }
        }
    }

    // Admin: enviar LIVE_STATE en cada cambio de canción/línea/play
    LaunchedEffect(currentIndex, highlightLine, isPlaying) {
        if (!isAdmin || bandId <= 0 || setlistSongs.isEmpty()) return@LaunchedEffect
        LiveSyncManager.sendState(
            bandId = bandId,
            eventId = event.id,
            songIndex = currentIndex,
            lineIndex = highlightLine,
            isPlaying = isPlaying
        )
    }

    // Miembro: aplicar estado recibido del admin
    LaunchedEffect(remoteLiveEvent) {
        if (isAdmin) return@LaunchedEffect
        val remoteEvent = remoteLiveEvent ?: return@LaunchedEffect
        when (remoteEvent.type) {
            "LIVE_STATE" -> {
                if (remoteEvent.songIndex != currentIndex) currentIndex = remoteEvent.songIndex
                if (remoteEvent.lineIndex != highlightLine) seekToLine = remoteEvent.lineIndex
                if (remoteEvent.isPlaying != isPlaying) isPlaying = remoteEvent.isPlaying
            }
        }
    }

    // Mostrar comentario cuando llega (admin y músicos)
    LaunchedEffect(remoteLiveEvent) {
        val remoteEvent = remoteLiveEvent ?: return@LaunchedEffect
        if (remoteEvent.type == "LIVE_COMMENT" && !remoteEvent.comment.isNullOrBlank()) {
            visibleComment = remoteEvent.comment
            delay(5000)
            visibleComment = null
        }
    }

    // Get current song
    val currentSetlistItem = if (setlistSongs.isNotEmpty() && currentIndex in setlistSongs.indices) {
        setlistSongs[currentIndex]
    } else null
    val currentSong = currentSetlistItem?.song

    // Calculate ms per lyric line based on BPM and time signature
    fun calcMsPerLine(song: SongResponse): Long {
        val bpm = song.bpm ?: return 2000L
        if (bpm <= 0) return 2000L
        val beatsPerBar = try {
            val parts = (song.timeSignature ?: "4/4").split("/")
            parts[0].trim().toInt()
        } catch (_: Exception) { 4 }
        val secondsPerBar = (beatsPerBar.toDouble() * 60.0) / bpm.toDouble()
        val barsPerLine = 2
        return (secondsPerBar * barsPerLine * 1000).toLong().coerceIn(800L, 6000L)
    }

    // Count displayable lyric lines
    fun countLyricLines(text: String): Int {
        val lines = text.split("\n")
        var count = 0
        var i = 0
        while (i < lines.size) {
            val trimmed = lines[i].trim()
            if (isLiveSectionHeader(trimmed)) { i++; continue }
            if (isLiveChordLine(trimmed)) {
                val next = if (i + 1 < lines.size) lines[i + 1].trim() else ""
                if (next.isNotBlank() && !isLiveChordLine(next) && !isLiveSectionHeader(next)) {
                    count++; i += 2
                } else { i++ }
            } else if (trimmed.isBlank()) { i++ }
            else { count++; i++ }
        }
        return count
    }

    // Auto-scroll + highlight - uses seekToLine to know where to resume from
    // Key only on currentSong and seekToLine so pause/resume doesn't restart
    LaunchedEffect(currentSong, seekToLine) {
        if (currentSong != null) {
            val lyricsText = currentSong.lyrics ?: currentSong.content ?: ""
            if (lyricsText.isNotBlank()) {
                val msPerLine = calcMsPerLine(currentSong)
                val totalLyricLines = countLyricLines(lyricsText)
                val scrollMax = scrollState.maxValue

                // Start from the current highlight position (resume)
                val startLine = if (seekToLine >= 0) seekToLine else (highlightLine + 1).coerceAtLeast(0)

                // Scroll to proportional position when seeking
                if (seekToLine >= 0 && scrollMax > 0 && totalLyricLines > 0) {
                    val targetScroll = (scrollMax.toFloat() * seekToLine / totalLyricLines).toInt()
                    scrollState.scrollTo(targetScroll.coerceIn(0, scrollMax))
                    highlightLine = seekToLine
                    seekToLine = -1 // consumed
                }

                if (scrollMax > 0 && totalLyricLines > 0) {
                    val stepDelay = 50L
                    val stepsPerLine = msPerLine / stepDelay
                    val scrollPerLine = scrollMax.toFloat() / totalLyricLines
                    val scrollPerStep = scrollPerLine / stepsPerLine

                    for (line in startLine until totalLyricLines) {
                        // Wait for play state
                        while (!isPlaying) { delay(100) }

                        highlightLine = line
                        // Scroll smoothly for this line's duration
                        var accumulated = 0f
                        for (step in 0 until stepsPerLine) {
                            if (!isPlaying) break
                            // Check if user seeked while we were scrolling
                            if (seekToLine >= 0) return@LaunchedEffect
                            delay(stepDelay)
                            accumulated += scrollPerStep
                            if (accumulated >= 1f) {
                                val pixels = accumulated.toInt()
                                accumulated -= pixels
                                val target = (scrollState.value + pixels).coerceAtMost(scrollMax)
                                scrollState.scrollTo(target)
                            }
                        }
                    }

                    // Finished all lines
                    highlightLine = totalLyricLines

                    // Wait for play state before advancing
                    while (!isPlaying) { delay(100) }

                    delay(1000)
                    if (currentIndex < setlistSongs.size - 1) {
                        currentIndex++
                    } else {
                        isPlaying = false
                    }
                }
            }
        }
    }

    // Reset scroll and highlight when song changes
    LaunchedEffect(currentIndex) {
        scrollState.scrollTo(0)
        highlightLine = -1
        seekToLine = -1
    }

    // Diálogo para escribir comentario (solo admin)
    if (showCommentDialog) {
        AlertDialog(
            onDismissRequest = { showCommentDialog = false; commentInput = "" },
            title = { Text("Enviar comentario", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    placeholder = { Text("Escribe un comentario...") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val text = commentInput.trim()
                        if (text.isNotBlank() && bandId > 0) {
                            LiveSyncManager.sendComment(bandId, event.id, text)
                            visibleComment = text  // También visible para el propio admin
                        }
                        showCommentDialog = false
                        commentInput = ""
                    }
                ) { Text("Enviar", color = XoundYellow, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCommentDialog = false; commentInput = "" }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.screenBackground)
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 16.dp)
        ) {
            // Back button
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

            if (setlistLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = XoundNavy,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else if (currentSong != null) {
                // Event name
                Text(
                    text = event.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = XoundYellow,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Song title + EN VIVO badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = currentSong.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(XoundYellow, RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "EN VIVO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = XoundNavy
                            )
                        }
                    }
                }

                // Artist - BPM
                val subtitle = buildString {
                    if (!currentSong.artist.isNullOrBlank()) append(currentSong.artist)
                    if (currentSong.bpm != null) {
                        if (isNotEmpty()) append("  -  ")
                        append("${currentSong.bpm} BPM")
                    }
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!currentSong.tone.isNullOrBlank()) {
                        LiveBadge(currentSong.tone)
                    }
                    if (!currentSong.timeSignature.isNullOrBlank()) {
                        LiveBadge(currentSong.timeSignature)
                    }
                    LiveBadge("Sin capo")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Lyrics with chords
                val lyricsText = currentSong.lyrics ?: currentSong.content ?: ""
                if (lyricsText.isNotBlank()) {
                    LiveLyricsWithChords(
                        text = lyricsText,
                        highlightLine = highlightLine,
                        onLineClick = { lineIndex -> if (isAdmin) seekToLine = lineIndex }
                    )
                } else {
                    Text(
                        text = "No hay letra disponible",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                }

                // Extra space at bottom for auto-scroll to reach end
                Spacer(modifier = Modifier.height(200.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (setlistSongs.isEmpty()) "No hay canciones en el setlist"
                               else "No se pudieron cargar las canciones",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Bottom controls
        if (currentSong != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.screenBackground,
                shadowElevation = 8.dp
            ) {
                // Song counter
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${setlistSongs.size}",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )

                    // Next song preview
                    val nextSong = if (currentIndex < setlistSongs.size - 1) {
                        setlistSongs[currentIndex + 1].song
                    } else null

                    if (nextSong != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Siguiente: ",
                                fontSize = 11.sp,
                                color = colors.textHint
                            )
                            Text(
                                text = nextSong.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = XoundYellow,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    } else if (currentIndex == setlistSongs.size - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Última canción",
                            fontSize = 11.sp,
                            color = colors.textHint
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous
                        IconButton(
                            onClick = {
                                if (currentIndex > 0) {
                                    seekToLine = -1
                                    highlightLine = -1
                                    currentIndex--
                                    isPlaying = true
                                }
                            },
                            enabled = isAdmin && currentIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Anterior",
                                tint = if (currentIndex > 0) XoundYellow else Color(0xFFCCCCCC),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Play/Pause
                        IconButton(
                            onClick = { if (isAdmin) isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(56.dp)
                                .background(XoundYellow, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = XoundNavy,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Next
                        IconButton(
                            onClick = {
                                if (currentIndex < setlistSongs.size - 1) {
                                    seekToLine = -1
                                    highlightLine = -1
                                    currentIndex++
                                    isPlaying = true
                                }
                            },
                            enabled = isAdmin && currentIndex < setlistSongs.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Siguiente",
                                tint = if (currentIndex < setlistSongs.size - 1) XoundYellow else Color(0xFFCCCCCC),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Comentario (solo admin)
                        if (isAdmin) {
                            IconButton(onClick = { showCommentDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Comentario",
                                    tint = XoundYellow,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Banner de comentario (superpuesto, visible para todos)
    AnimatedVisibility(
        visible = visibleComment != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 48.dp)
                .background(XoundNavy.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = XoundYellow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = visibleComment ?: "",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    } // end Box
}

@Composable
private fun LiveBadge(text: String) {
    Box(
        modifier = Modifier
            .background(XoundYellow.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = LiveChordColor
        )
    }
}

@Composable
private fun LiveLyricsWithChords(text: String, highlightLine: Int, onLineClick: (Int) -> Unit = {}) {
    val colors = LocalXoundColors.current
    val lines = text.split("\n")
    var i = 0
    var lyricLineIndex = 0

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (isLiveSectionHeader(trimmed)) {
                Spacer(modifier = Modifier.height(12.dp))
                LiveSectionBadge(cleanLiveSectionName(trimmed))
                Spacer(modifier = Modifier.height(6.dp))
                i++
                continue
            }

            if (isLiveChordLine(trimmed)) {
                val nextLine = if (i + 1 < lines.size) lines[i + 1] else ""
                val nextTrimmed = nextLine.trim()

                if (nextTrimmed.isNotBlank() && !isLiveChordLine(nextTrimmed) && !isLiveSectionHeader(nextTrimmed)) {
                    val currentLyricIndex = lyricLineIndex
                    val isHighlighted = lyricLineIndex <= highlightLine
                    val lyricColor = if (isHighlighted) XoundYellow else colors.lyricsText
                    Column(
                        modifier = Modifier.clickable { onLineClick(currentLyricIndex) }
                    ) {
                        Text(
                            text = line.trimEnd(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHighlighted) XoundYellow else LiveChordColor,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = nextLine.trimEnd(),
                            fontSize = 15.sp,
                            color = lyricColor,
                            lineHeight = 22.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    lyricLineIndex++
                    i += 2
                } else {
                    Text(
                        text = trimmed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiveChordColor
                    )
                    i++
                }
            } else if (trimmed.isBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                i++
            } else {
                val currentLyricIndex = lyricLineIndex
                val isHighlighted = lyricLineIndex <= highlightLine
                Text(
                    text = trimmed,
                    fontSize = 15.sp,
                    color = if (isHighlighted) XoundYellow else colors.lyricsText,
                    lineHeight = 22.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { onLineClick(currentLyricIndex) }
                )
                lyricLineIndex++
                i++
            }
        }
    }
}

@Composable
private fun LiveSectionBadge(name: String) {
    Box(
        modifier = Modifier
            .background(XoundYellow.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = name.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = LiveChordColor
        )
    }
}

private fun isLiveSectionHeader(line: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.startsWith("[") && trimmed.endsWith("]")) return true
    val sectionNames = listOf(
        "intro", "verso", "verse", "coro", "chorus", "bridge", "puente",
        "pre-chorus", "pre-coro", "outro", "solo", "instrumental",
        "interludio", "interlude", "final", "estribillo"
    )
    val lower = trimmed.lowercase().removeSuffix(":")
    return sectionNames.any { lower.startsWith(it) } && trimmed.length < 30
}

private fun cleanLiveSectionName(line: String): String {
    return line.trim()
        .removePrefix("[").removeSuffix("]")
        .removeSuffix(":")
        .trim()
}

private fun isLiveChordLine(line: String): Boolean {
    if (line.isBlank()) return false
    val words = line.trim().split("\\s+".toRegex())
    if (words.isEmpty()) return false
    val chordPattern = Regex(
        "^[A-G][b#]?(m|maj|min|dim|aug|sus[24]?|add[0-9]*|[0-9]*)?(/[A-G][b#]?)?$"
    )
    val strumPattern = Regex("^[/|\\-]+$")
    val chordCount = words.count { word ->
        chordPattern.matches(word) || strumPattern.matches(word)
    }
    return chordCount.toFloat() / words.size >= 0.5f
}
