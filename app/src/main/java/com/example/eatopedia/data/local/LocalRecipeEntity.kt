package com.example.eatopedia.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_recipes") data class LocalRecipeEntity(

    @PrimaryKey(autoGenerate = false)
    val id: String,

    val title: String,
    val description: String? = null,
    val imageUrl: String? = null,

    val authorId: String? = null,
    val authorName: String? = null,
    val ingredients: String? = null,

    //на 100 грам
    val calories: Double = 0.0,
    val proteins: Double = 0.0,
    val fats: Double = 0.0,
    val carbs: Double = 0.0,

    val isFavorite: Boolean = false,
    val isPreloaded: Boolean = false

)
