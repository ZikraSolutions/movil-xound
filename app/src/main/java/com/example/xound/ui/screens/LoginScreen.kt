package com.example.xound.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.example.xound.R
import com.example.xound.ui.theme.XOUNDTheme
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow

// Usada también por RegisterScreen
@Composable
fun XoundLogo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "X",
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            color = XoundNavy,
            letterSpacing = (-1).sp
        )
        Canvas(modifier = Modifier.size(38.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.minDimension / 2f
            val innerR = outerR * 0.38f
            val holeR = outerR * 0.09f
            drawCircle(color = XoundNavy, radius = outerR, center = androidx.compose.ui.geometry.Offset(cx, cy))
            drawCircle(color = XoundYellow, radius = innerR, center = androidx.compose.ui.geometry.Offset(cx, cy))
            drawCircle(color = XoundNavy, radius = holeR, center = androidx.compose.ui.geometry.Offset(cx, cy))
        }
        Text(
            text = "UND",
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            color = XoundNavy,
            letterSpacing = (-1).sp
        )
    }
}

@Composable
fun LoginScreen(onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Círculo naranja — se dibuja primero para quedar detrás de la ola
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
                // Ola navy (misma curva que el SVG de React Native)
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
6
                // Logo XOUND (imagen)
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
                Text(
                    text = "Ingresa tu cuenta",
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email", color = Color(0xFF999999), fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE5E5E5),
                        focusedBorderColor = XoundNavy,
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedContainerColor = Color(0xFFFAFAFA)
                    )
                )

                Spacer(modifier = Modifier.height(15.dp))

                Text(
                    text = "Ingresa tu contraseña",
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Contraseña", color = Color(0xFF999999), fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = if (passwordVisible) "Ocultar" else "Ver",
                                fontSize = 12.sp,
                                color = XoundNavy
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE5E5E5),
                        focusedBorderColor = XoundNavy,
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedContainerColor = Color(0xFFFAFAFA)
                    )
                )

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = { /* TODO: lógica de login */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = XoundNavy),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Iniciar sesión",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
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
                        color = Color.Black,
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
