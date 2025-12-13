package com.example.eatopedia.data.mapper

import androidx.compose.ui.Modifier
import com.example.eatopedia.data.local.LocalRecipeEntity
import com.example.eatopedia.data.remote.RecipeDto
import com.example.eatopedia.data.remote.IngridientDto
import com.example.eatopedia.data.remote.ProfileDto

//чисто екстеншин функції

//Конвертує RecipeDto (з Supabase) в LocalRecipeEntity (Room)
fun RecipeDto.toEntity(
    authorName: String? = null,
    ingredientsText: String? = null,
    isFavorite: Boolean = false,
    isPreloaded: Boolean = false) : LocalRecipeEntity
{
    //коефіціент для підрахунку бжу на 100 грам
    val coef = if (totalWeight > 0) totalWeight / 100.0 else 1.0

    return LocalRecipeEntity(
        id = this.id,
        title = this.title,
        description = this.description,
        imageUrl = this.imageUrl,
        authorId = this.authorId,
        authorName = authorName,
        ingredientsText = ingredientsText,
        calories = this.totalCalories / coef,
        proteins = this.totalProteins / coef,
        fats = this.totalFats / coef,
        carbs = this.totalCarbs / coef,
        isFavorite = isFavorite,
        isPreloaded = isPreloaded

    )

}

//Конвертує LocalRecipeEntity (Room) в RecipeDto (Supabase)
//todo потім можна буде добавити можливість нотаток і вже з нотаток відправляти рецепти з локальної на сервер, а не зразу на сервер
fun LocalRecipeEntity.toDto(): RecipeDto {
    return RecipeDto(
        id = this.id,
        title = this.title,
        description = this.description,
        imageUrl = this.imageUrl,
        createdAt = null, // сервер сам поставить дату
        authorId = this.authorId,
        totalCalories = this.calories,
        totalProteins = this.proteins,
        totalFats = this.fats,
        totalCarbs = this.carbs,
        totalWeight = this.weight // ← тут також ставимо this
    )
}



