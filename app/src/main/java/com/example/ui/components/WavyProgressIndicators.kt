package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive Wavy Linear Progress Indicator.
 * - Smooth straight line when progress is below 4% (< 0.04f)
 * - Active flowing wavy animation when progress is between 4% and 90% (0.04f <= progress < 0.90f)
 * - Automatically stops waving and returns to a smooth straight bar when progress reaches 90% and above (>= 0.90f)
 */
@Composable
fun Material3WavyLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    strokeWidth: Dp = 8.dp,
    wavelength: Dp = 22.dp,
    maxWaveAmplitude: Dp = 3.5.dp
) {
    val currentProgress = progress().coerceIn(0f, 1f)
    // Wavy animation is active strictly from 4% to 90%
    val isWavy = currentProgress >= 0.04f && currentProgress < 0.90f

    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (isWavy) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "wavy_linear_amplitude"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wavy_phase_transition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavy_phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(strokeWidth + maxWaveAmplitude * 2 + 2.dp)
    ) {
        val width = size.width
        val centerY = size.height / 2f
        val strokePx = strokeWidth.toPx()
        val capRadius = strokePx / 2f

        // 1. Draw background track
        drawLine(
            color = trackColor,
            start = Offset(capRadius, centerY),
            end = Offset(width - capRadius, centerY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        // 2. Draw active progress segment
        val activeWidth = (width * currentProgress).coerceAtLeast(0f)
        if (activeWidth > strokePx) {
            val waveAmpPx = maxWaveAmplitude.toPx() * amplitudeMultiplier
            val wavelengthPx = wavelength.toPx()

            if (waveAmpPx > 0.1f && activeWidth > strokePx * 2) {
                val path = Path()
                path.moveTo(capRadius, centerY)
                val stepPx = 2f
                var x = capRadius
                val endX = activeWidth - capRadius
                while (x <= endX) {
                    // Smooth envelope at beginning and end so wave transitions smoothly
                    val progressFraction = ((x - capRadius) / (endX - capRadius)).coerceIn(0f, 1f)
                    val envelope = sin(progressFraction * Math.PI).toFloat().coerceIn(0f, 1f)
                    val y = centerY + sin(((x / wavelengthPx) * 2 * Math.PI - phase)).toFloat() * waveAmpPx * envelope
                    path.lineTo(x, y)
                    x += stepPx
                }
                path.lineTo(endX, centerY)

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else {
                // Straight smooth bar when wave is not active (< 4% or >= 90%)
                drawLine(
                    color = color,
                    start = Offset(capRadius, centerY),
                    end = Offset(activeWidth - capRadius, centerY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Material 3 Expressive Wavy Circular Progress Indicator.
 * - Smooth standard circular arc below 4% (< 0.04f)
 * - Wavy oscillating circular arc between 4% and 90% (0.04f <= progress < 0.90f)
 * - Returns to smooth circular arc when reaching 90% and above (>= 0.90f)
 */
@Composable
fun Material3WavyCircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    strokeWidth: Dp = 3.5.dp,
    waveCount: Int = 8,
    maxWaveAmplitude: Dp = 2.5.dp
) {
    val currentProgress = progress().coerceIn(0f, 1f)
    val isWavy = currentProgress >= 0.04f && currentProgress < 0.90f

    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (isWavy) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "wavy_circular_amplitude"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wavy_circular_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "circular_wave_phase"
    )

    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val ampPx = maxWaveAmplitude.toPx() * amplitudeMultiplier
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = (minOf(size.width, size.height) - strokePx - ampPx * 2) / 2f

        // Draw track circle
        drawCircle(
            color = trackColor,
            radius = baseRadius,
            center = center,
            style = Stroke(width = strokePx)
        )

        // Draw active progress arc
        val sweepAngle = 360f * currentProgress
        if (sweepAngle > 0f) {
            if (ampPx > 0.1f && sweepAngle > 12f) {
                val path = Path()
                val startAngleRad = Math.toRadians(-90.0)
                val sweepRad = Math.toRadians(sweepAngle.toDouble())
                val steps = (sweepAngle * 2).toInt().coerceAtLeast(24)

                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val currentAngle = startAngleRad + t * sweepRad
                    val envelope = sin(t * Math.PI).toFloat().coerceIn(0f, 1f)
                    val r = baseRadius + sin(waveCount * currentAngle - phase).toFloat() * ampPx * envelope

                    val px = center.x + r * cos(currentAngle).toFloat()
                    val py = center.y + r * sin(currentAngle).toFloat()

                    if (i == 0) {
                        path.moveTo(px, py)
                    } else {
                        path.lineTo(px, py)
                    }
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else {
                // Smooth circular arc when < 4% or >= 90%
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                    size = Size(baseRadius * 2, baseRadius * 2),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
    }
}
