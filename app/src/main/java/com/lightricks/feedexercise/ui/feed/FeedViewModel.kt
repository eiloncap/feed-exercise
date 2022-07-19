package com.lightricks.feedexercise.ui.feed

import android.content.Context
import androidx.lifecycle.*
import com.lightricks.feedexercise.data.FeedItem
import com.lightricks.feedexercise.data.FeedRepository
import com.lightricks.feedexercise.database.FeedDatabaseProvider
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.util.Event
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable


/**
 * This view model manages the data for [FeedFragment].
 */
open class FeedViewModel(private val feedRepository: FeedRepository) : ViewModel() {

    private val isLoading = MutableLiveData<Boolean>()
    private val isEmpty = MutableLiveData<Boolean>()
    private val feedItems = MediatorLiveData<List<FeedItem>>()
    private val networkErrorEvent = MutableLiveData<Event<String>>()
    private val compositeDisposable = CompositeDisposable()

    fun getIsLoading(): LiveData<Boolean> = isLoading
    fun getIsEmpty(): LiveData<Boolean> = isEmpty
    fun getFeedItems(): LiveData<List<FeedItem>> = feedItems
    fun getNetworkErrorEvent(): LiveData<Event<String>> = networkErrorEvent

    init {
        setUpFeedObserver()
        refresh()
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
            .observeOn(AndroidSchedulers.mainThread())
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
        networkErrorEvent.value = Event(error?.message ?: "Network error")
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }
}

/**
 * This class creates instances of [FeedViewModel].
 * It's not necessary to use this factory at this stage. But if we will need to inject
 * dependencies into [FeedViewModel] in the future, then this is the place to do it.
 */
class FeedViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            throw IllegalArgumentException("factory used with a wrong class")
        }
        val feedRepository =
            FeedRepository(FeedApiService.service, FeedDatabaseProvider.getDatabase(context))
        @Suppress("UNCHECKED_CAST")
        return FeedViewModel(feedRepository) as T
    }
}