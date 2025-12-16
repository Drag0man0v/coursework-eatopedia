package com.example.eatopedia.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar з пошуком
        TopAppBar(
            title = { Text("Eatopedia", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colorResource(id = R.color.splash_background)
            )
        )

        // Пошукове поле
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Шукати рецепти або авторів...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        // Список рецептів
        if (filteredRecipes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Рецептів не знайдено")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRecipes) { recipe ->
                    RecipeCard(
                        title = recipe.title,
                        authorName = recipe.authorName ?: "Невідомий автор",
                        imageUrl = recipe.imageUrl,
                        isFavorite = recipe.isFavorite,
                        onClick = { onRecipeClick(recipe.id) },
                        onFavoriteClick = {
                            viewModel.toggleFavorite(recipe.id, !recipe.isFavorite)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RecipeCard(
    title: String,
    authorName: String,
    imageUrl: String?,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Картинка
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Текст
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Автор: $authorName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Іконка серця
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Улюблене",
                    tint = if (isFavorite) colorResource(id = R.color.eatopedia_dark) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}