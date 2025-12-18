package com.example.eatopedia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eatopedia.data.repository.AuthRepository
import com.example.eatopedia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    // Стан UI
    private val _isSignUpMode = MutableStateFlow(true)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        if (authRepository.isUserLoggedIn()) {
            _isAuthenticated.value = true
        }
    }

    // Дії користувача
    fun toggleMode() {
        _isSignUpMode.value = !_isSignUpMode.value
        clearFields()
    }

    fun onEmailChanged(value: String) {
        _email.value = value
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
    }

    fun onUsernameChanged(value: String) {
        _username.value = value
    }

    fun onSubmit() {
        if (_isSignUpMode.value) {
            signUp()
        } else {
            signIn()
        }
    }

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
                _message.value = "Лист надіслано. Підтвердіть пошту перед входом."
                _isSignUpMode.value = false
            }.onFailure { error ->
                _message.value = error.message ?: "Помилка реєстрації"
            }
            _isLoading.value = false
        }
    }

    private fun signIn() {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null

            val result = authRepository.signIn(
                email = _email.value,
                password = _password.value
            )

            result.onSuccess {
                _message.value = "Вхід успішний"
                _isAuthenticated.value = true
            }.onFailure { error ->
                _message.value = error.message ?: "Невірний email або пароль"
            }

            _isLoading.value = false
        }
    }

    private fun clearFields() {
        _email.value = ""
        _password.value = ""
        _username.value = ""
        _message.value = null
    }

    fun clearMessage() {
        _message.value = null
    }
}