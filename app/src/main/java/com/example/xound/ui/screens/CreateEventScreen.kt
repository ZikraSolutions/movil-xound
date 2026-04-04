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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xound.ui.theme.LocalXoundColors
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow
import com.example.xound.ui.viewmodel.CreateEventState
import com.example.xound.ui.viewmodel.EventViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onBack: () -> Unit = {},
    onPublished: () -> Unit = {},
    eventViewModel: EventViewModel = viewModel()
) {
    val colors = LocalXoundColors.current
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var venue by remember { mutableStateOf("") }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val createState by eventViewModel.createState.collectAsState()
    val isLoading = createState is CreateEventState.Loading
    val isSuccess = createState is CreateEventState.Success
    var publishing by remember { mutableStateOf(false) }

    // Navegar tras publicar — fuera del bloque isSuccess para que no desaparezca
    LaunchedEffect(createState) {
        if (createState is CreateEventState.Idle && publishing) {
            onPublished()
        }
    }

    // Reset state when leaving
    DisposableEffect(Unit) {
        onDispose { eventViewModel.resetCreateState() }
    }

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

        // Title
        Text(
            text = "Crear Evento",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = XoundYellow
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Form fields
        Text(text = "Nombre del evento", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Nombre del evento", color = colors.textHint) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading && !isSuccess,
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

        Text(text = "Fecha del evento", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = selectedDate?.format(dateFormatter) ?: "",
            onValueChange = {},
            placeholder = { Text("dd/mm/aaaa", color = colors.textHint) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoading && !isSuccess) { showDatePicker = true },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            readOnly = true,
            enabled = false,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Seleccionar fecha",
                    tint = XoundNavy,
                    modifier = Modifier.clickable(enabled = !isLoading && !isSuccess) { showDatePicker = true }
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

        Text(text = "Lugar del evento", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = venue,
            onValueChange = { venue = it },
            placeholder = { Text("Lugar del evento", color = colors.textHint) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading && !isSuccess,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colors.inputBorder,
                focusedBorderColor = XoundNavy,
                unfocusedContainerColor = colors.inputBackground,
                focusedContainerColor = colors.inputBackground,
                unfocusedTextColor = colors.textPrimary,
                focusedTextColor = colors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Publish button (visible after creation)
        if (isSuccess) {
            val createdEvent = (createState as CreateEventState.Success).event

            // Success message
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colors.successColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Evento creado exitosamente",
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Publish button
            Button(
                onClick = {
                    publishing = true
                    eventViewModel.publishEvent(createdEvent.id)
                },
                enabled = !publishing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = XoundYellow),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (publishing) {
                    CircularProgressIndicator(
                        color = XoundNavy,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        tint = XoundNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Publicar evento",
                        color = XoundNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Go to events button
            OutlinedButton(
                onClick = {
                    eventViewModel.resetCreateState()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Ir a mis eventos",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

        }

        // Error message
        if (createState is CreateEventState.Error) {
            Text(
                text = (createState as CreateEventState.Error).message,
                color = colors.errorColor,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save button
        Button(
            onClick = {
                val dateToSend = selectedDate?.atStartOfDay()?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val venueToSend = if (venue.isBlank()) null else venue.trim()
                eventViewModel.createEvent(title, dateToSend, venueToSend)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isLoading && !isSuccess,
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
                    text = "Guardar nuevo evento",
                    color = XoundNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
