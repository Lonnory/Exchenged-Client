package com.exchenged.client.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.exchenged.client.dto.Node
import com.exchenged.client.model.protocol.protocolPrefixMap
import com.exchenged.client.utils.ColorMap

import androidx.compose.material.icons.outlined.Speed
import androidx.compose.ui.draw.scale

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun NodeCard(
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    node: Node,
    modifier: Modifier = Modifier,
    delete: (() -> Unit)? = null,
    onChoose: () -> Unit = {},
    onShare: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onTest: (() -> Unit)? = null,
    delayMs: Long = -1,
    testing: Boolean = false,
    selected: Boolean = false,
    enableTest: Boolean = true,
    roundCorner: Boolean = false,
    countryEmoji: String = "",
    contentColor: Color = Color.Unspecified
) {
    val context = LocalContext.current
    var showMoreMenu by remember { mutableStateOf(false) }

    val delayColor = when {
        delayMs == -2L -> MaterialTheme.colorScheme.error
        delayMs < 0 -> Color.Transparent
        delayMs < 300 -> Color(0xFF4CAF50)
        delayMs < 900 -> Color(0xFFFFA000)
        else -> Color(0xFFF44336)
    }
    
    val elevation by animateDpAsState(
        targetValue = if (selected) 8.dp else 2.dp,
        label = "elevation"
    )

    ElevatedCard(
        onClick = onChoose,
        modifier = modifier.fillMaxWidth(),
        shape = if (roundCorner) RoundedCornerShape(24.dp) else RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Node Icon / Emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ColorMap.getValue(node.subscriptionId).copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                if (countryEmoji.isNotEmpty()) {
                    Text(text = countryEmoji, style = MaterialTheme.typography.titleLarge)
                } else {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Node Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.remark ?: node.address,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                    color = if (contentColor != Color.Unspecified) contentColor else Color.Unspecified
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = protocolPrefixMap[node.protocolPrefix]?.protocolType ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (contentColor != Color.Unspecified) contentColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (testing) 0.4f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    if (delayMs != -1L || testing) {
                        val displayText = if (testing) "Testing..." else if (delayMs == -2L) "Timeout" else "${delayMs}ms"
                        val finalColor = if (testing) MaterialTheme.colorScheme.primary else delayColor
                        
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(finalColor.copy(alpha = 0.1f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .graphicsLayer { this.alpha = if (testing) alpha else 1f }
                        ) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.labelSmall,
                                color = finalColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onShare != null) {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                    }
                }
                
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ping") },
                            leadingIcon = { Icon(Icons.Outlined.Speed, null) },
                            onClick = {
                                showMoreMenu = false
                                onTest?.invoke()
                            }
                        )
                        if (onEdit != null) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    showMoreMenu = false
                                    onEdit()
                                }
                            )
                        }
                        if (delete != null) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = {
                                    showMoreMenu = false
                                    delete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DashboardCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "star"
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = countryCodeToFlagEmoji("SG")
            )
        }
    }
}

@Composable
@Preview
fun DashboardCardPreview() {
    DashboardCard()
}

fun countryCodeToFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return "🏳️"
    val base = 0x1F1E6 - 'A'.code
    val first = Character.toChars(base + countryCode[0].uppercaseChar().code)
    val second = Character.toChars(base + countryCode[1].uppercaseChar().code)
    return String(first) + String(second)
}
