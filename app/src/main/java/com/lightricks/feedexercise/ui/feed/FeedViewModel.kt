package com.lightricks.feedexercise.ui.feed

import android.app.Application
import androidx.lifecycle.*
import androidx.room.Room
import com.lightricks.feedexercise.data.FeedItem
import com.lightricks.feedexercise.data.FeedRepository
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.util.Event
import io.reactivex.disposables.CompositeDisposable


/**
 * This view model manages the data for [FeedFragment].
 */
open class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val isLoading = MutableLiveData<Boolean>()
    private val isEmpty = MutableLiveData<Boolean>()
    private val feedItems = MediatorLiveData<List<FeedItem>>()
    private val networkErrorEvent = MutableLiveData<Event<String>>()
    private val feedApiService: FeedApiService = FeedApiService.getFeedApiService()
    private lateinit var db: FeedDatabase
    private val compositeDisposable = CompositeDisposable()
    private val feedRepository: FeedRepository

    fun getIsLoading(): LiveData<Boolean> = isLoading
    fun getIsEmpty(): LiveData<Boolean> = isEmpty
    fun getFeedItems(): LiveData<List<FeedItem>> = feedItems
    fun getNetworkErrorEvent(): LiveData<Event<String>> = networkErrorEvent

    init {
        setupDb(application)
        feedRepository = FeedRepository(feedApiService, db)
        setUpFeedObserver()
        refresh()
    }

    private fun setupDb(application: Application) {
        db = Room.databaseBuilder(
            application,
            FeedDatabase::class.java, "feed_data_base"
        ).build()
    }

    private fun setUpFeedObserver() {
        feedItems.addSource(feedRepository.feedItems) {
            feedItems.value = it
            isEmpty.value = it.isEmpty()
        }
    }

    fun refresh() {
        isLoading.value = true

        val disposable = feedRepository.refresh()
            .subscribe({
                isLoading.value = false
            }, { error ->
                handleNetworkError(error)
            })
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