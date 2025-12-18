package com.example.eatopedia.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.eatopedia.data.local.AppDatabase
import com.example.eatopedia.data.local.FridgeItemDao
import com.example.eatopedia.data.local.LocalRecipeDao
import com.example.eatopedia.data.local.PreloadedRecipes
import com.example.eatopedia.data.repository.AuthRepository
import com.example.eatopedia.data.repository.FridgeRepository
import com.example.eatopedia.data.repository.RecipeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        //  provider, щоб уникнути циклічної залежності при виклику DAO в Callback
        recipeDaoProvider: Provider<LocalRecipeDao>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "eatopedia_db"
        )

            .addCallback(object : RoomDatabase.Callback() {
                // Спрацьовує тільки при створенні файлу бази (після видалення додатка)
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val dao = recipeDaoProvider.get()
                            val recipes = PreloadedRecipes

                            Log.d("DB_FILL", "Починаємо додавати ${recipes.defaultRecipes.size} рецептів через цикл...")


                            recipes.defaultRecipes.forEach { recipe ->
                                dao.insertRecipe(recipe)
                            }

                            Log.d("DB_FILL", "Всі рецепти успішно додано!")
                        } catch (e: Exception) {
                            Log.e("DB_FILL", "Помилка при додаванні: ${e.message}")
                        }
                    }
                }
            })
            .build()
    }

    @Provides
    fun provideRecipeDao(db: AppDatabase): LocalRecipeDao {
        return db.recipeDao()
    }

    @Provides
    fun provideFridgeDao(db: AppDatabase): FridgeItemDao {
        return db.fridgeDao()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository {
        return AuthRepository()
    }

    @Provides
    @Singleton
    fun provideRecipeRepository(recipeDao: LocalRecipeDao): RecipeRepository {
        return RecipeRepository(recipeDao)
    }

    @Provides
    @Singleton
    fun provideFridgeRepository(fridgeDao: FridgeItemDao): FridgeRepository {
        return FridgeRepository(fridgeDao)
    }
}