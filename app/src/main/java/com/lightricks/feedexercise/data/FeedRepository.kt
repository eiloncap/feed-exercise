package com.lightricks.feedexercise.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedEntity
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.network.GetFeedResponse
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * This is our data layer abstraction. Users of this class don't need to know
 * where the data actually comes from (network, database or somewhere else).
 */
class FeedRepository(private val apiService: FeedApiService, private val db: FeedDatabase) {

    // todo: should I dispose them? when?
    private val compositeDisposable = CompositeDisposable()
    val feedItems: LiveData<List<FeedItem>> =
        Transformations.map(db.feedDao().getAll()) { it.toFeedItems() }

    fun refresh(): Completable {
        return apiService.getFeed()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapCompletable { feedResponse ->
                handleResponse(feedResponse)
                return@flatMapCompletable Completable.complete()
            }
    }

    private fun handleResponse(feedResponse: GetFeedResponse) {
        val feedEntities = feedResponse.templatesMetadata.map {
            FeedEntity(
                it.id,
                FeedApiService.thumbnailURIPrefix + it.templateThumbnailURI,
                it.isPremium
            )
        }

        val disposable = db.feedDao().insertAll(feedEntities)
            .subscribeOn(Schedulers.io())
            .onErrorComplete()
            .subscribe()

        compositeDisposable.add(disposable)
    }
}

private fun List<FeedEntity>.toFeedItems(): List<FeedItem> {
    return this.map { FeedItem(it.id, it.thumbnailUrl, it.isPremium) }
}
