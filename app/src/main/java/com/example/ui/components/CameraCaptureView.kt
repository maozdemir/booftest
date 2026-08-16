package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HudBackground
import com.example.ui.theme.IndigoGlow
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.RoseHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraCaptureView(
    capturedBitmaps: List<Bitmap>,
    onCaptureBitmap: (Bitmap) -> Unit,
    onRemoveBitmap: (Int) -> Unit,
    onClearAll: () -> Unit,
    onStartReconstruction: () -> Unit,
    onLoadPreset: (String) -> Unit,
    isReconstructing: Boolean,
    modifier: Modifier = Modifier
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var simulatedAngle by remember { mutableFloatStateOf(0f) }

    // Pulsing reticle animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
    ) {
        // Camera Preview or Fallback Visualizer
        if (cameraPermissionState.status.isGranted) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (exc: Exception) {
                            exc.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // High-tech Simulated Viewfinder with permission trigger
            SimulatedViewfinder(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
            )
        }

        // Futuristic Floating Keypoint Vertices Overlay
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val random = Random(123)
            val w = size.width
            val h = size.height

            // Simulated real-time feature tracking nodes
            val numKeypoints = 48
            for (i in 0 until numKeypoints) {
                val px = (0.15f + (i * 37 % 100) / 130f) * w
                val py = (0.2f + (i * 53 % 100) / 140f) * h
                drawCircle(
                    color = Color(0x99D0BCFF),
                    radius = 2.5f,
                    center = Offset(px, py)
                )
                // Draw subtle optical flow lines
                drawLine(
                    color = Color(0x44818CF8),
                    start = Offset(px, py),
                    end = Offset(px + (i % 7 - 3) * 3f, py + (i % 5 - 2) * 3f),
                    strokeWidth = 1f
                )
            }
        }

        // Center Aiming Reticle (Immersive UI Style)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Outer circle
                drawCircle(
                    color = Color(0x40FFFFFF),
                    radius = size.width * 0.48f,
                    style = Stroke(width = 1.2f)
                )

                // Middle circle
                drawCircle(
                    color = Color(0x26FFFFFF),
                    radius = size.width * 0.32f,
                    style = Stroke(width = 1.0f)
                )

                // Center diamond / reticle
                drawCircle(
                    color = PurplePrimary,
                    radius = 7f,
                    center = center
                )

                // Crosshair tick marks
                val tickLen = 14f
                val radius = size.width * 0.48f
                drawLine(
                    color = PurplePrimary,
                    start = Offset(center.x - radius, center.y),
                    end = Offset(center.x - radius + tickLen, center.y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = PurplePrimary,
                    start = Offset(center.x + radius - tickLen, center.y),
                    end = Offset(center.x + radius, center.y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = PurplePrimary,
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x, center.y - radius + tickLen),
                    strokeWidth = 2f
                )
                drawLine(
                    color = PurplePrimary,
                    start = Offset(center.x, center.y + radius - tickLen),
                    end = Offset(center.x, center.y + radius),
                    strokeWidth = 2f
                )
            }
        }

        // Top Left Status Badge & Feature Telemetry
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 20.dp, start = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x99000000))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (capturedBitmaps.size >= 3) EmeraldSuccess else RoseHighlight)
                )
                Text(
                    text = if (isReconstructing) "SCENE: COMPUTING MVS" else if (capturedBitmaps.size >= 3) "MVS READY (>=3 VIEWS)" else "NEED MULTI-VIEW PHOTOS",
                    fontSize = 10.sp,
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
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "LIVE FEATURES: ${if (capturedBitmaps.isEmpty()) 2480 else capturedBitmaps.size * 3120}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = PurplePrimary
                )
            }
        }

        // Top Right Preset Selector
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x99000000))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                    .clickable { onLoadPreset("bust_marble") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = "Load Marble Bust Preset",
                    tint = PurplePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (capturedBitmaps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x99000000))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { onClearAll() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Clear Captured Photos",
                        tint = RoseHighlight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Overlay: Orientation Telemetry, Captured Ribbon & Capture Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000), Color(0xFA000000))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Orientation & Depth Signal HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "ANGULAR DISPARITY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Color(0x80FFFFFF)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%.1f", (capturedBitmaps.size * 45f + 42.8f) % 360f),
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "°",
                            fontSize = 14.sp,
                            color = PurplePrimary
                        )
                    }
                }

                // Depth Signal Strength Bars
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PurplePrimary)
                        )
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x99D0BCFF))
                        )
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PurplePrimary)
                        )
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x66D0BCFF))
                        )
                    }
                    Text(
                        text = "MVS DEPTH SIGNAL",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0x80FFFFFF)
                    )
                }
            }

            // Captured Frames Reel Strip
            if (capturedBitmaps.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    itemsIndexed(capturedBitmaps) { index, bmp ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, PurplePrimary, RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "View #${index + 1}",
                                modifier = Modifier.fillMaxSize()
                            )
                            // Index badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .background(Color(0xAA000000), RoundedCornerShape(bottomEnd = 8.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimary
                                )
                            }
                            // Delete button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(Color(0xAA000000), CircleShape)
                                    .clickable { onRemoveBitmap(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove frame",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Trigger Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Preset Dataset Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x3349454F))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                        .clickable {
                            val presets = listOf("bust_marble", "drone_terrain", "pavilion_arch")
                            val nextPreset = presets[(capturedBitmaps.size) % presets.size]
                            onLoadPreset(nextPreset)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Load Preset",
                            tint = Color(0xCCFFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "PRESET",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )
                    }
                }

                // Primary Capture Shutter Trigger
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                        .clickable {
                            val capture = imageCapture
                            if (capture != null && cameraPermissionState.status.isGranted) {
                                capture.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bmp = image.toBitmap()
                                            image.close()
                                            onCaptureBitmap(bmp)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            val simulatedBmp = generateSimulatedCapture(capturedBitmaps.size)
                                            onCaptureBitmap(simulatedBmp)
                                        }
                                    }
                                )
                            } else {
                                // Generate photogrammetry view frame with realistic multi-angle parallax
                                val simulatedBmp = generateSimulatedCapture(capturedBitmaps.size)
                                onCaptureBitmap(simulatedBmp)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .border(3.5.dp, DarkCanvas, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkCanvas)
                        )
                    }
                }

                // Start MVS Reconstruction Trigger
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (capturedBitmaps.size >= 2) PurplePrimary else Color(0x3349454F))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                        .clickable(enabled = capturedBitmaps.size >= 2 && !isReconstructing) {
                            onStartReconstruction()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isReconstructing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Compute MVS 3D",
                                tint = if (capturedBitmaps.size >= 2) DarkCanvas else Color(0x66FFFFFF),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "3D MVS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (capturedBitmaps.size >= 2) DarkCanvas else Color(0x66FFFFFF)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulatedViewfinder(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1A2E), Color(0xFF0D0C14))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Camera",
                tint = PurplePrimary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Uncalibrated Camera Mode",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Capture 3+ multi-view photos around any object, or tap PRESET to load demo 3D photogrammetry sets.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
            ) {
                Text(
                    text = "Enable Live Device Camera",
                    color = DarkCanvas,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun generateSimulatedCapture(frameIndex: Int): Bitmap {
    val width = 640
    val height = 480
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Dark background with gradient
    paint.color = android.graphics.Color.rgb(20, 20, 30)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    // Center 3D simulated subject with shift based on frame angle
    val angleRad = (frameIndex * 45f) * (Math.PI.toFloat() / 180f)
    val shiftX = sin(angleRad) * 60f

    paint.color = android.graphics.Color.rgb(208, 188, 255)
    canvas.drawCircle(width / 2f + shiftX, height / 2f - 20f, 90f, paint)

    paint.color = android.graphics.Color.rgb(129, 140, 248)
    canvas.drawOval(
        width / 2f + shiftX - 120f,
        height / 2f + 40f,
        width / 2f + shiftX + 120f,
        height / 2f + 160f,
        paint
    )

    // Add texture spots for feature detection
    val rand = Random(frameIndex * 31 + 7)
    for (i in 0 until 120) {
        val px = (rand.nextFloat() * width)
        val py = (rand.nextFloat() * height)
        paint.color = android.graphics.Color.rgb(
            (180 + rand.nextInt(75)),
            (180 + rand.nextInt(75)),
            (210 + rand.nextInt(45))
        )
        canvas.drawCircle(px, py, 3.5f, paint)
    }

    return bitmap
}
