package com.example.eatopedia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eatopedia.data.local.LocalRecipeEntity
import com.example.eatopedia.data.remote.ProfileDto
import com.example.eatopedia.data.repository.AuthRepository
import com.example.eatopedia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ViewModel для екрану профілю
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    // ==========================================
    // STATE
    // ==========================================

    // Поточний користувач
    private val _currentUser = MutableStateFlow<ProfileDto?>(null)
    val currentUser: StateFlow<ProfileDto?> = _currentUser.asStateFlow()

    // Улюблені рецепти
    val favoriteRecipes: StateFlow<List<LocalRecipeEntity>> = recipeRepository
        .getFavoriteRecipes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Чи відкрито екран додавання рецепта
    private val _showAddRecipeScreen = MutableStateFlow(false)
    val showAddRecipeScreen: StateFlow<Boolean> = _showAddRecipeScreen.asStateFlow()

    // Тема (темна/світла)
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Повідомлення
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadCurrentUser()
    }

    // ==========================================
    // ACTIONS
    // ==========================================

    // Завантажити поточного користувача
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val result = authRepository.getCurrentUser()
            result.onSuccess { profile ->
                _currentUser.value = profile
            }
        }
    }

    // Відкрити екран додавання рецепта
    fun openAddRecipeScreen() {
        _showAddRecipeScreen.value = true
    }

    // Закрити екран додавання рецепта
    fun closeAddRecipeScreen() {
        _showAddRecipeScreen.value = false
    }

    // Перемикач теми
    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
        // TODO: Зберегти налаштування в DataStore
    }

    // Вийти з акаунту
    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signOut()

            result.onSuccess {
                _message.value = "Ви вийшли з акаунту"
                onSignedOut()
            }.onFailure { error ->
                _message.value = error.message
            }
        }
    }

    // Очистити повідомлення
    fun clearMessage() {
        _message.value = null
    }
}