package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReconstructedScene
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoGlow
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.RoseHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FeatureMatchVisualizer(
    scene: ReconstructedScene,
    modifier: Modifier = Modifier
) {
    var selectedPairIndex by remember { mutableIntStateOf(0) }
    val pairs = scene.pairwiseMatches

    if (pairs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No pairwise feature matches recorded for this scene.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        return
    }

    val activePair = pairs.getOrElse(selectedPairIndex) { pairs.first() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pair Selector Tab Row
        if (pairs.size > 1) {
            ScrollableTabRow(
                selectedTabIndex = selectedPairIndex,
                containerColor = DarkSurface,
                contentColor = PurplePrimary,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedPairIndex]),
                        color = PurplePrimary,
                        height = 2.dp
                    )
                },
                divider = {}
            ) {
                pairs.forEachIndexed { index, pair ->
                    Tab(
                        selected = selectedPairIndex == index,
                        onClick = { selectedPairIndex = index },
                        text = {
                            Text(
                                text = "Pair ${pair.viewIndex1 + 1} ↔ ${pair.viewIndex2 + 1}",
                                fontSize = 12.sp,
                                fontWeight = if (selectedPairIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedPairIndex == index) PurplePrimary else TextSecondary
                            )
                        }
                    )
                }
            }
        }

        // Telemetry Pills Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoChip(
                icon = Icons.Default.AutoAwesome,
                label = "Matches",
                value = "${activePair.matches.size} pts",
                accentColor = PurplePrimary,
                modifier = Modifier.weight(1f)
            )
            InfoChip(
                icon = Icons.Default.CheckCircle,
                label = "Inlier Ratio",
                value = "${(activePair.inlierRatio * 100).toInt()}%",
                accentColor = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )
            InfoChip(
                icon = Icons.Default.Layers,
                label = "F-Matrix",
                value = String.format("%.2f", activePair.fundamentalMatrixScore),
                accentColor = IndigoGlow,
                modifier = Modifier.weight(1f)
            )
        }

        // Epipolar Match Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0C0B10))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val leftBoxWidth = w * 0.46f
                val rightBoxLeft = w * 0.54f
                val rightBoxWidth = w * 0.46f

                // Draw View 1 Box
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0x2238BDF8), Color(0x0538BDF8))),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(leftBoxWidth, h)
                )

                // Draw View 2 Box
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0x22D0BCFF), Color(0x05D0BCFF))),
                    topLeft = Offset(rightBoxLeft, 0f),
                    size = androidx.compose.ui.geometry.Size(rightBoxWidth, h)
                )

                // Draw Feature Matches & Correspondence Lines
                for (match in activePair.matches) {
                    val p1X = match.x1 * leftBoxWidth
                    val p1Y = match.y1 * h

                    val p2X = rightBoxLeft + match.x2 * rightBoxWidth
                    val p2Y = match.y2 * h

                    val lineColor = if (match.isInlier) {
                        Color(0x77D0BCFF)
                    } else {
                        Color(0x44F43F5E)
                    }

                    // Connecting Line
                    drawLine(
                        color = lineColor,
                        start = Offset(p1X, p1Y),
                        end = Offset(p2X, p2Y),
                        strokeWidth = if (match.isInlier) 1.2f else 0.8f
                    )

                    // Point 1
                    drawCircle(
                        color = if (match.isInlier) Color(0xFF38BDF8) else RoseHighlight,
                        radius = 3.5f,
                        center = Offset(p1X, p1Y)
                    )

                    // Point 2
                    drawCircle(
                        color = if (match.isInlier) Color(0xFFD0BCFF) else RoseHighlight,
                        radius = 3.5f,
                        center = Offset(p2X, p2Y)
                    )
                }

                // Center separation line
                drawLine(
                    color = Color(0x33FFFFFF),
                    start = Offset(w / 2f, 0f),
                    end = Offset(w / 2f, h),
                    strokeWidth = 1f
                )
            }

            // Overlay Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "VIEW #${activePair.viewIndex1 + 1}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "VIEW #${activePair.viewIndex2 + 1}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PurplePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceElevated)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RANSAC Inliers",
                    fontSize = 11.sp,
                    color = TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RoseHighlight)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Epipolar Outliers",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = "BoofCV SURF / FAST",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = PurplePrimary
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label,
                fontSize = 9.sp,
                color = TextSecondary,
                textDecoration = null
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}
