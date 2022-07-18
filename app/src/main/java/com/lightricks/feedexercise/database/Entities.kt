package com.lightricks.feedexercise.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// todo: ask why do we need the Entity separate to the FeedItem

@Entity(tableName = "feed")
data class FeedEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String,
    @ColumnInfo(name = "is_premium") val isPremium: Boolean
)