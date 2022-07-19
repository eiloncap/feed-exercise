package com.lightricks.feedexercise.network

import android.content.Context
import com.lightricks.feedexercise.database.FeedItemEntity
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Single
import okio.buffer
import okio.source
import retrofit2.Response


class MockFeedApiService(context: Context) : FeedApiService {

    private var shouldThrowError = false
    private val feedResponse: GetFeedResponse?
    val expectedFeedItemEntities: Array<FeedItemEntity>

    init {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val jsonAdapter: JsonAdapter<GetFeedResponse> = moshi.adapter(GetFeedResponse::class.java)
         feedResponse = jsonAdapter.fromJson(
            context.assets.open("get_feed_response.json").source().buffer()
        )
        expectedFeedItemEntities = feedResponse?.templatesMetadata?.map {
            FeedItemEntity(
                it.id,
                FeedApiService.thumbnailURIPrefix + it.templateThumbnailURI,
                it.isPremium
            )
        }?.toTypedArray() ?: emptyArray()
    }

    override fun getFeed(): Single<GetFeedResponse> {
        if (shouldThrowError) {
            shouldThrowError = false
            return Single.error(MockNetworkException())
        }
        return Single.just(feedResponse)
    }

    fun throwErrorOnNextGetFeed() {
        shouldThrowError = true
    }
}

class MockNetworkException : RuntimeException()