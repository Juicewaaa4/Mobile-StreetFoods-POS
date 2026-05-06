package com.streetfood.pos.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.streetfood.pos.data.dao.ProductDao
import com.streetfood.pos.data.dao.TransactionDao
import com.streetfood.pos.data.models.Product
import com.streetfood.pos.data.models.Transaction

@Database(
    entities = [Product::class, Transaction::class],
    version = 1,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "streetfood_pos_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
