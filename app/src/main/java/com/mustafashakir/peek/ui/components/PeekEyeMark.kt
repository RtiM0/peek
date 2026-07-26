package com.mustafashakir.peek.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustafashakir.peek.R
import com.mustafashakir.peek.ui.theme.Geist
import com.mustafashakir.peek.ui.theme.PeekAccent
import com.mustafashakir.peek.ui.theme.PeekInk

@Composable
fun PeekLockup(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        PeekEyeMark(Modifier.size(width = 20.dp, height = 12.dp))
        Text(
            text = stringResource(R.string.peek_wordmark),
            color = PeekInk,
            style = TextStyle(fontFamily = Geist, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        )
    }
}

@Composable
fun PeekEyeMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0.8f, size.height / 2f)
            quadraticTo(size.width / 2f, -1.5f, size.width - 0.8f, size.height / 2f)
            quadraticTo(size.width / 2f, size.height + 1.5f, 0.8f, size.height / 2f)
            close()
        }
        drawPath(path, color = PeekAccent, style = Stroke(width = 1.5.dp.toPx()))
        drawOval(
            color = PeekAccent,
            topLeft = Offset(size.width / 2f - 2.dp.toPx(), size.height / 2f - 2.dp.toPx()),
            size = Size(4.dp.toPx(), 4.dp.toPx()),
        )
    }
}
