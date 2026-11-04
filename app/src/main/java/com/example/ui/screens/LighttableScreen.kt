package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.model.PhotoItem
import com.example.model.RatingComparator
import com.example.ui.components.COLOR_LABEL_MAP
import com.example.ui.components.ColorLabelDisplayBadges
import com.example.ui.components.ColorLabelMultiSelector
import com.example.ui.components.RatingBar
import com.example.ui.theme.DtAmberDark
import com.example.ui.theme.DtAmberPrimary
import com.example.ui.theme.DtBorder
import com.example.ui.theme.DtCardDark
import com.example.ui.theme.DtDeepDark
import com.example.ui.theme.DtLabelRed
import com.example.ui.theme.DtPanelDark
import com.example.ui.theme.DtSecondaryBlue
import com.example.ui.theme.DtTealFilmic
import com.example.ui.theme.DtTextPrimary
import com.example.ui.theme.DtTextSecondary
import com.example.ui.theme.DtTextTertiary
import com.example.viewmodel.DarktableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LighttableScreen(
    viewModel: DarktableViewModel,
    modifier: Modifier = Modifier
) {
    val photos by viewModel.filteredPhotos.collectAsState()
    val ratingFilter by viewModel.ratingFilter.collectAsState()
    val ratingComparator by viewModel.ratingComparator.collectAsState()
    val selectedColorLabels by viewModel.selectedColorLabels.collectAsState()
    val colorFilterMode by viewModel.colorFilterMode.collectAsState()
    val showRejected by viewModel.showRejected.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()

    var showSearchRow by remember { mutableStateOf(false) }
    var contextMenuPhoto by remember { mutableStateOf<PhotoItem?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPhoto(uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DtDeepDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DtPanelDark)
                    .border(width = 0.5.dp, color = DtBorder)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DtAmberPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Darktable Logo",
                                tint = DtDeepDark,
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
                                    color = DtTextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(DtAmberDark.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIGHTTABLE",
                                        color = DtAmberPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${photos.size} photograph${if (photos.size == 1) "" else "s"}",
                                color = DtTextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { showSearchRow = !showSearchRow },
                            modifier = Modifier.testTag("btn_toggle_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search catalog",
                                tint = if (searchQuery.isNotEmpty() || showSearchRow) DtAmberPrimary else DtTextSecondary
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
                                tint = DtTextSecondary
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
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = DtTextSecondary)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DtCardDark,
                                unfocusedContainerColor = DtCardDark,
                                focusedBorderColor = DtAmberPrimary,
                                unfocusedBorderColor = DtBorder,
                                focusedTextColor = DtTextPrimary,
                                unfocusedTextColor = DtTextPrimary
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
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Star Rating Selection & Comparator Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Comparator Mode Button (Exact =, At most ≤, At least ≥)
                            val hasActiveRating = ratingFilter != null
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (hasActiveRating) DtAmberDark.copy(alpha = 0.4f) else DtCardDark)
                                    .border(
                                        width = if (hasActiveRating) 1.dp else 0.5.dp,
                                        color = if (hasActiveRating) DtAmberPrimary else DtBorder,
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
                                    color = if (hasActiveRating) DtAmberPrimary else DtTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = ratingComparator.label,
                                    color = if (hasActiveRating) DtAmberPrimary else DtTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Cycle comparison mode",
                                    tint = if (hasActiveRating) DtAmberPrimary else DtTextTertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // ALL Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (ratingFilter == null) DtAmberPrimary else DtCardDark)
                                    .clickable { viewModel.setRatingFilter(null) }
                                    .padding(horizontal = 7.dp, vertical = 5.dp)
                                    .testTag("filter_stars_all")
                            ) {
                                Text(
                                    text = "ALL",
                                    color = if (ratingFilter == null) DtDeepDark else DtTextSecondary,
                                    fontSize = 11.sp,
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
                                        .background(if (active) DtAmberPrimary else DtCardDark)
                                        .clickable {
                                            if (active) viewModel.setRatingFilter(null) else viewModel.setRatingFilter(stars)
                                        }
                                        .padding(horizontal = 5.dp, vertical = 5.dp)
                                        .testTag("filter_stars_$stars")
                                ) {
                                    Text(
                                        text = "$stars",
                                        color = if (active) DtDeepDark else DtTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "$stars stars",
                                        tint = if (active) DtDeepDark else if (stars == 0) DtTextTertiary else DtAmberPrimary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // FILTER BAR ROW 2: Color Tag Multi-Selection & Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Color Tag Multi-Selector Dots
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "TAGS:",
                                color = DtTextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            ColorLabelMultiSelector(
                                selectedLabels = selectedColorLabels,
                                onToggleLabel = { label -> viewModel.toggleColorFilter(label) },
                                dotSize = 18,
                                spacing = 6,
                                showCheckMark = true
                            )

                            if (selectedColorLabels.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DtCardDark)
                                        .clickable { viewModel.clearColorFilter() }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                        .testTag("btn_clear_color_filter")
                                ) {
                                    Text(
                                        text = "CLEAR",
                                        color = DtTextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Color Filter Mode Toggle Button ("At least one" vs "Only selected")
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (selectedColorLabels.isNotEmpty()) DtAmberDark.copy(alpha = 0.35f) else DtCardDark
                                )
                                .border(
                                    width = if (selectedColorLabels.isNotEmpty()) 1.dp else 0.5.dp,
                                    color = if (selectedColorLabels.isNotEmpty()) DtAmberPrimary else DtBorder,
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
                                color = if (selectedColorLabels.isNotEmpty()) DtAmberPrimary else DtTextTertiary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = colorFilterMode.label,
                                color = if (selectedColorLabels.isNotEmpty()) Color.White else DtTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ACTIVE FILTER STATUS BREADCRUMB
                    if (ratingFilter != null || selectedColorLabels.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DtCardDark.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
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
                                    color = DtAmberPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                if (ratingFilter != null) {
                                    val ratingDesc = when (ratingComparator) {
                                        RatingComparator.EXACT -> "$ratingFilter★ only"
                                        RatingComparator.LESS_EQUAL -> "$ratingFilter★ and less"
                                        RatingComparator.GREATER_EQUAL -> "$ratingFilter★ and more"
                                    }
                                    Text(
                                        text = "Rating: $ratingDesc",
                                        color = DtTextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (ratingFilter != null && selectedColorLabels.isNotEmpty()) {
                                    Text("•", color = DtTextTertiary, fontSize = 10.sp)
                                }

                                if (selectedColorLabels.isNotEmpty()) {
                                    val colorsStr = selectedColorLabels.joinToString("+") { it.name.lowercase().capitalize() }
                                    val modeDesc = if (colorFilterMode == ColorFilterMode.ONLY_SELECTED) "Only" else "At least 1"
                                    Text(
                                        text = "Colors: $colorsStr ($modeDesc)",
                                        color = DtTealFilmic,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                text = "RESET",
                                color = DtAmberPrimary,
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
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                containerColor = DtAmberPrimary,
                contentColor = DtDeepDark,
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
                        tint = DtTextTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No photographs matching filter",
                        color = DtTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Try switching rating condition (exact vs at most) or color tag mode, or import a new photo.",
                        color = DtTextSecondary,
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
                        Text("RESET FILTERS", color = DtAmberPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    end = 10.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos, key = { it.id }) { photo ->
                    PhotoGridCard(
                        photo = photo,
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

    // Long press context menu dialog
    contextMenuPhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { contextMenuPhoto = null },
            containerColor = DtPanelDark,
            titleContentColor = DtTextPrimary,
            textContentColor = DtTextSecondary,
            title = {
                Text(text = photo.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Format: ${photo.fileFormat} • ${photo.dimensions}")
                    Text("Camera: ${photo.cameraModel}")
                    Text("Lens: ${photo.lens}")
                    Text("Exposure: ${photo.aperture} • ${photo.shutterSpeed} • ISO ${photo.iso}")
                    if (photo.colorLabels.isNotEmpty()) {
                        Text("Color Tags: ${photo.colorLabels.joinToString { it.name }}")
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.duplicatePhoto(photo)
                            contextMenuPhoto = null
                        }
                    ) {
                        Text("DUPLICATE", color = DtAmberPrimary)
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
                    Text("CLOSE", color = DtTextSecondary)
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, DtBorder, RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onLongClick
            )
            .testTag("photo_card_${photo.id}"),
        colors = CardDefaults.cardColors(containerColor = DtCardDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Image Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(DtDeepDark)
            ) {
                if (photo.uriString.startsWith("res://")) {
                    val resName = photo.uriString.removePrefix("res://")
                    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                    if (resId != 0) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = resId),
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    AsyncImage(
                        model = photo.uriString,
                        contentDescription = photo.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
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

                // If Darktable parameters are applied (edited indicator)
                if (photo.history.size > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(DtAmberPrimary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EDITED",
                            color = DtDeepDark,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Card Footer: Title, EXIF summary, Ratings, and Color Tags
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = photo.title,
                    color = DtTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${photo.cameraModel} • ${photo.focalLength} • ${photo.aperture}",
                    color = DtTextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
