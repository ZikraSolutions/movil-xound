package com.example.xound.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow
import com.example.xound.ui.viewmodel.CreateEventState
import com.example.xound.ui.viewmodel.EventViewModel

private val XoundCream = Color(0xFFF5F0E8)

@Composable
fun CreateEventScreen(
    onBack: () -> Unit = {},
    eventViewModel: EventViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }

    val createState by eventViewModel.createState.collectAsState()
    val isLoading = createState is CreateEventState.Loading
    val isSuccess = createState is CreateEventState.Success

    // Reset state when leaving
    DisposableEffect(Unit) {
        onDispose { eventViewModel.resetCreateState() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XoundCream)
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
                tint = XoundNavy
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
        FormLabel("Nombre del evento")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Nombre del evento", color = Color(0xFF999999)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading && !isSuccess,
            colors = formFieldColors()
        )

        Spacer(modifier = Modifier.height(20.dp))

        FormLabel("Fecha del evento")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = eventDate,
            onValueChange = { eventDate = it },
            placeholder = { Text("Fecha del evento", color = Color(0xFF999999)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading && !isSuccess,
            colors = formFieldColors()
        )

        Spacer(modifier = Modifier.height(20.dp))

        FormLabel("Lugar del evento")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = venue,
            onValueChange = { venue = it },
            placeholder = { Text("Lugar del evento", color = Color(0xFF999999)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading && !isSuccess,
            colors = formFieldColors()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Adjuntar section
        FormLabel("Adjuntar")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AttachChip("PDF")
            AttachChip("Imagen")
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Publish button (visible after creation)
        if (isSuccess) {
            val createdEvent = (createState as CreateEventState.Success).event

            OutlinedButton(
                onClick = { eventViewModel.publishEvent(createdEvent.id) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = null,
                    tint = XoundNavy,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Publicar",
                    color = XoundNavy,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Success message
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Evento creado exitosamente",
                    fontSize = 14.sp,
                    color = Color(0xFF333333)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Error message
        if (createState is CreateEventState.Error) {
            Text(
                text = (createState as CreateEventState.Error).message,
                color = Color.Red,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save button
        Button(
            onClick = {
                val dateToSend = if (eventDate.isBlank()) null else eventDate.trim()
                val venueToSend = if (venue.isBlank()) null else venue.trim()
                eventViewModel.createEvent(title, dateToSend, venueToSend)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isLoading && !isSuccess,
            colors = ButtonDefaults.buttonColors(containerColor = XoundNavy),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = "Guardar nuevo evento",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black
    )
}

@Composable
private fun AttachChip(label: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(10.dp))
            .clickable { /* TODO: file picker */ }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
private fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = Color(0xFFE5E5E5),
    focusedBorderColor = XoundNavy,
    unfocusedContainerColor = Color.White,
    focusedContainerColor = Color.White
)
