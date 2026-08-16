package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ColorMode
import com.example.model.NavigationTab
import com.example.ui.components.CameraCaptureView
import com.example.ui.components.ExportSceneView
import com.example.ui.components.FeatureMatchVisualizer
import com.example.ui.components.Point3DCanvas
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoGlow
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.RoseHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.SceneReconstructionViewModel

@Composable
fun MainMvsScreen(
    viewModel: SceneReconstructionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showColorMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        // Immersive Header (Boof Engine)
        HeaderSection(
            activeSceneName = uiState.currentScene.title,
            onToggleSettings = { showColorMenu = true }
        )

        // Color Mode Dropdown
        DropdownMenu(
            expanded = showColorMenu,
            onDismissRequest = { showColorMenu = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            ColorMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = mode.label,
                            color = if (uiState.selectedColorMode == mode) PurplePrimary else TextPrimary,
                            fontWeight = if (uiState.selectedColorMode == mode) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        viewModel.setColorMode(mode)
                        showColorMenu = false
                    }
                )
            }
        }

        // Reconstruction Progress Overlay Bar
        AnimatedVisibility(
            visible = uiState.reconstructionProgress.isProcessing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.reconstructionProgress.currentStage,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PurplePrimary
                    )
                    Text(
                        text = "${(uiState.reconstructionProgress.progressFraction * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }
                LinearProgressIndicator(
                    progress = { uiState.reconstructionProgress.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = PurplePrimary,
                    trackColor = Color(0x33D0BCFF)
                )
            }
        }

        // Main Visualizer Viewport Container (rounded-32px, shadow-2xl, ring-1 ring-white/10)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black)
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(32.dp))
                .testTag("main_viewport_container")
        ) {
            when (uiState.activeTab) {
                NavigationTab.CAPTURE -> {
                    CameraCaptureView(
                        capturedBitmaps = uiState.capturedImages,
                        onCaptureBitmap = { viewModel.addCapturedImage(it) },
                        onRemoveBitmap = { viewModel.removeCapturedImage(it) },
                        onClearAll = { viewModel.clearCapturedImages() },
                        onStartReconstruction = { viewModel.startReconstruction() },
                        onLoadPreset = { viewModel.loadPresetScene(it) },
                        isReconstructing = uiState.reconstructionProgress.isProcessing,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                NavigationTab.POINTS -> {
                    FeatureMatchVisualizer(
                        scene = uiState.currentScene,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                NavigationTab.SCENE -> {
                    Point3DCanvas(
                        scene = uiState.currentScene,
                        colorMode = uiState.selectedColorMode,
                        pointSize = uiState.pointSize,
                        showCameras = uiState.showCameras,
                        showGrid = uiState.showGrid,
                        autoRotate = uiState.autoRotate,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 3D Scene HUD Overlays
                    SceneHudOverlay(
                        scene = uiState.currentScene,
                        autoRotate = uiState.autoRotate,
                        showGrid = uiState.showGrid,
                        showCameras = uiState.showCameras,
                        colorMode = uiState.selectedColorMode,
                        onToggleAutoRotate = { viewModel.toggleAutoRotate() },
                        onToggleGrid = { viewModel.toggleGrid() },
                        onToggleCameras = { viewModel.toggleCameras() },
                        onCycleColorMode = {
                            val all = ColorMode.values()
                            val next = all[(uiState.selectedColorMode.ordinal + 1) % all.size]
                            viewModel.setColorMode(next)
                        }
                    )
                }
                NavigationTab.EXPORT -> {
                    ExportSceneView(
                        scene = uiState.currentScene,
                        onExport = { format -> viewModel.exportScene(context, format) },
                        successMessage = uiState.exportSuccessMessage,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Immersive Footer Navigation
        FooterSection(
            activeTab = uiState.activeTab,
            onTabSelected = { viewModel.selectTab(it) }
        )
    }
}

@Composable
private fun HeaderSection(
    activeSceneName: String,
    onToggleSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "BOOF ENGINE",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = PurplePrimary
            )
            Text(
                text = "Uncalibrated MVS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.5).sp
            )
        }

        // Isometric 3D Cube Icon Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x2649454F))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .clickable { onToggleSettings() }
                .testTag("header_cube_button"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(2.dp, Color(0xCCFFFFFF), RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun SceneHudOverlay(
    scene: com.example.model.ReconstructedScene,
    autoRotate: Boolean,
    showGrid: Boolean,
    showCameras: Boolean,
    colorMode: ColorMode,
    onToggleAutoRotate: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleCameras: () -> Unit,
    onCycleColorMode: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top Left Status Badge
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 18.dp, start = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x99000000))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(EmeraldSuccess)
                )
                Text(
                    text = "SCENE: RECONSTRUCTED",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x99000000))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "VERTICES: ${scene.points.size}",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = PurplePrimary
                )
            }
        }

        // Top Right HUD Tools Strip
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HudToolButton(
                icon = Icons.Default.Palette,
                isActive = true,
                onClick = onCycleColorMode,
                contentDescription = "Cycle Color Shader (${colorMode.label})"
            )
            HudToolButton(
                icon = Icons.Default.Sync,
                isActive = autoRotate,
                onClick = onToggleAutoRotate,
                contentDescription = "Auto Orbit Turntable"
            )
            HudToolButton(
                icon = Icons.Default.GridOn,
                isActive = showGrid,
                onClick = onToggleGrid,
                contentDescription = "Toggle Ground Grid"
            )
            HudToolButton(
                icon = Icons.Default.Videocam,
                isActive = showCameras,
                onClick = onToggleCameras,
                contentDescription = "Toggle Camera Poses"
            )
        }

        // Bottom HUD: Orientation & Depth Signal Equalizer
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "ORIENTATION",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Color(0x80FFFFFF)
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "42.8",
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "°",
                        fontSize = 12.sp,
                        color = PurplePrimary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PurplePrimary)
                    )
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x99D0BCFF))
                    )
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PurplePrimary)
                    )
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x66D0BCFF))
                    )
                }
                Text(
                    text = "DEPTH SIGNAL STRENGTH",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0x80FFFFFF)
                )
            }
        }
    }
}

@Composable
private fun HudToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) Color(0xCC26242E) else Color(0x80000000))
            .border(
                1.dp,
                if (isActive) PurplePrimary else Color(0x26FFFFFF),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag(contentDescription),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) PurplePrimary else Color(0x80FFFFFF),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FooterSection(
    activeTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavigationTab.values().forEach { tab ->
            val isSelected = activeTab == tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("nav_${tab.name.lowercase()}")
            ) {
                Text(
                    text = tab.label.uppercase(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.8.sp,
                    color = if (isSelected) PurplePrimary else Color(0x66FFFFFF)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (isSelected) PurplePrimary else Color.Transparent)
                )
            }
        }
    }
}
