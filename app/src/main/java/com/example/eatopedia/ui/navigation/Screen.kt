package com.example.eatopedia.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth_screen")
    object Home : Screen("home_screen")
    object Fridge : Screen("fridge_screen")
    object Profile : Screen("profile_screen")
    object AddRecipe : Screen("add_recipe_screen")
    // Екран деталей приймає аргумента ID рецепта
    object RecipeDetail : Screen("recipe_detail_screen/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe_detail_screen/$recipeId"
    }
}