package com.example.eatopedia.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow 

@Dao
interface FridgeItemDao {

    @Query("Select * from fridge_items")
    fun getAllItems(): Flow<List<FridgeItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItem(item: FridgeItemEntity)

    @Delete
    suspend fun deleteItem(item: FridgeItemEntity)

    @Query("DELETE FROM fridge_items")
    suspend fun deleteAll()
}