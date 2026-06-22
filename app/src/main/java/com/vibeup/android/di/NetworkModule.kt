package com.vibeup.android.di

import com.vibeup.android.data.remote.api.LyricsApiService
import com.vibeup.android.data.remote.api.SaavnApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LyricsRetrofit


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL =
        "https://jiosaavn-api.abenanthan-p-2024-cse.workers.dev/"
    private const val LYRICS_BASE_URL = "https://lrclib.net/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideSaavnApiService(retrofit: Retrofit): SaavnApiService {
        return retrofit.create(SaavnApiService::class.java)
    }

    @Provides
    @Singleton
    @LyricsRetrofit
    fun provideLyricsRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(LYRICS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                okHttpClient.newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideLyricsApiService(
        @LyricsRetrofit retrofit: Retrofit
    ): LyricsApiService {
        return retrofit.create(LyricsApiService::class.java)
    }
}
/*```

Replace your entire `NetworkModule.kt` with this! ✅

Also — since we're removing NewPipe, **delete these files** if they exist:
```
❌ NewPipeDownloader.kt   → delete
❌ YouTubeSearchService.kt → delete
❌ YouTubeExtractor.kt    → delete
 */