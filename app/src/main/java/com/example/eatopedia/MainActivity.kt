package com.example.eatopedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eatopedia.ui.screens.AuthScreen
import com.example.eatopedia.ui.screens.FridgeScreen
import com.example.eatopedia.ui.screens.HomeScreen
import com.example.eatopedia.ui.screens.ProfileScreen
import com.example.eatopedia.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.example.eatopedia.ui.navigation.Screen


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            mainViewModel.isLoading.value
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startDestination by mainViewModel.startDestination.collectAsState()
                    val isLoading by mainViewModel.isLoading.collectAsState()

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else if (startDestination != null) {
                        AppNavigation(startDestination = startDestination!!)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(startDestination: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Fridge.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                EatopediaBottomBar(navController = navController)
            }
        }
    ) { innerPadding ->

        val originalBottomPadding = innerPadding.calculateBottomPadding()
        val reducedBottomPadding = if (originalBottomPadding > 20.dp) {
            originalBottomPadding - 20.dp
        } else {
            0.dp
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = reducedBottomPadding
                )
        ) {
            composable(route = Screen.Auth.route) {
                AuthScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onRecipeClick = { recipeId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                    }
                )
            }
            composable(Screen.Fridge.route) {
                FridgeScreen(onRecipeClick = { })
            }
            composable(Screen.Profile.route) {
                ProfileScreen(navigateToSettings = { })
            }
        }
    }
}


@Composable
fun EatopediaBottomBar(navController: NavController) {
    val items = listOf(
        Screen.Home to Icons.Default.Home,
        Screen.Fridge to Icons.Default.Kitchen,
        Screen.Profile to Icons.Default.Person
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { (screen, icon) ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                },
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2E7D32),
                    indicatorColor = Color(0xFFE8F5E9),
                    unselectedIconColor = Color.Gray
                )
            )
        }
    }
}