package com.recipegrabber.presentation.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FoodBank
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.recipegrabber.presentation.ui.components.RecipeArtwork
import com.recipegrabber.presentation.ui.components.ToolIcon
import androidx.hilt.navigation.compose.hiltViewModel
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.presentation.viewmodel.RecipeDetailViewModel
import com.recipegrabber.presentation.util.originalVideoIntent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Rezept", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    uiState.recipe?.recipe?.let { recipe ->
                        ToolIcon(Icons.Default.Share, "Teilen", { viewModel.shareRecipe(context) })
                        ToolIcon(if (recipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorit", { viewModel.toggleFavorite() }, selected = recipe.isFavorite)
                        ToolIcon(Icons.Default.Delete, "Löschen", { showDeleteDialog = true })
                    }
                }
            )
        }
    ) { paddingValues ->
        uiState.recipe?.let { recipeWithDetails ->
            val recipe = recipeWithDetails.recipe

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(recipe.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                if (com.recipegrabber.presentation.util.recipeArtworkUrl(recipe.thumbnailUrl, recipe.sourceUrl) != null) {
                    item {
                        RecipeArtwork(recipe.thumbnailUrl, Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(8.dp)), sourceUrl = recipe.sourceUrl)
                    }
                }
                item {
                    OriginalVideoButton(recipe.sourceUrl) { intent ->
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            scope.launch { snackbarHostState.showSnackbar("Keine App zum \u00d6ffnen des Videos verf\u00fcgbar.") }
                        } catch (_: SecurityException) {
                            scope.launch { snackbarHostState.showSnackbar("Das Originalvideo konnte nicht ge\u00f6ffnet werden.") }
                        }
                    }
                }
                item {
                    RecipeHeader(
                        servings = recipe.servings,
                        scaledServings = uiState.scaledServings,
                        prepTime = recipe.prepTimeMinutes,
                        cookTime = recipe.cookTimeMinutes,
                        onIncrement = { viewModel.incrementServings() },
                        onDecrement = { viewModel.decrementServings() }
                    )
                }

                if (recipe.description.isNotBlank()) {
                    item {
                        Text(
                            text = recipe.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Text(
                        text = "Zutaten",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                itemsIndexed(recipeWithDetails.ingredients.sortedBy { it.orderIndex }) { _, ingredient ->
                    IngredientItem(
                        ingredient = ingredient,
                        scaledAmount = viewModel.scaleAmount(ingredient.amount)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Zubereitung",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                itemsIndexed(recipeWithDetails.steps.sortedBy { it.order }) { index, step ->
                    StepItem(step = step, stepNumber = index + 1)
                }
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Rezept nicht gefunden")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Rezept löschen") },
            text = { Text("Möchtest du dieses Rezept wirklich löschen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecipe(onNavigateBack)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
fun OriginalVideoButton(sourceUrl: String, onOpen: (Intent) -> Unit) {
    val intent = remember(sourceUrl) { originalVideoIntent(sourceUrl) } ?: return
    FilledTonalButton(onClick = { onOpen(intent) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text("Originalvideo ansehen", modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun RecipeHeader(
    servings: Int,
    scaledServings: Int,
    prepTime: Int,
    cookTime: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FoodBank,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Portionen",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = onDecrement,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Weniger")
                    }

                    Text(
                        text = "$scaledServings",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 64.dp).padding(horizontal = 8.dp).testTag("servings-value")
                    )

                    FilledIconButton(
                        onClick = onIncrement,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Mehr")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Vorbereitung",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${prepTime} Min.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Kochzeit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "${cookTime} Min.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Gesamt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "${prepTime + cookTime} Min.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
    }
}

@Composable
fun IngredientItem(
    ingredient: Ingredient,
    scaledAmount: Double?
) {
    var checked by rememberSaveable(ingredient.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(checked, role = Role.Checkbox, onValueChange = { checked = it })
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(end = 12.dp).size(32.dp))
        Text(
            text = buildString {
                if (scaledAmount != null && scaledAmount > 0) {
                    append(com.recipegrabber.presentation.util.formatIngredientAmount(scaledAmount))
                    if (ingredient.unit.isNotBlank()) {
                        append(" ${ingredient.unit}")
                    }
                    append(" ")
                }
                append(ingredient.name)
                if (ingredient.notes.isNotBlank()) {
                    append(" (${ingredient.notes})")
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StepItem(step: Step, stepNumber: Int) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center) {
                Text(
                text = "$stepNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.bodyLarge
                )
                step.duration?.let { duration ->
                    if (duration > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${duration} Min.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
}

