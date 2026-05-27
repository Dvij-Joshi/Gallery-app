package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PinchZoomImage(
    imageUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    // Spring-animated states for ultra-fluid 120 FPS transitions
    val scale = remember { Animatable(1f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // Swipe down to dismiss gesture state (active when scale is zoomed out)
    var isSwipingDown by remember { mutableStateOf(false) }
    var dismissFraction by remember { mutableStateOf(0f) }

    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun triggerHapticFeedback(v: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            v.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        } else {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Handle Tap & Double Tap to zoom natively with beautiful spring transitions
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        triggerHapticFeedback(view)
                        coroutineScope.launch {
                            if (scale.value > 1.05f) {
                                // Dynamic return to baseline
                                launch { scale.animateTo(1f, springSpec) }
                                launch { offsetX.animateTo(0f, springSpec) }
                                launch { offsetY.animateTo(0f, springSpec) }
                            } else {
                                // Zoom into specific centroid
                                launch { scale.animateTo(2.5f, springSpec) }
                                // Center zoom focusing on double-tap region
                                val targetX = (size.width / 2f - tapOffset.x) * 1.5f
                                val targetY = (size.height / 2f - tapOffset.y) * 1.5f
                                launch { offsetX.animateTo(targetX, springSpec) }
                                launch { offsetY.animateTo(targetY, springSpec) }
                            }
                        }
                    },
                    onTap = {
                        // Regular single tap can trigger standard overlay toggles (handled by viewer)
                    }
                )
            }
            .pointerInput(Unit) {
                // Multi-finger zoom/panning & swipe-down-to-dismiss gestures
                detectTransformGestures { _, pan, zoom, _ ->
                    coroutineScope.launch {
                        // 1. Calculate active scale update
                        val newScale = (scale.value * zoom).coerceIn(0.5f, 5.0f)
                        scale.snapTo(newScale)

                        // 2. Gesture zooming/panning calculations
                        if (newScale > 1.01f) {
                            isSwipingDown = false
                            // Pan relative to scale factors
                            val newX = offsetX.value + pan.x * scale.value
                            val newY = offsetY.value + pan.y * scale.value
                            
                            // Visual bound restrictions (prevent infinitely dragging ofscreen)
                            val maxPanX = size.width * (newScale - 1f) / 2f
                            val maxPanY = size.height * (newScale - 1f) / 2f
                            
                            offsetX.snapTo(newX.coerceIn(-maxPanX, maxPanX))
                            offsetY.snapTo(newY.coerceIn(-maxPanY, maxPanY))
                        } else {
                            // 3. Swipe down to dismiss gesture (when scale is normal/default)
                            if (pan.y > 0 || isSwipingDown) {
                                isSwipingDown = true
                                val dragY = offsetY.value + pan.y
                                offsetY.snapTo(dragY)
                                
                                // Drag visual compression factor for background alpha fading
                                dismissFraction = (dragY / 400.0f).coerceIn(0f, 1f)
                                
                                if (dragY > 450f) {
                                    triggerHapticFeedback(view)
                                    onDismiss()
                                }
                            } else {
                                // Standard horizontal panning limits when bounds are normal
                                val dragX = offsetX.value + pan.x
                                offsetX.snapTo(dragX.coerceIn(-100f, 100f))
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                // Wait for touch releases to spring things back to center if below limits
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val allReleased = event.changes.all { !it.pressed }
                        if (allReleased) {
                            coroutineScope.launch {
                                // If scale was pinched below normal (less than 1), spring back
                                if (scale.value < 1.0f) {
                                    launch { scale.animateTo(1f, springSpec) }
                                }
                                
                                // Slide back if released before fully dismissing
                                if (offsetY.value != 0f && scale.value <= 1.01f) {
                                    launch { offsetY.animateTo(0f, springSpec) }
                                    launch { offsetX.animateTo(0f, springSpec) }
                                }
                                isSwipingDown = false
                                dismissFraction = 0f
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        offsetX.value.roundToInt(),
                        offsetY.value.roundToInt()
                    )
                }
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    alpha = (1f - dismissFraction).coerceIn(0.2f, 1f)
                ),
            alignment = Alignment.Center
        )
    }
}
