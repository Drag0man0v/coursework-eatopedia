package com.example.eatopedia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eatopedia.data.local.LocalRecipeEntity
import com.example.eatopedia.data.repository.AuthRepository
import com.example.eatopedia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// ViewModel для екрану додавання рецепта
@HiltViewModel
class AddRecipeViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // ==========================================
    // STATE - Поля форми
    // ==========================================

    // Назва страви
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    // Фото (URL або локальний шлях)
    private val _imageUrl = MutableStateFlow<String?>(null)
    val imageUrl: StateFlow<String?> = _imageUrl.asStateFlow()

    // Інгредієнти (список: назва + грамовка)
    private val _ingredients = MutableStateFlow<List<IngredientInput>>(emptyList())
    val ingredients: StateFlow<List<IngredientInput>> = _ingredients.asStateFlow()

    // Покрокова інструкція (список пунктів)
    private val _instructions = MutableStateFlow<List<String>>(listOf(""))
    val instructions: StateFlow<List<String>> = _instructions.asStateFlow()

    // КБЖУ (опціонально, користувач може не вводити)
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

    // Чи йде збереження
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // Повідомлення
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // ==========================================
    // ACTIONS - Оновлення полів
    // ==========================================

    fun onTitleChanged(value: String) {
        _title.value = value
    }

    fun onImageUrlChanged(value: String) {
        _imageUrl.value = value
    }

    fun onCaloriesChanged(value: String) {
        _calories.value = value
    }

    fun onProteinsChanged(value: String) {
        _proteins.value = value
    }

    fun onFatsChanged(value: String) {
        _fats.value = value
    }

    fun onCarbsChanged(value: String) {
        _carbs.value = value
    }

    fun onWeightChanged(value: String) {
        _weight.value = value
    }

    // ==========================================
    // ACTIONS - Інгредієнти
    // ==========================================

    // Додати інгредієнт
    fun addIngredient() {
        val current = _ingredients.value.toMutableList()
        current.add(IngredientInput(name = "", grams = ""))
        _ingredients.value = current
    }

    // Видалити інгредієнт
    fun removeIngredient(index: Int) {
        val current = _ingredients.value.toMutableList()
        current.removeAt(index)
        _ingredients.value = current
    }

    // Оновити назву інгредієнта
    fun onIngredientNameChanged(index: Int, value: String) {
        val current = _ingredients.value.toMutableList()
        current[index] = current[index].copy(name = value)
        _ingredients.value = current
    }

    // Оновити грамовку інгредієнта
    fun onIngredientGramsChanged(index: Int, value: String) {
        val current = _ingredients.value.toMutableList()
        current[index] = current[index].copy(grams = value)
        _ingredients.value = current
    }

    // ==========================================
    // ACTIONS - Інструкція
    // ==========================================

    // Додати пункт інструкції
    fun addInstructionStep() {
        val current = _instructions.value.toMutableList()
        current.add("")
        _instructions.value = current
    }

    // Видалити пункт інструкції
    fun removeInstructionStep(index: Int) {
        val current = _instructions.value.toMutableList()
        current.removeAt(index)
        _instructions.value = current
    }

    // Оновити текст пункту інструкції
    fun onInstructionChanged(index: Int, value: String) {
        val current = _instructions.value.toMutableList()
        current[index] = value
        _instructions.value = current
    }

    // ==========================================
    // ACTIONS - Збереження
    // ==========================================

    // Зберегти рецепт
    fun saveRecipe(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Валідація
            if (_title.value.isBlank()) {
                _message.value = "Введіть назву страви"
                return@launch
            }

            if (_ingredients.value.isEmpty()) {
                _message.value = "Додайте хоча б один інгредієнт"
                return@launch
            }

            _isSaving.value = true

            // Формуємо текст інгредієнтів
            val ingredientsText = _ingredients.value.joinToString(", ") {
                "${it.name} (${it.grams}г)"
            }

            // Формуємо інструкцію (розділяємо через *)
            val instructionsText = _instructions.value
                .filter { it.isNotBlank() }
                .joinToString(" * ")

            // Створюємо рецепт
            val userId = authRepository.getCurrentUserId()
            val userProfile = authRepository.getCurrentUser().getOrNull()

            val recipe = LocalRecipeEntity(
                id = "local_${UUID.randomUUID()}", // Локальний ID
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

            // Зберігаємо
            val result = recipeRepository.createRecipe(recipe)

            result.onSuccess {
                _message.value = "✅ Рецепт створено!"
                clearForm()
                onSuccess()
            }.onFailure { error ->
                _message.value = "Помилка: ${error.message}"
            }

            _isSaving.value = false
        }
    }

    // Очистити форму
    private fun clearForm() {
        _title.value = ""
        _imageUrl.value = null
        _ingredients.value = emptyList()
        _instructions.value = listOf("")
        _calories.value = ""
        _proteins.value = ""
        _fats.value = ""
        _carbs.value = ""
        _weight.value = ""
    }

    // Очистити повідомлення
    fun clearMessage() {
        _message.value = null
    }
}

// Модель для інгредієнта у формі
data class IngredientInput(
    val name: String,
    val grams: String
)