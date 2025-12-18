package com.example.eatopedia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.eatopedia.R
import com.example.eatopedia.ui.viewmodel.RecipeDetailViewModel

@Composable
fun RecipeDetailContent(
    recipeId: String,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(recipeId) {
        viewModel.loadRecipe(recipeId)
    }

    val recipe by viewModel.recipe.collectAsState()

    if (recipe == null) {
        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 30.dp)
    ) {
        if (!recipe?.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = recipe?.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.default_img)
            )
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = recipe?.title ?: "",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Автор: ${recipe?.authorName ?: "Невідомий"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                if (recipe?.calories != null && (recipe?.calories ?: 0.0) > 0) {
                    Text(
                        text = "🔥 ${recipe?.calories?.toInt()} ккал",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.eatopedia_dark)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val proteins = recipe?.proteins ?: 0.0
            val fats = recipe?.fats ?: 0.0
            val carbs = recipe?.carbs ?: 0.0

            if (proteins > 0 || fats > 0 || carbs > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NutrientItem(label = "Білки", value = "${proteins.toInt()} г")
                    VerticalDivider(modifier = Modifier.height(24.dp), color = Color.Gray.copy(alpha = 0.3f))
                    NutrientItem(label = "Жири", value = "${fats.toInt()} г")
                    VerticalDivider(modifier = Modifier.height(24.dp), color = Color.Gray.copy(alpha = 0.3f))
                    NutrientItem(label = "Вуглеводи", value = "${carbs.toInt()} г")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            val ingredientsString = recipe?.ingredientsText
            if (!ingredientsString.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Інгредієнти:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val ingredientsList = remember(ingredientsString) {
                        ingredientsString.split(",").map { it.trim() }
                    }

                    ingredientsList.forEachIndexed { index, item ->
                        IngredientRow(item)
                        if (index < ingredientsList.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.Gray.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = "Приготування:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val formattedDescription = recipe?.description?.replace("*", "\n• ") ?: ""

            Text(
                text = formattedDescription,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun IngredientRow(rawText: String) {
    val parts = rawText.split(":").map { it.trim() }
    val name = parts.getOrNull(0) ?: rawText
    val amount = parts.getOrNull(1)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colorResource(id = R.color.eatopedia_dark), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (amount != null) {
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NutrientItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}