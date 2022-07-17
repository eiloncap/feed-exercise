package com.lightricks.feedexercise.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Single
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET


interface FeedApiService {
    @GET("Android/demo/feed.json")
    fun getFeed(): Single<GetFeedResponse>
}

class FeedApiServiceProvider {
    companion object {

        private val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        private val retrofit = Retrofit.Builder()
            .baseUrl("https://assets.swishvideoapp.com/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        const val thumbnailURIPrefix =
            "https://assets.swishvideoapp.com/Android/demo/catalog/thumbnails/"

        fun getFeedApiService(): FeedApiService {
            return retrofit.create(FeedApiService::class.java)
        }
    }
}