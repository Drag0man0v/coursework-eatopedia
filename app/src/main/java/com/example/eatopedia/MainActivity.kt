package com.example.eatopedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.eatopedia.ui.navigation.Screen
import com.example.eatopedia.ui.screens.AuthScreen
import com.example.eatopedia.ui.screens.HomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route // Стартуємо з екрану входу
    ) {

        // 1. Екран авторизації
        composable(route = Screen.Auth.route) {
            AuthScreen(
                onLoginSuccess = {
                    // Коли вхід успішний - йдемо на Home і видаляємо Auth з історії (щоб "Назад" не повертало на логін)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Головний екран (поки що заглушка)
        composable(Screen.Home.route) {
            HomeScreen(
                onRecipeClick = { recipeId ->
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                }
            )
        }
    }
}