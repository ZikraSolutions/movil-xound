package com.example.xound.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xound.R
import com.example.xound.data.local.SessionManager
import com.example.xound.data.network.RetrofitClient
import com.example.xound.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun RoleSelectionScreen(
    onSelectAdmin: () -> Unit,
    onSelectMusician: () -> Unit
) {
    val colors = LocalXoundColors.current
    val isDark = ThemeState.isDark(isSystemInDarkTheme())
    val userName = SessionManager.getUserName().ifBlank { "Usuario" }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var selectedRole by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submitCode() {
        if (code.isBlank()) {
            errorMessage = "Ingresa el código de acceso"
            return
        }
        focusManager.clearFocus()
        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                if (selectedRole == "musician") {
                    RetrofitClient.apiService.joinBand(mapOf("inviteCode" to code.trim().uppercase()))
                    SessionManager.setUserMode("musician")
                    onSelectMusician()
                } else {
                    RetrofitClient.apiService.useAdminCode(mapOf("code" to code.trim().uppercase()))
                    SessionManager.setUserMode("admin")
                    onSelectAdmin()
                }
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string() ?: ""
                when {
                    body.contains("Ya eres administrador", ignoreCase = true) -> {
                        SessionManager.setUserMode("admin")
                        onSelectAdmin()
                        return@launch
                    }
                    body.contains("ya es miembro", ignoreCase = true) -> {
                        SessionManager.setUserMode("musician")
                        onSelectMusician()
                        return@launch
                    }
                    body.contains("inválido", ignoreCase = true) -> errorMessage = "Código inválido"
                    e.code() == 403 -> errorMessage = "No tienes permisos"
                    e.code() == 400 -> errorMessage = "Código inválido"
                    else -> errorMessage = "Error: ${e.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión"
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) colors.screenBackground else Color.White)
    ) {
        // Decorative circle
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = (-40).dp)
                .size(160.dp)
                .background(XoundYellow, CircleShape)
        )

        // Wave
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val h = size.height
            val wavePath = Path().apply {
                moveTo(-0.35f * w, 0.53f * h)
                cubicTo(
                    0.023f * w, 1.06f * h,
                    0.695f * w, 0.106f * h,
                    1.05f * w, 0.636f * h
                )
                lineTo(1.05f * w, 1.167f * h)
                lineTo(-0.35f * w, 1.167f * h)
                close()
            }
            drawPath(wavePath, XoundNavy)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "XOUND Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(80.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "¡Bienvenido, $userName!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (selectedRole == null) "¿Cómo deseas usar XOUND?"
                       else "Ingresa tu código de acceso",
                fontSize = 15.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (selectedRole == null) {
                // Option: Join a band
                RoleCard(
                    title = "Unirse a una Banda",
                    description = "Ingresa el código que te dio el administrador de tu banda.",
                    icon = Icons.Default.Groups,
                    backgroundColor = XoundYellow,
                    contentColor = XoundNavy,
                    onClick = { selectedRole = "musician" }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Option: Be admin
                RoleCard(
                    title = "Ser Administrador",
                    description = "Ingresa el código proporcionado por el super administrador.",
                    icon = Icons.Default.AdminPanelSettings,
                    backgroundColor = XoundNavy,
                    contentColor = Color.White,
                    onClick = { selectedRole = "admin" }
                )
            } else {
                // Code input
                val roleLabel = if (selectedRole == "musician") "Código de banda" else "Código de admin"
                val roleColor = if (selectedRole == "musician") XoundYellow else XoundNavy
                val roleIcon = if (selectedRole == "musician") Icons.Default.Groups else Icons.Default.AdminPanelSettings

                // Selected role indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = roleColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = roleIcon,
                            contentDescription = null,
                            tint = if (selectedRole == "musician") XoundNavy else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (selectedRole == "musician") "Unirse a una Banda" else "Ser Administrador",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedRole == "musician") XoundNavy else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Code text field
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.uppercase()
                        errorMessage = null
                    },
                    label = { Text(roleLabel) },
                    placeholder = { Text("Ej: A1B2C3D4") },
                    singleLine = true,
                    isError = errorMessage != null,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitCode() }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XoundYellow,
                        cursorColor = XoundYellow,
                        focusedLabelColor = XoundYellow
                    )
                )

                // Error message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit button
                Button(
                    onClick = { submitCode() },
                    enabled = !isLoading && code.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XoundYellow,
                        contentColor = XoundNavy
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = XoundNavy,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Confirmar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Back button
                TextButton(
                    onClick = {
                        selectedRole = null
                        code = ""
                        errorMessage = null
                    },
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Volver",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        contentColor.copy(alpha = 0.15f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
