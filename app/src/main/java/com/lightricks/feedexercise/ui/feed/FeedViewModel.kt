package com.lightricks.feedexercise.ui.feed

import android.app.Application
import androidx.lifecycle.*
import androidx.room.Room
import com.lightricks.feedexercise.data.FeedItem
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedEntity
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.network.FeedApiServiceProvider
import com.lightricks.feedexercise.network.GetFeedResponse
import com.lightricks.feedexercise.util.Event
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers


/**
 * This view model manages the data for [FeedFragment].
 */
open class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val isLoading = MutableLiveData<Boolean>()
    private val isEmpty = MutableLiveData<Boolean>()
    private val feedItems = MediatorLiveData<List<FeedItem>>()
    private val networkErrorEvent = MutableLiveData<Event<String>>()
    private val feedApiService: FeedApiService = FeedApiServiceProvider.getFeedApiService()
    private lateinit var db: FeedDatabase
    private val compositeDisposable = CompositeDisposable()

    fun getIsLoading(): LiveData<Boolean> = isLoading
    fun getIsEmpty(): LiveData<Boolean> = isEmpty
    fun getFeedItems(): LiveData<List<FeedItem>> = feedItems
    fun getNetworkErrorEvent(): LiveData<Event<String>> = networkErrorEvent

    init {
        setupDb(application)
        refresh()
    }

    private fun setupDb(application: Application) {
        db = Room.databaseBuilder(
            application,
            FeedDatabase::class.java, "feed_data_base"
        ).build()
    }

    // todo: should I dispose them? when?
    fun refresh() {
        isLoading.value = true
        val disposable = feedApiService.getFeed()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ feedResponse ->
                handleResponse(feedResponse)
            }, { error ->
                handleNetworkError(error)
            })
        compositeDisposable.add(disposable)
    }

    private fun handleResponse(feedResponse: GetFeedResponse) {
        val feedEntities = feedResponse.templatesMetadata.map {
            FeedEntity(
                it.id,
                FeedApiServiceProvider.thumbnailURIPrefix + it.templateThumbnailURI,
                it.isPremium
            )
        }
        feedResponse.templatesMetadata.map {
            FeedItem(
                it.id,
                FeedApiServiceProvider.thumbnailURIPrefix + it.templateThumbnailURI,
                it.isPremium
            )
        }.toList()
            .let {
                isLoading.value = false
                isEmpty.value = it.isEmpty()
                feedItems.value = it
            }

        val disposable = db.feedDao().insertAll(feedEntities)
            .subscribeOn(Schedulers.io())
            .subscribe()

        compositeDisposable.add(disposable)
    }

    private fun handleNetworkError(error: Throwable?) {
        isLoading.value = false
        isEmpty.value = true
        networkErrorEvent.value = Event(error?.message ?: "")
    }

    override fun onCleared() {
        compositeDisposable.clear()
        compositeDisposable.dispose()
        super.onCleared()
    }
}

/**
 * This class creates instances of [FeedViewModel].
 * It's not necessary to use this factory at this stage. But if we will need to inject
 * dependencies into [FeedViewModel] in the future, then this is the place to do it.
 */
class FeedViewModelFactory(private var application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            throw IllegalArgumentException("factory used with a wrong class")
        }
        @Suppress("UNCHECKED_CAST")
        return FeedViewModel(application) as T
    }
}