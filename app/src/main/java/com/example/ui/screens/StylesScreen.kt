package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DtLabelRed
import com.example.ui.theme.DtThemeColors
import com.example.viewmodel.DarktableMode
import com.example.viewmodel.DarktableViewModel

@Composable
fun StylesScreen(
    viewModel: DarktableViewModel,
    modifier: Modifier = Modifier
) {
    val palette = DtThemeColors.current
    val styles by viewModel.styles.collectAsState()
    val photo by viewModel.selectedPhoto.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    var showSaveStyleDialog by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.setMode(if (photo != null) DarktableMode.DARKROOM else DarktableMode.LIGHTTABLE)
    }

    val categories = listOf("All") + styles.map { it.category }.distinct()
    val filteredStyles = if (selectedCategory == "All") styles else styles.filter { it.category == selectedCategory }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.setMode(if (photo != null) DarktableMode.DARKROOM else DarktableMode.LIGHTTABLE)
                            },
                            modifier = Modifier.testTag("btn_back_from_styles")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                        }

                        Column {
                            Text(
                                text = "STYLES & PRESETS",
                                color = palette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Curated digital darkroom development recipes",
                                color = palette.textTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (photo != null) {
                        Button(
                            onClick = { showSaveStyleDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accentPrimary, contentColor = palette.onAccent),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp).testTag("btn_save_as_style")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Save Current Recipe", modifier = Modifier.size(16.dp))
                            Text("SAVE STYLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Category selector row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.accentPrimary,
                                selectedLabelColor = palette.onAccent,
                                containerColor = palette.cardDark,
                                labelColor = palette.textSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = palette.border,
                                selectedBorderColor = palette.accentPrimary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredStyles, key = { it.id }) { style ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                        .testTag("style_card_${style.id}"),
                    colors = CardDefaults.cardColors(containerColor = palette.cardDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = style.name,
                                    color = palette.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(palette.accentContainer.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = style.category.uppercase(),
                                    color = palette.accentPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Text(
                            text = style.description,
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        // Module Highlights
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (style.params.filmicEnabled) {
                                Box(
                                    modifier = Modifier
                                        .background(palette.deepDark, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text("Filmic S-Curve", color = palette.tealAccent, fontSize = 9.sp)
                                }
                            }
                            if (style.params.isMonochrome) {
                                Box(
                                    modifier = Modifier
                                        .background(palette.deepDark, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text("Monochrome Tri-X", color = palette.textPrimary, fontSize = 9.sp)
                                }
                            }
                            if (style.params.splitToningEnabled) {
                                Box(
                                    modifier = Modifier
                                        .background(palette.deepDark, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text("Split Toning", color = palette.accentPrimary, fontSize = 9.sp)
                                }
                            }
                        }

                        // Apply and Delete Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!style.isBuiltIn) {
                                IconButton(
                                    onClick = { viewModel.deleteStyle(style) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete custom style", tint = DtLabelRed, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Button(
                                onClick = {
                                    viewModel.applyStyle(style)
                                    viewModel.setMode(DarktableMode.DARKROOM)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accentPrimary,
                                    contentColor = palette.onAccent
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("btn_apply_style_${style.id}")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Apply", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("APPLY STYLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Save Style Dialog
    if (showSaveStyleDialog) {
        var styleName by remember { mutableStateOf("") }
        var styleDesc by remember { mutableStateOf("") }
        var styleCat by remember { mutableStateOf("Custom") }

        AlertDialog(
            onDismissRequest = { showSaveStyleDialog = false },
            containerColor = palette.panelDark,
            titleContentColor = palette.textPrimary,
            title = { Text("Save Current Parameters as Style") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = styleName,
                        onValueChange = { styleName = it },
                        label = { Text("Style Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.cardDark,
                            unfocusedContainerColor = palette.cardDark,
                            focusedBorderColor = palette.accentPrimary,
                            unfocusedBorderColor = palette.border,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = styleDesc,
                        onValueChange = { styleDesc = it },
                        label = { Text("Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.cardDark,
                            unfocusedContainerColor = palette.cardDark,
                            focusedBorderColor = palette.accentPrimary,
                            unfocusedBorderColor = palette.border,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = styleCat,
                        onValueChange = { styleCat = it },
                        label = { Text("Category") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.cardDark,
                            unfocusedContainerColor = palette.cardDark,
                            focusedBorderColor = palette.accentPrimary,
                            unfocusedBorderColor = palette.border,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (styleName.isNotBlank()) {
                            viewModel.saveCurrentStyle(
                                name = styleName,
                                description = styleDesc.ifBlank { "User custom style" },
                                category = styleCat.ifBlank { "Custom" }
                            )
                            showSaveStyleDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentPrimary, contentColor = palette.onAccent),
                    enabled = styleName.isNotBlank()
                ) {
                    Text("SAVE RECIPE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveStyleDialog = false }) {
                    Text("CANCEL", color = palette.textSecondary)
                }
            }
        )
    }
}
