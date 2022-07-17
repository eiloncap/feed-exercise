package com.lightricks.feedexercise.ui.feed

import android.annotation.SuppressLint
import androidx.lifecycle.*
import com.lightricks.feedexercise.data.FeedItem
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.network.FeedApiServiceProvider
import com.lightricks.feedexercise.network.GetFeedResponse
import com.lightricks.feedexercise.util.Event
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * This view model manages the data for [FeedFragment].
 */
open class FeedViewModel : ViewModel() {
    private val isLoading = MutableLiveData<Boolean>()
    private val isEmpty = MutableLiveData<Boolean>()
    private val feedItems = MediatorLiveData<List<FeedItem>>()
    private val networkErrorEvent = MutableLiveData<Event<String>>()
    private val feedApiService: FeedApiService = FeedApiServiceProvider.getFeedApiService()

    fun getIsLoading(): LiveData<Boolean> = isLoading
    fun getIsEmpty(): LiveData<Boolean> = isEmpty
    fun getFeedItems(): LiveData<List<FeedItem>> = feedItems
    fun getNetworkErrorEvent(): LiveData<Event<String>> = networkErrorEvent

    init {
        refresh()
    }

    @SuppressLint("CheckResult")
    fun refresh() {
        isLoading.value = true
        feedApiService.getFeed()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ feedResponse ->
                handleResponse(feedResponse)
            }, { error ->
                handleNetworkError(error)
            })
    }

    private fun handleResponse(feedResponse: GetFeedResponse) {
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
    }

    private fun handleNetworkError(error: Throwable?) {
        isLoading.value = false
        isEmpty.value = true
        networkErrorEvent.value = Event(error?.message ?: "")
    }
}

/**
 * This class creates instances of [FeedViewModel].
 * It's not necessary to use this factory at this stage. But if we will need to inject
 * dependencies into [FeedViewModel] in the future, then this is the place to do it.
 */
class FeedViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            throw IllegalArgumentException("factory used with a wrong class")
        }
        @Suppress("UNCHECKED_CAST")
        return FeedViewModel() as T
    }
}