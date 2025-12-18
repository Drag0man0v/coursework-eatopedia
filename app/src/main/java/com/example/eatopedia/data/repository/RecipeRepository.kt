package com.example.eatopedia.data.repository

import android.util.Log

import com.example.eatopedia.data.local.LocalRecipeDao
import com.example.eatopedia.data.local.LocalRecipeEntity
import com.example.eatopedia.data.local.PreloadedRecipes
import com.example.eatopedia.data.mapper.toEntity
import com.example.eatopedia.data.mapper.toDto
import com.example.eatopedia.data.remote.RecipeDto
import com.example.eatopedia.data.remote.RecipeIngredientDto
import com.example.eatopedia.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns//шоб отримувати певні колонки
import io.github.jan.supabase.postgrest.query.filter.TextSearchType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow//асинхронний потік даних
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow//створення власного потоку
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class RecipeRepository( private val recipeDao: LocalRecipeDao) {
    private val supabase = SupabaseClient.client
    private val TAG = "RecipeRepository"

    //Отримати всі рецепти (спочатку локальні, потім синхронізація)
    fun getAllRecipes(): Flow<List<LocalRecipeEntity>> {
        // Запускаємо синхронізацію один раз у фоні
        CoroutineScope(Dispatchers.IO).launch {
            syncRecipesFromSupabase()
        }

        // Просто повертаємо Flow з Room
        return recipeDao.getAllRecieps()
    }

    //Отримати улюблені рецепти
    fun getFavoriteRecipes(): Flow<List<LocalRecipeEntity>> {
        return recipeDao.getFavoiriteRecipes()
    }

    //Отримати попередньо завантажені рецепти
    fun getPreloadedRecipes(): Flow<List<LocalRecipeEntity>> {
        return recipeDao.getPreloadedRecipes()
    }

    //Отримати рецепт за ID
    suspend fun getRecipeById(recipeId: String): Result<LocalRecipeEntity> =
        withContext(Dispatchers.IO) {
            try {
                // Спочатку перевіряємо локально
                val localRecipe = recipeDao.getRecipeById(recipeId)

                if (localRecipe != null) {
                    // Якщо знайшли локально - оновлюємо з Supabase у фоні
                    try {
                        val remoteRecipe = supabase.from("recipes")
                            .select(columns = Columns.list("*")) {
                                filter { eq("id", recipeId) }
                            }.decodeSingle<RecipeDto>()

                        // Використовуємо mapper
                        val updatedEntity = remoteRecipe.toEntity(
                            isFavorite = localRecipe.isFavorite,
                            isPreloaded = localRecipe.isPreloaded
                        )
                        recipeDao.insertRecipe(updatedEntity)

                        Result.success(updatedEntity)
                    } catch (e: Exception) {
                        Log.e(TAG, "Remote fetch failed, returning local", e)
                        Result.success(localRecipe)
                    }
                } else {
                    // Якщо немає локально - завантажуємо з Supabase
                    val remoteRecipe = supabase.from("recipes")
                        .select(columns = Columns.list("*")) {
                            filter { eq("id", recipeId) }
                        }.decodeSingle<RecipeDto>()

                    val entity = remoteRecipe.toEntity()
                    recipeDao.insertRecipe(entity)

                    Result.success(entity)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting recipe by ID: $recipeId", e)
                Result.failure(e)
            }
        }


    @Serializable
    data class IngredientNameDto(val id: String, val name: String)
    fun searchRecipesByFridge(userIngredients: List<String>): Flow<List<LocalRecipeEntity>> {
        // Якщо інгредієнтів немає - повертаємо пустий список
        if (userIngredients.isEmpty()) return flow { emit(emptyList()) }

        return recipeDao.getAllRecieps().map { allRecipes ->
            // Фільтруємо локальні рецепти (перевіряємо чи є інгредієнт в тексті)
            allRecipes.filter { recipe ->
                val text = recipe.ingredientsText?.lowercase() ?: ""
                userIngredients.any { ing -> text.contains(ing.trim().lowercase()) }
            }
                // Сортуємо: рецепти з найбільшою кількістю збігів зверху
                .sortedByDescending { recipe ->
                    val text = recipe.ingredientsText?.lowercase() ?: ""
                    userIngredients.count { text.contains(it.trim().lowercase()) }
                }
        }.onStart {
            // 2. ЦЕЙ БЛОК ЗАПУСКАЄТЬСЯ ПАРАЛЕЛЬНО (Background Sync)
            // Ми не чекаємо його виконання, щоб показати дані.
            // Якщо інтернет є і бази не пусті - він додасть рецепти в Room, і Flow (пункт 1) спрацює знову.
            CoroutineScope(Dispatchers.IO).launch {
                findRecipesRelational(userIngredients)
            }
        }
    }

    private suspend fun findRecipesRelational(ingredientNames: List<String>) {
        if (ingredientNames.isEmpty()) return

        try {
            Log.d(TAG, " Шукаємо ID для інгредієнтів: $ingredientNames")

            // Знаходимо ID інгредієнтів (шукаємо нестрого через ilike)
            val foundIngredientIds = mutableListOf<String>()

            // Supabase SDK не вміє робити "ilike any", тому проходимось циклом
            // Це нормально, бо інгредієнтів зазвичай мало (3-5 штук)
            ingredientNames.forEach { name ->
                val dtos = supabase.from("ingredients")
                    .select(columns = Columns.list("id, name")) {
                        // ilike - ігнорує регістр (знайде "Молоко", "молоко", "МОЛОКО")
                        filter { ilike("name", "%${name.trim()}%") }
                    }.decodeList<IngredientNameDto>()

                foundIngredientIds.addAll(dtos.map { it.id })
            }

            if (foundIngredientIds.isEmpty()) {
                Log.d(TAG, "Інгредієнти в базі не знайдені. Перевір таблицю 'ingredients'.")
                return
            }

            Log.d(TAG, "Знайдено ID інгредієнтів: $foundIngredientIds")


            val links = supabase.from("recipe_ingredients")
                .select(columns = Columns.list("recipe_id, ingredient_id")) {
                    filter { isIn("ingredient_id", foundIngredientIds) }
                }.decodeList<RecipeIngredientDto>()

            val recipeIds = links.map { it.recipeId }.distinct()

            if (recipeIds.isEmpty()) {
                Log.d(TAG, "Рецептів з такими інгредієнтами немає.")
                return
            }

            Log.d(TAG, "Знайдено ID рецептів: $recipeIds")


            // Завантажуємо самі рецепти
            val recipes = supabase.from("recipes")
                .select(columns = Columns.list("*")) {
                    filter { isIn("id", recipeIds) }
                }.decodeList<RecipeDto>()

            Log.d(TAG, "Завантажено рецептів: ${recipes.size}")

            // Зберігаємо в локальну БД
            recipes.forEach { dto ->
                val existing = recipeDao.getRecipeById(dto.id)
                // якщо немає, або оновлюємо
                val entity = dto.toEntity(
                    isFavorite = existing?.isFavorite ?: false,
                    isPreloaded = existing?.isPreloaded ?: false
                )
                recipeDao.insertRecipe(entity)
            }

        } catch (e: Exception) {
            Log.e(TAG, "помилка реляційного пошуку", e)
        }
    }


    @Serializable
    data class IngredientIdDto(val id: String)

    @Serializable
    data class RecipeIdDto(val recipe_id: String)
    suspend fun findRecipesByIngredientsRemote(ingredients: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (ingredients.isEmpty()) return@withContext Result.success(Unit)

            Log.d(TAG, "Searching for ingredients: $ingredients")


            // Перетворюємо назви інгредієнтів у їхні ID
            val ingredientIdsDto = supabase.from("ingredients")
                .select(columns = Columns.list("id")) {
                    // Шукаємо інгредієнти, назви яких є у нашому списку
                    filter { isIn("name", ingredients) }
                }.decodeList<IngredientIdDto>()

            val ingredientIds = ingredientIdsDto.map { it.id }

            if (ingredientIds.isEmpty()) {
                Log.d(TAG, "Ingredients not found in DB")
                return@withContext Result.success(Unit)
            }


            // Шукаємо ID рецептів, використовуючи ID інгредієнтів
            val rawIds = supabase.from("recipe_ingredients")
                .select(columns = Columns.list("recipe_id")) {
                    filter { isIn("ingredient_id", ingredientIds) }
                }.decodeList<RecipeIdDto>()


            //  Прибираємо дублікати
            val uniqueRecipeIds = rawIds.map { it.recipe_id }.distinct()

            if (uniqueRecipeIds.isEmpty()) {
                Log.d(TAG, "No recipes found for these ingredients")
                return@withContext Result.success(Unit)
            }


            // КРОК 3: Завантажуємо самі рецепти
            val recipes = supabase.from("recipes")
                .select(columns = Columns.list("*")) {
                    filter {
                        isIn("id", uniqueRecipeIds)
                    }
                }.decodeList<RecipeDto>()

            recipes.forEach { dto ->
                val existing = recipeDao.getRecipeById(dto.id)
                val entity = dto.toEntity(
                    isFavorite = existing?.isFavorite ?: false,
                    isPreloaded = existing?.isPreloaded ?: false
                )
                recipeDao.insertRecipe(entity)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            Result.failure(e)
        }
    }


    /** todo зробити ось цю функцію
     * Знайти рецепти, які можна приготувати з переліку інгредієнтів.
     * Приймає список назв продуктів (наприклад, ["Яйця", "Молоко", "Борошно"])
     */




    //Синхронізувати рецепти з Supabase (тільки ті, що не локальні)
    suspend fun syncRecipesFromSupabase(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync from Supabase...")

            // 1. Отримуємо список ID всіх локальних рецептів
            val localRecipes = recipeDao.getAllRecieps().first()

            // 2. Фільтруємо - беремо тільки ті, що не ачтоматично я додав (id не починається з "local_")
            val supabaseRecipes = localRecipes.filter { !it.id.startsWith("local_") }

            if (supabaseRecipes.isEmpty()) {
                Log.d(TAG, "No Supabase recipes to sync")
                return@withContext Result.success(Unit)
            }

            val supabaseIds = supabaseRecipes.map { it.id }
            Log.d(TAG, "Syncing ${supabaseIds.size} recipes from Supabase...")

            // 3. Завантажуємо з Supabase ТІЛЬКИ ті рецепти, які є з Supabase
            val recipes = supabase.from("recipes")
                .select(columns = Columns.list("*")) {
                    filter {
                        isIn("id", supabaseIds) // ← Фільтруємо по ID
                    }
                }.decodeList<RecipeDto>()

            Log.d(TAG, "Fetched ${recipes.size} recipes from Supabase")

            // 4. Оновлюємо локальні рецепти
            recipes.forEach { dto ->
                val existing = recipeDao.getRecipeById(dto.id)
                val entity = dto.toEntity(
                    isFavorite = existing?.isFavorite ?: false,
                    isPreloaded = existing?.isPreloaded ?: false
                )
                recipeDao.insertRecipe(entity)
            }

            Log.d(TAG, "Sync completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.failure(e)
        }
    }


    //Попереднє завантаження популярних рецептів (для першого запуску)
    //todo можливо і не буду юзати, ну бо будуть якісь дефолтні просто завантажені за допомогою loadDefaultRecipes()
    suspend fun preloadPopularRecipes(limit: Int = 20): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val recipes = supabase.from("recipes")
                .select(columns = Columns.list("*")) {
                    limit(limit.toLong())
                }.decodeList<RecipeDto>()

            recipes.forEach { dto ->
                val entity = dto.toEntity(isPreloaded = true)
                recipeDao.insertRecipe(entity)
            }

            Log.d(TAG, "Preloaded ${recipes.size} recipes")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Preload failed", e)
            Result.failure(e)
        }
    }

    //Завантажує дефолтні рецепти з файлу PreloadedRecipes у локальну базу.
    suspend fun loadDefaultRecipes(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Отримуємо список рецептів з нашого об'єкта
            val defaultRecipes = PreloadedRecipes.defaultRecipes

            Log.d(TAG, "Starting preload of ${defaultRecipes.size} recipes...")

            // Проходимось по списку і вставляємо в Room
            defaultRecipes.forEach { recipe -> recipeDao.insertRecipe(recipe) }

            Log.d(TAG, "Successfully preloaded ${defaultRecipes.size} recipes")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load default recipes", e)
            Result.failure(e)
        }
    }



    //Створити новий рецепт (спочатку локально, потім на Supabase)
    suspend fun createRecipe(recipe: LocalRecipeEntity): Result<LocalRecipeEntity> = withContext(Dispatchers.IO) {
        try {
            // Спочатку зберігаємо локально (для офлайн режиму)
            recipeDao.insertRecipe(recipe)
            Log.d(TAG, "Recipe saved locally: ${recipe.id}")

            // Намагаємось відправити на Supabase
            try {
                val dto = recipe.toDto()
                val inserted = supabase.from("recipes")
                    .insert(dto) {
                        select()
                    }.decodeSingle<RecipeDto>()

                // Оновлюємо локальну версію з серверним ID
                val updatedEntity = inserted.toEntity(
                    isFavorite = recipe.isFavorite,
                    isPreloaded = recipe.isPreloaded
                )
                recipeDao.insertRecipe(updatedEntity)

                Log.d(TAG, "Recipe synced to Supabase: ${recipe.id}")
                Result.success(updatedEntity)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync to Supabase, saved locally", e)
                Result.success(recipe) // Повертаємо локальну версію
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create recipe", e)
            Result.failure(e)
        }
    }

    //Оновити рецепт
    suspend fun updateRecipe(recipe: LocalRecipeEntity): Result<LocalRecipeEntity> = withContext(Dispatchers.IO) {
        try {
            // 1. Оновлюємо локально
            recipeDao.insertRecipe(recipe)

            // 2. Намагаємось оновити на Supabase
            try {
                val dto = recipe.toDto()
                supabase.from("recipes")
                    .update(dto) {
                        filter { eq("id", recipe.id) }
                    }

                Log.d(TAG, "Recipe ${recipe.id} synced to Supabase")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync update to Supabase", e)
            }

            Result.success(recipe)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update recipe", e)
            Result.failure(e)
        }
    }

    //Видалити рецепт
    suspend fun deleteRecipe(recipe: LocalRecipeEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Видаляємо локально
            recipeDao.deleteRecipe(recipe)
            Log.d(TAG, "Recipe deleted locally: ${recipe.id}")

            // 2. Видаляємо з Supabase
            try {
                supabase.from("recipes")
                    .delete {
                        filter { eq("id", recipe.id) }
                    }

                Log.d(TAG, "Recipe deleted from Supabase: ${recipe.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete from Supabase", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recipe", e)
            Result.failure(e)
        }
    }


    // УЛЮБЛЕНІ
    suspend fun toggleFavorite(recipeId: String, isFavorite: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            recipeDao.updateFavoriteStatus(recipeId, isFavorite)
            Log.d(TAG, "Recipe $recipeId favorite: $isFavorite")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update favorite status", e)
            Result.failure(e)
        }
    }


}
