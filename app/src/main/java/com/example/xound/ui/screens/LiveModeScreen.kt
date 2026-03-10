package com.example.xound.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import com.example.xound.data.model.EventResponse
import com.example.xound.data.model.SongResponse
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow
import com.example.xound.ui.viewmodel.EventViewModel
import kotlinx.coroutines.delay

private val XoundCream = Color(0xFFF5F0E8)
private val LiveChordColor = Color(0xFFE5A100)

@Composable
fun LiveModeScreen(
    event: EventResponse,
    onBack: () -> Unit = {},
    eventViewModel: EventViewModel
) {
    val setlistSongs by eventViewModel.setlistSongs.collectAsState()
    val setlistLoading by eventViewModel.setlistLoading.collectAsState()

    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var highlightLine by remember { mutableIntStateOf(-1) }

    val scrollState = rememberScrollState()

    LaunchedEffect(event.id) {
        eventViewModel.fetchSetlist(event.id)
    }

    // Get current song
    val currentSetlistItem = if (setlistSongs.isNotEmpty() && currentIndex in setlistSongs.indices) {
        setlistSongs[currentIndex]
    } else null
    val currentSong = currentSetlistItem?.song

    // Calculate ms per lyric line based on BPM and time signature
    // ~2 bars per lyric line; secondsPerBar = (beatsPerBar * 60) / BPM
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

    // Count displayable lyric lines (non-blank, non-section-header, non-chord-only)
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
                    count++ // chord+lyric pair counts as 1 lyric line
                    i += 2
                } else {
                    i++ // chord-only line, skip
                }
            } else if (trimmed.isBlank()) { i++ }
            else { count++; i++ }
        }
        return count
    }

    // Auto-scroll + highlight when playing
    LaunchedEffect(isPlaying, currentSong) {
        if (isPlaying && currentSong != null) {
            val lyricsText = currentSong.lyrics ?: currentSong.content ?: ""
            if (lyricsText.isNotBlank()) {
                val msPerLine = calcMsPerLine(currentSong)
                val totalLyricLines = countLyricLines(lyricsText)
                val totalDurationMs = totalLyricLines * msPerLine
                val scrollMax = scrollState.maxValue
                if (scrollMax > 0 && totalDurationMs > 0) {
                    val stepDelay = 50L
                    val totalSteps = totalDurationMs / stepDelay
                    val scrollPerStep = scrollMax.toFloat() / totalSteps.toFloat()
                    val stepsPerLine = if (totalLyricLines > 0) totalSteps / totalLyricLines else totalSteps
                    var accumulated = 0f
                    var stepCount = 0L

                    while (isPlaying && scrollState.value < scrollMax) {
                        delay(stepDelay)
                        stepCount++
                        // Update highlight line
                        val newLine = if (stepsPerLine > 0) (stepCount / stepsPerLine).toInt() else 0
                        if (newLine != highlightLine) highlightLine = newLine

                        accumulated += scrollPerStep
                        if (accumulated >= 1f) {
                            val pixels = accumulated.toInt()
                            accumulated -= pixels
                            scrollState.scrollTo(scrollState.value + pixels)
                        }
                    }

                    // Highlight last line
                    highlightLine = totalLyricLines

                    // When auto-scroll finishes, go to next song
                    if (isPlaying && scrollState.value >= scrollMax - 5) {
                        delay(1000) // brief pause before next song
                        if (currentIndex < setlistSongs.size - 1) {
                            currentIndex++
                        } else {
                            isPlaying = false
                        }
                    }
                }
            }
        }
    }

    // Reset scroll and highlight when song changes
    LaunchedEffect(currentIndex) {
        scrollState.scrollTo(0)
        highlightLine = -1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XoundCream)
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
                    tint = XoundNavy
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
                        color = Color.Black,
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
                        color = Color(0xFF666666)
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
                    LiveLyricsWithChords(lyricsText, highlightLine)
                } else {
                    Text(
                        text = "No hay letra disponible",
                        fontSize = 14.sp,
                        color = Color(0xFF888888)
                    )
                }

                // Extra space at bottom for auto-scroll to reach end
                Spacer(modifier = Modifier.height(200.dp))
            } else if (setlistSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay canciones en el setlist",
                        color = Color(0xFF888888),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Bottom controls
        if (currentSong != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = XoundCream,
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
                        color = Color(0xFF888888)
                    )

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
                                    highlightLine = -1
                                    currentIndex--
                                    isPlaying = true
                                }
                            },
                            enabled = currentIndex > 0
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
                            onClick = { isPlaying = !isPlaying },
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
                                    highlightLine = -1
                                    currentIndex++
                                    isPlaying = true
                                }
                            },
                            enabled = currentIndex < setlistSongs.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Siguiente",
                                tint = if (currentIndex < setlistSongs.size - 1) XoundYellow else Color(0xFFCCCCCC),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
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
private fun LiveLyricsWithChords(text: String, highlightLine: Int) {
    val lines = text.split("\n")
    var i = 0
    var lyricLineIndex = 0 // tracks which displayable lyric line we're on

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
                    val isHighlighted = lyricLineIndex <= highlightLine
                    val lyricColor = if (isHighlighted) XoundYellow else Color.Black
                    Column {
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
                    // Chord-only line (intro chords etc) - not counted as lyric line
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
                val isHighlighted = lyricLineIndex <= highlightLine
                Text(
                    text = trimmed,
                    fontSize = 15.sp,
                    color = if (isHighlighted) XoundYellow else Color.Black,
                    lineHeight = 22.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
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
