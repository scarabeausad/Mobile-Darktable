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
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.model.matchesRatingFilter
import com.example.model.sortedByOption
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

    private val _showRejected = MutableStateFlow(true)
    val showRejected = _showRejected.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _gridColumns = MutableStateFlow(2)
    val gridColumns = _gridColumns.asStateFlow()

    // Lighttable Sort state (persisted via DataStore)
    private val _sortField = MutableStateFlow(SortField.CAPTURE_DATE)
    val sortField: StateFlow<SortField> = _sortField.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.DESCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()
    private var sortUserOverridden = false

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

    // Export & Import Status
    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage = _exportMessage.asStateFlow()

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage = _importMessage.asStateFlow()

    private val _photoNeedingXmpAttachment = MutableStateFlow<PhotoItem?>(null)
    val photoNeedingXmpAttachment = _photoNeedingXmpAttachment.asStateFlow()

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
        val query: String,
        val sortField: SortField,
        val sortDirection: SortDirection
    )

    val filteredPhotos: StateFlow<List<PhotoItem>> = combine(
        combine(ratingFilter, ratingComparator, selectedColorLabels) { r, comp, colors ->
            Triple(r, comp, colors)
        },
        combine(colorFilterMode, showRejected, searchQuery) { mode, rej, q ->
            Triple(mode, rej, q)
        },
        combine(sortField, sortDirection) { field, dir -> field to dir }
    ) { (r, comp, colors), (mode, rej, q), (field, dir) ->
        FilterCriteria(r, comp, colors, mode, rej, q, field, dir)
    }.combine(rawPhotos) { criteria, photos ->
        photos.filter { photo ->
            val matchRating = photo.matchesRatingFilter(criteria.rating, criteria.comparator)

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
        }.sortedByOption(criteria.sortField, criteria.sortDirection)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureInitialDataSeeded()
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { saved ->
                if (!sortUserOverridden) {
                    _sortField.value = saved.sortField
                    _sortDirection.value = saved.sortDirection
                }
            }
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

    fun setSortField(field: SortField) {
        sortUserOverridden = true
        val newDir = if (_sortField.value == field) {
            // Tapping the already-active sort field toggles Asc <-> Desc
            if (_sortDirection.value == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
        } else {
            // Sensible default direction when switching fields: Filename A-Z (Asc), Date/Rating newest/highest first (Desc)
            if (field == SortField.FILENAME) SortDirection.ASCENDING else SortDirection.DESCENDING
        }
        _sortField.value = field
        _sortDirection.value = newDir
        viewModelScope.launch {
            settingsRepository.setSortOption(field, newDir)
        }
    }

    fun setSortOption(field: SortField, direction: SortDirection) {
        sortUserOverridden = true
        _sortField.value = field
        _sortDirection.value = direction
        viewModelScope.launch {
            settingsRepository.setSortOption(field, direction)
        }
    }

    fun toggleSortDirection() {
        sortUserOverridden = true
        val newDir = if (_sortDirection.value == SortDirection.ASCENDING) {
            SortDirection.DESCENDING
        } else {
            SortDirection.ASCENDING
        }
        val currentField = _sortField.value
        _sortDirection.value = newDir
        viewModelScope.launch {
            settingsRepository.setSortOption(currentField, newDir)
        }
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

    fun importPhoto(
        uri: Uri,
        fileName: String? = null,
        companionXmpUri: Uri? = null
    ) {
        viewModelScope.launch {
            val (item, hadXmp) = importSingleInternal(uri, fileName, companionXmpUri)
            selectPhoto(item, navigateToDarkroom = true)
            if (hadXmp) {
                _photoNeedingXmpAttachment.value = null
                _importMessage.value = "Imported '${item.title}' with XMP sidecar edits"
            } else {
                _photoNeedingXmpAttachment.value = item
                _importMessage.value = "Imported '${item.title}' (no sibling .xmp accessible)"
            }
        }
    }

    fun importFromMultipleUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val app = getApplication<Application>()
            val resolver = app.contentResolver
            val images = mutableListOf<Pair<String, Uri>>()
            val xmps = mutableMapOf<String, Uri>()

            for (uri in uris) {
                val name = DarktableEngine.queryDisplayName(app, uri) ?: uri.lastPathSegment ?: "file"
                val mime = try { resolver.getType(uri) } catch (_: Exception) { null }
                when {
                    DarktableEngine.isUriLikelyXmp(app, uri, name, mime) -> xmps[name] = uri
                    DarktableEngine.isImageFileName(name, mime) -> images.add(name to uri)
                }
            }

            if (images.isEmpty()) {
                // If user picked only an .xmp while a photo is already selected, apply it directly!
                val selected = _selectedPhoto.value
                if (selected != null && xmps.size == 1) {
                    applyXmpToPhoto(selected, xmps.values.first())
                } else {
                    _importMessage.value = "Select an image file along with its .xmp sidecar to import"
                }
                return@launch
            }

            val xmpNames = xmps.keys
            val singlePairFallback = if (images.size == 1 && xmps.size == 1) xmps.values.first() else null
            var importedWithXmp = 0
            var lastItem: PhotoItem? = null

            for ((imgName, imgUri) in images) {
                val matchedXmpName = com.example.engine.XmpSidecarManager.findMatchingXmpName(imgName, xmpNames)
                val xmpUri = matchedXmpName?.let { xmps[it] } ?: singlePairFallback
                val (item, hadXmp) = importSingleInternal(imgUri, null, xmpUri)
                lastItem = item
                if (hadXmp) importedWithXmp++
            }

            if (images.size == 1 && lastItem != null) {
                selectPhoto(lastItem, navigateToDarkroom = true)
                if (importedWithXmp > 0) {
                    _photoNeedingXmpAttachment.value = null
                    _importMessage.value = "Imported '${lastItem.title}' with XMP sidecar edits"
                } else {
                    _photoNeedingXmpAttachment.value = lastItem
                    _importMessage.value = "Imported '${lastItem.title}' (no .xmp found — tap ATTACH .XMP)"
                }
            } else {
                _photoNeedingXmpAttachment.value = null
                _importMessage.value = if (importedWithXmp > 0) {
                    "Imported ${images.size} photos ($importedWithXmp with XMP edits)"
                } else {
                    "Imported ${images.size} photos"
                }
            }
        }
    }

    fun importFromFolderTree(treeUri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                app.contentResolver.takePersistableUriPermission(
                    treeUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            val pairs = DarktableEngine.listFolderImagesWithXmp(app, treeUri)
            if (pairs.isEmpty()) {
                _importMessage.value = "No supported images found in selected folder"
                return@launch
            }
            var importedWithXmp = 0
            var lastItem: PhotoItem? = null
            for ((imgUri, xmpUri) in pairs) {
                val (item, hadXmp) = importSingleInternal(imgUri, null, xmpUri)
                lastItem = item
                if (hadXmp) importedWithXmp++
            }
            if (pairs.size == 1 && lastItem != null) {
                selectPhoto(lastItem, navigateToDarkroom = true)
            }
            _photoNeedingXmpAttachment.value = null
            _importMessage.value = if (importedWithXmp > 0) {
                "Imported ${pairs.size} photo${if (pairs.size == 1) "" else "s"} ($importedWithXmp with XMP edits)"
            } else {
                "Imported ${pairs.size} photo${if (pairs.size == 1) "" else "s"}"
            }
        }
    }

    fun applyXmpToPhoto(photo: PhotoItem, xmpUri: Uri) {
        viewModelScope.launch {
            val xmpData = DarktableEngine.readXmpFromUri(getApplication(), xmpUri)
            if (xmpData == null) {
                _importMessage.value = "Could not parse edits from selected .xmp file"
                return@launch
            }
            val updated = repository.applyXmpSidecar(photo, xmpData)
            if (_selectedPhoto.value?.id == updated.id) {
                _selectedPhoto.value = updated
                _activeParams.value = updated.params
            }
            _photoNeedingXmpAttachment.value = null
            _importMessage.value = "Applied XMP edits to '${updated.title}'"
        }
    }

    private suspend fun importSingleInternal(
        uri: Uri,
        fileName: String?,
        companionXmpUri: Uri?
    ): Pair<PhotoItem, Boolean> {
        val app = getApplication<Application>()
        val importedInfo = DarktableEngine.importToInternalStorage(app, uri, companionXmpUri)
        return if (importedInfo != null) {
            val finalInfo = if (fileName != null) importedInfo.copy(title = fileName) else importedInfo
            val item = repository.importPhotoWithMetadata(finalInfo)
            item to (finalInfo.xmpSidecarData != null)
        } else {
            val fallbackXmp = DarktableEngine.findXmpDataForImageUri(app, uri, null, companionXmpUri)
            val title = fileName
                ?: DarktableEngine.queryDisplayName(app, uri)?.substringBeforeLast(".")
                ?: "Imported Photo ${System.currentTimeMillis() % 1000}"
            val item = repository.importPhoto(uri.toString(), title, xmpData = fallbackXmp)
            item to (fallbackXmp != null)
        }
    }

    fun clearImportMessage() {
        _importMessage.value = null
        _photoNeedingXmpAttachment.value = null
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
