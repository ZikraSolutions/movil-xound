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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xound.data.model.EventResponse
import com.example.xound.ui.theme.LocalXoundColors
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow
import com.example.xound.ui.viewmodel.EventViewModel
import com.example.xound.ui.viewmodel.EventWithSetlistCount
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onBack: () -> Unit = {},
    onCreateEvent: () -> Unit = {},
    onEventClick: (EventResponse) -> Unit = {},
    onEditEvent: (EventResponse) -> Unit = {},
    eventViewModel: EventViewModel = viewModel()
) {
    val colors = LocalXoundColors.current
    val events by eventViewModel.events.collectAsState()
    val isLoading by eventViewModel.isLoading.collectAsState()
    val error by eventViewModel.error.collectAsState()

    var eventToDelete by remember { mutableStateOf<EventResponse?>(null) }

    LaunchedEffect(Unit) {
        eventViewModel.fetchEvents()
    }

    val upcomingCount = events.count { getEventStatus(it.event) == "Próximo" }

    // Delete confirmation dialog
    if (eventToDelete != null) {
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
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
                    text = "Ocultar evento",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    "¿Estás seguro de que quieres ocultar \"${eventToDelete?.title}\"? El evento no se eliminará permanentemente.",
                    color = colors.textPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        eventToDelete?.let { eventViewModel.deleteEvent(it.id) }
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Ocultar", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { eventToDelete = null }) {
                    Text("Cancelar", color = colors.textPrimary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.screenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp)
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

            // Title
            Text(
                text = "Eventos",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = XoundYellow
            )
            Text(
                text = "$upcomingCount próximos",
                fontSize = 14.sp,
                color = colors.textSecondary
            )

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
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "Error desconocido",
                        color = colors.errorColor,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(events, key = { it.event.id }) { eventItem ->
                        SwipeableEventCard(
                            eventItem = eventItem,
                            onClick = { onEventClick(eventItem.event) },
                            onDelete = { eventToDelete = eventItem.event },
                            onEdit = { onEditEvent(eventItem.event) }
                        )
                    }

                    if (events.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay eventos aún",
                                    color = colors.textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom button
        Button(
            onClick = onCreateEvent,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = XoundYellow),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = XoundNavy,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Crear nuevo evento",
                color = XoundNavy,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun SwipeableEventCard(
    eventItem: EventWithSetlistCount,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
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
            .clip(RoundedCornerShape(16.dp))
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
                            offsetX = newOffset.coerceIn(-cardWidth * threshold, cardWidth * threshold)
                        }
                    )
                }
        ) {
            EventCard(eventItem = eventItem, onClick = onClick)
        }
    }
}

@Composable
private fun EventCard(eventItem: EventWithSetlistCount, onClick: () -> Unit = {}) {
    val colors = LocalXoundColors.current
    val event = eventItem.event
    val status = getEventStatus(event)
    val formattedDate = formatEventDate(event.eventDate)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.navyCardDark),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Date
            Text(
                text = formattedDate,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = XoundYellow,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = event.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Venue
            if (!event.venue.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.venue,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom row: setlist count + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${eventItem.setlistCount} canciones en el setlist",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                // Status badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = when (status) {
                                    "Próximo" -> XoundYellow
                                    "Finalizado" -> Color(0xFF4CAF50)
                                    else -> Color.White.copy(alpha = 0.5f)
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = status,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (status) {
                            "Próximo" -> XoundYellow
                            "Finalizado" -> Color(0xFF4CAF50)
                            else -> Color.White.copy(alpha = 0.5f)
                        }
                    )
                }
            }
        }
    }
}

private fun getEventStatus(event: EventResponse): String {
    if (!event.published) return "Borrador"
    return try {
        val date = LocalDateTime.parse(event.eventDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        if (date.isBefore(LocalDateTime.now())) "Finalizado" else "Próximo"
    } catch (_: Exception) {
        if (event.published) "Próximo" else "Borrador"
    }
}

private fun formatEventDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "Sin fecha"
    return try {
        val date = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm 'h'", Locale("es", "MX"))
        date.format(formatter).uppercase()
    } catch (_: Exception) {
        dateStr
    }
}
