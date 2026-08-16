package com.example.model

import com.example.engine.SurfaceMeshingEngine
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object SampleDatasets {

    fun generateSculptureBustScene(): ReconstructedScene {
        val points = mutableListOf<Point3D>()
        val random = Random(42)

        // Head ellipsoid
        for (i in 0 until 1800) {
            val u = random.nextFloat() * 2 * Math.PI.toFloat()
            val v = (random.nextFloat() - 0.5f) * Math.PI.toFloat()
            val radX = 0.85f + (random.nextFloat() - 0.5f) * 0.05f
            val radY = 1.15f + (random.nextFloat() - 0.5f) * 0.05f
            val radZ = 0.95f + (random.nextFloat() - 0.5f) * 0.05f

            val x = radX * cos(v) * cos(u)
            val y = radY * sin(v) + 0.6f
            val z = radZ * cos(v) * sin(u)

            // Marble stone color with subtle warm tint
            val shade = (210 + random.nextInt(45)).coerceIn(0, 255)
            val r = (shade + 10).coerceIn(0, 255)
            val g = (shade + 5).coerceIn(0, 255)
            val b = shade
            points.add(Point3D(x, y, z, r, g, b))
        }

        // Facial details (nose, chin, brow ridge)
        for (i in 0 until 800) {
            val ny = random.nextFloat() * 0.6f + 0.4f
            val nx = (random.nextFloat() - 0.5f) * 0.35f
            val nz = 0.9f + (1.0f - (ny - 0.7f) * (ny - 0.7f) * 4f).coerceAtLeast(0f) * 0.35f + (random.nextFloat() - 0.5f) * 0.05f
            val r = (230 + random.nextInt(25)).coerceIn(0, 255)
            val g = (220 + random.nextInt(25)).coerceIn(0, 255)
            val b = 215
            points.add(Point3D(nx, ny, nz, r, g, b))
        }

        // Shoulder & Torso Base
        for (i in 0 until 2200) {
            val theta = random.nextFloat() * 2 * Math.PI.toFloat()
            val height = -random.nextFloat() * 1.4f
            val radiusX = 1.6f + (-height * 0.4f) + (random.nextFloat() - 0.5f) * 0.06f
            val radiusZ = 0.8f + (-height * 0.2f) + (random.nextFloat() - 0.5f) * 0.06f

            val x = radiusX * cos(theta)
            val y = height
            val z = radiusZ * sin(theta)

            val baseShade = (180 + random.nextInt(50)).coerceIn(0, 255)
            points.add(Point3D(x, y, z, baseShade, baseShade, baseShade + 15))
        }

        // Pedestal base
        for (i in 0 until 1200) {
            val px = (random.nextFloat() - 0.5f) * 2.6f
            val pz = (random.nextFloat() - 0.5f) * 1.8f
            val py = -1.4f - random.nextFloat() * 0.35f
            val r = 160 + random.nextInt(40)
            points.add(Point3D(px, py, pz, r, r, r + 20))
        }

        // Camera trajectory poses surrounding the bust
        val cameras = mutableListOf<CameraPose>()
        val numCams = 8
        for (i in 0 until numCams) {
            val angle = (i.toFloat() / numCams) * 2 * Math.PI.toFloat()
            val camDist = 3.4f
            val camX = camDist * sin(angle)
            val camZ = camDist * cos(angle)
            val camY = 0.4f + (if (i % 2 == 0) 0.3f else -0.2f)
            val yawDeg = (Math.toDegrees(angle.toDouble()) + 180.0).toFloat() % 360f
            val pitchDeg = -6.0f

            cameras.add(
                CameraPose(
                    id = i,
                    name = "View #${i + 1} (${yawDeg.toInt()}°)",
                    position = Point3D(camX, camY, camZ),
                    rotationYaw = yawDeg,
                    rotationPitch = pitchDeg,
                    inliersCount = 420 + (i * 28) % 150
                )
            )
        }

        val bustMesh = SurfaceMeshingEngine.generateSurfaceMesh(points, gridResolution = 40)

        return ReconstructedScene(
            id = "bust_marble",
            title = "Classical Marble Bust",
            description = "8-view uncalibrated multi-view stereo reconstruction with dense disparity depth fusion and Poisson surface meshing.",
            points = points,
            cameras = cameras,
            mesh = bustMesh,
            pairwiseMatches = createSamplePairwiseMatches(8),
            totalFeaturesDetected = 18402,
            processingDurationMs = 1240,
            meanReprojectionError = 0.48f,
            minBound = Point3D(-1.5f, -1.8f, -1.2f),
            maxBound = Point3D(1.5f, 1.8f, 1.4f)
        )
    }

    fun generateDroneTerrainScene(): ReconstructedScene {
        val points = mutableListOf<Point3D>()
        val random = Random(99)
        val gridSize = 65

        for (ix in 0 until gridSize) {
            for (iz in 0 until gridSize) {
                val x = (ix.toFloat() / gridSize - 0.5f) * 6.0f
                val z = (iz.toFloat() / gridSize - 0.5f) * 6.0f

                // Valley & hill elevation formula
                val dist = sqrt(x * x + z * z)
                val hill1 = (cos(x * 1.2f) * sin(z * 1.2f)) * 0.7f
                val hill2 = (cos(dist * 1.5f)) * 0.4f
                val noise = (random.nextFloat() - 0.5f) * 0.08f
                val y = hill1 + hill2 + noise - 0.5f

                // Terrain color based on elevation
                val r: Int
                val g: Int
                val b: Int
                when {
                    y > 0.4f -> {
                        // Rocky peak
                        r = 210 + random.nextInt(35)
                        g = 210 + random.nextInt(35)
                        b = 220
                    }
                    y > -0.1f -> {
                        // Grassland
                        r = 50 + random.nextInt(40)
                        g = 140 + random.nextInt(60)
                        b = 60 + random.nextInt(30)
                    }
                    else -> {
                        // River basin / dark earth
                        r = 60 + random.nextInt(30)
                        g = 80 + random.nextInt(40)
                        b = 130 + random.nextInt(50)
                    }
                }
                points.add(Point3D(x, y, z, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255)))
            }
        }

        // Camera flight grid
        val cameras = mutableListOf<CameraPose>()
        val flightRows = 3
        val flightCols = 3
        var camIdx = 0
        for (r in 0 until flightRows) {
            for (c in 0 until flightCols) {
                val cx = (c.toFloat() / (flightCols - 1) - 0.5f) * 4.0f
                val cz = (r.toFloat() / (flightRows - 1) - 0.5f) * 4.0f
                val cy = 3.5f
                cameras.add(
                    CameraPose(
                        id = camIdx,
                        name = "Aerial #${camIdx + 1}",
                        position = Point3D(cx, cy, cz),
                        rotationYaw = 0f,
                        rotationPitch = -75f,
                        inliersCount = 680 + (camIdx * 35) % 200
                    )
                )
                camIdx++
            }
        }

        val terrainMesh = SurfaceMeshingEngine.generateSurfaceMesh(points, gridResolution = 42)

        return ReconstructedScene(
            id = "drone_terrain",
            title = "Drone Topographic Elevation",
            description = "9-view aerial nadir uncalibrated MVS mapping of rugged natural terrain with contour surface mesh.",
            points = points,
            cameras = cameras,
            mesh = terrainMesh,
            pairwiseMatches = createSamplePairwiseMatches(9),
            totalFeaturesDetected = 24190,
            processingDurationMs = 1860,
            meanReprojectionError = 0.39f,
            minBound = Point3D(-3.0f, -1.2f, -3.0f),
            maxBound = Point3D(3.0f, 1.4f, 3.0f)
        )
    }

    fun generateGeometricStructureScene(): ReconstructedScene {
        val points = mutableListOf<Point3D>()
        val random = Random(123)

        // Geometric pavilion arches
        val numArches = 4
        for (arch in 0 until numArches) {
            val rotAngle = (arch.toFloat() / numArches) * Math.PI.toFloat() * 2f
            val cosR = cos(rotAngle)
            val sinR = sin(rotAngle)

            // Arch curve
            for (t in 0..120) {
                val phi = (t.toFloat() / 120f) * Math.PI.toFloat()
                val archRad = 1.6f
                val archY = sin(phi) * archRad
                val archRadial = cos(phi) * archRad

                for (w in -3..3) {
                    val archOffset = w * 0.05f
                    val rx = (archRadial) * cosR - archOffset * sinR
                    val rz = (archRadial) * sinR + archOffset * cosR
                    val ry = archY - 0.2f

                    val r = (208 + random.nextInt(35)).coerceIn(0, 255)
                    val g = (188 + random.nextInt(40)).coerceIn(0, 255)
                    val b = 255
                    points.add(Point3D(rx, ry, rz, r, g, b))
                }
            }
        }

        // Dome lattice
        for (i in 0 until 1500) {
            val u = random.nextFloat() * 2 * Math.PI.toFloat()
            val v = random.nextFloat() * (Math.PI.toFloat() / 2f)
            val rad = 1.7f + (random.nextFloat() - 0.5f) * 0.04f
            val x = rad * cos(v) * cos(u)
            val y = rad * sin(v) + 0.3f
            val z = rad * cos(v) * sin(u)

            val neonCyan = 56 + random.nextInt(60)
            points.add(Point3D(x, y, z, 130, 210, 255))
        }

        // Cameras
        val cameras = mutableListOf<CameraPose>()
        val numCams = 6
        for (i in 0 until numCams) {
            val angle = (i.toFloat() / numCams) * 2 * Math.PI.toFloat()
            val camX = 3.6f * sin(angle)
            val camZ = 3.6f * cos(angle)
            val camY = 1.0f
            val yawDeg = (Math.toDegrees(angle.toDouble()) + 180.0).toFloat() % 360f

            cameras.add(
                CameraPose(
                    id = i,
                    name = "Perspective #${i + 1}",
                    position = Point3D(camX, camY, camZ),
                    rotationYaw = yawDeg,
                    rotationPitch = -15f,
                    inliersCount = 540 + (i * 40) % 180
                )
            )
        }

        val pavilionMesh = SurfaceMeshingEngine.generateSurfaceMesh(points, gridResolution = 38)

        return ReconstructedScene(
            id = "pavilion_arch",
            title = "Cyber Geometric Pavilion",
            description = "6-view architectural structure reconstructed with BoofCV multi-baseline stereo and lattice mesh generation.",
            points = points,
            cameras = cameras,
            mesh = pavilionMesh,
            pairwiseMatches = createSamplePairwiseMatches(6),
            totalFeaturesDetected = 16340,
            processingDurationMs = 1120,
            meanReprojectionError = 0.42f,
            minBound = Point3D(-2.0f, -1.0f, -2.0f),
            maxBound = Point3D(2.0f, 2.2f, 2.0f)
        )
    }

    private fun createSamplePairwiseMatches(numViews: Int): List<PairwiseMatchResult> {
        val list = mutableListOf<PairwiseMatchResult>()
        val random = Random(77)

        for (v1 in 0 until numViews - 1) {
            val v2 = v1 + 1
            val matches = mutableListOf<FeatureMatch>()
            val numFeatures = 45 + random.nextInt(25)

            for (f in 0 until numFeatures) {
                val x1 = 0.1f + random.nextFloat() * 0.8f
                val y1 = 0.15f + random.nextFloat() * 0.7f
                // Parallax shift
                val disparityX = (random.nextFloat() - 0.48f) * 0.18f
                val disparityY = (random.nextFloat() - 0.5f) * 0.04f
                val x2 = (x1 + disparityX).coerceIn(0.05f, 0.95f)
                val y2 = (y1 + disparityY).coerceIn(0.05f, 0.95f)
                val isInlier = random.nextFloat() > 0.08f

                matches.add(FeatureMatch(x1, y1, x2, y2, isInlier, confidence = 0.85f + random.nextFloat() * 0.14f))
            }

            list.add(
                PairwiseMatchResult(
                    viewIndex1 = v1,
                    viewIndex2 = v2,
                    matches = matches,
                    inlierRatio = 0.91f + (random.nextFloat() * 0.07f),
                    fundamentalMatrixScore = 0.96f
                )
            )
        }
        return list
    }
}
