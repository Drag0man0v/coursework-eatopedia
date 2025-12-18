package com.example.eatopedia.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun EatopediaBottomBar(navController: NavController) {
    // Список кнопок: (Маршрут, Іконка)
    val items = listOf(
        "home" to Icons.Default.Home,
        "fridge" to Icons.Default.Kitchen,
        "profile" to Icons.Default.Person
    )

    NavigationBar(
        containerColor = Color(0x30FFFFFF),
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { (route, icon) ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.size(28.dp)
                    )
                },
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2E7D32), // Зелений, коли вибрано
                    indicatorColor = Color(0x10E8F5E9),    // Світлий фон під іконкою
                    unselectedIconColor = Color.Gray       // Сірий, коли не вибрано
                )
            )
        }
    }
}