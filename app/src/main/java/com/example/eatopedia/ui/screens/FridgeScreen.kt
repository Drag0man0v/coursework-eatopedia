package com.example.eatopedia.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.eatopedia.R
import com.example.eatopedia.ui.viewmodel.FridgeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgeScreen(
    onRecipeClick: (String) -> Unit = {},
    viewModel: FridgeViewModel = hiltViewModel()
) {
    val fridgeItems by viewModel.fridgeItems.collectAsState()
    val isEmpty by viewModel.isEmpty.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val showSearchResults by viewModel.showSearchResults.collectAsState()
    val foundRecipes by viewModel.foundRecipes.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val message by viewModel.message.collectAsState()

    val context = LocalContext.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedRecipeId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!showSearchResults) {
                FloatingActionButton(
                    onClick = { viewModel.openAddDialog() },
                    containerColor = colorResource(id = R.color.eatopedia_dark),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Додати продукт")
                }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = !showSearchResults,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally()
            ) {
                if (isEmpty) {
                    EmptyFridgeState()
                } else {
                    FridgeContentScreen(
                        fridgeItems = fridgeItems,
                        onDeleteItem = { viewModel.deleteProduct(it) },
                        onSearchRecipes = { viewModel.searchRecipesByFridge() },
                        onClearFridge = { viewModel.clearFridge() }
                    )
                }
            }

            AnimatedVisibility(
                visible = showSearchResults,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
            ) {
                SearchResultsScreen(
                    recipes = foundRecipes,
                    isSearching = isSearching,
                    onRecipeClick = { recipeId ->
                        selectedRecipeId = recipeId
                        showBottomSheet = true
                    },
                    onBackClick = { viewModel.closeSearchResults() }
                )
            }
        }

        if (showAddDialog) {
            AddProductDialog(viewModel = viewModel)
        }

        if (showBottomSheet && selectedRecipeId != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    selectedRecipeId = null
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                RecipeDetailContent(recipeId = selectedRecipeId!!)
            }
        }
    }
}

@Composable
fun EmptyFridgeState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🧊",
            fontSize = 80.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Холодильник порожній",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Додайте продукти, щоб знайти рецепти!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
fun FridgeContentScreen(
    fridgeItems: List<com.example.eatopedia.data.local.FridgeItemEntity>,
    onDeleteItem: (com.example.eatopedia.data.local.FridgeItemEntity) -> Unit,
    onSearchRecipes: () -> Unit,
    onClearFridge: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Мій холодильник",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Продуктів: ${fridgeItems.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSearchRecipes,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.eatopedia_dark),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Що приготувати?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onClearFridge,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Red
            )
        ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Очистити холодильник")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(fridgeItems, key = { it.id }) { item ->
                FridgeItemCard(
                    item = item,
                    onDelete = { onDeleteItem(item) }
                )
            }
        }
    }
}

@Composable
fun FridgeItemCard(
    item: com.example.eatopedia.data.local.FridgeItemEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ){

            Spacer(Modifier.width(16.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Видалити",
                    tint = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    recipes: List<com.example.eatopedia.data.local.LocalRecipeEntity>,
    isSearching: Boolean,
    onRecipeClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Знайдені рецепти") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = colorResource(id = R.color.eatopedia_dark)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Шукаємо рецепти...")
                    }
                }
            } else if (recipes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😔", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Рецептів не знайдено",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Спробуйте додати більше продуктів",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Знайдено: ${recipes.size} рецептів",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(recipes) { recipe ->
                        RecipeCardCompact(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeCardCompact(
    recipe: com.example.eatopedia.data.local.LocalRecipeEntity,
    onClick: () -> Unit
) {
    val imageModel = if (recipe.imageUrl.isNullOrEmpty()) {
        R.drawable.default_img
    } else {
        recipe.imageUrl
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = imageModel,
                contentDescription = recipe.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Автор: ${recipe.authorName ?: "Невідомий"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun AddProductDialog(viewModel: FridgeViewModel) {
    val productInput by viewModel.productInput.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    AlertDialog(
        onDismissRequest = { viewModel.closeAddDialog() },
        title = {
            Text(
                "Додати продукт",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = productInput,
                    onValueChange = { viewModel.onProductInputChanged(it) },
                    label = { Text("Назва продукту") },
                    placeholder = { Text("Наприклад: Молоко") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.LocalDining, contentDescription = null)
                    }
                )

                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Підказки:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))

                    suggestions.forEach { suggestion ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.addProduct(suggestion) },
                            color = colorResource(id = R.color.splash_background),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = suggestion,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.addProduct() },
                enabled = productInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.eatopedia_dark)
                )
            ) {
                Text("Додати")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.closeAddDialog() }) {
                Text("Скасувати", color = Color.Gray)
            }
        }
    )
}