package com.exchenged.client.ui.component

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import android.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.verticalScroll
import androidx.core.content.res.ResourcesCompat
import com.exchenged.client.R
import com.google.gson.Gson
import com.exchenged.client.viewmodel.SettingsViewmodel
import kotlin.math.sqrt

enum class PatternType {
    Mosaic, Lotus, Blocks, Sprinkles
}

val NotoEmojiFontFamily = FontFamily(
    Font(R.font.notoemoji_light, FontWeight.Light),
    Font(R.font.notoemoji_regular, FontWeight.Normal),
    Font(R.font.notoemoji_medium, FontWeight.Medium),
    Font(R.font.notoemoji_semibold, FontWeight.SemiBold),
    Font(R.font.notoemoji_bold, FontWeight.Bold)
)

data class WallpaperState(
    val emojis: List<String> = listOf("🌸", "✨", "🍃"),
    val patternType: PatternType = PatternType.Mosaic,
    val density: Float = 0.5f, // 0.1f to 1.0f
    val emojiSize: Float = 0.5f, // 0.1f to 1.0f
    val backgroundColor: Color = Color(0xFFF0F4FF),
    val backgroundBrushId: Int = -1, // -1 for none, index for gradients
    val outlineColor: Color = Color.Black,
    val strokeWidth: Float = 3f,
    val fontWeight: FontWeight = FontWeight.Normal
) {
    val backgroundBrush: Brush?
        get() = if (backgroundBrushId >= 0 && backgroundBrushId < Gradients.size) Gradients[backgroundBrushId].first else null
}

val Gradients = listOf(
    Brush.verticalGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))) to "Пурпурный",
    Brush.verticalGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d))) to "Зеленый",
    Brush.verticalGradient(listOf(Color(0xFFff9966), Color(0xFFff5e62))) to "Оранжевый",
    Brush.horizontalGradient(listOf(Color(0xFF2193b0), Color(0xFF6dd5ed))) to "Циан",
    Brush.linearGradient(listOf(Color(0xFFee0979), Color(0xFFff6a00))) to "Розовый",
    Brush.verticalGradient(listOf(Color(0xFFFF5F6D), Color(0xFFFFC371))) to "Закат",
    Brush.verticalGradient(listOf(Color(0xFF2193B0), Color(0xFF6DD5ED))) to "Океан",
    Brush.linearGradient(listOf(Color(0xFF00B4DB), Color(0xFF0083B0))) to "Темно-синий",
    Brush.horizontalGradient(listOf(Color(0xFFa8ff78), Color(0xFF78ffd6))) to "Зелень",
    Brush.linearGradient(listOf(Color(0xFF74ebd5), Color(0xFFACB6E5))) to "Утро",
    Brush.verticalGradient(listOf(Color(0xFFC9FFBF), Color(0xFFFFAFBD))) to "Пастель",
    Brush.verticalGradient(listOf(Color(0xFF833ab4), Color(0xFFfd1d1d), Color(0xFFfcb045))) to "Инстаграм"
)

@Composable
fun rememberNotoEmojiTypeface(weight: FontWeight): Typeface? {
    val context = LocalContext.current
    return remember(weight) {
        val fontRes = when (weight) {
            FontWeight.Light -> R.font.notoemoji_light
            FontWeight.Medium -> R.font.notoemoji_medium
            FontWeight.SemiBold -> R.font.notoemoji_semibold
            FontWeight.Bold -> R.font.notoemoji_bold
            else -> R.font.notoemoji_regular
        }
        ResourcesCompat.getFont(context, fontRes)
    }
}

@Composable
fun WallpaperEngine(state: WallpaperState) {
    val typeface = rememberNotoEmojiTypeface(state.fontWeight) ?: Typeface.DEFAULT
    
    val modifier = state.backgroundBrush?.let {
        Modifier.background(it)
    } ?: Modifier.background(state.backgroundColor)

    Box(modifier = Modifier.fillMaxSize().then(modifier)) {
        EmojiGridWallpaperCanvas(
            emojis = state.emojis,
            patternType = state.patternType,
            gridSpacing = 200f / state.density,
            emojiSize = 200f * state.emojiSize,
            outlineColor = state.outlineColor,
            strokeWidth = state.strokeWidth,
            typeface = typeface
        )
    }
}

@Composable
fun EmojiWallpaperScreen(viewmodel: SettingsViewmodel, onBack: () -> Unit) {
    val settingsState by viewmodel.settingsState.collectAsState()
    val gson = remember { Gson() }
    
    var state by remember { 
        mutableStateOf(
            if (settingsState.emojiWorkshopConfig.isNotEmpty()) {
                try {
                    gson.fromJson(settingsState.emojiWorkshopConfig, WallpaperState::class.java)
                } catch (e: Exception) {
                    WallpaperState()
                }
            } else {
                WallpaperState()
            }
        )
    }
    
    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperEngine(state = state)

        // UI Settings Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
                Button(onClick = { showSettings = true }) {
                    Text(stringResource(R.string.configure_emoji))
                }
            }
        }

        if (showSettings) {
            EmojiSettingsBottomSheet(
                state = state,
                onStateChange = { state = it },
                onSave = {
                    val configJson = gson.toJson(state)
                    viewmodel.setEmojiWorkshopConfig(configJson)
                    showSettings = false
                },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
fun EmojiGridWallpaperCanvas(
    emojis: List<String>,
    patternType: PatternType = PatternType.Mosaic,
    gridSpacing: Float = 200f,
    emojiSize: Float = 100f,
    outlineColor: Color = Color.Black,
    strokeWidth: Float = 5f,
    typeface: Typeface = Typeface.DEFAULT
) {
    // Состояние касания
    var touchOffset by remember { mutableStateOf(Offset(-1000f, -1000f)) }

    val paint = remember(emojiSize, outlineColor, strokeWidth, typeface) {
        Paint().apply {
            this.typeface = typeface
            textSize = emojiSize
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = outlineColor.toArgb()
            colorFilter = PorterDuffColorFilter(outlineColor.toArgb(), PorterDuff.Mode.SRC_IN)
        }
    }

    val pathCache = remember(paint) { mutableMapOf<String, Path>() }
    val boundsCache = remember(paint) { mutableMapOf<String, Rect>() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ -> touchOffset = change.position },
                    onDragEnd = { touchOffset = Offset(-1000f, -1000f) }
                )
            }
    ) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            
            // Adjust rows/cols based on pattern
            val rowSpacing = when(patternType) {
                PatternType.Lotus -> gridSpacing * 0.866f
                else -> gridSpacing
            }
            
            val columns = (size.width / gridSpacing).toInt() + 2
            val rows = (size.height / rowSpacing).toInt() + 2

            for (row in -1..rows) {
                for (col in -1..columns) {
                    var x = col * gridSpacing
                    var y = row * rowSpacing
                    
                    val emojiIndex = when(patternType) {
                        PatternType.Mosaic -> (row + col).coerceAtLeast(0) % emojis.size
                        PatternType.Lotus -> {
                            if (row % 2 != 0) x += gridSpacing / 2f
                            (row + col).coerceAtLeast(0) % emojis.size
                        }
                        PatternType.Blocks -> {
                            val blockRow = row / 2
                            val blockCol = col / 2
                            (blockRow + blockCol).coerceAtLeast(0) % emojis.size
                        }
                        PatternType.Sprinkles -> {
                            val seed = (row + 100) * 1000 + (col + 100)
                            val random = java.util.Random(seed.toLong())
                            x += (random.nextFloat() - 0.5f) * gridSpacing * 0.7f
                            y += (random.nextFloat() - 0.5f) * rowSpacing * 0.7f
                            (row + col + seed).coerceAtLeast(0) % emojis.size
                        }
                    }
                    
                    val emoji = emojis[emojiIndex % emojis.size]

                    // Физика: расстояние до пальца
                    val dx = x - touchOffset.x
                    val dy = y - touchOffset.y
                    val dist = sqrt(dx * dx + dy * dy)
                    val influence = (1f - (dist / 300f).coerceIn(0f, 1f))

                    val path = pathCache.getOrPut(emoji) {
                        Path().apply { paint.getTextPath(emoji, 0, emoji.length, 0f, 0f, this) }
                    }
                    val bounds = boundsCache.getOrPut(emoji) {
                        Rect().apply { paint.getTextBounds(emoji, 0, emoji.length, this) }
                    }

                    nativeCanvas.save()
                    nativeCanvas.translate(x, y - bounds.centerY())

                    // Эффект прогибания под пальцем
                    if (influence > 0) {
                        nativeCanvas.skew(influence * 0.5f * (if (dx > 0) 1f else -1f), 0f)
                        nativeCanvas.scale(1f + influence * 0.3f, 1f + influence * 0.3f)
                    }

                    nativeCanvas.drawPath(path, paint)
                    nativeCanvas.restore()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiSettingsBottomSheet(
    state: WallpaperState,
    onStateChange: (WallpaperState) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var showEmojiPicker by remember { mutableStateOf(false) }

    if (showEmojiPicker) {
        EmojiPickerBottomSheet(
            currentEmojis = state.emojis,
            onEmojisSelected = { onStateChange(state.copy(emojis = it)) },
            onDismiss = { showEmojiPicker = false }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Text(stringResource(R.string.emoji_title), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.emojis.take(5).forEach { emoji ->
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(emoji, fontFamily = NotoEmojiFontFamily)
                        }
                    }
                }
                if (state.emojis.size > 5) {
                    Text("+${state.emojis.size - 5}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { showEmojiPicker = true }) {
                    Text(stringResource(R.string.choose))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.pattern_type), style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PatternType.entries.forEach { type ->
                    val labelRes = when(type) {
                        PatternType.Mosaic -> R.string.pattern_mosaic
                        PatternType.Lotus -> R.string.pattern_lotus
                        PatternType.Blocks -> R.string.pattern_blocks
                        PatternType.Sprinkles -> R.string.pattern_sprinkles
                    }
                    FilterChip(
                        selected = state.patternType == type,
                        onClick = { onStateChange(state.copy(patternType = type)) },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("${stringResource(R.string.density)}: ${(state.density * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = state.density,
                onValueChange = { onStateChange(state.copy(density = it)) },
                valueRange = 0.2f..1f
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("${stringResource(R.string.emoji_size)}: ${(state.emojiSize * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = state.emojiSize,
                onValueChange = { onStateChange(state.copy(emojiSize = it)) },
                valueRange = 0.1f..1.5f
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.background), style = MaterialTheme.typography.titleMedium)
            val bgColors = listOf(
                Color(0xFFF0F4FF) to "Синий",
                Color(0xFFFFEBEE) to "Красный",
                Color(0xFFE8F5E9) to "Зеленый",
                Color(0xFFFFF3E0) to "Оранжевый",
                Color(0xFFF3E5F5) to "Лаванда",
                Color(0xFFE0F7FA) to "Циан",
                Color(0xFFFFF9C4) to "Желтый",
                Color(0xFFEFEBE9) to "Песочный",
                Color(0xFF263238) to "Серо-синий",
                Color(0xFF212121) to "Темный",
                Color(0xFFFFFFFF) to "Белый"
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                bgColors.forEach { (color, _) ->
                    Surface(
                        modifier = Modifier.size(40.dp),
                        color = color,
                        shape = MaterialTheme.shapes.small,
                        border = if (state.backgroundColor == color && state.backgroundBrushId == -1) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        onClick = { onStateChange(state.copy(backgroundColor = color, backgroundBrushId = -1)) }
                    ) {}
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.background_gradients), style = MaterialTheme.typography.bodySmall)
            
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Gradients.forEachIndexed { index, (brush, _) ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(brush)
                            .border(if (state.backgroundBrushId == index) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.dp, Color.Transparent), MaterialTheme.shapes.small)
                            .clickable { onStateChange(state.copy(backgroundBrushId = index)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.outline_settings), style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.font_weight), style = MaterialTheme.typography.bodySmall)
            val weights = listOf(
                FontWeight.Light to "Тонкий",
                FontWeight.Normal to "Стандарт",
                FontWeight.Medium to "Средний",
                FontWeight.SemiBold to "Полужирный",
                FontWeight.Bold to "Жирный"
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weights.forEach { (weight, label) ->
                    FilterChip(
                        selected = state.fontWeight == weight,
                        onClick = { onStateChange(state.copy(fontWeight = weight)) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.outline_color), style = MaterialTheme.typography.bodySmall)
            val outlineColors = listOf(
                Color.Black, Color.White, Color.Gray, Color.Red, 
                Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta, 
                Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF795548)
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                outlineColors.forEach { color ->
                    Surface(
                        modifier = Modifier.size(40.dp),
                        color = color,
                        shape = CircleShape,
                        border = if (state.outlineColor == color) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        onClick = { onStateChange(state.copy(outlineColor = color)) }
                    ) {}
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("${stringResource(R.string.outline_thickness)}: ${state.strokeWidth.toInt()}", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = state.strokeWidth,
                onValueChange = { onStateChange(state.copy(strokeWidth = it)) },
                valueRange = 1f..15f
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    onSave()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.save_settings))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerBottomSheet(
    currentEmojis: List<String>,
    onEmojisSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var showDisclaimer by remember { mutableStateOf(true) }
    val allEmojis = remember {
        val emojis = mutableListOf<String>()
        fun addRange(start: Int, end: Int) {
            for (i in start..end) {
                if (Character.isValidCodePoint(i)) {
                    emojis.add(String(Character.toChars(i)))
                }
            }
        }
        // Эмодзи из разных блоков Unicode
        addRange(0x1F600, 0x1F64F) // Смайлики и лица
        addRange(0x1F300, 0x1F5FF) // Разные символы и пиктограммы
        addRange(0x1F680, 0x1F6FF) // Транспорт и карты
        addRange(0x1F900, 0x1F9FF) // Дополнительные символы (еда, животные и др.)
        addRange(0x1FA70, 0x1FAFF) // Расширенные символы-A
        addRange(0x2600, 0x26FF)   // Разные символы
        addRange(0x2700, 0x27BF)   // Дингбаты
        emojis
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 600.dp)
                .navigationBarsPadding()
        ) {
            if (showDisclaimer) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.emoji_disclaimer),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showDisclaimer = false }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }

            Text(stringResource(R.string.select_emoji), style = MaterialTheme.typography.titleLarge)
            Text("${stringResource(R.string.total_available)}: ${allEmojis.size}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(allEmojis) { emoji ->
                    val isSelected = currentEmojis.contains(emoji)
                    Surface(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(48.dp),
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        onClick = {
                            val newList = if (isSelected) {
                                if (currentEmojis.size > 1) currentEmojis - emoji else currentEmojis
                            } else {
                                currentEmojis + emoji
                            }
                            onEmojisSelected(newList)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                emoji, 
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = NotoEmojiFontFamily
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.done))
            }
        }
    }
}
