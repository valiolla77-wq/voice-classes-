package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoPrimary
import kotlin.random.Random

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    isRecording: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 28
    val animatedAmp by animateFloatAsState(
        targetValue = if (isRecording && !isPaused) amplitude.coerceIn(0.08f, 1f) else 0.05f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "waveform_amp"
    )

    // Pre-calculate pseudo-random heights multipliers for organic audio wave look
    val randomFactors = remember {
        List(barCount) { index ->
            val centerDist = kotlin.math.abs(index - (barCount / 2f)) / (barCount / 2f)
            val bellCurve = (1f - centerDist * 0.5f).coerceIn(0.4f, 1f)
            bellCurve * (0.5f + Random.nextFloat() * 0.5f)
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            IndigoPrimary,
            CyanAccent
        )
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val width = size.width
        val height = size.height
        val totalSpacing = width * 0.35f
        val barWidth = (width - totalSpacing) / barCount
        val barGap = totalSpacing / (barCount - 1)

        val centerY = height / 2f

        for (i in 0 until barCount) {
            val factor = randomFactors[i]
            val waveHeight = if (isRecording && !isPaused) {
                (height * 0.85f * animatedAmp * factor).coerceAtLeast(6f)
            } else {
                6f
            }

            val x = i * (barWidth + barGap)
            val topY = centerY - (waveHeight / 2f)

            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, topY),
                size = Size(barWidth, waveHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
