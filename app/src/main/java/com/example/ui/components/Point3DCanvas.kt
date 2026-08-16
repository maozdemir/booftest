package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.CameraPose
import com.example.model.ColorMode
import com.example.model.Point3D
import com.example.model.ReconstructedScene
import com.example.model.RenderMode
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun Point3DCanvas(
    scene: ReconstructedScene,
    renderMode: RenderMode = RenderMode.SHADED,
    colorMode: ColorMode = ColorMode.TRUE_RGB,
    pointSize: Float = 3.5f,
    showCameras: Boolean = true,
    showGrid: Boolean = true,
    autoRotate: Boolean = false,
    modifier: Modifier = Modifier
) {
    var yawDeg by remember { mutableFloatStateOf(35f) }
    var pitchDeg by remember { mutableFloatStateOf(20f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Auto rotate effect
    LaunchedEffect(autoRotate) {
        while (autoRotate) {
            yawDeg = (yawDeg + 0.6f) % 360f
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.25f, 6.0f)
                    panOffsetX += pan.x
                    panOffsetY += pan.y
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    yawDeg = (yawDeg + dragAmount.x * 0.4f) % 360f
                    pitchDeg = (pitchDeg - dragAmount.y * 0.4f).coerceIn(-85f, 85f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW / 2f + panOffsetX
            val centerY = canvasH / 2f + panOffsetY

            val fovDist = (canvasW * 0.85f) * zoomScale
            val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
            val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()

            val cosYaw = cos(yawRad)
            val sinYaw = sin(yawRad)
            val cosPitch = cos(pitchRad)
            val sinPitch = sin(pitchRad)

            val cameraDist = 4.5f

            // Draw 3D Ground Grid
            if (showGrid) {
                draw3DGrid(
                    centerX = centerX,
                    centerY = centerY,
                    fovDist = fovDist,
                    cosYaw = cosYaw,
                    sinYaw = sinYaw,
                    cosPitch = cosPitch,
                    sinPitch = sinPitch,
                    gridSize = 7,
                    spacing = 0.8f,
                    gridY = scene.minBound.y - 0.2f
                )
            }

            val mesh = scene.mesh

            if (renderMode == RenderMode.POINTS || mesh == null || mesh.triangles.isEmpty()) {
                // --- RENDER POINT CLOUD ---
                val projectedPoints = ArrayList<ProjectedPoint>(scene.points.size)

                for (pt in scene.points) {
                    val x1 = pt.x * cosYaw - pt.z * sinYaw
                    val z1 = pt.x * sinYaw + pt.z * cosYaw
                    val y1 = pt.y

                    val y2 = y1 * cosPitch - z1 * sinPitch
                    val z2 = y1 * sinPitch + z1 * cosPitch
                    val x2 = x1

                    val eyeZ = z2 + cameraDist

                    if (eyeZ > 0.1f) {
                        val scale = fovDist / eyeZ
                        val screenX = centerX + x2 * scale
                        val screenY = centerY - y2 * scale

                        val pointColor = computePointColor(pt, colorMode, scene)

                        projectedPoints.add(
                            ProjectedPoint(
                                screenX = screenX,
                                screenY = screenY,
                                depth = eyeZ,
                                color = pointColor,
                                radius = (pointSize * (3.5f / eyeZ).coerceIn(0.5f, 2.2f))
                            )
                        )
                    }
                }

                projectedPoints.sortByDescending { it.depth }

                for (pp in projectedPoints) {
                    if (pp.screenX in -20f..(canvasW + 20f) && pp.screenY in -20f..(canvasH + 20f)) {
                        drawCircle(
                            color = pp.color,
                            radius = pp.radius,
                            center = Offset(pp.screenX, pp.screenY)
                        )
                    }
                }
            } else {
                // --- RENDER SURFACE MESH (Wireframe, Shaded, or Textured) ---
                val vertices = mesh.vertices
                val projectedVertices = arrayOfNulls<ProjectedVertex>(vertices.size)

                for ((idx, pt) in vertices.withIndex()) {
                    val x1 = pt.x * cosYaw - pt.z * sinYaw
                    val z1 = pt.x * sinYaw + pt.z * cosYaw
                    val y1 = pt.y

                    val y2 = y1 * cosPitch - z1 * sinPitch
                    val z2 = y1 * sinPitch + z1 * cosPitch
                    val x2 = x1

                    val eyeZ = z2 + cameraDist

                    if (eyeZ > 0.1f) {
                        val scale = fovDist / eyeZ
                        val screenX = centerX + x2 * scale
                        val screenY = centerY - y2 * scale

                        val vColor = computePointColor(pt, colorMode, scene)
                        projectedVertices[idx] = ProjectedVertex(screenX, screenY, eyeZ, vColor)
                    }
                }

                // Directional light vector normalized in camera space (coming from top-right-front)
                val lightX = 0.408f
                val lightY = 0.707f
                val lightZ = 0.577f

                val projectedTriangles = ArrayList<ProjectedTriangle>(mesh.triangles.size)

                for (tri in mesh.triangles) {
                    val pv1 = if (tri.v1 in projectedVertices.indices) projectedVertices[tri.v1] else null
                    val pv2 = if (tri.v2 in projectedVertices.indices) projectedVertices[tri.v2] else null
                    val pv3 = if (tri.v3 in projectedVertices.indices) projectedVertices[tri.v3] else null

                    if (pv1 != null && pv2 != null && pv3 != null) {
                        // Average depth for back-to-front sorting
                        val avgDepth = (pv1.depth + pv2.depth + pv3.depth) / 3f

                        // Transform normal to camera space
                        val nx1 = tri.normalX * cosYaw - tri.normalZ * sinYaw
                        val nz1 = tri.normalX * sinYaw + tri.normalZ * cosYaw
                        val ny1 = tri.normalY

                        val ny2 = ny1 * cosPitch - nz1 * sinPitch
                        val nz2 = ny1 * sinPitch + nz1 * cosPitch
                        val nx2 = nx1

                        // Diffuse Lambertian reflection with ambient light floor
                        val dot = (nx2 * lightX + ny2 * lightY + nz2 * lightZ)
                        val diffuse = dot.coerceIn(0f, 1f)
                        val lightFactor = (0.28f + 0.72f * diffuse).coerceIn(0.2f, 1.0f)

                        projectedTriangles.add(
                            ProjectedTriangle(
                                p1 = Offset(pv1.screenX, pv1.screenY),
                                p2 = Offset(pv2.screenX, pv2.screenY),
                                p3 = Offset(pv3.screenX, pv3.screenY),
                                avgDepth = avgDepth,
                                lightFactor = lightFactor,
                                c1 = pv1.color,
                                c2 = pv2.color,
                                c3 = pv3.color
                            )
                        )
                    }
                }

                // Sort triangles from back to front (Painter's algorithm)
                projectedTriangles.sortByDescending { it.avgDepth }

                val path = Path()

                when (renderMode) {
                    RenderMode.WIREFRAME -> {
                        for (ptri in projectedTriangles) {
                            path.reset()
                            path.moveTo(ptri.p1.x, ptri.p1.y)
                            path.lineTo(ptri.p2.x, ptri.p2.y)
                            path.lineTo(ptri.p3.x, ptri.p3.y)
                            path.close()

                            val wireAlpha = (0.85f * (3.8f / ptri.avgDepth).coerceIn(0.4f, 1f))
                            drawPath(
                                path = path,
                                color = Color(0xFFD0BCFF).copy(alpha = wireAlpha),
                                style = Stroke(width = 1.2f)
                            )
                        }
                    }
                    RenderMode.SHADED -> {
                        for (ptri in projectedTriangles) {
                            path.reset()
                            path.moveTo(ptri.p1.x, ptri.p1.y)
                            path.lineTo(ptri.p2.x, ptri.p2.y)
                            path.lineTo(ptri.p3.x, ptri.p3.y)
                            path.close()

                            // Base shaded surface material (Sculpture marble / cyber lilac)
                            val baseRed = 0.82f * ptri.lightFactor
                            val baseGreen = 0.76f * ptri.lightFactor
                            val baseBlue = 0.94f * ptri.lightFactor

                            drawPath(
                                path = path,
                                color = Color(baseRed, baseGreen, baseBlue),
                                style = Fill
                            )

                            // Subtle polygon edge definition
                            drawPath(
                                path = path,
                                color = Color(0x331C1B1F),
                                style = Stroke(width = 0.8f)
                            )
                        }
                    }
                    RenderMode.TEXTURED -> {
                        for (ptri in projectedTriangles) {
                            path.reset()
                            path.moveTo(ptri.p1.x, ptri.p1.y)
                            path.lineTo(ptri.p2.x, ptri.p2.y)
                            path.lineTo(ptri.p3.x, ptri.p3.y)
                            path.close()

                            // Average vertex colors with lighting
                            val avgR = ((ptri.c1.red + ptri.c2.red + ptri.c3.red) / 3f * ptri.lightFactor).coerceIn(0f, 1f)
                            val avgG = ((ptri.c1.green + ptri.c2.green + ptri.c3.green) / 3f * ptri.lightFactor).coerceIn(0f, 1f)
                            val avgB = ((ptri.c1.blue + ptri.c2.blue + ptri.c3.blue) / 3f * ptri.lightFactor).coerceIn(0f, 1f)

                            drawPath(
                                path = path,
                                color = Color(avgR, avgG, avgB),
                                style = Fill
                            )

                            // Subtle seam outline
                            drawPath(
                                path = path,
                                color = Color(0x22FFFFFF),
                                style = Stroke(width = 0.6f)
                            )
                        }
                    }
                    RenderMode.POINTS -> {} // Handled above
                }
            }

            // Draw Camera Poses & Frustums
            if (showCameras) {
                for (cam in scene.cameras) {
                    drawCameraFrustum(
                        cam = cam,
                        centerX = centerX,
                        centerY = centerY,
                        fovDist = fovDist,
                        cosYaw = cosYaw,
                        sinYaw = sinYaw,
                        cosPitch = cosPitch,
                        sinPitch = sinPitch
                    )
                }
            }

            // Draw 3D Orientation Axis Triad at bottom-left
            drawAxisTriad(
                cosYaw = cosYaw,
                sinYaw = sinYaw,
                cosPitch = cosPitch,
                sinPitch = sinPitch
            )
        }
    }
}

private data class ProjectedVertex(
    val screenX: Float,
    val screenY: Float,
    val depth: Float,
    val color: Color
)

private data class ProjectedTriangle(
    val p1: Offset,
    val p2: Offset,
    val p3: Offset,
    val avgDepth: Float,
    val lightFactor: Float,
    val c1: Color,
    val c2: Color,
    val c3: Color
)

private data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val depth: Float,
    val color: Color,
    val radius: Float
)

private fun computePointColor(pt: Point3D, mode: ColorMode, scene: ReconstructedScene): Color {
    return when (mode) {
        ColorMode.TRUE_RGB -> Color(pt.r, pt.g, pt.b)
        ColorMode.DEPTH_SPECTRUM -> {
            val normDepth = ((pt.z - scene.minBound.z) / (scene.maxBound.z - scene.minBound.z).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
            when {
                normDepth < 0.33f -> {
                    val t = normDepth / 0.33f
                    Color(
                        red = (56 + t * (208 - 56)).toInt() / 255f,
                        green = (189 + t * (188 - 189)).toInt() / 255f,
                        blue = (248 + t * (255 - 248)).toInt() / 255f
                    )
                }
                normDepth < 0.66f -> {
                    val t = (normDepth - 0.33f) / 0.33f
                    Color(
                        red = (208 + t * (251 - 208)).toInt() / 255f,
                        green = (188 + t * (191 - 188)).toInt() / 255f,
                        blue = (255 + t * (36 - 255)).toInt() / 255f
                    )
                }
                else -> {
                    val t = (normDepth - 0.66f) / 0.34f
                    Color(
                        red = (251 + t * (244 - 251)).toInt() / 255f,
                        green = (191 + t * (63 - 191)).toInt() / 255f,
                        blue = (36 + t * (94 - 36)).toInt() / 255f
                    )
                }
            }
        }
        ColorMode.ELEVATION_HEATMAP -> {
            val normY = ((pt.y - scene.minBound.y) / (scene.maxBound.y - scene.minBound.y).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
            Color(
                red = (52 + normY * 180).toInt().coerceIn(0, 255) / 255f,
                green = (211 - normY * 60).toInt().coerceIn(0, 255) / 255f,
                blue = (153 + normY * 102).toInt().coerceIn(0, 255) / 255f
            )
        }
        ColorMode.NEON_CYBER -> {
            val glow = ((pt.intensity * 0.6f + 0.4f) * 255).toInt().coerceIn(0, 255)
            Color(208, 188, glow)
        }
    }
}

private fun DrawScope.draw3DGrid(
    centerX: Float,
    centerY: Float,
    fovDist: Float,
    cosYaw: Float,
    sinYaw: Float,
    cosPitch: Float,
    sinPitch: Float,
    gridSize: Int,
    spacing: Float,
    gridY: Float
) {
    val half = gridSize / 2
    val cameraDist = 4.5f

    for (i in -half..half) {
        val xVal = i * spacing
        val zStart = -half * spacing
        val zEnd = half * spacing

        // Line along Z
        val p1 = project3D(xVal, gridY, zStart, centerX, centerY, fovDist, cosYaw, sinYaw, cosPitch, sinPitch, cameraDist)
        val p2 = project3D(xVal, gridY, zEnd, centerX, centerY, fovDist, cosYaw, sinYaw, cosPitch, sinPitch, cameraDist)
        if (p1 != null && p2 != null) {
            drawLine(
                color = Color(0x22D0BCFF),
                start = p1,
                end = p2,
                strokeWidth = 1f
            )
        }

        // Line along X
        val q1 = project3D(zStart, gridY, xVal, centerX, centerY, fovDist, cosYaw, sinYaw, cosPitch, sinPitch, cameraDist)
        val q2 = project3D(zEnd, gridY, xVal, centerX, centerY, fovDist, cosYaw, sinYaw, cosPitch, sinPitch, cameraDist)
        if (q1 != null && q2 != null) {
            drawLine(
                color = Color(0x22D0BCFF),
                start = q1,
                end = q2,
                strokeWidth = 1f
            )
        }
    }
}

private fun DrawScope.drawCameraFrustum(
    cam: CameraPose,
    centerX: Float,
    centerY: Float,
    fovDist: Float,
    cosYaw: Float,
    sinYaw: Float,
    cosPitch: Float,
    sinPitch: Float
) {
    val cameraDist = 4.5f
    val cp = cam.position

    val centerProj = project3D(cp.x, cp.y, cp.z, centerX, centerY, fovDist, cosYaw, sinYaw, cosPitch, sinPitch, cameraDist) ?: return

    // Camera apex
    drawCircle(
        color = Color(0xFFD0BCFF),
        radius = 5.5f,
        center = centerProj
    )

    // Direction pyramid
    val dirRad = Math.toRadians(cam.rotationYaw.toDouble()).toFloat()
    val fwdX = -sin(dirRad) * 0.4f
    val fwdZ = -cos(dirRad) * 0.4f
    val fwdY = 0f

    val fwdProj = project3D(cp.x + fwdX, cp.y + fwdY, cp.z + fwdZ, centerX, centerY, fovDist, cosYaw, sinYaw, cosPitch, sinPitch, cameraDist)
    if (fwdProj != null) {
        drawLine(
            color = Color(0xFF818CF8),
            start = centerProj,
            end = fwdProj,
            strokeWidth = 2.5f
        )
    }
}

private fun DrawScope.drawAxisTriad(
    cosYaw: Float,
    sinYaw: Float,
    cosPitch: Float,
    sinPitch: Float
) {
    val originX = 50f
    val originY = size.height - 50f
    val axisLen = 32f

    // X axis (Red/Rose)
    val xx = cosYaw * axisLen
    val xy = sinYaw * sinPitch * axisLen
    drawLine(
        color = Color(0xFFF43F5E),
        start = Offset(originX, originY),
        end = Offset(originX + xx, originY - xy),
        strokeWidth = 2.5f
    )

    // Y axis (Emerald/Green)
    val yx = 0f
    val yy = cosPitch * axisLen
    drawLine(
        color = Color(0xFF34D399),
        start = Offset(originX, originY),
        end = Offset(originX + yx, originY - yy),
        strokeWidth = 2.5f
    )

    // Z axis (Sky Blue)
    val zx = -sinYaw * axisLen
    val zy = cosYaw * sinPitch * axisLen
    drawLine(
        color = Color(0xFF38BDF8),
        start = Offset(originX, originY),
        end = Offset(originX + zx, originY - zy),
        strokeWidth = 2.5f
    )
}

private fun project3D(
    x: Float,
    y: Float,
    z: Float,
    centerX: Float,
    centerY: Float,
    fovDist: Float,
    cosYaw: Float,
    sinYaw: Float,
    cosPitch: Float,
    sinPitch: Float,
    cameraDist: Float
): Offset? {
    val x1 = x * cosYaw - z * sinYaw
    val z1 = x * sinYaw + z * cosYaw
    val y1 = y

    val y2 = y1 * cosPitch - z1 * sinPitch
    val z2 = y1 * sinPitch + z1 * cosPitch
    val x2 = x1

    val eyeZ = z2 + cameraDist
    if (eyeZ <= 0.1f) return null

    val scale = fovDist / eyeZ
    return Offset(centerX + x2 * scale, centerY - y2 * scale)
}

