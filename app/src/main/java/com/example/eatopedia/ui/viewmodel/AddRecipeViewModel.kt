package com.example.eatopedia.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eatopedia.data.local.IngredientsKbgv
import com.example.eatopedia.data.local.LocalRecipeEntity
import com.example.eatopedia.data.repository.AuthRepository
import com.example.eatopedia.data.repository.FridgeRepository
import com.example.eatopedia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@HiltViewModel
class AddRecipeViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val authRepository: AuthRepository,
    private val fridgeRepository: FridgeRepository
) : ViewModel() {

    // Стан полів форми
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _imageUrl = MutableStateFlow<String?>(null)
    val imageUrl: StateFlow<String?> = _imageUrl.asStateFlow()

    private val _ingredients = MutableStateFlow<List<IngredientInput>>(listOf(IngredientInput("", "")))
    val ingredients: StateFlow<List<IngredientInput>> = _ingredients.asStateFlow()

    private val _instructions = MutableStateFlow<List<String>>(listOf(""))
    val instructions: StateFlow<List<String>> = _instructions.asStateFlow()

    // Розраховані дані КБЖУ
    private val _calories = MutableStateFlow("")
    val calories: StateFlow<String> = _calories.asStateFlow()

    private val _proteins = MutableStateFlow("")
    val proteins: StateFlow<String> = _proteins.asStateFlow()

    private val _fats = MutableStateFlow("")
    val fats: StateFlow<String> = _fats.asStateFlow()

    private val _carbs = MutableStateFlow("")
    val carbs: StateFlow<String> = _carbs.asStateFlow()

    private val _weight = MutableStateFlow("")
    val weight: StateFlow<String> = _weight.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val ingredientSearchFlows = MutableStateFlow<Map<Int, StateFlow<List<String>>>>(emptyMap())

    // Методи оновлення полів
    fun onTitleChanged(value: String) {
        _title.value = value
    }

    fun onImageUrlChanged(value: String) {
        _imageUrl.value = value
    }

    // Робота з інгредієнтами
    fun addIngredient() {
        val current = _ingredients.value.toMutableList()
        current.add(IngredientInput(name = "", grams = ""))
        _ingredients.value = current
    }

    fun removeIngredient(index: Int) {
        val current = _ingredients.value.toMutableList()
        current.removeAt(index)
        _ingredients.value = current
        recalculateNutrients()
    }

    fun onIngredientNameChanged(index: Int, value: String) {
        val current = _ingredients.value.toMutableList()
        current[index] = current[index].copy(name = value)
        _ingredients.value = current
        recalculateNutrients()
    }

    fun onIngredientGramsChanged(index: Int, value: String) {
        val current = _ingredients.value.toMutableList()
        current[index] = current[index].copy(grams = value)
        _ingredients.value = current
        recalculateNutrients()
    }

    fun selectIngredientSuggestion(index: Int, suggestion: String) {
        onIngredientNameChanged(index, suggestion)
    }

    fun getIngredientsSearchResults(index: Int): StateFlow<List<String>> {
        return _ingredients.map { ingredients ->
            val query = ingredients.getOrNull(index)?.name ?: ""
            if (query.length < 2) {
                emptyList()
            } else {
                val fromDatabase = IngredientsKbgv.database.keys.filter {
                    it.contains(query.lowercase())
                }
                fromDatabase.take(5)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Розрахунок харчової цінності
    private fun recalculateNutrients() {
        viewModelScope.launch {
            var totalCalories = 0.0
            var totalProteins = 0.0
            var totalFats = 0.0
            var totalCarbs = 0.0
            var totalGrams = 0.0

            _ingredients.value.forEach { ingredient ->
                val name = ingredient.name.lowercase().trim()
                val grams = ingredient.grams.toDoubleOrNull() ?: 0.0

                IngredientsKbgv.database[name]?.let { nutrients ->
                    val multiplier = grams / 100.0

                    totalCalories += nutrients.calories * multiplier
                    totalProteins += nutrients.proteins * multiplier
                    totalFats += nutrients.fats * multiplier
                    totalCarbs += nutrients.carbs * multiplier

                    totalGrams += grams

                    Log.d("NutrientsCalc", "Added: $name, weight: $grams")
                } ?: run {
                    if (grams > 0) {
                        totalGrams += grams
                        Log.d("NutrientsCalc", "Not found: $name, weight included: $grams")
                    }
                }
            }

            _weight.value = totalGrams.toInt().toString()

            if (totalGrams > 0) {
                val calPer100 = (totalCalories / totalGrams) * 100
                val proPer100 = (totalProteins / totalGrams) * 100
                val fatPer100 = (totalFats / totalGrams) * 100
                val carbPer100 = (totalCarbs / totalGrams) * 100

                _calories.value = "%.0f".format(calPer100)
                _proteins.value = "%.1f".format(proPer100)
                _fats.value = "%.1f".format(fatPer100)
                _carbs.value = "%.1f".format(carbPer100)
            } else {
                _calories.value = "0"
                _proteins.value = "0"
                _fats.value = "0"
                _carbs.value = "0"
            }
        }
    }

    // Робота з інструкціями
    fun addInstructionStep() {
        val current = _instructions.value.toMutableList()
        current.add("")
        _instructions.value = current
    }

    fun removeInstructionStep(index: Int) {
        val current = _instructions.value.toMutableList()
        current.removeAt(index)
        _instructions.value = current
    }

    fun onInstructionChanged(index: Int, value: String) {
        val current = _instructions.value.toMutableList()
        current[index] = value
        _instructions.value = current
    }

    fun saveRecipe(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (_title.value.isBlank()) {
                _message.value = "Введіть назву страви"
                return@launch
            }

            if (_ingredients.value.isEmpty() || _ingredients.value.all { it.name.isBlank() }) {
                _message.value = "Додайте хоча б один інгредієнт"
                return@launch
            }

            _isSaving.value = true

            val ingredientsText = _ingredients.value
                .filter { it.name.isNotBlank() }
                .joinToString(", ") {
                    if (it.grams.isNotBlank()) {
                        "${it.name}: ${it.grams}г"
                    } else {
                        it.name
                    }
                }

            val instructionsText = _instructions.value
                .filter { it.isNotBlank() }
                .joinToString(" * ")

            val userId = authRepository.getCurrentUserId()
            val userProfile = authRepository.getCurrentUser().getOrNull()

            val recipe = LocalRecipeEntity(
                id = "local_${UUID.randomUUID()}",
                title = _title.value,
                description = instructionsText,
                imageUrl = _imageUrl.value,
                authorId = userId,
                authorName = userProfile?.username,
                ingredientsText = ingredientsText,
                calories = _calories.value.toDoubleOrNull() ?: 0.0,
                proteins = _proteins.value.toDoubleOrNull() ?: 0.0,
                fats = _fats.value.toDoubleOrNull() ?: 0.0,
                carbs = _carbs.value.toDoubleOrNull() ?: 0.0,
                weight = _weight.value.toDoubleOrNull() ?: 0.0,
                isFavorite = false,
                isPreloaded = false
            )

            val result = recipeRepository.createRecipe(recipe)

            result.onSuccess {
                _message.value = "Рецепт створено"
                clearForm()
                onSuccess()
            }.onFailure { error ->
                _message.value = "Помилка: ${error.message}"
            }

            _isSaving.value = false
        }
    }

    private fun clearForm() {
        _title.value = ""
        _imageUrl.value = null
        _ingredients.value = listOf(IngredientInput("", ""))
        _instructions.value = listOf("")
        _calories.value = ""
        _proteins.value = ""
        _fats.value = ""
        _carbs.value = ""
        _weight.value = ""
    }

    fun clearMessage() {
        _message.value = null
    }
}

data class IngredientInput(
    val name: String,
    val grams: String
)

data class Nutrients(
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double
)