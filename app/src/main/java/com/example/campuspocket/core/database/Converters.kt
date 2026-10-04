package com.example.campuspocket.core.database

import androidx.room.TypeConverter
import java.time.LocalDate

/** Conversores de Room. Las fechas se guardan como día epoch (Long). */
class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let { LocalDate.ofEpochDay(it) }
}
