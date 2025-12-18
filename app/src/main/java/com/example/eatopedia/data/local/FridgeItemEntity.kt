package com.example.eatopedia.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fridge_items") data class FridgeItemEntity(
@PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String

)