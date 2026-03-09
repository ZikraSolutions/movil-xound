package com.example.xound.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xound.data.local.SessionManager
import com.example.xound.ui.theme.XoundNavy
import com.example.xound.ui.theme.XoundYellow


private val XoundCream = Color(0xFFF5F0E8)

@Composable
fun HomeScreen(
    onLogout: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {}
) {
    val userName = SessionManager.getUserName().ifBlank { "Usuario" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XoundCream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 24.dp)
    ) {
        // Logout button
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onLogout,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    tint = XoundNavy,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Greeting
        Text(
            text = "¡Hola de nuevo!,",
            fontSize = 14.sp,
            color = Color.Black
        )
        Text(
            text = userName,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = XoundYellow
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("0", "Canciones", Modifier.weight(1f))
            StatCard("0", "Tonalidades", Modifier.weight(1f))
            StatCard("0", "Eventos", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action grid - fila 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionCard(
                title = "Modo en Vivo",
                subtitle = "Toca ahora",
                icon = Icons.Default.MusicNote,
                backgroundColor = XoundYellow,
                contentColor = XoundNavy,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Nuevo Evento",
                subtitle = "Crear set list",
                icon = Icons.Default.Event,
                backgroundColor = XoundNavy,
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToEvents
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action grid - fila 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionCard(
                title = "Biblioteca",
                subtitle = "Mis canciones",
                icon = Icons.Default.LibraryMusic,
                backgroundColor = XoundNavy,
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToLibrary
            )
            ActionCard(
                title = "Agregar",
                subtitle = "Nueva canción",
                icon = Icons.Default.Add,
                backgroundColor = XoundNavy,
                contentColor = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sección Recientes
        Text(
            text = "RECIENTES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF888888),
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Placeholder cuando no hay canciones
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = XoundNavy)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Agrega tu primera canción",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StatCard(number: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = XoundNavy)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Text(
                text = number,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.height(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
