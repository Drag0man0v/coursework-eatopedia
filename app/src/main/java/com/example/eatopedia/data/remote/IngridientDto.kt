package com.example.eatopedia.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class IngridientDto(
    val id: String,
    val name: String,

    val calories: Double = 0.0,
    val proteins: Double = 0.0,
    val fats: Double = 0.0,
    val carbs: Double = 0.0
)