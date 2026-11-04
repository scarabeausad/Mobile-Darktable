package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.DarktableDatabase
import com.example.engine.DarktableEngine
import com.example.model.ColorFilterMode
import com.example.model.ColorLabel
import com.example.model.DarktableStyle
import com.example.model.EditParams
import com.example.model.HistogramData
import com.example.model.PhotoItem
import com.example.model.RatingComparator
import com.example.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DarktableMode {
    LIGHTTABLE, DARKROOM, STYLES, EXIF_INFO, EXPORT
}

class DarktableViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhotoRepository

    init {
        val db = DarktableDatabase.getDatabase(application)
        repository = PhotoRepository(db.darktableDao())
        viewModelScope.launch {
            repository.ensureInitialDataSeeded()
        }
    }

    val rawPhotos = repository.allPhotos
    val styles = repository.allStyles.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DarktableStyle.BUILT_IN_STYLES
    )

    // Mode
    private val _currentMode = MutableStateFlow(DarktableMode.LIGHTTABLE)
    val currentMode: StateFlow<DarktableMode> = _currentMode.asStateFlow()

    // Lighttable Filter state
    // _ratingFilter: null = ALL, or 0..5
    private val _ratingFilter = MutableStateFlow<Int?>(null)
    val ratingFilter = _ratingFilter.asStateFlow()

    private val _ratingComparator = MutableStateFlow(RatingComparator.EXACT)
    val ratingComparator = _ratingComparator.asStateFlow()

    private val _selectedColorLabels = MutableStateFlow<Set<ColorLabel>>(emptySet())
    val selectedColorLabels = _selectedColorLabels.asStateFlow()

    private val _colorFilterMode = MutableStateFlow(ColorFilterMode.AT_LEAST_ONE)
    val colorFilterMode = _colorFilterMode.asStateFlow()

    private val _showRejected = MutableStateFlow(false)
    val showRejected = _showRejected.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _gridColumns = MutableStateFlow(2)
    val gridColumns = _gridColumns.asStateFlow()

    // Backwards-compatible flows
    val minRatingFilter = _ratingFilter.map { it ?: 0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val colorLabelFilter = _selectedColorLabels.map { it.firstOrNull() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private data class FilterCriteria(
        val rating: Int?,
        val comparator: RatingComparator,
        val selectedColors: Set<ColorLabel>,
        val colorMode: ColorFilterMode,
        val showRej: Boolean,
        val query: String
    )

    // Filtered photos stream
    val filteredPhotos: StateFlow<List<PhotoItem>> = combine(
        combine(ratingFilter, ratingComparator, selectedColorLabels) { r, comp, colors ->
            Triple(r, comp, colors)
        },
        combine(colorFilterMode, showRejected, searchQuery) { mode, rej, q ->
            Triple(mode, rej, q)
        }
    ) { (r, comp, colors), (mode, rej, q) ->
        FilterCriteria(r, comp, colors, mode, rej, q)
    }.combine(rawPhotos) { criteria, photos ->
        photos.filter { photo ->
            // 1. Star Rating match
            val matchRating = if (criteria.rating == null) {
                true
            } else {
                when (criteria.comparator) {
                    RatingComparator.EXACT -> photo.rating == criteria.rating
                    RatingComparator.LESS_EQUAL -> photo.rating <= criteria.rating
                    RatingComparator.GREATER_EQUAL -> photo.rating >= criteria.rating
                }
            }

            // 2. Color Tags match
            val matchColor = if (criteria.selectedColors.isEmpty()) {
                true
            } else {
                when (criteria.colorMode) {
                    ColorFilterMode.AT_LEAST_ONE -> {
                        // Images that have at least one of the selected colors
                        photo.colorLabels.any { it in criteria.selectedColors }
                    }
                    ColorFilterMode.ONLY_SELECTED -> {
                        // Images that have ONLY the selected colors (and at least one color)
                        photo.colorLabels.isNotEmpty() && photo.colorLabels.all { it in criteria.selectedColors }
                    }
                }
            }

            // 3. Rejection & Search
            val matchRej = criteria.showRej || !photo.isRejected
            val matchQuery = criteria.query.isEmpty() ||
                    photo.title.contains(criteria.query, ignoreCase = true) ||
                    photo.cameraModel.contains(criteria.query, ignoreCase = true) ||
                    photo.tags.any { it.contains(criteria.query, ignoreCase = true) }

            matchRating && matchColor && matchRej && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Photo & Darkroom state
    private val _selectedPhoto = MutableStateFlow<PhotoItem?>(null)
    val selectedPhoto: StateFlow<PhotoItem?> = _selectedPhoto.asStateFlow()

    private val _activeParams = MutableStateFlow(EditParams())
    val activeParams: StateFlow<EditParams> = _activeParams.asStateFlow()

    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    private val _currentHistogram = MutableStateFlow(HistogramData())
    val currentHistogram: StateFlow<HistogramData> = _currentHistogram.asStateFlow()

    // Viewport Comparison tools
    private val _isSplitCompareActive = MutableStateFlow(false)
    val isSplitCompareActive = _isSplitCompareActive.asStateFlow()

    private val _isHoldOriginalActive = MutableStateFlow(false)
    val isHoldOriginalActive = _isHoldOriginalActive.asStateFlow()

    // Export Status
    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage = _exportMessage.asStateFlow()

    fun setMode(mode: DarktableMode) {
        _currentMode.value = mode
    }

    fun setRatingFilter(rating: Int?) {
        _ratingFilter.value = rating
    }

    fun setRatingComparator(comparator: RatingComparator) {
        _ratingComparator.value = comparator
    }

    fun cycleRatingComparator() {
        _ratingComparator.value = when (_ratingComparator.value) {
            RatingComparator.EXACT -> RatingComparator.LESS_EQUAL
            RatingComparator.LESS_EQUAL -> RatingComparator.GREATER_EQUAL
            RatingComparator.GREATER_EQUAL -> RatingComparator.EXACT
        }
    }

    fun toggleColorFilter(label: ColorLabel) {
        val current = _selectedColorLabels.value
        _selectedColorLabels.value = if (current.contains(label)) {
            current - label
        } else {
            current + label
        }
    }

    fun setColorFilterMode(mode: ColorFilterMode) {
        _colorFilterMode.value = mode
    }

    fun cycleColorFilterMode() {
        _colorFilterMode.value = when (_colorFilterMode.value) {
            ColorFilterMode.AT_LEAST_ONE -> ColorFilterMode.ONLY_SELECTED
            ColorFilterMode.ONLY_SELECTED -> ColorFilterMode.AT_LEAST_ONE
        }
    }

    fun clearColorFilter() {
        _selectedColorLabels.value = emptySet()
    }

    fun setMinRatingFilter(rating: Int) {
        _ratingFilter.value = if (rating <= 0) null else rating
    }

    fun setColorLabelFilter(label: ColorLabel?) {
        _selectedColorLabels.value = if (label == null || label == ColorLabel.NONE) emptySet() else setOf(label)
    }

    fun toggleShowRejected() {
        _showRejected.value = !_showRejected.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGridColumns(cols: Int) {
        _gridColumns.value = cols.coerceIn(1, 3)
    }

    fun selectPhoto(photo: PhotoItem, navigateToDarkroom: Boolean = true) {
        _selectedPhoto.value = photo
        _activeParams.value = photo.params
        if (navigateToDarkroom) {
            _currentMode.value = DarktableMode.DARKROOM
        }
        loadBitmapForPhoto(photo)
    }

    private fun loadBitmapForPhoto(photo: PhotoItem) {
        viewModelScope.launch {
            val bmp = DarktableEngine.loadBitmap(getApplication(), photo.uriString, 2048)
            _currentBitmap.value = bmp
            if (bmp != null) {
                val histo = DarktableEngine.computeHistogram(bmp)
                _currentHistogram.value = histo
            }
        }
    }

    fun updateParamLive(newParams: EditParams) {
        _activeParams.value = newParams
    }

    fun commitParamChange(newParams: EditParams, moduleName: String, description: String) {
        _activeParams.value = newParams
        val photo = _selectedPhoto.value ?: return
        viewModelScope.launch {
            repository.updateParams(photo, newParams, moduleName, description)
            _selectedPhoto.value = photo.copy(params = newParams)
        }
    }

    fun resetActiveParams() {
        val defaultParams = EditParams()
        _activeParams.value = defaultParams
        val photo = _selectedPhoto.value ?: return
        viewModelScope.launch {
            repository.updateParams(photo, defaultParams, "Reset All", "Restored to neutral original state")
            _selectedPhoto.value = photo.copy(params = defaultParams)
        }
    }

    fun applyStyle(style: DarktableStyle) {
        _activeParams.value = style.params
        val photo = _selectedPhoto.value ?: return
        viewModelScope.launch {
            repository.updateParams(
                photo,
                style.params,
                "Style: ${style.name}",
                "Applied ${style.category} preset"
            )
            _selectedPhoto.value = photo.copy(params = style.params)
        }
    }

    fun saveCurrentStyle(name: String, description: String, category: String) {
        val params = _activeParams.value
        viewModelScope.launch {
            repository.saveCustomStyle(name, description, category, params)
        }
    }

    fun deleteStyle(style: DarktableStyle) {
        viewModelScope.launch {
            repository.deleteStyle(style)
        }
    }

    fun restoreHistoryStep(stepIndex: Int) {
        val photo = _selectedPhoto.value ?: return
        viewModelScope.launch {
            repository.restoreHistoryStep(photo, stepIndex)
            val updated = repository.getPhotoById(photo.id)
            if (updated != null) {
                _selectedPhoto.value = updated
                _activeParams.value = updated.params
            }
        }
    }

    fun saveSnapshotA() {
        val photo = _selectedPhoto.value ?: return
        viewModelScope.launch {
            repository.saveSnapshotA(photo)
            _selectedPhoto.value = photo.copy(snapshotA = _activeParams.value)
        }
    }

    fun saveSnapshotB() {
        val photo = _selectedPhoto.value ?: return
        viewModelScope.launch {
            repository.saveSnapshotB(photo)
            _selectedPhoto.value = photo.copy(snapshotB = _activeParams.value)
        }
    }

    fun restoreSnapshot(snapshotParams: EditParams) {
        commitParamChange(snapshotParams, "Restore Snapshot", "Restored saved parameter snapshot")
    }

    fun toggleSplitCompare() {
        _isSplitCompareActive.value = !_isSplitCompareActive.value
    }

    fun setHoldOriginal(hold: Boolean) {
        _isHoldOriginalActive.value = hold
    }

    fun updateRating(photo: PhotoItem, rating: Int) {
        viewModelScope.launch {
            repository.updateRating(photo, rating)
            if (_selectedPhoto.value?.id == photo.id) {
                _selectedPhoto.value = photo.copy(rating = rating)
            }
        }
    }

    fun updateColorLabel(photo: PhotoItem, label: ColorLabel) {
        viewModelScope.launch {
            repository.updateColorLabel(photo, label)
            val updatedLabels = if (label == ColorLabel.NONE) {
                emptySet()
            } else if (photo.colorLabels.contains(label)) {
                photo.colorLabels - label
            } else {
                photo.colorLabels + label
            }
            if (_selectedPhoto.value?.id == photo.id) {
                _selectedPhoto.value = photo.copy(colorLabels = updatedLabels)
            }
        }
    }

    fun togglePhotoColorLabel(photo: PhotoItem, label: ColorLabel) {
        updateColorLabel(photo, label)
    }

    fun toggleReject(photo: PhotoItem) {
        viewModelScope.launch {
            repository.toggleReject(photo)
            if (_selectedPhoto.value?.id == photo.id) {
                _selectedPhoto.value = photo.copy(isRejected = !photo.isRejected)
            }
        }
    }

    fun duplicatePhoto(photo: PhotoItem) {
        viewModelScope.launch {
            repository.duplicatePhoto(photo)
        }
    }

    fun deletePhoto(photo: PhotoItem) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
            if (_selectedPhoto.value?.id == photo.id) {
                _selectedPhoto.value = null
                _currentBitmap.value = null
                _currentMode.value = DarktableMode.LIGHTTABLE
            }
        }
    }

    fun importPhoto(uri: Uri, fileName: String? = null) {
        viewModelScope.launch {
            val title = fileName ?: "Imported Photo ${System.currentTimeMillis() % 1000}"
            val newItem = repository.importPhoto(uri.toString(), title)
            selectPhoto(newItem, navigateToDarkroom = true)
        }
    }

    fun exportPhoto(
        format: String = "JPEG",
        quality: Int = 92,
        maxDimension: Int = 0,
        onSuccess: (Uri) -> Unit
    ) {
        val photo = _selectedPhoto.value ?: return
        val bmp = _currentBitmap.value ?: return
        val params = _activeParams.value

        _isExporting.value = true
        _exportMessage.value = "Developing & rendering non-destructive pipeline..."

        viewModelScope.launch {
            try {
                val rendered = DarktableEngine.renderFinalBitmap(bmp, params, maxDimension)
                val savedUri = DarktableEngine.saveToGallery(
                    getApplication(),
                    rendered,
                    photo.title.replace(" ", "_"),
                    format,
                    quality
                )
                _isExporting.value = false
                if (savedUri != null) {
                    _exportMessage.value = "Saved successfully to Pictures/Darktable!"
                    onSuccess(savedUri)
                } else {
                    _exportMessage.value = "Export failed to write image."
                }
            } catch (e: Exception) {
                _isExporting.value = false
                _exportMessage.value = "Export error: ${e.message}"
            }
        }
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }
}
