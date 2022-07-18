package com.lightricks.feedexercise.network

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Single
import okio.buffer
import okio.source


class MockFeedApiService(private val context: Context) : FeedApiService {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    override fun getFeed(): Single<GetFeedResponse> {
        val jsonAdapter: JsonAdapter<GetFeedResponse> = moshi.adapter(GetFeedResponse::class.java)
        val feedResponse = jsonAdapter.fromJson(
            context.assets.open("get_feed_response.json").source().buffer()
        )
        return Single.just(feedResponse)
    }
}