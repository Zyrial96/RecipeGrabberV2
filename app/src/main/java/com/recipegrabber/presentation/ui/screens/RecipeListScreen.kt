package com.recipegrabber.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import com.recipegrabber.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.recipegrabber.data.local.entity.RecipeWithDetails
import com.recipegrabber.presentation.ui.components.RecipeArtwork
import com.recipegrabber.presentation.ui.components.RecipeMetadata
import com.recipegrabber.presentation.ui.components.ToolIcon
import com.recipegrabber.presentation.viewmodel.RecipeListViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecipeListScreen(
    onRecipeClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: RecipeListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val extractionState by viewModel.extractionState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<RecipeWithDetails?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    val recipes = uiState.recipes.filter { !favoritesOnly || it.recipe.isFavorite }
    val progress by animateFloatAsState(extractionState.progress, label = "library-progress")
    LaunchedEffect(query) { viewModel.updateSearchQuery(query) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(extractionState.error) {
        extractionState.error?.let { snackbar.showSnackbar(it); viewModel.resetExtractionState() }
    }
    LaunchedEffect(extractionState.isExtracting, extractionState.progress) {
        if (!extractionState.isExtracting && extractionState.progress == 1f) {
            snackbar.showSnackbar("Rezept gespeichert")
            viewModel.resetExtractionState()
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Recipe Grabber", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = { ToolIcon(Icons.Default.Settings, "Settings", onSettingsClick) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Add Recipe")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Mein Kochbuch", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(), placeholder = { Text("Rezepte suchen") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) ToolIcon(Icons.Default.Close, "Suche leeren", {
                            query = ""
                        })
                    },
                    singleLine = true, shape = MaterialTheme.shapes.small
                )
                LibraryFilters(favoritesOnly, { favoritesOnly = it }, recipes.size)
                AnimatedVisibility(extractionState.isExtracting) {
                    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(extractionState.message, style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                recipes.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(if (favoritesOnly) Icons.Default.FavoriteBorder else Icons.Default.MenuBook,
                            null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(when { query.isNotBlank() -> "Keine Treffer"; favoritesOnly -> "Noch keine Favoriten"; else -> "Dein Kochbuch beginnt hier" },
                            style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        if (query.isBlank() && !favoritesOnly) {
                            FilledTonalButton(onClick = { showAdd = true }) {
                                Icon(Icons.Default.AddLink, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Rezept importieren")
                            }
                        }
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(320.dp), modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recipes, key = { it.recipe.id }) { entry ->
                        Box(Modifier.animateItemPlacement()) {
                            RecipeCard(entry, { onRecipeClick(entry.recipe.id) },
                                { viewModel.toggleFavorite(entry.recipe) }, { deleting = entry })
                        }
                    }
                }
            }
        }
    }
    if (showAdd) AddRecipeDialog({ showAdd = false }) { viewModel.extractRecipeFromUrl(it); showAdd = false }
    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null }, icon = { Icon(Icons.Default.DeleteOutline, null) },
            title = { Text("Rezept l\u00f6schen?") }, text = { Text(entry.recipe.title) },
            confirmButton = { TextButton(onClick = { viewModel.deleteRecipe(entry.recipe); deleting = null }) { Text("L\u00f6schen") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Abbrechen") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryFilters(favoritesOnly: Boolean, onChange: (Boolean) -> Unit, count: Int) {
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(selected = !favoritesOnly, onClick = { onChange(false) }, label = { Text("Alle") },
            leadingIcon = { Icon(Icons.Default.MenuBook, null, Modifier.size(18.dp)) })
        FilterChip(selected = favoritesOnly, onClick = { onChange(true) }, label = { Text("Favoriten") },
            leadingIcon = { Icon(Icons.Default.FavoriteBorder, null, Modifier.size(18.dp)) })
        Text(pluralStringResource(R.plurals.recipe_count, count, count), Modifier.align(Alignment.CenterVertically).padding(8.dp),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeCard(recipeWithDetails: RecipeWithDetails, onClick: () -> Unit, onFavoriteClick: () -> Unit, onDeleteClick: () -> Unit) {
    val recipe = recipeWithDetails.recipe
    var menu by remember { mutableStateOf(false) }
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                RecipeArtwork(recipe.thumbnailUrl, Modifier.size(76.dp).clip(RoundedCornerShape(6.dp)), sourceUrl = recipe.sourceUrl)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(recipe.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (recipe.description.isNotBlank()) Text(recipe.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecipeMetadata(Icons.Default.Restaurant, pluralStringResource(R.plurals.ingredient_count,
                        recipeWithDetails.ingredients.size, recipeWithDetails.ingredients.size))
                    RecipeMetadata(Icons.Default.FormatListNumbered, pluralStringResource(R.plurals.step_count,
                        recipeWithDetails.steps.size, recipeWithDetails.steps.size))
                    if (recipe.prepTimeMinutes + recipe.cookTimeMinutes > 0) RecipeMetadata(Icons.Default.Schedule, "${recipe.prepTimeMinutes + recipe.cookTimeMinutes} Min.")
                }
                ToolIcon(if (recipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Favorite", onFavoriteClick, selected = recipe.isFavorite)
                Box {
                    ToolIcon(Icons.Default.MoreVert, "Rezeptaktionen", { menu = true })
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("L\u00f6schen") }, leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                            onClick = { menu = false; onDeleteClick() })
                    }
                }
            }
        }
    }
}

@Composable
fun AddRecipeDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var url by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, icon = { Icon(Icons.Default.AddLink, null) }, title = { Text("Rezept importieren") },
        text = { OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Video-Link") },
            placeholder = { Text("https://...") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onConfirm(url.trim()) }, enabled = url.isNotBlank()) { Text("Importieren") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
