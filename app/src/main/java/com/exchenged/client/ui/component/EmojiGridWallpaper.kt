package com.exchenged.client.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PhysicsController(private val scope: CoroutineScope) {
    private val _physicsState = Animatable(Offset(0f, 1f), Offset.VectorConverter)
    val physicsState by derivedStateOf { _physicsState.value }

    fun applyPhysics(velocity: Float) {
        val targetSkew = (velocity / 8000f).coerceIn(-0.25f, 0.25f)
        val targetScaleY = 1f + (abs(velocity) / 4000f).coerceIn(0f, 0.5f)

        scope.launch {
            _physicsState.animateTo(
                targetValue = Offset(targetSkew, targetScaleY),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    fun reset() {
        scope.launch {
            _physicsState.animateTo(
                Offset(0f, 1f),
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
            )
        }
    }
}

@Composable
fun rememberPhysicsController(scrollState: LazyListState): PhysicsController {
    val scope = rememberCoroutineScope()
    val controller = remember { PhysicsController(scope) }

    LaunchedEffect(scrollState) {
        var lastOffset = 0
        var lastTime = System.currentTimeMillis()

        snapshotFlow { 
            scrollState.firstVisibleItemIndex * 1000 + scrollState.firstVisibleItemScrollOffset 
        }
        .distinctUntilChanged()
        .collect { currentOffset ->
            val currentTime = System.currentTimeMillis()
            val timeDelta = currentTime - lastTime
            if (timeDelta > 0) {
                val velocity = ((currentOffset - lastOffset).toFloat() / timeDelta) * 1000f
                controller.applyPhysics(velocity)
            }
            lastOffset = currentOffset
            lastTime = currentTime
        }
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {
            controller.reset()
        }
    }

    return controller
}

@Composable
fun EmojiGridWallpaper(
    emojis: List<String>,
    gridSpacing: Float = 200f,
    emojiSize: Float = 100f
) {
    var touchOffset by remember { mutableStateOf(Offset(-1000f, -1000f)) }

    val animatedTouchOffset by animateOffsetAsState(
        targetValue = touchOffset,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ -> touchOffset = change.position },
                    onDragEnd = { touchOffset = Offset(-1000f, -1000f) }
                )
            }
    ) {
        for (i in 0 until 50) {
            val x = (i % 6) * gridSpacing
            val y = (i / 6) * gridSpacing

            EmojiItem(
                emoji = emojis[i % emojis.size],
                position = Offset(x, y),
                touchOffset = animatedTouchOffset,
                emojiSize = emojiSize
            )
        }
    }
}

@Composable
fun EmojiItem(emoji: String, position: Offset, touchOffset: Offset, emojiSize: Float) {
    val dx = position.x - touchOffset.x
    val dy = position.y - touchOffset.y
    val dist = sqrt(dx * dx + dy * dy)
    val influence = (1f - (dist / 300f).coerceIn(0f, 1f))

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = position.x + (dx * influence * 0.5f)
                translationY = position.y + (dy * influence * 0.5f)
                scaleX = 1f + influence * 0.5f
                scaleY = 1f + influence * 0.5f
                rotationZ = influence * 20f * (if (dx > 0) 1f else -1f)
            }
    ) {
        Text(text = emoji, fontSize = (emojiSize / 2).sp)
    }
}

@Composable
fun EmojiItem(
    emoji: String,
    position: Offset,
    touchOffset: Offset,
    globalPhysics: Offset,
    textStyle: TextStyle,
    radiusPx: Float,
    emojiSize: Float
) {
    val dx = position.x - touchOffset.x
    val dy = position.y - touchOffset.y
    val distance = sqrt(dx * dx + dy * dy)

    val influence = if (distance < radiusPx) (1f - distance / radiusPx).coerceIn(0f, 1f) else 0f
    val localScale = 1f + (influence * 0.5f)
    val localSkewX = (dx / radiusPx) * influence * 0.7f
    val finalSkewX = globalPhysics.x + localSkewX

    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.roundToInt() - (emojiSize/2).roundToInt(), position.y.roundToInt() - (emojiSize/2).roundToInt()) }
            .graphicsLayer {
                scaleX = localScale
                scaleY = globalPhysics.y * localScale
                translationX = localSkewX * 50f
                rotationZ = finalSkewX * 20f
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            style = textStyle,
            textAlign = TextAlign.Center
        )
    }
}
