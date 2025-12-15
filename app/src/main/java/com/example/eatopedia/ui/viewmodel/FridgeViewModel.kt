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

// ViewModel для екрану "Мій Холодильник"
@HiltViewModel
class FridgeViewModel @Inject constructor(
    private val fridgeRepository: FridgeRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    // ==========================================
    // STATE
    // ==========================================
    // Список продуктів у холодильнику
    val fridgeItems: StateFlow<List<FridgeItemEntity>> = fridgeRepository
        .getMyFridgeItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Чи холодильник пустий (для Empty State)
    val isEmpty: StateFlow<Boolean> = fridgeItems
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Діалог додавання продукту
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    // Поле вводу в діалозі
    private val _productInput = MutableStateFlow("")
    val productInput: StateFlow<String> = _productInput.asStateFlow()

    // Підказки (Autocomplete)
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    // Знайдені рецепти (після "Що приготувати?")
    private val _foundRecipes = MutableStateFlow<List<LocalRecipeEntity>>(emptyList())
    val foundRecipes: StateFlow<List<LocalRecipeEntity>> = _foundRecipes.asStateFlow()

    // Чи відкрито екран результатів пошуку
    private val _showSearchResults = MutableStateFlow(false)
    val showSearchResults: StateFlow<Boolean> = _showSearchResults.asStateFlow()

    // Чи йде пошук
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Повідомлення
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // ==========================================
    // ACTIONS - Керування продуктами
    // ==========================================

    // Відкрити діалог додавання
    fun openAddDialog() {
        _showAddDialog.value = true
    }

    // Закрити діалог додавання
    fun closeAddDialog() {
        _showAddDialog.value = false
        _productInput.value = ""
        _suggestions.value = emptyList()
    }

    // Оновити поле вводу (з автопідказками)
    fun onProductInputChanged(value: String) {
        _productInput.value = value

        if (value.isBlank()) {
            _suggestions.value = emptyList()
            return
        }

        // Показуємо підказки
        viewModelScope.launch {
            val results = fridgeRepository.searchIngredients(value)
            _suggestions.value = results
        }
    }

    // Додати продукт (з діалогу або з підказки)
    fun addProduct(name: String = _productInput.value) {
        viewModelScope.launch {
            val result = fridgeRepository.addProduct(name)

            result.onSuccess {
                _message.value = "$name додано"
                closeAddDialog()
            }.onFailure { error ->
                _message.value = error.message
            }
        }
    }

    // Видалити продукт (свайп або іконка)
    fun deleteProduct(item: FridgeItemEntity) {
        viewModelScope.launch {
            val result = fridgeRepository.deleteProduct(item)

            result.onSuccess {
                _message.value = "🗑️ ${item.name} видалено"
            }.onFailure { error ->
                _message.value = error.message
            }
        }
    }

    // Очистити весь холодильник
    fun clearFridge() {
        viewModelScope.launch {
            val result = fridgeRepository.clearFridge()

            result.onSuccess {
                _message.value = "Холодильник очищено"
            }.onFailure { error ->
                _message.value = error.message
            }
        }
    }

    // ==========================================
    // ACTIONS - Пошук рецептів
    // ==========================================

    // Кнопка "Що приготувати?" - шукає рецепти за всіма продуктами
    fun searchRecipesByFridge() {
        val items = fridgeItems.value

        if (items.isEmpty()) {
            _message.value = "Додайте продукти в холодильник"
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _showSearchResults.value = true

            val ingredients = items.map { it.name }

            // Шукаємо локально + віддалено
            recipeRepository.searchByIngredient(ingredients).collect { recipes ->
                _foundRecipes.value = recipes
            }

            _isSearching.value = false
        }
    }

    // Закрити екран результатів пошуку
    fun closeSearchResults() {
        _showSearchResults.value = false
        _foundRecipes.value = emptyList()
    }

    // Очистити повідомлення
    fun clearMessage() {
        _message.value = null
    }
}