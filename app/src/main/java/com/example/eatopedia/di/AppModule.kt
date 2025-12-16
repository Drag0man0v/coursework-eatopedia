package com.example.eatopedia.di

import android.content.Context
import com.example.eatopedia.data.local.AppDatabase
import com.example.eatopedia.data.local.FridgeItemDao
import com.example.eatopedia.data.local.LocalRecipeDao
import com.example.eatopedia.data.repository.AuthRepository
import com.example.eatopedia.data.repository.FridgeRepository
import com.example.eatopedia.data.repository.RecipeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Живе протягом всього додатку
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
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