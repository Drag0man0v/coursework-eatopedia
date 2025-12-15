package com.example.eatopedia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eatopedia.data.local.LocalRecipeEntity
import com.example.eatopedia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ViewModel для головного екрану (стрічка рецептів)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    // ==========================================
    // STATE
    // ==========================================

    // Список всіх рецептів
    val recipes: StateFlow<List<LocalRecipeEntity>> = recipeRepository
        .getAllRecipes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Пошуковий запит
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Відфільтровані рецепти
    val filteredRecipes: StateFlow<List<LocalRecipeEntity>> = combine(
        recipes,
        _searchQuery
    ) { recipesList, query ->
        if (query.isBlank()) {
            recipesList
        } else {
            recipesList.filter { recipe ->
                recipe.title.contains(query, ignoreCase = true) ||
                        recipe.authorName?.contains(query, ignoreCase = true) == true
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Чи йде оновлення (pull-to-refresh)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Повідомлення
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // ==========================================
    // ACTIONS
    // ==========================================

    // Оновити пошуковий запит
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // Оновити рецепти з сервера (pull-to-refresh)
    fun refreshRecipes() {
        viewModelScope.launch {
            _isRefreshing.value = true

            val result = recipeRepository.syncRecipesFromSupabase()

            result.onSuccess {
                _message.value = "Рецепти оновлено"
            }.onFailure { error ->
                _message.value = "Помилка: ${error.message}"
            }

            _isRefreshing.value = false
        }
    }

    // Додати/прибрати з улюблених
    fun toggleFavorite(recipeId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            recipeRepository.toggleFavorite(recipeId, isFavorite)
        }
    }

    // Очистити повідомлення
    fun clearMessage() {
        _message.value = null
    }
}