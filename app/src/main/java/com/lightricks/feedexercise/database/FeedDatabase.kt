package com.lightricks.feedexercise.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FeedItemEntity::class], version = 1)
abstract class FeedDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
}

object FeedDatabaseProvider {
    private lateinit var INSTANCE: FeedDatabase

    fun getDatabase(context: Context): FeedDatabase {
        synchronized(FeedDatabase::class.java) {
            if (!::INSTANCE.isInitialized) {
                INSTANCE = createDatabase(context)
            }
        }
        return INSTANCE
    }

    private fun createDatabase(context: Context): FeedDatabase {
        return Room.databaseBuilder(
            context,
            FeedDatabase::
            class.java, "feed_data_base"
        ).build()
    }
}