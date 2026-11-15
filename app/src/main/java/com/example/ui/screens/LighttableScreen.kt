package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ColorFilterMode
import com.example.model.ColorLabel
import com.example.model.EditParams
import com.example.model.PhotoItem
import com.example.model.RATING_REJECTED
import com.example.model.RatingComparator
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.ui.components.ColorLabelDisplayBadges
import com.example.ui.components.ColorLabelMultiSelector
import com.example.ui.components.RatingBar
import com.example.ui.theme.DtLabelRed
import com.example.ui.theme.DtThemeColors
import com.example.viewmodel.DarktableViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LighttableScreen(
    viewModel: DarktableViewModel,
    modifier: Modifier = Modifier
) {
    val palette = DtThemeColors.current
    val photos by viewModel.filteredPhotos.collectAsState()
    val ratingFilter by viewModel.ratingFilter.collectAsState()
    val ratingComparator by viewModel.ratingComparator.collectAsState()
    val selectedColorLabels by viewModel.selectedColorLabels.collectAsState()
    val colorFilterMode by viewModel.colorFilterMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()

    var showSearchRow by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var contextMenuPhoto by remember { mutableStateOf<PhotoItem?>(null) }
    var pendingXmpTargetPhoto by remember { mutableStateOf<PhotoItem?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPhoto(uri)
        }
    }

    val multiFileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFromMultipleUris(uris)
        }
    }

    val folderImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            viewModel.importFromFolderTree(treeUri)
        }
    }

    val loadXmpForPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { xmpUri: Uri? ->
        val target = pendingXmpTargetPhoto
        pendingXmpTargetPhoto = null
        if (target != null && xmpUri != null) {
            viewModel.applyXmpToPhoto(target, xmpUri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = palette.deepDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.panelDark)
                    .border(width = 0.5.dp, color = palette.border)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(palette.accentPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Darktable Logo",
                                tint = palette.onAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "DARKTABLE",
                                    color = palette.textPrimary,
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    maxLines = 1
                                )
                                Box(
                                    modifier = Modifier
                                        .background(palette.accentContainer.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIGHTTABLE",
                                        color = palette.accentPrimary,
                                        fontSize = 9.sp,
                                        lineHeight = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${photos.size} photograph${if (photos.size == 1) "" else "s"}",
                                color = palette.textTertiary,
                                fontSize = 11.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        IconButton(
                            onClick = { showSearchRow = !showSearchRow },
                            modifier = Modifier.testTag("btn_toggle_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search catalog",
                                tint = if (searchQuery.isNotEmpty() || showSearchRow) palette.accentPrimary else palette.textSecondary
                            )
                        }

                        IconButton(
                            onClick = {
                                val next = if (gridColumns == 1) 2 else if (gridColumns == 2) 3 else 1
                                viewModel.setGridColumns(next)
                            },
                            modifier = Modifier.testTag("btn_toggle_grid_cols")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Grid zoom ($gridColumns columns)",
                                tint = palette.textSecondary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.openSettings() },
                            modifier = Modifier.testTag("btn_open_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Application settings",
                                tint = palette.textSecondary
                            )
                        }
                    }
                }

                // Optional Search Bar
                if (showSearchRow) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Filter by title, camera, or tags...", fontSize = 13.sp) },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = palette.textSecondary)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = palette.cardDark,
                                unfocusedContainerColor = palette.cardDark,
                                focusedBorderColor = palette.accentPrimary,
                                unfocusedBorderColor = palette.border,
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("input_search_query")
                        )
                    }
                }

                // FILTER BAR ROW 1: Star Rating Comparator & Star Selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Comparator Mode Button (Exact =, At most ≤, At least ≥)
                        val hasActiveRating = ratingFilter != null
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (hasActiveRating) palette.accentContainer.copy(alpha = 0.4f) else palette.cardDark)
                                .border(
                                    width = if (hasActiveRating) 1.dp else 0.5.dp,
                                    color = if (hasActiveRating) palette.accentPrimary else palette.border,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { viewModel.cycleRatingComparator() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("btn_cycle_rating_comparator"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = ratingComparator.symbol,
                                color = if (hasActiveRating) palette.accentPrimary else palette.textPrimary,
                                fontSize = 13.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = ratingComparator.label,
                                color = if (hasActiveRating) palette.accentPrimary else palette.textSecondary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Cycle comparison mode",
                                tint = if (hasActiveRating) palette.accentPrimary else palette.textTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // ALL Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (ratingFilter == null) palette.accentPrimary else palette.cardDark)
                                .border(0.5.dp, if (ratingFilter == null) palette.accentPrimary else palette.border, RoundedCornerShape(4.dp))
                                .clickable { viewModel.setRatingFilter(null) }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("filter_stars_all")
                        ) {
                            Text(
                                text = "ALL",
                                color = if (ratingFilter == null) palette.onAccent else palette.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // REJECTED Selection Button
                        val isRejectedFilterActive = ratingFilter == RATING_REJECTED
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isRejectedFilterActive) DtLabelRed else palette.cardDark)
                                .border(
                                    width = 0.5.dp,
                                    color = if (isRejectedFilterActive) DtLabelRed else palette.border,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    if (isRejectedFilterActive) {
                                        viewModel.setRatingFilter(null)
                                    } else {
                                        viewModel.setRatingFilter(RATING_REJECTED)
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("filter_rejected")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Filter rejected photos",
                                tint = if (isRejectedFilterActive) Color.White else DtLabelRed,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "REJ",
                                color = if (isRejectedFilterActive) Color.White else palette.textSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 0★ to 5★ Buttons
                        for (stars in 0..5) {
                            val active = ratingFilter == stars
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (active) palette.accentPrimary else palette.cardDark)
                                    .border(0.5.dp, if (active) palette.accentPrimary else palette.border, RoundedCornerShape(4.dp))
                                    .clickable {
                                        if (active) viewModel.setRatingFilter(null) else viewModel.setRatingFilter(stars)
                                    }
                                    .padding(horizontal = 5.dp, vertical = 4.dp)
                                    .testTag("filter_stars_$stars")
                            ) {
                                Text(
                                    text = "$stars",
                                    color = if (active) palette.onAccent else palette.textPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "$stars stars",
                                    tint = if (active) palette.onAccent else if (stars == 0) palette.textTertiary else palette.accentPrimary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    // FILTER BAR ROW 2: Mode Toggle on Left & Color Tag Multi-Selection on Right (matching Star Rating layout)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Color Filter Mode Toggle Button ("At least one" vs "Only selected") on the LEFT
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (selectedColorLabels.isNotEmpty()) palette.accentContainer.copy(alpha = 0.35f) else palette.cardDark
                                )
                                .border(
                                    width = if (selectedColorLabels.isNotEmpty()) 1.dp else 0.5.dp,
                                    color = if (selectedColorLabels.isNotEmpty()) palette.accentPrimary else palette.border,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { viewModel.cycleColorFilterMode() }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("btn_toggle_color_filter_mode"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "MODE:",
                                color = if (selectedColorLabels.isNotEmpty()) palette.accentPrimary else palette.textTertiary,
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = colorFilterMode.label,
                                color = if (selectedColorLabels.isNotEmpty()) palette.textPrimary else palette.textSecondary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Color Tag Multi-Selector Dots on the RIGHT
                        ColorLabelMultiSelector(
                            selectedLabels = selectedColorLabels,
                            onToggleLabel = { label -> viewModel.toggleColorFilter(label) },
                            modifier = Modifier.testTag("filter_color_labels"),
                            dotSize = 18,
                            spacing = 6,
                            showCheckMark = true
                        )

                        if (selectedColorLabels.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.cardDark)
                                    .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                                    .clickable { viewModel.clearColorFilter() }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .testTag("btn_clear_color_filter")
                            ) {
                                Text(
                                    text = "CLEAR",
                                    color = palette.textSecondary,
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // SORT BAR ROW 3: Direction Toggle on Left & Sort Fields (EXIF Date, Filename, Star Rating) on Right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.cardDark)
                                .border(
                                    width = 0.5.dp,
                                    color = palette.border,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { viewModel.toggleSortDirection() }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("btn_toggle_sort_direction"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "SORT:",
                                color = palette.accentPrimary,
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Icon(
                                imageVector = if (sortDirection == SortDirection.ASCENDING) {
                                    Icons.Default.ArrowUpward
                                } else {
                                    Icons.Default.ArrowDownward
                                },
                                contentDescription = "Toggle sort direction (${sortDirection.label})",
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = sortDirection.shortLabel,
                                color = palette.textPrimary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        SortField.entries.forEach { field ->
                            val selected = sortField == field
                            val fieldTag = when (field) {
                                SortField.CAPTURE_DATE -> "sort_field_capture_date"
                                SortField.FILENAME -> "sort_field_filename"
                                SortField.STAR_RATING -> "sort_field_star_rating"
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selected) palette.accentPrimary else palette.cardDark)
                                    .border(
                                        width = 0.5.dp,
                                        color = if (selected) palette.accentPrimary else palette.border,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { viewModel.setSortField(field) }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                                    .testTag(fieldTag)
                            ) {
                                Text(
                                    text = field.shortLabel,
                                    color = if (selected) palette.onAccent else palette.textSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selected) {
                                    Text(
                                        text = sortDirection.symbol,
                                        color = palette.onAccent,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // ACTIVE FILTER STATUS BREADCRUMB
                    if (ratingFilter != null || selectedColorLabels.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.cardDark.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ACTIVE:",
                                    color = palette.accentPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                if (ratingFilter != null) {
                                    val ratingLabel = if (ratingFilter == RATING_REJECTED) "Rejected" else "${ratingFilter}★"
                                    val ratingDesc = when (ratingComparator) {
                                        RatingComparator.EXACT -> "$ratingLabel only"
                                        RatingComparator.LESS_EQUAL -> "$ratingLabel and less"
                                        RatingComparator.GREATER_EQUAL -> "$ratingLabel and more"
                                    }
                                    Text(
                                        text = "Rating: $ratingDesc",
                                        color = if (ratingFilter == RATING_REJECTED) DtLabelRed else palette.textPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (ratingFilter != null && selectedColorLabels.isNotEmpty()) {
                                    Text("•", color = palette.textTertiary, fontSize = 10.sp)
                                }

                                if (selectedColorLabels.isNotEmpty()) {
                                    val colorsStr = selectedColorLabels.joinToString("+") {
                                        it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                                    }
                                    val modeDesc = if (colorFilterMode == ColorFilterMode.ONLY_SELECTED) "Only" else "At least 1"
                                    Text(
                                        text = "Colors: $colorsStr ($modeDesc)",
                                        color = palette.tealAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                text = "RESET",
                                color = palette.accentPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.setRatingFilter(null)
                                        viewModel.clearColorFilter()
                                    }
                                    .testTag("btn_reset_active_filters")
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showImportDialog = true },
                containerColor = palette.accentPrimary,
                contentColor = palette.onAccent,
                icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Import Photo") },
                text = { Text("IMPORT RAW / PHOTO", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_import_photo")
            )
        }
    ) { innerPadding ->
        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "No photos found",
                        tint = palette.textTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No photographs matching filter",
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Try switching rating condition (exact vs at most) or color tag mode, or import a new photo.",
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    TextButton(
                        onClick = {
                            viewModel.setRatingFilter(null)
                            viewModel.clearColorFilter()
                            viewModel.setSearchQuery("")
                        }
                    ) {
                        Text("RESET FILTERS", color = palette.accentPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    end = 10.dp,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("lighttable_photo_grid")
            ) {
                items(photos, key = { it.id }) { photo ->
                    val editedBitmap by produceState(
                        initialValue = viewModel.getCachedEditedThumbnail(photo),
                        photo.id,
                        photo.uriString,
                        photo.params
                    ) {
                        value = viewModel.getOrRenderEditedThumbnail(photo)
                    }
                    PhotoGridCard(
                        photo = photo,
                        editedBitmap = editedBitmap,
                        onOpen = { viewModel.selectPhoto(photo, navigateToDarkroom = true) },
                        onLongClick = { contextMenuPhoto = photo },
                        onRatingChange = { rating -> viewModel.updateRating(photo, rating) },
                        onRejectToggle = { viewModel.toggleReject(photo) },
                        onToggleColorLabel = { lbl -> viewModel.togglePhotoColorLabel(photo, lbl) }
                    )
                }
            }
        }
    }

    // Import source selection dialog (supports Image + .XMP files, Folder auto-matching, and Photo Picker)
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = palette.panelDark,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = "IMPORT PHOTOGRAPHY",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select files with .xmp sidecars or pick a folder to automatically load existing Darktable edits.",
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.cardDark)
                            .border(1.dp, palette.accentPrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable {
                                showImportDialog = false
                                multiFileImportLauncher.launch(arrayOf("*/*"))
                            }
                            .padding(12.dp)
                            .testTag("btn_import_files_with_xmp"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Select image and XMP files",
                            tint = palette.accentPrimary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Files (Image + .XMP)",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pick photo(s) together with their .xmp sidecar",
                                color = palette.textTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.cardDark)
                            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                            .clickable {
                                showImportDialog = false
                                folderImportLauncher.launch(null)
                            }
                            .padding(12.dp)
                            .testTag("btn_import_folder_xmp"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Import folder with XMP sidecars",
                            tint = palette.secondaryBlue
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Folder (Auto-Match .XMP)",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatically pair images with sibling .xmp files",
                                color = palette.textTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.cardDark)
                            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                            .clickable {
                                showImportDialog = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(12.dp)
                            .testTag("btn_import_gallery"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Quick gallery photo picker",
                            tint = palette.textSecondary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gallery Photo Picker",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Quick pick from device gallery (reads embedded XMP)",
                                color = palette.textTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("CANCEL", color = palette.textSecondary)
                }
            }
        )
    }

    // Long press context menu dialog
    contextMenuPhoto?.let { photo ->
        val dialogPhoto = photos.find { it.id == photo.id } ?: photo
        AlertDialog(
            onDismissRequest = { contextMenuPhoto = null },
            containerColor = palette.panelDark,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(text = dialogPhoto.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Format: ${dialogPhoto.fileFormat} • ${dialogPhoto.dimensions}")
                    Text("Camera: ${dialogPhoto.cameraModel}")
                    Text("Lens: ${dialogPhoto.lens}")
                    Text("Exposure: ${dialogPhoto.aperture} • ${dialogPhoto.shutterSpeed} • ISO ${dialogPhoto.iso}")
                    if (dialogPhoto.colorLabels.isNotEmpty()) {
                        Text("Color Tags: ${dialogPhoto.colorLabels.joinToString { it.name }}")
                    }

                    // Rating & Rejected status editor inside context menu
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RatingBar(
                            rating = dialogPhoto.rating,
                            isRejected = dialogPhoto.isRejected,
                            onRatingChange = { newRating ->
                                viewModel.updateRating(dialogPhoto, newRating)
                                contextMenuPhoto = dialogPhoto.copy(rating = newRating, isRejected = false)
                            },
                            onRejectToggle = {
                                viewModel.toggleReject(dialogPhoto)
                                contextMenuPhoto = dialogPhoto.copy(isRejected = !dialogPhoto.isRejected)
                            },
                            starSize = 22
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (dialogPhoto.isRejected) DtLabelRed else palette.cardDark)
                                .border(
                                    0.5.dp,
                                    if (dialogPhoto.isRejected) DtLabelRed else palette.border,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    viewModel.toggleReject(dialogPhoto)
                                    contextMenuPhoto = dialogPhoto.copy(isRejected = !dialogPhoto.isRejected)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("btn_dialog_toggle_reject")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Toggle rejected status",
                                tint = if (dialogPhoto.isRejected) Color.White else DtLabelRed,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (dialogPhoto.isRejected) "REJECTED" else "MARK REJECTED",
                                color = if (dialogPhoto.isRejected) Color.White else palette.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            pendingXmpTargetPhoto = photo
                            contextMenuPhoto = null
                            loadXmpForPhotoLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.testTag("btn_load_xmp_for_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = palette.secondaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("LOAD .XMP", color = palette.secondaryBlue)
                    }
                    TextButton(
                        onClick = {
                            viewModel.duplicatePhoto(photo)
                            contextMenuPhoto = null
                        }
                    ) {
                        Text("DUPLICATE", color = palette.accentPrimary)
                    }
                    TextButton(
                        onClick = {
                            viewModel.deletePhoto(photo)
                            contextMenuPhoto = null
                        }
                    ) {
                        Text("DELETE", color = DtLabelRed)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { contextMenuPhoto = null }) {
                    Text("CLOSE", color = palette.textSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGridCard(
    photo: PhotoItem,
    onOpen: () -> Unit,
    onLongClick: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onRejectToggle: () -> Unit,
    onToggleColorLabel: (ColorLabel) -> Unit,
    modifier: Modifier = Modifier,
    editedBitmap: Bitmap? = null
) {
    val context = LocalContext.current
    val palette = DtThemeColors.current
    val hasGeometryFraming = photo.params.rotationAngle != 0f ||
        photo.params.cropAspectRatio != "Original" ||
        photo.params.cropLeft > 0.001f ||
        photo.params.cropTop > 0.001f ||
        photo.params.cropRight < 0.999f ||
        photo.params.cropBottom < 0.999f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (photo.isRejected) 1.5.dp else 1.dp,
                color = if (photo.isRejected) DtLabelRed.copy(alpha = 0.75f) else palette.border,
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onLongClick
            )
            .testTag("photo_card_${photo.id}"),
        colors = CardDefaults.cardColors(containerColor = palette.cardDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Image Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .background(palette.surfaceDark)
            ) {
                val thumbAlpha = if (photo.isRejected) 0.6f else 1f
                if (editedBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = editedBitmap.asImageBitmap(),
                        contentDescription = photo.title,
                        contentScale = if (hasGeometryFraming) ContentScale.Fit else ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(thumbAlpha)
                            .testTag("photo_edited_thumbnail_${photo.id}")
                    )
                } else if (photo.uriString.startsWith("res://")) {
                    val resName = photo.uriString.removePrefix("res://")
                    var resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                    if (resId == 0) {
                        resId = context.resources.getIdentifier(resName, "drawable", "com.example")
                    }
                    if (resId != 0) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = resId),
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(thumbAlpha)
                        )
                    }
                } else {
                    AsyncImage(
                        model = photo.uriString,
                        contentDescription = photo.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(thumbAlpha)
                    )
                }

                // Color Label Badges (Top-Right)
                if (photo.colorLabels.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        ColorLabelDisplayBadges(
                            labels = photo.colorLabels,
                            badgeSize = 10,
                            spacing = 3
                        )
                    }
                }

                // Format & Resolution Chip (Bottom-Left)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${photo.fileFormat.take(4)} • ${photo.megaPixels}",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Top-Start Status Badges (REJECTED / EDITED)
                val isEdited = photo.history.size > 1 || photo.params != EditParams()
                if (photo.isRejected || isEdited) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (photo.isRejected) {
                            Box(
                                modifier = Modifier
                                    .background(DtLabelRed, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                    .testTag("badge_rejected_${photo.id}")
                            ) {
                                Text(
                                    text = "REJECTED",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        if (isEdited) {
                            Box(
                                modifier = Modifier
                                    .background(palette.accentPrimary, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "EDITED",
                                    color = palette.onAccent,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // Card Footer: Title, EXIF summary, Ratings, and Color Tags
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = photo.title,
                    color = palette.textPrimary,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val captureDateText = remember(photo.captureDate) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(photo.captureDate))
                }
                Text(
                    text = "${photo.cameraModel} • ${photo.focalLength} • ${photo.aperture}",
                    color = palette.textSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = captureDateText,
                    color = palette.textTertiary,
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("photo_capture_date_${photo.id}")
                )

                // Rating stars and label dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RatingBar(
                        rating = photo.rating,
                        isRejected = photo.isRejected,
                        onRatingChange = onRatingChange,
                        onRejectToggle = onRejectToggle,
                        starSize = 15
                    )

                    ColorLabelMultiSelector(
                        selectedLabels = photo.colorLabels,
                        onToggleLabel = onToggleColorLabel,
                        dotSize = 11,
                        spacing = 4
                    )
                }
            }
        }
    }
}
