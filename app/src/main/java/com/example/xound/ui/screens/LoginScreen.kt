package com.example.xound.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xound.R
import com.example.xound.ui.theme.*
import com.example.xound.ui.viewmodel.AuthUiState
import com.example.xound.ui.viewmodel.AuthViewModel

// Usada también por RegisterScreen
@Composable
fun XoundLogo() {
    val isDark = ThemeState.isDark(isSystemInDarkTheme())
    val logoColor = if (isDark) Color.White else XoundNavy
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "X",
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            color = logoColor,
            letterSpacing = (-1).sp
        )
        Canvas(modifier = Modifier.size(38.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.minDimension / 2f
            val innerR = outerR * 0.38f
            val holeR = outerR * 0.09f
            drawCircle(color = if (isDark) Color.White else XoundNavy, radius = outerR, center = androidx.compose.ui.geometry.Offset(cx, cy))
            drawCircle(color = XoundYellow, radius = innerR, center = androidx.compose.ui.geometry.Offset(cx, cy))
            drawCircle(color = if (isDark) Color.White else XoundNavy, radius = holeR, center = androidx.compose.ui.geometry.Offset(cx, cy))
        }
        Text(
            text = "UND",
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            color = logoColor,
            letterSpacing = (-1).sp
        )
    }
}

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val colors = LocalXoundColors.current
    val isDark = ThemeState.isDark(isSystemInDarkTheme())
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val uiState by authViewModel.uiState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading

    // Navegar cuando el login es exitoso
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
            authViewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) colors.screenBackground else Color.White)
    ) {
        // Círculo naranja — detrás de la ola
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-40).dp, y = 20.dp)
                .size(120.dp)
                .background(XoundYellow, CircleShape)
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // Header: ola navy + logo.png
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val wavePath = Path().apply {
                        moveTo(-0.15f * w, 0.34f * h)
                        cubicTo(
                            0.162f * w, 0.60f * h,
                            0.717f * w, 0.016f * h,
                            1.15f * w,  0.34f * h
                        )
                        lineTo(1.15f * w, 0f)
                        lineTo(-0.15f * w, 0f)
                        close()
                    }
                    drawPath(wavePath, XoundNavy)
                }

                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "XOUND Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.6f)
                        .height(100.dp)
                )
            }

            // Formulario
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .padding(top = 20.dp)
            ) {
                Text(text = "Ingresa tu cuenta", fontSize = 14.sp, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Username", color = colors.textHint, fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
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

                Spacer(modifier = Modifier.height(15.dp))

                Text(text = "Ingresa tu contraseña", fontSize = 14.sp, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Contraseña", color = colors.textHint, fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = if (passwordVisible) "Ocultar" else "Ver",
                                fontSize = 12.sp,
                                color = if (isDark) XoundYellow else XoundNavy
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = colors.inputBorder,
                        focusedBorderColor = XoundNavy,
                        unfocusedContainerColor = colors.inputBackground,
                        focusedContainerColor = colors.inputBackground,
                        unfocusedTextColor = colors.textPrimary,
                        focusedTextColor = colors.textPrimary
                    )
                )

                // Mensaje de error
                if (uiState is AuthUiState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (uiState as AuthUiState.Error).message,
                        color = colors.errorColor,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { authViewModel.login(username, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = XoundNavy),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = "Iniciar sesión",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(25.dp))

                TextButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("¿No tienes cuenta? ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Regístrate")
                            }
                        },
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    XOUNDTheme {
        LoginScreen(onNavigateToRegister = {})
    }
}
