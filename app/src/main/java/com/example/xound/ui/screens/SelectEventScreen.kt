package com.example.xound.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xound.data.model.EventResponse
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow
import com.example.xound.ui.viewmodel.EventViewModel
import com.example.xound.ui.viewmodel.EventWithSetlistCount
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val XoundCream = Color(0xFFF5F0E8)

@Composable
fun SelectEventScreen(
    onBack: () -> Unit = {},
    onSelectEvent: (EventResponse) -> Unit = {},
    eventViewModel: EventViewModel
) {
    val events by eventViewModel.events.collectAsState()
    val isLoading by eventViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        eventViewModel.fetchEvents()
    }

    // Only show events with setlist songs
    val eventsWithSongs = events.filter { it.setlistCount > 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XoundCream)
            .padding(top = 48.dp)
    ) {
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
                    tint = XoundNavy
                )
            }

            Text(
                text = "Modo en Vivo",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = XoundYellow
            )
            Text(
                text = "Selecciona un evento",
                fontSize = 14.sp,
                color = Color(0xFF888888)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

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
        } else if (eventsWithSongs.isEmpty()) {
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
                        text = "No hay eventos con canciones",
                        color = Color(0xFF888888),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Agrega canciones a un evento primero",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(eventsWithSongs, key = { it.event.id }) { eventItem ->
                    SelectEventCard(
                        eventItem = eventItem,
                        onClick = { onSelectEvent(eventItem.event) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectEventCard(eventItem: EventWithSetlistCount, onClick: () -> Unit) {
    val event = eventItem.event
    val formattedDate = formatSelectDate(event.eventDate)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = XoundNavy),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(XoundYellow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = XoundNavy,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (!event.venue.isNullOrBlank()) {
                    Text(
                        text = event.venue,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "${eventItem.setlistCount} canciones  •  $formattedDate",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = XoundYellow,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun formatSelectDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "Sin fecha"
    return try {
        val date = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "MX"))
        date.format(formatter)
    } catch (_: Exception) {
        dateStr
    }
}
