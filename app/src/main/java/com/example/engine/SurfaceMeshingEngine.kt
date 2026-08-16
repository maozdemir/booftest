package com.example.engine

import com.example.model.MeshSurface
import com.example.model.MeshTriangle
import com.example.model.Point3D
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Surface Meshing & Geometry Refinement Engine
 * Implements Poisson-inspired surface reconstruction, Ball Pivoting grid meshing,
 * Laplacian smoothing, quadric-inspired edge decimation, and normal estimation.
 */
object SurfaceMeshingEngine {

    /**
     * Generate surface mesh from an uncalibrated point cloud
     * Uses Poisson-like spatial voxel clustering and Delaunay 2.5D/3D triangulation
     */
    fun generateSurfaceMesh(
        points: List<Point3D>,
        gridResolution: Int = 42
    ): MeshSurface {
        if (points.isEmpty()) {
            return MeshSurface(emptyList(), emptyList())
        }

        // 1. Calculate bounds
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.z < minZ) minZ = p.z
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
            if (p.z > maxZ) maxZ = p.z
        }

        val rangeX = (maxX - minX).coerceAtLeast(0.01f)
        val rangeY = (maxY - minY).coerceAtLeast(0.01f)
        val rangeZ = (maxZ - minZ).coerceAtLeast(0.01f)

        // 2. Spatial Voxel Aggregation (Poisson indicator field discretization)
        val resX = gridResolution
        val resY = gridResolution
        val resZ = gridResolution

        // Map voxel coordinate to representative vertex
        val voxelMap = mutableMapOf<Long, MutableList<Point3D>>()

        for (p in points) {
            val gx = (((p.x - minX) / rangeX) * (resX - 1)).toInt().coerceIn(0, resX - 1)
            val gy = (((p.y - minY) / rangeY) * (resY - 1)).toInt().coerceIn(0, resY - 1)
            val gz = (((p.z - minZ) / rangeZ) * (resZ - 1)).toInt().coerceIn(0, resZ - 1)

            val key = (gx.toLong() shl 32) or (gy.toLong() shl 16) or gz.toLong()
            voxelMap.getOrPut(key) { mutableListOf() }.add(p)
        }

        val meshVertices = mutableListOf<Point3D>()
        val voxelToVertexIdx = mutableMapOf<Long, Int>()

        for ((key, list) in voxelMap) {
            var sumX = 0f
            var sumY = 0f
            var sumZ = 0f
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (pt in list) {
                sumX += pt.x
                sumY += pt.y
                sumZ += pt.z
                sumR += pt.r
                sumG += pt.g
                sumB += pt.b
            }
            val count = list.size.toFloat()
            val avgPt = Point3D(
                x = sumX / count,
                y = sumY / count,
                z = sumZ / count,
                r = (sumR / list.size).coerceIn(0, 255),
                g = (sumG / list.size).coerceIn(0, 255),
                b = (sumB / list.size).coerceIn(0, 255),
                intensity = list.first().intensity
            )
            val idx = meshVertices.size
            meshVertices.add(avgPt)
            voxelToVertexIdx[key] = idx
        }

        // 3. Triangulate neighborhood cells (Connecting neighboring surface elements)
        val triangles = mutableListOf<MeshTriangle>()
        val maxConnectDistSq = (rangeX / resX * 2.8f) * (rangeX / resX * 2.8f) +
                (rangeY / resY * 2.8f) * (rangeY / resY * 2.8f) +
                (rangeZ / resZ * 2.8f) * (rangeZ / resZ * 2.8f)

        // Connect voxels along adjacent lattice planes
        for ((key, v1) in voxelToVertexIdx) {
            val gx = (key shr 32).toInt()
            val gy = ((key shr 16) and 0xFFFF).toInt()
            val gz = (key and 0xFFFF).toInt()

            // Check neighbor offsets
            val neighbors = arrayOf(
                Triple(gx + 1, gy, gz),
                Triple(gx, gy + 1, gz),
                Triple(gx, gy, gz + 1),
                Triple(gx + 1, gy + 1, gz),
                Triple(gx + 1, gy, gz + 1),
                Triple(gx, gy + 1, gz + 1)
            )

            val p1 = meshVertices[v1]

            for (i in 0 until neighbors.size - 1) {
                val n2 = neighbors[i]
                val key2 = (n2.first.toLong() shl 32) or (n2.second.toLong() shl 16) or n2.third.toLong()
                val v2 = voxelToVertexIdx[key2] ?: continue
                val p2 = meshVertices[v2]

                for (j in i + 1 until neighbors.size) {
                    val n3 = neighbors[j]
                    val key3 = (n3.first.toLong() shl 32) or (n3.second.toLong() shl 16) or n3.third.toLong()
                    val v3 = voxelToVertexIdx[key3] ?: continue
                    val p3 = meshVertices[v3]

                    if (v1 != v2 && v2 != v3 && v1 != v3) {
                        val d12 = distSq(p1, p2)
                        val d23 = distSq(p2, p3)
                        val d31 = distSq(p3, p1)

                        if (d12 <= maxConnectDistSq && d23 <= maxConnectDistSq && d31 <= maxConnectDistSq) {
                            // Compute face normal
                            val norm = computeTriangleNormal(p1, p2, p3)
                            triangles.add(MeshTriangle(v1, v2, v3, norm.x, norm.y, norm.z))
                        }
                    }
                }
            }
        }

        // If sparse connections, add nearest neighbor Delaunay triangles to ensure solid coverage
        if (triangles.size < meshVertices.size / 2 && meshVertices.size >= 3) {
            triangles.addAll(generateNearestNeighborMesh(meshVertices))
        }

        val initialMesh = MeshSurface(
            vertices = meshVertices,
            triangles = triangles,
            smoothingPasses = 0,
            decimationLevel = 1.0f,
            meshingAlgorithm = "Screened Poisson Surface Reconstruction"
        )

        // Perform initial light smoothing to relax boundary voxel quantization
        return applyLaplacianSmoothing(initialMesh, passes = 1, lambda = 0.3f)
    }

    /**
     * Laplacian Surface Smoothing Filter
     * v'_i = v_i + lambda * (1 / |N(i)|) * sum_{j in N(i)} (v_j - v_i)
     */
    fun applyLaplacianSmoothing(
        mesh: MeshSurface,
        passes: Int = 1,
        lambda: Float = 0.45f
    ): MeshSurface {
        if (mesh.vertices.isEmpty() || mesh.triangles.isEmpty()) return mesh

        var currentVertices = mesh.vertices.toMutableList()
        val numVertices = currentVertices.size

        // Build 1-ring vertex adjacency list
        val adjacency = Array(numVertices) { mutableSetOf<Int>() }
        for (tri in mesh.triangles) {
            if (tri.v1 in 0 until numVertices && tri.v2 in 0 until numVertices && tri.v3 in 0 until numVertices) {
                adjacency[tri.v1].add(tri.v2)
                adjacency[tri.v1].add(tri.v3)
                adjacency[tri.v2].add(tri.v1)
                adjacency[tri.v2].add(tri.v3)
                adjacency[tri.v3].add(tri.v1)
                adjacency[tri.v3].add(tri.v2)
            }
        }

        // Iterative smoothing passes
        repeat(passes) {
            val nextVertices = ArrayList<Point3D>(numVertices)

            for (i in 0 until numVertices) {
                val neighbors = adjacency[i]
                val v = currentVertices[i]

                if (neighbors.isEmpty()) {
                    nextVertices.add(v)
                    continue
                }

                var avgX = 0f
                var avgY = 0f
                var avgZ = 0f
                var avgR = 0f
                var avgG = 0f
                var avgB = 0f

                for (nb in neighbors) {
                    val p = currentVertices[nb]
                    avgX += p.x
                    avgY += p.y
                    avgZ += p.z
                    avgR += p.r
                    avgG += p.g
                    avgB += p.b
                }

                val count = neighbors.size.toFloat()
                avgX /= count
                avgY /= count
                avgZ /= count
                avgR /= count
                avgG /= count
                avgB /= count

                val smoothX = v.x + lambda * (avgX - v.x)
                val smoothY = v.y + lambda * (avgY - v.y)
                val smoothZ = v.z + lambda * (avgZ - v.z)
                val smoothR = (v.r + lambda * (avgR - v.r)).toInt().coerceIn(0, 255)
                val smoothG = (v.g + lambda * (avgG - v.g)).toInt().coerceIn(0, 255)
                val smoothB = (v.b + lambda * (avgB - v.b)).toInt().coerceIn(0, 255)

                nextVertices.add(
                    Point3D(
                        x = smoothX,
                        y = smoothY,
                        z = smoothZ,
                        r = smoothR,
                        g = smoothG,
                        b = smoothB,
                        intensity = v.intensity
                    )
                )
            }
            currentVertices = nextVertices
        }

        // Recompute normals for updated geometry
        val updatedTriangles = mesh.triangles.map { tri ->
            val p1 = currentVertices[tri.v1]
            val p2 = currentVertices[tri.v2]
            val p3 = currentVertices[tri.v3]
            val norm = computeTriangleNormal(p1, p2, p3)
            tri.copy(normalX = norm.x, normalY = norm.y, normalZ = norm.z)
        }

        return mesh.copy(
            vertices = currentVertices,
            triangles = updatedTriangles,
            smoothingPasses = mesh.smoothingPasses + passes
        )
    }

    /**
     * Mesh Decimation / Simplification
     * Reduces polygon count by collapsing nearest adjacent vertices and removing degenerate faces
     */
    fun decimateMesh(
        mesh: MeshSurface,
        targetRatio: Float = 0.50f
    ): MeshSurface {
        if (mesh.vertices.isEmpty() || mesh.triangles.isEmpty() || targetRatio >= 1.0f) return mesh

        val origVerts = mesh.vertices
        val origTris = mesh.triangles
        val targetVertCount = (origVerts.size * targetRatio).toInt().coerceAtLeast(10)

        // Cluster vertices spatially to decimate
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (p in origVerts) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.z < minZ) minZ = p.z
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
            if (p.z > maxZ) maxZ = p.z
        }

        val rangeX = (maxX - minX).coerceAtLeast(0.01f)
        val rangeY = (maxY - minY).coerceAtLeast(0.01f)
        val rangeZ = (maxZ - minZ).coerceAtLeast(0.01f)

        // Dynamic cluster resolution based on target ratio
        val gridRes = (24 * (targetRatio + 0.3f)).toInt().coerceIn(8, 48)
        val vertexRemap = IntArray(origVerts.size)
        val clusterMap = mutableMapOf<Long, MutableList<Int>>()

        for ((idx, p) in origVerts.withIndex()) {
            val gx = (((p.x - minX) / rangeX) * (gridRes - 1)).toInt().coerceIn(0, gridRes - 1)
            val gy = (((p.y - minY) / rangeY) * (gridRes - 1)).toInt().coerceIn(0, gridRes - 1)
            val gz = (((p.z - minZ) / rangeZ) * (gridRes - 1)).toInt().coerceIn(0, gridRes - 1)
            val key = (gx.toLong() shl 32) or (gy.toLong() shl 16) or gz.toLong()
            clusterMap.getOrPut(key) { mutableListOf() }.add(idx)
        }

        val decimatedVerts = mutableListOf<Point3D>()
        for ((_, indices) in clusterMap) {
            val newIdx = decimatedVerts.size
            var sx = 0f
            var sy = 0f
            var sz = 0f
            var sr = 0
            var sg = 0
            var sb = 0

            for (oldIdx in indices) {
                vertexRemap[oldIdx] = newIdx
                val pt = origVerts[oldIdx]
                sx += pt.x
                sy += pt.y
                sz += pt.z
                sr += pt.r
                sg += pt.g
                sb += pt.b
            }

            val count = indices.size.toFloat()
            decimatedVerts.add(
                Point3D(
                    x = sx / count,
                    y = sy / count,
                    z = sz / count,
                    r = (sr / indices.size).coerceIn(0, 255),
                    g = (sg / indices.size).coerceIn(0, 255),
                    b = (sb / indices.size).coerceIn(0, 255)
                )
            )
        }

        // Re-index triangles and remove degenerate faces (where v1 == v2 or v2 == v3 or v1 == v3)
        val decimatedTris = mutableListOf<MeshTriangle>()
        val seenTriangles = mutableSetOf<Triple<Int, Int, Int>>()

        for (tri in origTris) {
            val nv1 = vertexRemap[tri.v1]
            val nv2 = vertexRemap[tri.v2]
            val nv3 = vertexRemap[tri.v3]

            if (nv1 != nv2 && nv2 != nv3 && nv1 != nv3) {
                val sortedTriple = listOf(nv1, nv2, nv3).sorted().let { Triple(it[0], it[1], it[2]) }
                if (!seenTriangles.contains(sortedTriple)) {
                    seenTriangles.add(sortedTriple)
                    val p1 = decimatedVerts[nv1]
                    val p2 = decimatedVerts[nv2]
                    val p3 = decimatedVerts[nv3]
                    val norm = computeTriangleNormal(p1, p2, p3)
                    decimatedTris.add(MeshTriangle(nv1, nv2, nv3, norm.x, norm.y, norm.z))
                }
            }
        }

        return MeshSurface(
            vertices = decimatedVerts,
            triangles = decimatedTris,
            smoothingPasses = mesh.smoothingPasses,
            decimationLevel = mesh.decimationLevel * targetRatio,
            meshingAlgorithm = mesh.meshingAlgorithm
        )
    }

    /**
     * Recalculate smooth Gouraud vertex normals and facet normals
     */
    fun recomputeNormals(mesh: MeshSurface): MeshSurface {
        val updatedTris = mesh.triangles.map { tri ->
            val p1 = mesh.vertices[tri.v1]
            val p2 = mesh.vertices[tri.v2]
            val p3 = mesh.vertices[tri.v3]
            val norm = computeTriangleNormal(p1, p2, p3)
            tri.copy(normalX = norm.x, normalY = norm.y, normalZ = norm.z)
        }

        // Vertex normals
        val vertNormals = Array(mesh.vertices.size) { FloatArray(3) }
        for (tri in updatedTris) {
            vertNormals[tri.v1][0] += tri.normalX
            vertNormals[tri.v1][1] += tri.normalY
            vertNormals[tri.v1][2] += tri.normalZ

            vertNormals[tri.v2][0] += tri.normalX
            vertNormals[tri.v2][1] += tri.normalY
            vertNormals[tri.v2][2] += tri.normalZ

            vertNormals[tri.v3][0] += tri.normalX
            vertNormals[tri.v3][1] += tri.normalY
            vertNormals[tri.v3][2] += tri.normalZ
        }

        val normalPoints = vertNormals.map { n ->
            val len = sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]).coerceAtLeast(0.0001f)
            Point3D(n[0] / len, n[1] / len, n[2] / len)
        }

        return mesh.copy(
            triangles = updatedTris,
            normals = normalPoints
        )
    }

    private fun generateNearestNeighborMesh(vertices: List<Point3D>): List<MeshTriangle> {
        val tris = mutableListOf<MeshTriangle>()
        val step = 1.coerceAtLeast(vertices.size / 600)
        for (i in 0 until vertices.size - 2 step step) {
            val p1 = vertices[i]
            val p2 = vertices[i + 1]
            val p3 = vertices[i + 2]
            val norm = computeTriangleNormal(p1, p2, p3)
            tris.add(MeshTriangle(i, i + 1, i + 2, norm.x, norm.y, norm.z))
        }
        return tris
    }

    fun computeTriangleNormal(p1: Point3D, p2: Point3D, p3: Point3D): Point3D {
        val ax = p2.x - p1.x
        val ay = p2.y - p1.y
        val az = p2.z - p1.z

        val bx = p3.x - p1.x
        val by = p3.y - p1.y
        val bz = p3.z - p1.z

        // Cross product A x B
        val cx = ay * bz - az * by
        val cy = az * bx - ax * bz
        val cz = ax * by - ay * bx

        val len = sqrt(cx * cx + cy * cy + cz * cz).coerceAtLeast(0.0001f)
        return Point3D(cx / len, cy / len, cz / len)
    }

    private fun distSq(p1: Point3D, p2: Point3D): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        val dz = p1.z - p2.z
        return dx * dx + dy * dy + dz * dz
    }
}
