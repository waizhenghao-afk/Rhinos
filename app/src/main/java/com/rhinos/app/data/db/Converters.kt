package com.rhinos.app.data.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Dates are stored as ISO-8601 strings ("2026-10-04"), which keeps the database
 * and exported JSON human-readable. Corrupted values fall back to the epoch
 * instead of crashing Room mapping.
 */
class Converters {

    @TypeConverter
    fun dateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToDate(value: String?): LocalDate? {
        if (value == null) return null
        return try {
            LocalDate.parse(value)
        } catch (e: DateTimeParseException) {
            LocalDate.EPOCH
        }
    }
}
