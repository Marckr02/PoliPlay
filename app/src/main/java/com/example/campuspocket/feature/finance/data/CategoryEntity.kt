package com.example.campuspocket.feature.finance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CategoryKind { EXPENSE, INCOME }

@Entity(
    tableName = "categories",
    indices = [Index("kind")]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,
    val iconKey: String = "category",
    val colorArgb: Int = 0xFF4C8DFF.toInt(),
    val archived: Boolean = false
) {
    companion object {
        const val TABLE_NAME = "categories"
    }
}