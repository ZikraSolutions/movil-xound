package com.example.xound.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xound.data.model.CreateSongRequest
import com.example.xound.data.model.SongResponse
import com.example.xound.data.network.RetrofitClient
import com.example.xound.ui.theme.LocalXoundColors
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class EditSongState {
    object Idle : EditSongState()
    object Loading : EditSongState()
    object Success : EditSongState()
    data class Error(val message: String) : EditSongState()
}

class EditSongViewModel : ViewModel() {
    private val _state = MutableStateFlow<EditSongState>(EditSongState.Idle)
    val state: StateFlow<EditSongState> = _state.asStateFlow()

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

    fun updateSong(id: Long, title: String, artist: String?, tone: String?, bpm: Int?, timeSignature: String?, lyrics: String?) {
        if (title.isBlank()) {
            _state.value = EditSongState.Error("El nombre es requerido")
            return
        }
        viewModelScope.launch {
            _state.value = EditSongState.Loading
            try {
                RetrofitClient.apiService.updateSong(
                    id,
                    CreateSongRequest(
                        title = title.trim(),
                        artist = artist?.trim()?.ifBlank { null },
                        tone = tone?.trim()?.ifBlank { null },
                        bpm = bpm,
                        timeSignature = timeSignature?.trim()?.ifBlank { null },
                        lyrics = lyrics?.trim()?.ifBlank { null }
                    )
                )
                _state.value = EditSongState.Success
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                _state.value = EditSongState.Error("Error ${e.code()}: ${body ?: e.message()}")
            } catch (e: Exception) {
                _state.value = EditSongState.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun resetState() {
        _state.value = EditSongState.Idle
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

@Composable
fun EditSongScreen(
    song: SongResponse,
    onBack: () -> Unit = {},
    editSongViewModel: EditSongViewModel = viewModel()
) {
    val colors = LocalXoundColors.current

    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist ?: "") }
    var tone by remember { mutableStateOf(song.tone ?: "") }
    var bpmText by remember { mutableStateOf(song.bpm?.toString() ?: "") }
    var timeSignature by remember { mutableStateOf(song.timeSignature ?: "") }
    var lyrics by remember { mutableStateOf(song.lyrics ?: "") }
    val state by editSongViewModel.state.collectAsState()
    val fetchState by editSongViewModel.fetchState.collectAsState()
    val isLoading = state is EditSongState.Loading
    val isFetching = fetchState is FetchLyricsState.Loading
    val isSuccess = state is EditSongState.Success

    // Update lyrics and tone when fetched
    LaunchedEffect(fetchState) {
        if (fetchState is FetchLyricsState.Success) {
            val result = fetchState as FetchLyricsState.Success
            lyrics = result.lyrics
            if (!result.tone.isNullOrBlank() && tone.isBlank()) {
                tone = result.tone
            }
        }
    }

    LaunchedEffect(state) {
        if (state is EditSongState.Success) {
            onBack()
            editSongViewModel.resetState()
        }
    }

    DisposableEffect(Unit) {
        onDispose { editSongViewModel.resetState() }
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
            text = "Editar Canción",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = XoundYellow
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nombre
        EditLabel("Nombre de la canción", colors.textPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading,
            colors = editFieldColors(colors)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Artista
        EditLabel("Artista", colors.textPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading,
            colors = editFieldColors(colors)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tonalidad, BPM, Ritmo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                EditLabel("Tonalidad", colors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = tone,
                    onValueChange = { tone = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    colors = editFieldColors(colors)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                EditLabel("BPM", colors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = bpmText,
                    onValueChange = { bpmText = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = editFieldColors(colors)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                EditLabel("Ritmo", colors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = timeSignature,
                    onValueChange = { timeSignature = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    colors = editFieldColors(colors)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Fetch button
        Button(
            onClick = { editSongViewModel.fetchLyrics(artist, title) },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            enabled = !isFetching && !isLoading,
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

        // Letra
        EditLabel("Letra", colors.textPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = lyrics,
            onValueChange = { lyrics = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading,
            maxLines = 20,
            colors = editFieldColors(colors)
        )

        // Error
        if (state is EditSongState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (state as EditSongState.Error).message,
                color = colors.errorColor,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save button
        Button(
            onClick = {
                editSongViewModel.updateSong(
                    song.id, title, artist, tone,
                    bpmText.toIntOrNull(), timeSignature, lyrics
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isLoading && !isFetching,
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

@Composable
private fun EditLabel(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = color
    )
}

@Composable
private fun EditSourceChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun editFieldColors(colors: com.example.xound.ui.theme.XoundColorScheme) = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = colors.inputBorder,
    focusedBorderColor = XoundNavy,
    unfocusedContainerColor = colors.inputBackground,
    focusedContainerColor = colors.inputBackground,
    unfocusedTextColor = colors.textPrimary,
    focusedTextColor = colors.textPrimary
)
