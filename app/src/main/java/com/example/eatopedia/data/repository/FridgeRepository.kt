package com.example.eatopedia.data.repository

import android.util.Log
import com.example.eatopedia.data.IngredientDictionary
import com.example.eatopedia.data.local.FridgeItemDao
import com.example.eatopedia.data.remote.IngridientDto
import com.example.eatopedia.data.local.FridgeItemEntity
import com.example.eatopedia.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FridgeRepository(private val fridgeDao: FridgeItemDao
) {
    private val supabase = SupabaseClient.client
    private val TAG = "FridgeRepository"

    //Отримати всі продукти з холодильника
    fun getMyFridgeItems(): Flow<List<FridgeItemEntity>> {
        return fridgeDao.getAllItems()
    }

    //Додати продукт у холодильник
    suspend fun addProduct(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (name.isBlank()) {
                return@withContext Result.failure(Exception("Назва не може бути пустою"))
            }

            val newItem = FridgeItemEntity(name = name.trim())
            fridgeDao.addItem(newItem)
            Log.d(TAG, "Product added: $name")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add product", e)
            Result.failure(e)
        }
    }

    //Видалити продукт з холодильника
    suspend fun deleteProduct(item: FridgeItemEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            fridgeDao.deleteItem(item)
            Log.d(TAG, "Product deleted: ${item.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete product", e)
            Result.failure(e)
        }
    }

    //Очистити весь холодильник
    suspend fun clearFridge(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            fridgeDao.deleteAll()
            Log.d(TAG, "Fridge cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear fridge", e)
            Result.failure(e)
        }
    }


    // Пошук інгрідієнтів(для підказок при вводі)
    suspend fun searchIngredients(query: String): List<String> = withContext(Dispatchers.Default) {
        if (query.isBlank()) return@withContext emptyList()
        IngredientDictionary.Ingredients.filter { it.contains(query, ignoreCase = true) }.take(5).sorted()
    }

    //Пошук інгредієнтів у Supabase (з повною інформацією про КБЖУ)
    suspend fun searchIngredientsFromSupabase(query: String): Result<List<IngridientDto>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val ingredients = supabase.from("ingredients")
                .select {
                    filter {
                        ilike("name", "%$query%")
                    }
                    limit(10)
                }.decodeList<IngridientDto>()

            Log.d(TAG, "Found ${ingredients.size} ingredients for query: $query")
            Result.success(ingredients)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search ingredients in Supabase", e)
            Result.failure(e)
        }
    }

    //Отримати інгредієнт за ID з Supabase
    suspend fun getIngredientById(ingredientId: String): Result<IngridientDto> = withContext(Dispatchers.IO) {
        try {
            val ingredient = supabase.from("ingredients")
                .select {
                    filter { eq("id", ingredientId) }
                }.decodeSingle<IngridientDto>()

            Result.success(ingredient)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ingredient by ID", e)
            Result.failure(e)
        }
    }


}
