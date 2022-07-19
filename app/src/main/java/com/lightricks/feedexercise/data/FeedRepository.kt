package com.lightricks.feedexercise.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedItemEntity
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.network.GetFeedResponse
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * This is our data layer abstraction. Users of this class don't need to know
 * where the data actually comes from (network, database or somewhere else).
 */
class FeedRepository(private val apiService: FeedApiService, private val db: FeedDatabase) {

    val feedItems: LiveData<List<FeedItem>> =
        Transformations.map(db.feedDao().getAll()) {
            it.toFeedItems()
        }

    fun refresh(): Completable {
        return apiService.getFeed()
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { feedResponse ->
                handleResponse(feedResponse)
            }
    }

    private fun handleResponse(feedResponse: GetFeedResponse): Completable {
        val feedEntities = feedResponse.templatesMetadata.map {
            FeedItemEntity(
                it.id,
                FeedApiService.thumbnailURIPrefix + it.templateThumbnailURI,
                it.isPremium
            )
        }
        return db.feedDao().insertAll(feedEntities)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }
}

fun List<FeedItemEntity>.toFeedItems(): List<FeedItem> {
    return this.map { FeedItem(it.id, it.thumbnailUrl, it.isPremium) }
}
