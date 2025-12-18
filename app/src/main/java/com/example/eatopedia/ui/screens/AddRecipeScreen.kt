package com.example.eatopedia.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.eatopedia.R
import com.example.eatopedia.ui.viewmodel.AddRecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AddRecipeViewModel = hiltViewModel()
) {
    val title by viewModel.title.collectAsState()
    val imageUrl by viewModel.imageUrl.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val instructions by viewModel.instructions.collectAsState()
    val calories by viewModel.calories.collectAsState()
    val proteins by viewModel.proteins.collectAsState()
    val fats by viewModel.fats.collectAsState()
    val carbs by viewModel.carbs.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val message by viewModel.message.collectAsState()

    val context = LocalContext.current

    // Показуємо Toast
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новий рецепт", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveRecipe(onSuccess) },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colorResource(id = R.color.eatopedia_dark)
                            )
                        } else {
                            Text(
                                "Зберегти",
                                color = colorResource(id = R.color.eatopedia_dark),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. НАЗВА СТРАВИ
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text("Назва страви *") },
                placeholder = { Text("Наприклад: Борщ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // 2. URL ФОТО (опціонально)
            OutlinedTextField(
                value = imageUrl ?: "",
                onValueChange = { viewModel.onImageUrlChanged(it) },
                label = { Text("URL фото (необов'язково)") },
                placeholder = { Text("https://example.com/image.jpg") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))

            // 3. ІНГРЕДІЄНТИ З АВТОПІДКАЗКАМИ
            Text(
                "Інгредієнти *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "КБЖУ розраховується автоматично",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(Modifier.height(8.dp))

            ingredients.forEachIndexed { index, ingredient ->
                IngredientInputRowWithSuggestions(
                    ingredient = ingredient,
                    onNameChanged = { viewModel.onIngredientNameChanged(index, it) },
                    onGramsChanged = { viewModel.onIngredientGramsChanged(index, it) },
                    onDelete = { viewModel.removeIngredient(index) },
                    onSuggestionSelected = { suggestion ->
                        viewModel.selectIngredientSuggestion(index, suggestion)
                    },
                    viewModel = viewModel,
                    index = index
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.addIngredient() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Додати інгредієнт")
            }

            Spacer(Modifier.height(24.dp))

            // 4. ПОКРОКОВА ІНСТРУКЦІЯ
            Text(
                "Інструкція приготування *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            instructions.forEachIndexed { index, step ->
                InstructionStepRow(
                    index = index,
                    step = step,
                    onStepChanged = { viewModel.onInstructionChanged(index, it) },
                    onDelete = { viewModel.removeInstructionStep(index) }
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.addInstructionStep() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Додати крок")
            }

            Spacer(Modifier.height(24.dp))

            // 5. АВТОМАТИЧНО РОЗРАХОВАНА ХАРЧОВА ЦІННІСТЬ
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.eatopedia_dark).copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📊 Харчова цінність (автоматично)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NutrientChip("🔥 ${calories.ifEmpty { "0" }} ккал")
                        NutrientChip("💪 ${proteins.ifEmpty { "0" }}г білків")
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NutrientChip("🥑 ${fats.ifEmpty { "0" }}г жирів")
                        NutrientChip("🍞 ${carbs.ifEmpty { "0" }}г вугл.")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun NutrientChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

//КОМПОНЕНТ З АВТОПІДКАЗКАМИ
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientInputRowWithSuggestions(
    ingredient: com.example.eatopedia.ui.viewmodel.IngredientInput,
    onNameChanged: (String) -> Unit,
    onGramsChanged: (String) -> Unit,
    onDelete: () -> Unit,
    onSuggestionSelected: (String) -> Unit,
    viewModel: AddRecipeViewModel,
    index: Int
) {
    val suggestions by viewModel.getIngredientsSearchResults(index).collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Поле назви з автопідказками
                ExposedDropdownMenuBox(
                    expanded = expanded && suggestions.isNotEmpty(),
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(2f)
                ) {
                    OutlinedTextField(
                        value = ingredient.name,
                        onValueChange = {
                            onNameChanged(it)
                            expanded = it.isNotEmpty()
                        },
                        label = { Text("Назва") },
                        placeholder = { Text("Молоко") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = expanded && suggestions.isNotEmpty(),
                        onDismissRequest = { expanded = false }
                    ) {
                        suggestions.take(5).forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    onSuggestionSelected(suggestion)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = ingredient.grams,
                    onValueChange = onGramsChanged,
                    label = { Text("Грами") },
                    placeholder = { Text("200") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Видалити",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

// КОМПОНЕНТ ДЛЯ ОДНОГО КРОКУ ІНСТРУКЦІЇ
@Composable
fun InstructionStepRow(
    index: Int,
    step: String,
    onStepChanged: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "${index + 1}.",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, end = 8.dp)
            )

            OutlinedTextField(
                value = step,
                onValueChange = onStepChanged,
                label = { Text("Крок ${index + 1}") },
                placeholder = { Text("Опишіть дію...") },
                modifier = Modifier.weight(1f),
                minLines = 2,
                maxLines = 5
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Видалити",
                    tint = Color.Red
                )
            }
        }
    }
}

