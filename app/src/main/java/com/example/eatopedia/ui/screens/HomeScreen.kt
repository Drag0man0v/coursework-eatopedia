package com.example.eatopedia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.eatopedia.R
import com.example.eatopedia.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRecipeClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val filteredRecipes by viewModel.filteredRecipes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedRecipeId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            SearchBarCustom(
                query = searchQuery,
                onQueryChanged = { viewModel.onSearchQueryChanged(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (searchQuery.isNotEmpty()) {
                Text(
                    text = "Результати пошуку",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredRecipes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🍽️",
                            fontSize = 80.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Рецептів не знайдено",
                            color = Color.Gray,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredRecipes) { recipe ->
                        RecipeCardPretty(
                            title = recipe.title,
                            authorName = recipe.authorName ?: "Шеф-кухар",
                            imageUrl = recipe.imageUrl,
                            isFavorite = recipe.isFavorite,
                            onClick = {
                                selectedRecipeId = recipe.id
                                showBottomSheet = true
                            },
                            onFavoriteClick = {
                                viewModel.toggleFavorite(recipe.id, !recipe.isFavorite)
                            }
                        )
                    }
                }
            }
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
fun SearchBarCustom(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = { Text("") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.Gray
            )
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorResource(id = R.color.eatopedia_dark),
            unfocusedBorderColor = Color.LightGray,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        singleLine = true
    )
}


@Composable
fun RecipeCardPretty(
    title: String,
    authorName: String,
    imageUrl: String?,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val imageModel = if (imageUrl.isNullOrEmpty()) {
        R.drawable.default_img
    } else {
        imageUrl
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isFavorite) Color.Red else Color.Gray
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Автор: $authorName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}