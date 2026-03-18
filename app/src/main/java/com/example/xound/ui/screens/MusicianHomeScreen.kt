package com.example.xound.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xound.data.local.SessionManager
import com.example.xound.data.model.EventResponse
import com.example.xound.data.model.SongResponse
import com.example.xound.ui.theme.*
import com.example.xound.ui.viewmodel.EventViewModel
import com.example.xound.ui.viewmodel.SongViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MusicianHomeScreen(
    onLogout: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToLiveMode: () -> Unit = {},
    onEventClick: (EventResponse) -> Unit = {},
    onSongClick: (SongResponse) -> Unit = {},
    eventViewModel: EventViewModel = viewModel(),
    songViewModel: SongViewModel = viewModel()
) {
    val colors = LocalXoundColors.current
    val systemDark = isSystemInDarkTheme()
    val isDark = ThemeState.isDark(systemDark)
    val userName = SessionManager.getUserName().ifBlank { "Músico" }
    val events by eventViewModel.events.collectAsState()
    val songs by songViewModel.songs.collectAsState()

    LaunchedEffect(Unit) {
        eventViewModel.fetchEvents()
        songViewModel.fetchSongs()
    }

    // Count unique tones
    val uniqueTones = songs.mapNotNull { it.tone?.takeIf { t -> t.isNotBlank() } }.distinct().size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 24.dp)
    ) {
        // Top bar: logout + dark mode
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onLogout,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    tint = if (isDark) Color.White else XoundNavy,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(
                onClick = { ThemeState.toggleDarkMode(systemDark) },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Cambiar tema",
                    tint = XoundYellow,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Greeting
        Text(
            text = "¡Hola de nuevo!,",
            fontSize = 14.sp,
            color = colors.textPrimary
        )
        Text(
            text = userName,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = XoundYellow
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats row - 3 cards (Canciones, Tonalidades, Eventos)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MusicianStatCard("${songs.size}", "Canciones", Modifier.weight(1f))
            MusicianStatCard("$uniqueTones", "Tonalidades", Modifier.weight(1f))
            MusicianStatCard("${events.size}", "Eventos", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action cards - row 1: Modo en Vivo + Eventos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MusicianActionCard(
                title = "Modo en Vivo",
                subtitle = "Toca ahora",
                icon = Icons.Default.MusicNote,
                backgroundColor = XoundYellow,
                contentColor = XoundNavy,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToLiveMode
            )
            MusicianActionCard(
                title = "Eventos",
                subtitle = "Ver eventos próximos",
                icon = Icons.Default.Event,
                backgroundColor = colors.navyCardDark,
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToEvents
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action card - Biblioteca (full width)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clickable { onNavigateToLibrary() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.navyCardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = "Biblioteca",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Biblioteca",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${songs.size} canciones",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recientes section - shows recent songs
        Text(
            text = "RECIENTES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (events.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.navyCardDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No hay eventos recientes",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            events.take(3).forEach { eventItem ->
                MusicianRecentEventCard(
                    event = eventItem.event,
                    onClick = { onEventClick(eventItem.event) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MusicianRecentEventCard(event: EventResponse, onClick: () -> Unit = {}) {
    val colors = LocalXoundColors.current
    val formattedDate = formatMusicianEventDate(event.eventDate)
    val status = getMusicianEventStatus(event)
    val statusColor = when (status) {
        "Próximo" -> XoundYellow
        "Finalizado" -> colors.successColor
        else -> colors.textSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.recentCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Calendar icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(colors.navyCardDark, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = XoundYellow,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Event info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!event.venue.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = event.venue,
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Date + status
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedDate,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = XoundYellow,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicianStatCard(number: String, label: String, modifier: Modifier = Modifier) {
    val colors = LocalXoundColors.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.navyCardDark)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Text(
                text = number,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MusicianActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun getMusicianEventStatus(event: EventResponse): String {
    if (!event.published) return "Borrador"
    return try {
        val date = LocalDateTime.parse(event.eventDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        if (date.isBefore(LocalDateTime.now())) "Finalizado" else "Próximo"
    } catch (_: Exception) {
        if (event.published) "Próximo" else "Borrador"
    }
}

private fun formatMusicianEventDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    return try {
        val date = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm 'h'", Locale("es", "MX"))
        date.format(formatter).uppercase()
    } catch (_: Exception) {
        dateStr
    }
}
