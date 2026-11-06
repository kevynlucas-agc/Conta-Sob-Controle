package br.com.contasobcontrole.presentation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF2E9D68)
private val LightScheme = lightColorScheme(primary = Green, secondary = Color(0xFF397A5B), background = Color(0xFFF5F7F5), surface = Color.White)
private val DarkScheme = darkColorScheme(primary = Color(0xFF71D6A2), secondary = Color(0xFF9ADDB8), background = Color(0xFF101512), surface = Color(0xFF1B241F))

@Composable
fun AppTheme(mode: String = "system", content: @Composable () -> Unit) {
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> androidx.compose.foundation.isSystemInDarkTheme() }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = Typography(), content = content)
}
