package com.lightricks.feedexercise.data

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedItemEntity
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.network.GetFeedResponse
import com.lightricks.feedexercise.network.MockFeedApiService
import com.lightricks.feedexercise.network.MockNetworkException
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.buffer
import okio.source
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
    private lateinit var feedApiService: MockFeedApiService
    private lateinit var expectedFeedItemEntityList: List<FeedItemEntity>

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        createDb(context)
        createApiService(context)
        createExpectedList(context)
    }

    private fun createExpectedList(context: Context) {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val jsonAdapter: JsonAdapter<GetFeedResponse> = moshi.adapter(GetFeedResponse::class.java)
        val feedResponse = jsonAdapter.fromJson(
            context.assets.open("get_feed_response.json").source().buffer()
        )
        expectedFeedItemEntityList = feedResponse?.templatesMetadata?.map {
            FeedItemEntity(
                it.id,
                FeedApiService.thumbnailURIPrefix + it.templateThumbnailURI,
                it.isPremium
            )
        } ?: emptyList()
    }

    private fun createDb(context: Context) {
        db = Room.inMemoryDatabaseBuilder(context, FeedDatabase::class.java).build()
    }

    private fun createApiService(context: Context) {
        feedApiService = MockFeedApiService(context)
    }

    @Test
    fun refresh_whenFetchDataFromNetwork_thenSaveFeedToDatabase() {
        val feedRepository = FeedRepository(feedApiService, db)

        feedRepository.feedItems.blockingObserve()
        assertThat(db.feedDao().count()).isEqualTo(0)
        feedRepository.refresh().test().await()
        feedRepository.feedItems.blockingObserve()

        val dbFeedItemsLiveData = db.feedDao().getAll()
        dbFeedItemsLiveData.blockingObserve()
        assertThat(dbFeedItemsLiveData.value)
            .containsExactly(*feedApiService.expectedFeedItemEntities)
    }

    @Test
    fun refresh_whenNetworkError_thenTheErrorIsPassed() {
        val feedRepository = FeedRepository(feedApiService, db)
        feedApiService.throwErrorOnNextGetFeed()
        feedRepository.refresh().test().await().assertError(MockNetworkException::class.java)
    }

    @Test
    fun feedItems_whenFeedItemsAddedToDataBase_thenFeedItemsContainFeedFromDatabase() {
        val feedRepository = FeedRepository(feedApiService, db)
        feedRepository.feedItems.blockingObserve()
        assertThat(feedRepository.feedItems.value?.size).isEqualTo(0)

        db.feedDao().insertAll(expectedFeedItemEntityList).test().await()
        val expectedListValues =
            feedApiService.expectedFeedItemEntities.asList().toFeedItems().toTypedArray()
        feedRepository.feedItems.blockingObserve()
        assertThat(feedRepository.feedItems.value).containsExactly(*expectedListValues)
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
