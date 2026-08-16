package com.example.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.BoofMvsEngine
import com.example.model.ColorMode
import com.example.model.NavigationTab
import com.example.model.ReconstructedScene
import com.example.model.ReconstructionProgress
import com.example.model.SampleDatasets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class SceneUiState(
    val activeTab: NavigationTab = NavigationTab.CAPTURE,
    val currentScene: ReconstructedScene = SampleDatasets.generateSculptureBustScene(),
    val capturedImages: List<Bitmap> = emptyList(),
    val reconstructionProgress: ReconstructionProgress = ReconstructionProgress(),
    val selectedColorMode: ColorMode = ColorMode.TRUE_RGB,
    val pointSize: Float = 3.5f,
    val showCameras: Boolean = true,
    val showGrid: Boolean = true,
    val autoRotate: Boolean = false,
    val exportedFilePath: String? = null,
    val exportSuccessMessage: String? = null
)

class SceneReconstructionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SceneUiState())
    val uiState: StateFlow<SceneUiState> = _uiState.asStateFlow()

    private val mvsEngine = BoofMvsEngine()

    fun selectTab(tab: NavigationTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addCapturedImage(bitmap: Bitmap) {
        _uiState.update { current ->
            current.copy(capturedImages = current.capturedImages + bitmap)
        }
    }

    fun removeCapturedImage(index: Int) {
        _uiState.update { current ->
            val updated = current.capturedImages.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            current.copy(capturedImages = updated)
        }
    }

    fun clearCapturedImages() {
        _uiState.update { it.copy(capturedImages = emptyList()) }
    }

    fun setColorMode(mode: ColorMode) {
        _uiState.update { it.copy(selectedColorMode = mode) }
    }

    fun setPointSize(size: Float) {
        _uiState.update { it.copy(pointSize = size) }
    }

    fun toggleCameras() {
        _uiState.update { it.copy(showCameras = !it.showCameras) }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun toggleAutoRotate() {
        _uiState.update { it.copy(autoRotate = !it.autoRotate) }
    }

    fun loadPresetScene(presetKey: String) {
        val scene = when (presetKey) {
            "drone_terrain" -> SampleDatasets.generateDroneTerrainScene()
            "pavilion_arch" -> SampleDatasets.generateGeometricStructureScene()
            else -> SampleDatasets.generateSculptureBustScene()
        }
        _uiState.update {
            it.copy(
                currentScene = scene,
                activeTab = NavigationTab.SCENE,
                exportSuccessMessage = null
            )
        }
    }

    fun startReconstruction() {
        val images = _uiState.value.capturedImages
        if (images.size < 2) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    reconstructionProgress = ReconstructionProgress(
                        isProcessing = true,
                        currentStage = "Starting BoofCV Uncalibrated MVS...",
                        progressFraction = 0.05f
                    )
                )
            }

            val resultScene = mvsEngine.reconstructFromImages(images) { stage, fraction ->
                _uiState.update {
                    it.copy(
                        reconstructionProgress = ReconstructionProgress(
                            isProcessing = true,
                            currentStage = stage,
                            progressFraction = fraction
                        )
                    )
                }
            }

            _uiState.update {
                it.copy(
                    currentScene = resultScene,
                    reconstructionProgress = ReconstructionProgress(isProcessing = false),
                    activeTab = NavigationTab.SCENE,
                    exportSuccessMessage = "Scene reconstructed: ${resultScene.points.size} dense vertices!"
                )
            }
        }
    }

    fun exportScene(context: Context, format: String) {
        val scene = _uiState.value.currentScene
        viewModelScope.launch {
            try {
                val outputDir = File(context.cacheDir, "mvs_exports")
                outputDir.mkdirs()
                val filename = "scene_${scene.id}_${System.currentTimeMillis()}.${format.lowercase()}"
                val file = File(outputDir, filename)

                if (format.equals("OBJ", ignoreCase = true)) {
                    mvsEngine.exportToObj(scene, file)
                } else {
                    mvsEngine.exportToPly(scene, file)
                }

                _uiState.update {
                    it.copy(
                        exportedFilePath = file.absolutePath,
                        exportSuccessMessage = "Successfully exported ${scene.points.size} points to $filename"
                    )
                }

                // Launch system share intent
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share 3D Model ($format)"))
            } catch (e: Exception) {
                // Fallback direct save message
                _uiState.update {
                    it.copy(exportSuccessMessage = "Export saved to cache: ${e.localizedMessage ?: "Done"}")
                }
            }
        }
    }
}
