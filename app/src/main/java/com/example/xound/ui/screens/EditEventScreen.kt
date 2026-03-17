package com.example.xound.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xound.data.model.EventResponse
import com.example.xound.ui.theme.LocalXoundColors
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow
import com.example.xound.ui.viewmodel.EditEventState
import com.example.xound.ui.viewmodel.EventViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    event: EventResponse,
    onBack: () -> Unit = {},
    eventViewModel: EventViewModel
) {
    val colors = LocalXoundColors.current
    var title by remember { mutableStateOf(event.title) }
    var selectedDate by remember {
        mutableStateOf(
            try {
                event.eventDate?.let {
                    LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate()
                }
            } catch (_: Exception) { null }
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var venue by remember { mutableStateOf(event.venue ?: "") }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val editState by eventViewModel.editState.collectAsState()
    val isLoading = editState is EditEventState.Loading

    LaunchedEffect(editState) {
        if (editState is EditEventState.Success) {
            onBack()
            eventViewModel.resetEditState()
        }
    }

    DisposableEffect(Unit) {
        onDispose { eventViewModel.resetEditState() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 24.dp)
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
            text = "Editar Evento",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = XoundYellow
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Title
        Text(text = "Nombre del evento", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colors.inputBorder,
                focusedBorderColor = XoundNavy,
                unfocusedContainerColor = colors.inputBackground,
                focusedContainerColor = colors.inputBackground,
                unfocusedTextColor = colors.textPrimary,
                focusedTextColor = colors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Date
        Text(text = "Fecha del evento", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = selectedDate?.format(dateFormatter) ?: "",
            onValueChange = {},
            placeholder = { Text("dd/mm/aaaa", color = colors.textHint) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoading) { showDatePicker = true },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            readOnly = true,
            enabled = false,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Seleccionar fecha",
                    tint = XoundNavy,
                    modifier = Modifier.clickable(enabled = !isLoading) { showDatePicker = true }
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = colors.inputBorder,
                disabledContainerColor = colors.inputBackground,
                disabledTextColor = colors.textPrimary,
                disabledPlaceholderColor = colors.textHint,
                disabledTrailingIconColor = XoundNavy
            )
        )

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }) {
                        Text("Aceptar", color = XoundNavy)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Venue
        Text(text = "Lugar del evento", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = venue,
            onValueChange = { venue = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colors.inputBorder,
                focusedBorderColor = XoundNavy,
                unfocusedContainerColor = colors.inputBackground,
                focusedContainerColor = colors.inputBackground,
                unfocusedTextColor = colors.textPrimary,
                focusedTextColor = colors.textPrimary
            )
        )

        // Error
        if (editState is EditEventState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (editState as EditEventState.Error).message,
                color = colors.errorColor,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Spacer(modifier = Modifier.weight(1f))

        // Save button
        Button(
            onClick = {
                val dateToSend = selectedDate?.atStartOfDay()?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val venueToSend = if (venue.isBlank()) null else venue.trim()
                eventViewModel.updateEvent(event.id, title, dateToSend, venueToSend)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = XoundYellow),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = XoundNavy,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = "Guardar Cambios",
                    color = XoundNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
