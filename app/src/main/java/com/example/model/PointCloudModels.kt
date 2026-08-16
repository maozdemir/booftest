package com.example.model

data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val r: Int = 208,
    val g: Int = 188,
    val b: Int = 255,
    val intensity: Float = 1.0f
)

data class MeshTriangle(
    val v1: Int,
    val v2: Int,
    val v3: Int,
    val normalX: Float = 0f,
    val normalY: Float = 0f,
    val normalZ: Float = 1f
)

data class MeshSurface(
    val vertices: List<Point3D>,
    val triangles: List<MeshTriangle>,
    val normals: List<Point3D> = emptyList(),
    val smoothingPasses: Int = 0,
    val decimationLevel: Float = 1.0f,
    val meshingAlgorithm: String = "Poisson Surface Reconstruction"
)

data class CameraPose(
    val id: Int,
    val name: String,
    val position: Point3D,
    val rotationYaw: Float,
    val rotationPitch: Float,
    val rotationRoll: Float = 0f,
    val inliersCount: Int = 0,
    val imageUri: String? = null
)

data class FeatureMatch(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val isInlier: Boolean = true,
    val confidence: Float = 0.95f
)

data class PairwiseMatchResult(
    val viewIndex1: Int,
    val viewIndex2: Int,
    val matches: List<FeatureMatch>,
    val inlierRatio: Float,
    val fundamentalMatrixScore: Float
)

data class ReconstructedScene(
    val id: String,
    val title: String,
    val description: String,
    val points: List<Point3D>,
    val cameras: List<CameraPose>,
    val mesh: MeshSurface? = null,
    val pairwiseMatches: List<PairwiseMatchResult> = emptyList(),
    val totalFeaturesDetected: Int,
    val processingDurationMs: Long,
    val meanReprojectionError: Float,
    val minBound: Point3D,
    val maxBound: Point3D,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ColorMode(val label: String) {
    TRUE_RGB("RGB Color"),
    DEPTH_SPECTRUM("Depth Map"),
    ELEVATION_HEATMAP("Elevation"),
    NEON_CYBER("Cyber Neon")
}

enum class RenderMode(val label: String) {
    POINTS("Points"),
    WIREFRAME("Wireframe"),
    SHADED("Shaded"),
    TEXTURED("Colored Mesh")
}

enum class NavigationTab(val label: String) {
    CAPTURE("Capture"),
    POINTS("Points"),
    SCENE("Scene"),
    EXPORT("Export")
}

data class ReconstructionProgress(
    val isProcessing: Boolean = false,
    val currentStage: String = "",
    val progressFraction: Float = 0f,
    val stageIndex: Int = 0,
    val totalStages: Int = 6
)

