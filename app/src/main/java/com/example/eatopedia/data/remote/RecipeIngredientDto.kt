package com.example.eatopedia.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecipeIngredientDto(
    val id: String,

    @SerialName("recipe_id")
    val recipeId: String,

    @SerialName("ingredient_id")
    val ingredientId: String,

    @SerialName("amount_grams")
    val amountGrams: Double


)