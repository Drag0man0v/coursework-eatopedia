package com.example.eatopedia.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.eatopedia.ui.viewmodel.AuthViewModel
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import com.example.eatopedia.R

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit, // Що робити, коли вхід успішний (навігація)
    viewModel: AuthViewModel = hiltViewModel() // Hilt сам знайде ViewModel
) {
    // Підписуємось на стани з ViewModel
    val isSignUpMode by viewModel.isSignUpMode.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val username by viewModel.username.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    val context = LocalContext.current

    // Слідкуємо за подіями (Успішний вхід або Помилка)
    LaunchedEffect(key1 = isAuthenticated, key2 = message) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
        if (isAuthenticated) {
            onLoginSuccess() // Переходимо на Головний екран
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignUpMode) "Створити акаунт" else "З поверненням!",
            color = colorResource(id = R.color.eatopedia_dark),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Поле Username (тільки для реєстрації)
        if (isSignUpMode) {
            OutlinedTextField(
                value = username,
                onValueChange = { viewModel.onUsernameChanged(it) },
                label = { Text("Ваше ім'я") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colorResource(id = R.color.eatopedia_dark), // Рамка
                focusedLabelColor = colorResource(id = R.color.eatopedia_dark),  // Текст "Ваше ім'я"
                cursorColor = colorResource(id = R.color.eatopedia_dark)         // Мигаюча паличка
            )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Поле Email
        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.onEmailChanged(it) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colorResource(id = R.color.eatopedia_dark), // Рамка
            focusedLabelColor = colorResource(id = R.color.eatopedia_dark),  // Текст "Ваше ім'я"
            cursorColor = colorResource(id = R.color.eatopedia_dark)   )      // Мигаюча паличка
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Поле Password
        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.onPasswordChanged(it) },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colorResource(id = R.color.eatopedia_dark), // Рамка
                focusedLabelColor = colorResource(id = R.color.eatopedia_dark),  // Текст "Ваше ім'я"
                cursorColor = colorResource(id = R.color.eatopedia_dark)  )       // Мигаюча паличка
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка Входу
        Button(
            onClick = { viewModel.onSubmit() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.eatopedia_dark),
            contentColor = androidx.compose.ui.graphics.Color.White)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = colorResource(id = R.color.eatopedia_dark),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = if (isSignUpMode) "Зареєструватися" else "Увійти",
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Перемикач режиму
        TextButton(onClick = { viewModel.toggleMode() }) {
            Text(
                text = if (isSignUpMode) "Вже є акаунт? Увійти" else "Немає акаунту? Зареєструватися",
                color = colorResource(id = R.color.eatopedia_dark)
            )
        }
    }
}