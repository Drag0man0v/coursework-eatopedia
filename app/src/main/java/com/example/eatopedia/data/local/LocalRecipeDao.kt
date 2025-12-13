package com.example.eatopedia.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow //імпортуємо потік

@Dao
interface LocalRecipeDao
{
@Query("Select * from local_recipes")
fun getAllRecieps(): Flow<List<LocalRecipeEntity>> //todo потім можна буде затестити і змінити на suspend

@Query("Select * from local_recipes where isFavorite = 1")
fun getFavoiriteRecipes(): Flow<List<LocalRecipeEntity>> //не нулбл бо може бути пустим саисокм тому ? не треба

@Query("Select * from local_recipes where isPreloaded = 1")
fun getPreloadedRecipes(): Flow<List<LocalRecipeEntity>>

@Query("Select * from local_recipes where id = :recipeId")//:paramname - підставляє значення з функції
fun getRecipeById(recipeId: String): LocalRecipeEntity?

@Query("Select * from local_recipes where ingredientsText like '%' || :ingredient || '%'")
fun getRecipeByIngredient(ingredient: String): Flow<List<LocalRecipeEntity>>

@Query("Update local_recipes set isFavorite =:isFavorite where id = :id")
suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

@Insert(onConflict = OnConflictStrategy.REPLACE)//onConflict = OnConflictStrategy.REPLACE - якшо id співпадає то зробить replace
suspend fun insertRecipe(recipe: LocalRecipeEntity)

@Delete
suspend fun deleteRecipe(recipe: LocalRecipeEntity)

}