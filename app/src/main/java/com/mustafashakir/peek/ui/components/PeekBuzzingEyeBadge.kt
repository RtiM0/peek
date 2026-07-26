package com.mustafashakir.peek.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mustafashakir.peek.ui.theme.PeekAccent
import com.mustafashakir.peek.ui.theme.PeekBorder
import com.mustafashakir.peek.ui.theme.PeekGround
import com.mustafashakir.peek.ui.theme.PeekInk
import com.mustafashakir.peek.ui.theme.PeekTile
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.isActive

private const val BADGE_SIZE_DP = 180
private const val SLOW_LIFT_FREQUENCY = 1.15f
private const val BUZZ_FREQUENCY = 8f
private const val HALF_PI = (PI / 2.0).toFloat()

/**
 * The loading badge from Pencil's `post-loading-orbit.glsl`.
 *
 * API 33+ runs an AGSL translation of the Pencil shader, so the motion, masks, and timing are
 * identical. The Canvas version preserves the same geometry on Android 8–12, where RuntimeShader
 * is unavailable.
 */
@Composable
fun PeekBuzzingEyeBadge(modifier: Modifier = Modifier) {
    val timeSeconds = rememberPencilTimeSeconds()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PencilShaderBadge(timeSeconds, modifier)
    } else {
        PencilCanvasBadge(timeSeconds, modifier)
    }
}

@Composable
private fun rememberPencilTimeSeconds(): Float {
    var timeSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameNanos ->
                timeSeconds = (frameNanos - startNanos) / 1_000_000_000f
            }
        }
    }

    return timeSeconds
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun PencilShaderBadge(timeSeconds: Float, modifier: Modifier) {
    val shader = remember { RuntimeShader(PENCIL_LOADER_SHADER) }
    val brush = remember(shader) { ShaderBrush(shader) }

    Canvas(modifier = modifier.size(BADGE_SIZE_DP.dp)) {
        shader.setFloatUniform("u_resolution", size.width, size.height)
        shader.setFloatUniform("u_time", timeSeconds)
        drawRect(brush)
    }
}

@Composable
private fun PencilCanvasBadge(timeSeconds: Float, modifier: Modifier) {
    Canvas(modifier = modifier.size(BADGE_SIZE_DP.dp)) {
        drawCanvasFallback(timeSeconds)
    }
}

private fun DrawScope.drawCanvasFallback(timeSeconds: Float) {
    val unit = size.minDimension
    val center = Offset(size.width / 2f, size.height / 2f)
    val slowLift = 0.050f * sin(timeSeconds * SLOW_LIFT_FREQUENCY)
    val tinyBuzz = 0.005f * sin(timeSeconds * BUZZ_FREQUENCY)
    val logoCenter = center.copy(y = center.y - (slowLift + tinyBuzz) * unit)
    val topPulse = 0.30f + 0.55f * (0.5f + 0.5f * sin(timeSeconds * SLOW_LIFT_FREQUENCY))
    val bottomPulse = 0.30f + 0.55f * (0.5f - 0.5f * sin(timeSeconds * SLOW_LIFT_FREQUENCY))
    val sideBuzz = 0.45f + 0.35f * sin(timeSeconds * BUZZ_FREQUENCY)

    drawRect(PeekGround)
    drawDash(center, 0f, -0.345f, 0.050f, 0.008f, unit, PeekBorder, topPulse)
    drawDash(center, 0f, -0.300f, 0.025f, 0.006f, unit, PeekTile, topPulse)
    drawDash(center, 0f, 0.345f, 0.050f, 0.008f, unit, PeekBorder, bottomPulse)
    drawDash(center, 0f, 0.300f, 0.025f, 0.006f, unit, PeekTile, bottomPulse)

    drawCircle(color = PeekTile, radius = 0.245f * unit, center = logoCenter)
    drawEyeOutline(logoCenter, unit)
    drawCircle(
        color = PeekInk,
        radius = 0.036f * unit,
        center = logoCenter.copy(y = logoCenter.y - 0.012f * sin(timeSeconds * SLOW_LIFT_FREQUENCY) * unit),
    )
    drawDash(logoCenter, -0.300f, 0f, 0.020f, 0.006f, unit, PeekAccent, sideBuzz)
    drawDash(
        logoCenter,
        0.300f,
        0f,
        0.020f,
        0.006f,
        unit,
        PeekAccent,
        1f - 0.45f * sin(timeSeconds * BUZZ_FREQUENCY),
    )
}

private fun DrawScope.drawEyeOutline(center: Offset, unit: Float) {
    val halfWidth = 0.145f
    val steps = 48
    fun lidHeight(nx: Float) = 0.065f * sin((nx + 1f) * HALF_PI)

    val path = Path().apply {
        for (index in 0..steps) {
            val nx = -1f + 2f * index / steps
            val x = center.x + nx * halfWidth * unit
            val y = center.y - lidHeight(nx) * unit
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
        for (index in steps downTo 0) {
            val nx = -1f + 2f * index / steps
            lineTo(center.x + nx * halfWidth * unit, center.y + lidHeight(nx) * unit)
        }
    }
    drawPath(path, color = PeekInk, style = Stroke(width = 0.020f * unit, cap = StrokeCap.Butt))
}

private fun DrawScope.drawDash(
    center: Offset,
    offsetX: Float,
    offsetY: Float,
    halfWidth: Float,
    halfHeight: Float,
    unit: Float,
    color: androidx.compose.ui.graphics.Color,
    alpha: Float,
) {
    val topLeft = Offset(center.x + (offsetX - halfWidth) * unit, center.y + (offsetY - halfHeight) * unit)
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = Size(halfWidth * 2f * unit, halfHeight * 2f * unit),
        cornerRadius = CornerRadius(halfHeight * unit, halfHeight * unit),
        alpha = alpha.coerceIn(0f, 1f),
    )
}

private const val PENCIL_LOADER_SHADER = """
    uniform float2 u_resolution;
    uniform float u_time;

    const float PI = 3.14159265359;

    float pixel() {
        return 1.0 / min(u_resolution.x, u_resolution.y);
    }

    float circleFill(float2 p, float radius) {
        return 1.0 - smoothstep(radius - pixel(), radius + pixel(), length(p));
    }

    float sdRoundBox(float2 p, float2 halfSize, float radius) {
        float2 q = abs(p) - halfSize + radius;
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    }

    float boxFill(float2 p, float2 halfSize, float radius) {
        float d = sdRoundBox(p, halfSize, radius);
        return 1.0 - smoothstep(-pixel(), pixel(), d);
    }

    float3 paint(float3 color, float3 layerColor, float mask) {
        return mix(color, layerColor, clamp(mask, 0.0, 1.0));
    }

    half4 main(float2 fragCoord) {
        float2 p = float2(
            fragCoord.x - 0.5 * u_resolution.x,
            0.5 * u_resolution.y - fragCoord.y
        ) / min(u_resolution.x, u_resolution.y);
        float t = u_time;

        float3 ground = float3(0.961, 0.953, 0.933);
        float3 ink = float3(0.106, 0.227, 0.157);
        float3 accent = float3(0.176, 0.369, 0.227);
        float3 sage = float3(0.784, 0.859, 0.737);
        float3 pale = float3(0.839, 0.867, 0.816);
        float3 color = ground;

        float slowLift = 0.050 * sin(t * 1.15);
        float tinyBuzz = 0.005 * sin(t * 8.0);
        float2 motion = float2(0.0, slowLift + tinyBuzz);
        float2 logoP = p - motion;

        float topPulse = 0.30 + 0.55 * (0.5 + 0.5 * sin(t * 1.15));
        float bottomPulse = 0.30 + 0.55 * (0.5 - 0.5 * sin(t * 1.15));

        float topDash = boxFill(p - float2(0.0, 0.345), float2(0.050, 0.008), 0.008);
        float topTick = boxFill(p - float2(0.0, 0.300), float2(0.025, 0.006), 0.006);
        color = paint(color, pale, topDash * topPulse);
        color = paint(color, sage, topTick * topPulse);

        float bottomDash = boxFill(p + float2(0.0, 0.345), float2(0.050, 0.008), 0.008);
        float bottomTick = boxFill(p + float2(0.0, 0.300), float2(0.025, 0.006), 0.006);
        color = paint(color, pale, bottomDash * bottomPulse);
        color = paint(color, sage, bottomTick * bottomPulse);

        float badge = circleFill(logoP, 0.245);
        color = paint(color, sage, badge);

        float halfWidth = 0.145;
        float nx = clamp(logoP.x / halfWidth, -1.0, 1.0);
        float lidHeight = 0.065 * sin((nx + 1.0) * PI * 0.5);
        float eyeDistance = abs(abs(logoP.y) - lidHeight);
        float eyeClip = 1.0 - smoothstep(halfWidth - 0.008, halfWidth + 0.004, abs(logoP.x));
        float eye = (1.0 - smoothstep(0.010, 0.014, eyeDistance)) * eyeClip;
        color = paint(color, ink, eye);

        float2 gaze = float2(0.0, 0.012 * sin(t * 1.15));
        float pupil = circleFill(logoP - gaze, 0.036);
        color = paint(color, ink, pupil);

        float sideBuzz = 0.45 + 0.35 * sin(t * 8.0);
        float leftTick = boxFill(logoP + float2(0.300, 0.0), float2(0.020, 0.006), 0.006);
        float rightTick = boxFill(logoP - float2(0.300, 0.0), float2(0.020, 0.006), 0.006);
        color = paint(color, accent, leftTick * sideBuzz);
        color = paint(color, accent, rightTick * (1.0 - 0.45 * sin(t * 8.0)));

        return half4(color, 1.0);
    }
"""
