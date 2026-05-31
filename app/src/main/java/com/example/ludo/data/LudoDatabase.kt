package com.example.ludo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProfileEntity::class, MatchHistoryEntity::class, SavedGameEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LudoDatabase : RoomDatabase() {
    abstract fun ludoDao(): LudoDao

    companion object {
        @Volatile
        private var INSTANCE: LudoDatabase? = null

        fun getDatabase(context: Context): LudoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LudoDatabase::class.java,
                    "royal_ludo_database"
                )
                .fallbackToDestructiveMigration() // Simple offline strategy for dev safety
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
