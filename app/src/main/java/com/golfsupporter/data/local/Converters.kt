package com.golfsupporter.data.local

import androidx.room.TypeConverter

/**
 * Room type converters for the small collection types used by the entities.
 * Values are stored as compact delimited strings to avoid pulling in a JSON
 * dependency for an offline-only app.
 */
class Converters {

    @TypeConverter
    fun intMapToString(map: Map<Int, Int>?): String? {
        if (map == null) return null
        return map.entries.joinToString(";") { "${it.key}:${it.value}" }
    }

    @TypeConverter
    fun stringToIntMap(value: String?): Map<Int, Int>? {
        if (value == null) return null
        if (value.isBlank()) return emptyMap()
        return value.split(";").associate {
            val (k, v) = it.split(":")
            k.toInt() to v.toInt()
        }
    }

    @TypeConverter
    fun stringListToString(list: List<String>?): String? {
        return list?.joinToString("|")
    }

    @TypeConverter
    fun stringToStringList(value: String?): List<String>? {
        if (value == null) return null
        if (value.isBlank()) return emptyList()
        return value.split("|")
    }
}
