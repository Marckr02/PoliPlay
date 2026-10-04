package com.example.campuspocket.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_hints")
data class ImportHintEntity(
    @PrimaryKey val rawKey: String,
    val cleanValue: String
) {
    companion object {
        const val TABLE_NAME = "import_hints"
    }
}