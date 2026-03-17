package com.example.xound.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xound.data.model.SongResponse
import com.example.xound.ui.theme.LocalXoundColors
import com.example.xound.ui.theme.XoundYellow

private val ChordColor = Color(0xFFE5A100)

@Composable
fun ViewSongScreen(
    song: SongResponse,
    eventName: String? = null,
    onBack: () -> Unit = {}
) {
    val colors = LocalXoundColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 24.dp)
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

        // Event name if coming from an event
        if (!eventName.isNullOrBlank()) {
            Text(
                text = eventName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Title
        Text(
            text = song.title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = XoundYellow
        )

        // Artist - BPM
        val subtitle = buildString {
            if (!song.artist.isNullOrBlank()) append(song.artist)
            if (song.bpm != null) {
                if (isNotEmpty()) append("  -  ")
                append("${song.bpm} BPM")
            }
        }
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Badges row: tone, time signature, capo
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!song.tone.isNullOrBlank()) {
                SongBadge(song.tone)
            }
            if (!song.timeSignature.isNullOrBlank()) {
                SongBadge(song.timeSignature)
            }
            SongBadge("Sin capo")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Lyrics with chords
        val lyricsText = song.lyrics ?: song.content ?: ""
        if (lyricsText.isNotBlank()) {
            LyricsWithChords(lyricsText)
        } else {
            Text(
                text = "No hay letra disponible",
                fontSize = 14.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun SongBadge(text: String) {
    Box(
        modifier = Modifier
            .background(XoundYellow.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = ChordColor
        )
    }
}

@Composable
private fun LyricsWithChords(text: String) {
    val colors = LocalXoundColors.current
    val lines = text.split("\n")
    var i = 0

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Section headers like [Intro], [Verse 1], [Chorus], etc.
            if (isSectionHeader(trimmed)) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionBadge(cleanSectionName(trimmed))
                Spacer(modifier = Modifier.height(6.dp))
                i++
                continue
            }

            // Check if this line is a chord line
            if (isChordLine(trimmed)) {
                // Check if next line is a lyrics line
                val nextLine = if (i + 1 < lines.size) lines[i + 1] else ""
                val nextTrimmed = nextLine.trim()

                if (nextTrimmed.isNotBlank() && !isChordLine(nextTrimmed) && !isSectionHeader(nextTrimmed)) {
                    // Chord line + lyrics line pair
                    ChordLyricPair(chordLine = line, lyricLine = nextLine, lyricsColor = colors.lyricsText)
                    i += 2
                } else {
                    // Chord-only line (like intro chords)
                    Text(
                        text = trimmed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChordColor
                    )
                    i++
                }
            } else if (trimmed.isBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                i++
            } else {
                // Regular lyrics line without chords above
                Text(
                    text = trimmed,
                    fontSize = 15.sp,
                    color = colors.lyricsText,
                    lineHeight = 22.sp
                )
                i++
            }
        }
    }
}

@Composable
private fun ChordLyricPair(chordLine: String, lyricLine: String, lyricsColor: Color) {
    Column {
        // Chords
        Text(
            text = chordLine.trimEnd(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ChordColor,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        // Lyrics
        Text(
            text = lyricLine.trimEnd(),
            fontSize = 15.sp,
            color = lyricsColor,
            lineHeight = 22.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@Composable
private fun SectionBadge(name: String) {
    Box(
        modifier = Modifier
            .background(XoundYellow.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = name.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ChordColor
        )
    }
}

// Detect section headers: [Intro], [Verse 1], [Chorus], etc. or lines ending with :
private fun isSectionHeader(line: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.startsWith("[") && trimmed.endsWith("]")) return true
    // Common section names
    val sectionNames = listOf(
        "intro", "verso", "verse", "coro", "chorus", "bridge", "puente",
        "pre-chorus", "pre-coro", "outro", "solo", "instrumental",
        "interludio", "interlude", "final", "estribillo"
    )
    val lower = trimmed.lowercase().removeSuffix(":")
    return sectionNames.any { lower.startsWith(it) } && trimmed.length < 30
}

private fun cleanSectionName(line: String): String {
    return line.trim()
        .removePrefix("[").removeSuffix("]")
        .removeSuffix(":")
        .trim()
}

// A chord line is one where most "words" are chord symbols
private fun isChordLine(line: String): Boolean {
    if (line.isBlank()) return false
    val words = line.trim().split("\\s+".toRegex())
    if (words.isEmpty()) return false

    val chordPattern = Regex(
        "^[A-G][b#]?(m|maj|min|dim|aug|sus[24]?|add[0-9]*|[0-9]*)?(/[A-G][b#]?)?$"
    )
    // Also match strumming patterns like //// or rhythm markers
    val strumPattern = Regex("^[/|\\-]+$")

    val chordCount = words.count { word ->
        chordPattern.matches(word) || strumPattern.matches(word)
    }

    return chordCount.toFloat() / words.size >= 0.5f
}
