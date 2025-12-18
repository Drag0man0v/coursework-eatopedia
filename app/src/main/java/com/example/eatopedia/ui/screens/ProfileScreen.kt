package com.example.eatopedia.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImage
import com.example.eatopedia.R
import com.example.eatopedia.data.local.LocalRecipeEntity
import com.example.eatopedia.data.remote.ProfileDto
import com.example.eatopedia.ui.viewmodel.ProfileViewModel

enum class RecipeViewMode { MY_RECIPES, SAVED_RECIPES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    navigateToSettings: () -> Unit = {}
) {
    val currentUser = viewModel.currentUser.collectAsState()
    val favoriteRecipes = viewModel.favoriteRecipes.collectAsState()
    val myRecipes = viewModel.myRecipes.collectAsState()
    val message = viewModel.message.collectAsState()
    val showAddRecipeScreen = viewModel.showAddRecipeScreen.collectAsState()

    val isLoading = currentUser.value == null
    val snackbarHostState = remember { SnackbarHostState() }
    var viewMode by remember { mutableStateOf(RecipeViewMode.MY_RECIPES) }

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedRecipeId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(message.value) {
        message.value?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            if (!showAddRecipeScreen.value) {
                ProfileAppBar(username = currentUser.value?.username, viewModel = viewModel)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (viewMode == RecipeViewMode.MY_RECIPES && !showAddRecipeScreen.value) {
                FloatingActionButton(
                    onClick = { viewModel.openAddRecipeScreen() },
                    containerColor = colorResource(id = R.color.eatopedia_dark),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Додати рецепт")
                }
            }
        }
    ) { paddingValues ->

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val user = currentUser.value

            if (user != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    UserInfoSection(user = user)
                    Spacer(modifier = Modifier.height(16.dp))

                    ViewModeButtons(
                        currentMode = viewMode,
                        onMyRecipesClick = { viewMode = RecipeViewMode.MY_RECIPES },
                        onSavedRecipesClick = { viewMode = RecipeViewMode.SAVED_RECIPES }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val recipesToShow = when (viewMode) {
                        RecipeViewMode.MY_RECIPES -> myRecipes.value
                        RecipeViewMode.SAVED_RECIPES -> favoriteRecipes.value
                    }

                    RecipeCardListContent(
                        posts = recipesToShow,
                        onRecipeClick = { recipeId ->
                            selectedRecipeId = recipeId
                            showBottomSheet = true
                        }
                    )
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Будь ласка, увійдіть, щоб переглянути профіль.")
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

        if (showAddRecipeScreen.value) {
            AddRecipeScreen(
                onDismiss = { viewModel.closeAddRecipeScreen() },
                onSuccess = { viewModel.closeAddRecipeScreen() }
            )
        }
    }
}

@Composable
fun ViewModeButtons(
    currentMode: RecipeViewMode,
    onMyRecipesClick: () -> Unit,
    onSavedRecipesClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onMyRecipesClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentMode == RecipeViewMode.MY_RECIPES)
                    colorResource(id = R.color.eatopedia_dark)
                else
                    Color.LightGray
            )
        ) {
            Text(
                "Мої Рецепти",
                color = if (currentMode == RecipeViewMode.MY_RECIPES) Color.White else Color.Black
            )
        }

        Button(
            onClick = onSavedRecipesClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentMode == RecipeViewMode.SAVED_RECIPES)
                    colorResource(id = R.color.eatopedia_dark)
                else
                    Color.LightGray
            )
        ) {
            Text(
                "Збережені",
                color = if (currentMode == RecipeViewMode.SAVED_RECIPES) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun UserInfoSection(user: ProfileDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter("android.resource://com.example.eatopedia/drawable/user_default"),
                contentDescription = "Аватар",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(3.dp, colorResource(id = R.color.eatopedia_dark), CircleShape)
            )

            Spacer(modifier = Modifier.width(20.dp))

            Text(
                text = user.username,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!user.bio.isNullOrBlank()) {
            Text(user.bio!!, fontSize = 14.sp)
        } else {
            Text("Поки немає опису.", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun RecipeCardListContent(
    posts: List<LocalRecipeEntity>,
    onRecipeClick: (String) -> Unit
) {
    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Немає збережених рецептів",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        }
    } else {
        val reversedPosts = remember(posts) { posts.reversed() }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(reversedPosts, key = { it.id }) { recipe ->
                RecipeCardPretty(
                    recipe = recipe,
                    onClick = { onRecipeClick(recipe.id) }
                )
            }
        }
    }
}

@Composable
fun RecipeCardPretty(
    recipe: LocalRecipeEntity,
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
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
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
                    contentDescription = recipe.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Автор: ${recipe.authorName ?: "Невідомий"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun RecipeGridContent(
    posts: List<LocalRecipeEntity>,
    onRecipeClick: (String) -> Unit
) {
    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83C\uDFDC\uFE0F", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Тут поки що порожньо",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(posts, key = { it.id }) { recipe ->
                RecipeGridItem(
                    recipe = recipe,
                    onClick = { onRecipeClick(recipe.id) }
                )
            }
        }
    }
}

@Composable
fun RecipeGridItem(
    recipe: LocalRecipeEntity,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .background(Color.LightGray)
            .clickable(onClick = onClick)
    ) {
        val model = if (recipe.imageUrl.isNullOrBlank()) {
            R.drawable.default_img
        } else {
            recipe.imageUrl
        }

        AsyncImage(
            model = model,
            contentDescription = recipe.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAppBar(
    username: String?,
    viewModel: ProfileViewModel = hiltViewModel(),
    navToLogin: () -> Unit = {},
    onPickAvatar: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = "Профіль",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        actions = {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.List, contentDescription = "Меню")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(12.dp),
                    containerColor = Color.White.copy(alpha = 0.95f)
                ) {
                    DropdownMenuItem(
                        text = { Text("Змінити фото") },
                        onClick = {
                            expanded = false
                            onPickAvatar()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Вийти") },
                        onClick = {
                            expanded = false
                            viewModel.signOut { navToLogin() }
                        }
                    )
                }
            }
        }
    )
}