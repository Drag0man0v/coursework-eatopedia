package com.example.eatopedia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eatopedia.data.repository.AuthRepository
import com.example.eatopedia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel // Hilt
import javax.inject.Inject // Hilt
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ViewModel для екрану авторизації/реєстрації
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    // ==========================================
    // STATE для UI
    // ==========================================

    // Режим: true = Реєстрація, false = Вхід
    private val _isSignUpMode = MutableStateFlow(true)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    // Поля введення
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    // Чи йде завантаження (показуємо спінер на кнопці)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Повідомлення/помилка для SnackBar
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Чи користувач успішно залогінився (навігація на головний екран)
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        // Перевіряємо чи користувач вже залогінений
        if (authRepository.isUserLoggedIn()) {
            _isAuthenticated.value = true
        }
    }

    // ==========================================
    // ACTIONS
    // ==========================================

    // Перемикач між Вхід/Реєстрація
    fun toggleMode() {
        _isSignUpMode.value = !_isSignUpMode.value
        clearFields()
    }

    // Оновлення полів вводу
    fun onEmailChanged(value: String) {
        _email.value = value
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
    }

    fun onUsernameChanged(value: String) {
        _username.value = value
    }

    // Кнопка "Поїхали!" - виконує вхід або реєстрацію
    fun onSubmit() {
        if (_isSignUpMode.value) {
            signUp()
        } else {
            signIn()
        }
    }

    // Реєстрація
    private fun signUp() {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null

            val result = authRepository.signUp(
                email = _email.value,
                password = _password.value,
                username = _username.value
            )

            result.onSuccess {
                _message.value = "Реєстрація успішна!"

                // Завантажуємо дефолтні рецепти для нового користувача
                recipeRepository.loadDefaultRecipes()

                _isAuthenticated.value = true
            }.onFailure { error ->
                _message.value = error.message ?: "Помилка реєстрації"
            }

            _isLoading.value = false
        }
    }

    // Вхід
    private fun signIn() {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null

            val result = authRepository.signIn(
                email = _email.value,
                password = _password.value
            )

            result.onSuccess {
                _message.value = "Вхід успішний!"
                _isAuthenticated.value = true
            }.onFailure { error ->
                _message.value = error.message ?: "Невірний email або пароль"
            }

            _isLoading.value = false
        }
    }

    // Очистити поля
    private fun clearFields() {
        _email.value = ""
        _password.value = ""
        _username.value = ""
        _message.value = null
    }

    // Очистити повідомлення
    fun clearMessage() {
        _message.value = null
    }
}