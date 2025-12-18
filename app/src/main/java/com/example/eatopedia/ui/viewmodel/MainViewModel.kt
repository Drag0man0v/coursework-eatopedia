package com.example.eatopedia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eatopedia.data.repository.AuthRepository
import com.example.eatopedia.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(Screen.Auth.route)
    val startDestination = _startDestination.asStateFlow()

    // Стан завантаження: TRUE - поки перевіряємо, FALSE - коли все готово
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val isUserLoggedIn = repository.isUserLoggedIn()

            if (isUserLoggedIn) {
                _startDestination.value = Screen.Home.route
            } else {
                _startDestination.value = Screen.Auth.route
            }

            _isLoading.value = false
        }
    }
}