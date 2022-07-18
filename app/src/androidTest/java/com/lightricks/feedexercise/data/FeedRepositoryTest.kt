package com.lightricks.feedexercise.data

import android.content.Context
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedEntity
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.network.MockFeedApiService
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class FeedRepositoryTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: FeedDatabase
    private lateinit var feedApiService: FeedApiService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        createDb(context)
        createApiService(context)
    }

    private fun createDb(context: Context) {
        db = Room.inMemoryDatabaseBuilder(
            context, FeedDatabase::class.java
        ).build()
    }

    private fun createApiService(context: Context) {
        feedApiService = MockFeedApiService(context)
    }

    @Test
    fun refreshMust_saveFeedToDatabase() {
        val feedRepository = FeedRepository(feedApiService, db)

        feedRepository.feedItems.blockingObserve()
        assertThat(feedRepository.feedItems.value?.size).isEqualTo(0)
        feedRepository.refresh().test().await()
        feedRepository.feedItems.blockingObserve()
        assertThat(feedRepository.feedItems.value?.size).isEqualTo(db.feedDao().count())
    }

    @Test
    fun feedItemsMust_containFeedFromDatabase() {
        val feedRepository = FeedRepository(feedApiService, db)

        feedRepository.refresh().test().await()
        feedRepository.feedItems.blockingObserve()
        val expectedListValues = getFeedItemsFromDb(db.feedDao().getAll())
        assertThat(feedRepository.feedItems.value)
            .containsExactly(*expectedListValues)
    }

    private fun getFeedItemsFromDb(ld: LiveData<List<FeedEntity>>): Array<FeedItem> {
        ld.blockingObserve()
        return ld.value?.map { FeedItem(it.id, it.thumbnailUrl, it.isPremium) }?.toTypedArray()
            ?: arrayOf()
    }

    @After
    fun closeDb() {
        db.close()
    }
}

private fun <T> LiveData<T>.blockingObserve(): T? {
    var value: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(t: T) {
            value = t
            latch.countDown()
            removeObserver(this)
        }
    }

    observeForever(observer)
    latch.await(5, TimeUnit.SECONDS)
    return value
}
