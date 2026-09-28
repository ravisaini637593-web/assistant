package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.assistant.EngineState
import com.example.ui.theme.CyberPink
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.NeonCyan
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlowingOrb(
    state: EngineState,
    rmsLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Dynamic scale driven by audio rms when listening
    val dynamicAudioMultiplier = remember(rmsLevel, state) {
        if (state == EngineState.LISTENING) {
            1f + (rmsLevel.coerceIn(0f, 10f) / 25f)
        } else if (state == EngineState.SPEAKING) {
            pulseScale
        } else {
            1f
        }
    }

    Box(
        modifier = modifier
            .size(170.dp)
            .semantics {
                contentDescription = when (state) {
                    EngineState.LISTENING -> "Listening to your voice. Tap to stop."
                    EngineState.SPEAKING -> "Assistant is speaking. Tap to interrupt."
                    EngineState.PROCESSING -> "Assistant is processing request."
                    EngineState.AWAITING_CONFIRMATION -> "Waiting for confirmation."
                    EngineState.IDLE -> "Tap to speak to Aura assistant."
                }
            }
            .testTag("assistant_orb_button")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = (size.minDimension / 2.6f) * dynamicAudioMultiplier

            // Outer ethereal glow ring
            if (state == EngineState.LISTENING || state == EngineState.SPEAKING) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.35f),
                            ElectricBlue.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.5f
                    ),
                    radius = baseRadius * 1.5f,
                    center = center
                )

                // Secondary pulsed ripple ring
                drawCircle(
                    color = LuminousViolet.copy(alpha = 0.4f),
                    radius = baseRadius * 1.25f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Rotating gradient core
            val rad = Math.toRadians(rotation.toDouble())
            val gradOffsetX = (cos(rad) * baseRadius).toFloat()
            val gradOffsetY = (sin(rad) * baseRadius).toFloat()

            val orbGradient = when (state) {
                EngineState.LISTENING -> listOf(NeonCyan, ElectricBlue, LuminousViolet, CyberPink)
                EngineState.SPEAKING -> listOf(ElectricBlue, LuminousViolet, CyberPink, NeonCyan)
                EngineState.PROCESSING -> listOf(LuminousViolet, ElectricBlue, NeonCyan)
                EngineState.AWAITING_CONFIRMATION -> listOf(CyberPink, LuminousViolet, ElectricBlue)
                EngineState.IDLE -> listOf(
                    ElectricBlue.copy(alpha = 0.8f),
                    LuminousViolet.copy(alpha = 0.8f),
                    NeonCyan.copy(alpha = 0.6f)
                )
            }

            // Core spherical fluid glow
            drawCircle(
                brush = Brush.linearGradient(
                    colors = orbGradient,
                    start = Offset(center.x - gradOffsetX, center.y - gradOffsetY),
                    end = Offset(center.x + gradOffsetX, center.y + gradOffsetY)
                ),
                radius = baseRadius,
                center = center
            )

            // Inner specular highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f),
                    radius = baseRadius * 0.7f
                ),
                radius = baseRadius * 0.7f,
                center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f)
            )

            // Outer crisp boundary line
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Center Action Icon
        Icon(
            imageVector = when (state) {
                EngineState.SPEAKING -> Icons.Filled.Stop
                EngineState.LISTENING -> Icons.Filled.Mic
                EngineState.PROCESSING -> Icons.Filled.Mic
                EngineState.AWAITING_CONFIRMATION -> Icons.Filled.Mic
                EngineState.IDLE -> Icons.Filled.Mic
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}
