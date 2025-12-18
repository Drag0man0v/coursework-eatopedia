package com.example.eatopedia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LocalRecipeEntity::class, FridgeItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase()
{
    // Room (за допомогою KSP) сам напише код, який поверне готові DAO.
    abstract fun recipeDao(): LocalRecipeDao
    abstract fun fridgeDao(): FridgeItemDao

    //все шо всередині цього буде належати класу а не обєкту(аналог static)
    companion object {

        // @Volatile означає: "Якщо один потік створив базу, всі інші потоки відразу це побачать(видимість змін змінних у різних потоках)(забороняємо кешування змінної)".
        @Volatile
        private var INSTANCE: AppDatabase? = null//Це змінна, яка зберігає посилання на вже відкриту базу даних

        //(Static Factory Method).
        fun getDatabase(context: Context): AppDatabase {
            // 1. Перевіряємо: чи база вже створена?
            // Якщо INSTANCE не null (база є) -> просто повертаємо її.
            return INSTANCE ?: synchronized(this) {
                // 2. Якщо бази немає -> блокуємо потік (synchronized),
                // щоб два потоки не почали створювати базу одночасно.

                //створення об'єкта, записуємо його в статичну змінну INSTANCE і повертаємо.
                val instance = Room.databaseBuilder(
                    context.applicationContext, // 1.Доступ до файлової системи телефону
                    AppDatabase::class.java,    // 2. Клас бази(генерує запити)
                    "eatopedia_db"
                ).fallbackToDestructiveMigration().build()

                INSTANCE = instance
                instance
            }
        }
    }

}

