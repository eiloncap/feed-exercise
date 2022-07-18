package com.lightricks.feedexercise.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.reactivex.Completable

@Dao
interface FeedDao {
    @Query("SELECT * FROM feed")
    fun getAll(): LiveData<List<FeedEntity>>

    @Query("SELECT COUNT(*) from feed")
    fun count(): Int

    @Insert
    fun insertAll(feeds: List<FeedEntity>): Completable

    @Query("DELETE FROM feed")
    fun deleteAll(): Completable
}