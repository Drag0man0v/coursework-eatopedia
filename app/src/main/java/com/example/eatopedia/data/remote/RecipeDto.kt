package com.example.eatopedia.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class RecipeDto (
    val id: String,
    val title: String,
    val description: String?,

    //у JSON це поле називається "image_url", навіть якщо в класі воно imageUrl.
    @SerialName("image_url")
    val imageUrl: String?,

    @SerialName("created_at")
    val createdAt: String?,

    @SerialName("author_id")
    val authorId: String?,//робимо нулбл, шоб якшо користувач видалив акаунт, рецепти лишились
    @SerialName("total_calories") val totalCalories: Double = 0.0,
    @SerialName("total_proteins") val totalProteins: Double = 0.0,
    @SerialName("total_fats")     val totalFats: Double = 0.0,
    @SerialName("total_carbs")    val totalCarbs: Double = 0.0,
    @SerialName("total_weight")   val totalWeight: Double = 0.0

    )

