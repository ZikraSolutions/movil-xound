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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xound.data.model.CreateSongRequest
import com.example.xound.data.network.RetrofitClient
import com.example.xound.ui.theme.LocalXoundColors
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

// ViewModel
sealed class AddSongState {
    object Idle : AddSongState()
    object Loading : AddSongState()
    object Success : AddSongState()
    data class Error(val message: String) : AddSongState()
}

sealed class FetchLyricsState {
    object Idle : FetchLyricsState()
    object Loading : FetchLyricsState()
    data class Success(val lyrics: String, val tone: String? = null) : FetchLyricsState()
    data class Error(val message: String) : FetchLyricsState()
}

class AddSongViewModel : ViewModel() {

    private val _saveState = MutableStateFlow<AddSongState>(AddSongState.Idle)
    val saveState: StateFlow<AddSongState> = _saveState.asStateFlow()

    private val _fetchState = MutableStateFlow<FetchLyricsState>(FetchLyricsState.Idle)
    val fetchState: StateFlow<FetchLyricsState> = _fetchState.asStateFlow()

    fun fetchLyrics(artist: String, title: String) {
        if (title.isBlank()) {
            _fetchState.value = FetchLyricsState.Error("Ingresa el nombre de la canción")
            return
        }
        viewModelScope.launch {
            _fetchState.value = FetchLyricsState.Loading
            try {
                val response = RetrofitClient.apiService.fetchLyricsAndChords(artist.trim(), title.trim())
                if (response["found"] == "true") {
                    val chords = response["chords"] ?: ""
                    val lyrics = response["lyrics"] ?: ""
                    val tone = response["tone"]
                    // CifraClub ya trae letra+acordes juntos; solo usar lyrics si no hay acordes
                    val content = if (chords.isNotBlank()) chords else lyrics
                    if (content.isNotBlank()) {
                        _fetchState.value = FetchLyricsState.Success(content, tone = tone)
                    } else {
                        _fetchState.value = FetchLyricsState.Error("No se encontró letra ni acordes")
                    }
                } else {
                    _fetchState.value = FetchLyricsState.Error("No se encontró letra ni acordes")
                }
            } catch (e: HttpException) {
                _fetchState.value = FetchLyricsState.Error("Error ${e.code()}: ${e.response()?.errorBody()?.string() ?: e.message()}")
            } catch (e: Exception) {
                _fetchState.value = FetchLyricsState.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun saveSong(title: String, artist: String?, tone: String?, bpm: Int?, timeSignature: String?, lyrics: String?) {
        if (title.isBlank()) {
            _saveState.value = AddSongState.Error("El nombre de la canción es requerido")
            return
        }
        viewModelScope.launch {
            _saveState.value = AddSongState.Loading
            try {
                RetrofitClient.apiService.createSong(
                    CreateSongRequest(
                        title = title.trim(),
                        artist = artist?.trim()?.ifBlank { null },
                        tone = tone?.trim()?.ifBlank { null },
                        bpm = bpm,
                        timeSignature = timeSignature?.trim()?.ifBlank { null },
                        lyrics = lyrics?.trim()?.ifBlank { null }
                    )
                )
                _saveState.value = AddSongState.Success
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                _saveState.value = AddSongState.Error("Error ${e.code()}: ${body ?: e.message()}")
            } catch (e: Exception) {
                _saveState.value = AddSongState.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun resetState() {
        _saveState.value = AddSongState.Idle
        _fetchState.value = FetchLyricsState.Idle
    }

    private fun extractTone(content: String): String? {
        val chordPattern = Regex("\\b([A-G][#b]?)(m|min|maj|dim|aug|sus|7|9)?\\b")
        val match = chordPattern.find(content)
        return match?.groupValues?.get(1)?.let { root ->
            val suffix = match.groupValues.getOrNull(2) ?: ""
            if (suffix == "m" || suffix == "min") "${root}m" else root
        }
    }
}

// Screen
@Composable
fun AddSongScreen(
    onBack: () -> Unit = {},
    addSongViewModel: AddSongViewModel = viewModel()
) {
    val colors = LocalXoundColors.current

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf("") }
    var bpmText by remember { mutableStateOf("") }
    var timeSignature by remember { mutableStateOf("4/4") }
    var lyrics by remember { mutableStateOf("") }
    val saveState by addSongViewModel.saveState.collectAsState()
    val fetchState by addSongViewModel.fetchState.collectAsState()
    val isSaving = saveState is AddSongState.Loading
    val isFetching = fetchState is FetchLyricsState.Loading

    // Update lyrics and tone when fetched
    LaunchedEffect(fetchState) {
        if (fetchState is FetchLyricsState.Success) {
            val result = fetchState as FetchLyricsState.Success
            lyrics = result.lyrics
            // Autocomplete tone from CIFRACLUB
            if (!result.tone.isNullOrBlank() && tone.isBlank()) {
                tone = result.tone
            }
        }
    }

    // Navigate back on save success
    LaunchedEffect(saveState) {
        if (saveState is AddSongState.Success) {
            onBack()
            addSongViewModel.resetState()
        }
    }

    DisposableEffect(Unit) {
        onDispose { addSongViewModel.resetState() }
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
                tint = XoundNavy
            )
        }

        // Title
        Text(
            text = "Agregar Canción",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = XoundYellow
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nombre de la canción
        FormLabel("Nombre de la canción")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Nombre de la canción", color = colors.textHint) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isSaving,
            colors = fieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Artista
        FormLabel("Artista")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            placeholder = { Text("Ej. Queen", color = colors.textHint) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isSaving,
            colors = fieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tonalidad, BPM, Ritmo row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FormLabel("Tonalidad")
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = tone,
                    onValueChange = { tone = it },
                    placeholder = { Text("G", color = colors.textHint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isSaving,
                    colors = fieldColors()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                FormLabel("BPM")
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = bpmText,
                    onValueChange = { bpmText = it.filter { c -> c.isDigit() } },
                    placeholder = { Text("120", color = colors.textHint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                FormLabel("Ritmo")
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = timeSignature,
                    onValueChange = { timeSignature = it },
                    placeholder = { Text("4/4", color = colors.textHint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isSaving,
                    colors = fieldColors()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Fetch button
        Button(
            onClick = { addSongViewModel.fetchLyrics(artist, title) },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            enabled = !isFetching && !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = XoundYellow),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isFetching) {
                CircularProgressIndicator(
                    color = XoundNavy,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = "Obtener Letra y Acordes",
                    color = XoundNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Fetch error
        if (fetchState is FetchLyricsState.Error) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = (fetchState as FetchLyricsState.Error).message,
                color = colors.errorColor,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Letra field
        FormLabel("Letra")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = lyrics,
            onValueChange = { lyrics = it },
            placeholder = { Text("Letra de la canción...", color = colors.textHint) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSaving,
            maxLines = 20,
            colors = fieldColors()
        )

        // Save error
        if (saveState is AddSongState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (saveState as AddSongState.Error).message,
                color = colors.errorColor,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save button
        Button(
            onClick = {
                val bpm = bpmText.toIntOrNull()
                addSongViewModel.saveSong(title, artist, tone, bpm, timeSignature, lyrics)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isSaving && !isFetching,
            colors = ButtonDefaults.buttonColors(containerColor = XoundYellow),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = XoundNavy,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = "Guardar Canción",
                    color = XoundNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SourceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = "• $label",
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color.White.copy(alpha = 0.2f),
            selectedLabelColor = Color.White,
            containerColor = Color.Transparent,
            labelColor = Color.White.copy(alpha = 0.7f)
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = Color.White.copy(alpha = 0.3f),
            selectedBorderColor = Color.White.copy(alpha = 0.6f),
            enabled = true,
            selected = selected
        ),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
private fun FormLabel(text: String) {
    val colors = LocalXoundColors.current
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textPrimary
    )
}

@Composable
private fun fieldColors(): TextFieldColors {
    val colors = LocalXoundColors.current
    return OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = colors.inputBorder,
        focusedBorderColor = XoundNavy,
        unfocusedContainerColor = colors.inputBackground,
        focusedContainerColor = colors.inputBackground,
        unfocusedTextColor = colors.textPrimary,
        focusedTextColor = colors.textPrimary
    )
}
