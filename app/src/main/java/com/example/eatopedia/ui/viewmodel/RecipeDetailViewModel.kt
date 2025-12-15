package com.example.eatopedia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eatopedia.data.local.FridgeItemEntity
import com.example.eatopedia.data.local.LocalRecipeEntity
import com.example.eatopedia.data.repository.FridgeRepository
import com.example.eatopedia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ViewModel для екрану деталей рецепта
@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val fridgeRepository: FridgeRepository
) : ViewModel() {

    // ==========================================
    // STATE
    // ==========================================

    // Поточний рецепт
    private val _recipe = MutableStateFlow<LocalRecipeEntity?>(null)
    val recipe: StateFlow<LocalRecipeEntity?> = _recipe.asStateFlow()

    // Список інгредієнтів (розпарсений)
    val ingredients: StateFlow<List<String>> = _recipe.map { recipe ->
        recipe?.ingredientsText?.split(",")?.map { it.trim() } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Продукти з холодильника
    private val fridgeItems: StateFlow<List<FridgeItemEntity>> = fridgeRepository
        .getMyFridgeItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Інгредієнти з позначками (✅ є в холодильнику, ❌ немає)
    val ingredientsWithStatus: StateFlow<List<IngredientStatus>> = combine(
        ingredients,
        fridgeItems
    ) { ingredientsList, fridge ->
        val fridgeNames = fridge.map { it.name.lowercase() }

        ingredientsList.map { ingredient ->
            val isAvailable = fridgeNames.any {
                it.contains(ingredient.lowercase()) || ingredient.lowercase().contains(it)
            }
            IngredientStatus(name = ingredient, isAvailable = isAvailable)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Інструкція (розбита по пунктам через *)
    val instructions: StateFlow<List<String>> = _recipe.map { recipe ->
        recipe?.description?.split("*")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Чи йде завантаження
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Помилка
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ==========================================
    // ACTIONS
    // ==========================================

    // Завантажити рецепт за ID
    fun loadRecipe(recipeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = recipeRepository.getRecipeById(recipeId)

            result.onSuccess { recipe ->
                _recipe.value = recipe
            }.onFailure { error ->
                _error.value = error.message
            }

            _isLoading.value = false
        }
    }

    // Додати/прибрати з улюблених
    fun toggleFavorite() {
        val currentRecipe = _recipe.value ?: return

        viewModelScope.launch {
            val newStatus = !currentRecipe.isFavorite
            recipeRepository.toggleFavorite(currentRecipe.id, newStatus)

            // Оновлюємо локальний стан
            _recipe.value = currentRecipe.copy(isFavorite = newStatus)
        }
    }

    // Видалити рецепт
    fun deleteRecipe(onDeleted: () -> Unit) {
        val currentRecipe = _recipe.value ?: return

        viewModelScope.launch {
            val result = recipeRepository.deleteRecipe(currentRecipe)

            result.onSuccess {
                onDeleted()
            }.onFailure { error ->
                _error.value = error.message
            }
        }
    }
}

// Модель для інгредієнта з статусом
data class IngredientStatus(
    val name: String,
    val isAvailable: Boolean // true = є в холодильнику, false =  немає
)