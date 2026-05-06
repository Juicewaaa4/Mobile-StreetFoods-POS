package com.streetfood.pos.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.streetfood.pos.data.models.TransactionItem

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromCartItemList(value: List<TransactionItem>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCartItemList(value: String): List<TransactionItem> {
        val listType = object : TypeToken<List<TransactionItem>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}
