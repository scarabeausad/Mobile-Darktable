package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.DarktableDatabase
import com.example.engine.DarktableEngine
import com.example.model.AppColorScheme
import com.example.model.AppSettings
import com.example.model.ColorFilterMode
import com.example.model.ColorLabel
import com.example.model.DarktableStyle
import com.example.model.EditParams
import com.example.model.HistogramData
import com.example.model.HistogramScale
import com.example.model.PhotoItem
import com.example.model.RatingComparator
import com.example.repository.PhotoRepository
import com.example.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DarktableMode {
    LIGHTTABLE, DARKROOM, STYLES, EXIF_INFO, EXPORT, SETTINGS
}

class DarktableViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PhotoRepository(
        DarktableDatabase.getDatabase(application).darktableDao(),
        application.filesDir
    )

    private val settingsRepository = SettingsRepository(application.applicationContext)

    // Mode
    private val _currentMode = MutableStateFlow(DarktableMode.LIGHTTABLE)
    val currentMode: StateFlow<DarktableMode> = _currentMode.asStateFlow()
    private var previousNonSettingsMode: DarktableMode = DarktableMode.LIGHTTABLE

    // Application Settings (persisted via DataStore)
    val appSettings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppSettings()
    )
    val colorScheme: StateFlow<AppColorScheme> = appSettings
        .map { it.colorScheme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppColorScheme.DARK)
    val histogramScale: StateFlow<HistogramScale> = appSettings
        .map { it.histogramScale }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HistogramScale.LINEAR)

    // Portrait / Workstation Edit Panel Dock Side (false = Right side, true = Left side)
    private val _editPanelOnLeft = MutableStateFlow(false)
    val editPanelOnLeft: StateFlow<Boolean> = _editPanelOnLeft.asStateFlow()

    // Track whether Geometry tab is active so preview shows crop frame vs cropped output
    private val _isGeometryTabActive = MutableStateFlow(false)
    val isGeometryTabActive: StateFlow<Boolean> = _isGeometryTabActive.asStateFlow()

    // Lighttable Filter state
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

    // Active Photo & Darkroom state
    private val _selectedPhoto = MutableStateFlow<PhotoItem?>(null)
    val selectedPhoto: StateFlow<PhotoItem?> = _selectedPhoto.asStateFlow()

    private val _activeParams = MutableStateFlow(EditParams())
    val activeParams: StateFlow<EditParams> = _activeParams.asStateFlow()

    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    private val _proxyBitmap = MutableStateFlow<Bitmap?>(null)
    val proxyBitmap: StateFlow<Bitmap?> = _proxyBitmap.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

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

    private var loadJob: Job? = null

    private val rawThumbProxyCache = LruCache<String, Bitmap>(24)
    private val editedThumbCache = LruCache<String, Bitmap>(48)

    val rawPhotos = repository.allPhotos
    val styles = repository.allStyles.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DarktableStyle.BUILT_IN_STYLES
    )

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
            val matchRating = if (criteria.rating == null) {
                true
            } else {
                when (criteria.comparator) {
                    RatingComparator.EXACT -> photo.rating == criteria.rating
                    RatingComparator.LESS_EQUAL -> photo.rating <= criteria.rating
                    RatingComparator.GREATER_EQUAL -> photo.rating >= criteria.rating
                }
            }

            val matchColor = if (criteria.selectedColors.isEmpty()) {
                true
            } else {
                when (criteria.colorMode) {
                    ColorFilterMode.AT_LEAST_ONE -> {
                        photo.colorLabels.any { it in criteria.selectedColors }
                    }
                    ColorFilterMode.ONLY_SELECTED -> {
                        photo.colorLabels.isNotEmpty() && photo.colorLabels.all { it in criteria.selectedColors }
                    }
                }
            }

            val matchRej = criteria.showRej || !photo.isRejected
            val matchQuery = criteria.query.isEmpty() ||
                    photo.title.contains(criteria.query, ignoreCase = true) ||
                    photo.cameraModel.contains(criteria.query, ignoreCase = true) ||
                    photo.tags.any { it.contains(criteria.query, ignoreCase = true) }

            matchRating && matchColor && matchRej && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureInitialDataSeeded()
        }
        observeLivePreviewPipeline()
    }

    private data class RenderRequest(
        val proxy: Bitmap?,
        val params: EditParams,
        val applyCrop: Boolean
    )

    private fun observeLivePreviewPipeline() {
        viewModelScope.launch(Dispatchers.Default) {
            combine(
                _proxyBitmap,
                _activeParams,
                _isGeometryTabActive,
                _isSplitCompareActive
            ) { proxy, params, isGeometry, isSplit ->
                val applyCrop = !isGeometry && !isSplit
                val effectiveParams = if (applyCrop) {
                    params
                } else {
                    params.copy(
                        cropAspectRatio = "Original",
                        cropLeft = 0f,
                        cropTop = 0f,
                        cropRight = 1f,
                        cropBottom = 1f
                    )
                }
                RenderRequest(proxy, effectiveParams, applyCrop)
            }.distinctUntilChanged().collectLatest { req ->
                val proxy = req.proxy
                if (proxy == null) {
                    _previewBitmap.value = null
                    return@collectLatest
                }
                val rendered = DarktableEngine.renderPreviewBitmap(
                    proxyBitmap = proxy,
                    params = req.params,
                    applyCrop = req.applyCrop
                )
                _previewBitmap.value = rendered
                val histo = DarktableEngine.computeHistogram(rendered)
                _currentHistogram.value = histo
            }
        }
    }

    fun setMode(mode: DarktableMode) {
        if (mode == DarktableMode.LIGHTTABLE && _currentMode.value == DarktableMode.DARKROOM) {
            val selected = _selectedPhoto.value
            val active = _activeParams.value
            if (selected != null && active != selected.params) {
                commitParamChange(active, "Darkroom Adjustment", "Updated parameters")
            }
        }
        if (mode != DarktableMode.SETTINGS) {
            previousNonSettingsMode = mode
        }
        _currentMode.value = mode
    }

    fun getCachedEditedThumbnail(photo: PhotoItem): Bitmap? {
        val key = "${photo.uriString}#${photo.params}"
        return synchronized(editedThumbCache) { editedThumbCache.get(key) }
    }

    suspend fun getOrRenderEditedThumbnail(photo: PhotoItem): Bitmap? {
        val key = "${photo.uriString}#${photo.params}"
        synchronized(editedThumbCache) {
            editedThumbCache.get(key)?.let { return it }
        }
        val proxy = synchronized(rawThumbProxyCache) { rawThumbProxyCache.get(photo.uriString) }
            ?: DarktableEngine.loadBitmap(getApplication(), photo.uriString, 420)?.let { loaded ->
                val scaled = DarktableEngine.createPreviewProxy(loaded, 420)
                if (scaled !== loaded) loaded.recycle()
                synchronized(rawThumbProxyCache) { rawThumbProxyCache.put(photo.uriString, scaled) }
                scaled
            }
            ?: return null

        val rendered = DarktableEngine.renderPreviewBitmap(
            proxyBitmap = proxy,
            params = photo.params,
            applyCrop = true
        )
        synchronized(editedThumbCache) {
            editedThumbCache.put(key, rendered)
        }
        return rendered
    }

    fun openSettings() {
        if (_currentMode.value != DarktableMode.SETTINGS) {
            previousNonSettingsMode = _currentMode.value
        }
        _currentMode.value = DarktableMode.SETTINGS
    }

    fun navigateBackFromSettings() {
        val target = if (previousNonSettingsMode == DarktableMode.DARKROOM && _selectedPhoto.value == null) {
            DarktableMode.LIGHTTABLE
        } else {
            previousNonSettingsMode
        }
        _currentMode.value = target
    }

    fun setColorScheme(scheme: AppColorScheme) {
        viewModelScope.launch {
            settingsRepository.setColorScheme(scheme)
        }
    }

    fun setHistogramScale(scale: HistogramScale) {
        viewModelScope.launch {
            settingsRepository.setHistogramScale(scale)
        }
    }

    fun toggleEditPanelSide() {
        _editPanelOnLeft.value = !_editPanelOnLeft.value
    }

    fun setGeometryTabActive(active: Boolean) {
        _isGeometryTabActive.value = active
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
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _currentBitmap.value = null
            _proxyBitmap.value = null
            _previewBitmap.value = null
            val bmp = DarktableEngine.loadBitmap(getApplication(), photo.uriString, 2048)
            _currentBitmap.value = bmp
            if (bmp != null) {
                val proxy = DarktableEngine.createPreviewProxy(bmp, 720)
                _proxyBitmap.value = proxy
            }
        }
    }

    fun updateParamLive(newParams: EditParams) {
        _activeParams.value = newParams
    }

    fun commitParamChange(newParams: EditParams, moduleName: String, description: String) {
        _activeParams.value = newParams
        val photo = _selectedPhoto.value ?: return
        _selectedPhoto.value = photo.copy(params = newParams)
        viewModelScope.launch {
            val latest = repository.getPhotoById(photo.id) ?: photo
            val updated = repository.updateParams(latest, newParams, moduleName, description)
            if (_selectedPhoto.value?.id == updated.id) {
                _selectedPhoto.value = updated
            }
        }
    }

    fun commitCurrentParams(moduleName: String, descriptionBuilder: (EditParams) -> String) {
        val current = _activeParams.value
        val photo = _selectedPhoto.value ?: return
        _selectedPhoto.value = photo.copy(params = current)
        viewModelScope.launch {
            val latest = repository.getPhotoById(photo.id) ?: photo
            val updated = repository.updateParams(latest, current, moduleName, descriptionBuilder(current))
            if (_selectedPhoto.value?.id == updated.id) {
                _selectedPhoto.value = updated
            }
        }
    }

    fun selectCropAspectRatio(ratio: String) {
        val current = _activeParams.value
        val updatedParams = when (ratio) {
            "Original" -> current.copy(
                cropAspectRatio = "Original",
                cropLeft = 0f,
                cropTop = 0f,
                cropRight = 1f,
                cropBottom = 1f
            )
            "Free" -> current.copy(cropAspectRatio = "Free")
            else -> {
                val bmp = _previewBitmap.value ?: _proxyBitmap.value ?: _currentBitmap.value
                val w = bmp?.width?.toFloat() ?: 4f
                val h = bmp?.height?.toFloat() ?: 3f
                val rect = DarktableEngine.computeCropRectNormalized(w, h, ratio)
                current.copy(
                    cropAspectRatio = ratio,
                    cropLeft = rect[0],
                    cropTop = rect[1],
                    cropRight = rect[2],
                    cropBottom = rect[3]
                )
            }
        }
        commitParamChange(updatedParams, "Aspect Crop", ratio)
    }

    fun resetActiveParams() {
        val defaultParams = EditParams()
        _activeParams.value = defaultParams
        val photo = _selectedPhoto.value ?: return
        viewModelScope.launch {
            val updated = repository.updateParams(photo, defaultParams, "Reset All", "Restored to neutral original state")
            _selectedPhoto.value = updated
        }
    }

    fun applyStyle(style: DarktableStyle) {
        _activeParams.value = style.params
        val photo = _selectedPhoto.value ?: return
        viewModelScope.launch {
            val updated = repository.updateParams(
                photo,
                style.params,
                "Style: ${style.name}",
                "Applied ${style.category} preset"
            )
            _selectedPhoto.value = updated
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
            val updated = repository.restoreHistoryStep(photo, stepIndex)
            if (updated != null) {
                _selectedPhoto.value = updated
                _activeParams.value = updated.params
            }
        }
    }

    fun saveSnapshotA() {
        val photo = _selectedPhoto.value ?: return
        viewModelScope.launch {
            val updated = repository.saveSnapshotA(photo.copy(params = _activeParams.value))
            _selectedPhoto.value = updated
        }
    }

    fun saveSnapshotB() {
        val photo = _selectedPhoto.value ?: return
        viewModelScope.launch {
            val updated = repository.saveSnapshotB(photo.copy(params = _activeParams.value))
            _selectedPhoto.value = updated
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
            val updated = repository.updateRating(photo, rating)
            if (_selectedPhoto.value?.id == photo.id) {
                _selectedPhoto.value = updated
            }
        }
    }

    fun updateColorLabel(photo: PhotoItem, label: ColorLabel) {
        viewModelScope.launch {
            val updated = repository.updateColorLabel(photo, label)
            if (_selectedPhoto.value?.id == photo.id) {
                _selectedPhoto.value = updated
            }
        }
    }

    fun togglePhotoColorLabel(photo: PhotoItem, label: ColorLabel) {
        updateColorLabel(photo, label)
    }

    fun toggleReject(photo: PhotoItem) {
        viewModelScope.launch {
            val updated = repository.toggleReject(photo)
            if (_selectedPhoto.value?.id == photo.id) {
                _selectedPhoto.value = updated
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
                _proxyBitmap.value = null
                _previewBitmap.value = null
                _currentMode.value = DarktableMode.LIGHTTABLE
            }
        }
    }

    fun importPhoto(uri: Uri, fileName: String? = null) {
        viewModelScope.launch {
            // Copy image into persistent internal storage so it remains available across app restarts
            val importedInfo = DarktableEngine.importToInternalStorage(getApplication(), uri)
            val newItem = if (importedInfo != null) {
                val finalInfo = if (fileName != null) importedInfo.copy(title = fileName) else importedInfo
                repository.importPhotoWithMetadata(finalInfo)
            } else {
                val title = fileName ?: "Imported Photo ${System.currentTimeMillis() % 1000}"
                repository.importPhoto(uri.toString(), title)
            }
            selectPhoto(newItem, navigateToDarkroom = true)
        }
    }

    suspend fun getXmpSidecarInfo(photo: PhotoItem): Pair<String, String> {
        return repository.getXmpSidecarPathAndContent(photo)
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
