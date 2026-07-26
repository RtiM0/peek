package com.mustafashakir.peek.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PeekColorScheme = lightColorScheme(
    primary = PeekAccent,
    onPrimary = Color.White,
    background = PeekGround,
    onBackground = PeekInk,
    surface = PeekGround,
    onSurface = PeekInk,
    outline = PeekBorder,
)

@Composable
fun PeekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PeekColorScheme,
        typography = Typography(),
        content = content,
    )
}
