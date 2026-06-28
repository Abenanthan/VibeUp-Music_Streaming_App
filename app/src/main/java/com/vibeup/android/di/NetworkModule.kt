package com.vibeup.android.di

import com.vibeup.android.data.remote.api.JioSaavnDirectApiService
import com.vibeup.android.data.remote.api.LyricsApiService
import com.vibeup.android.data.remote.api.SaavnApiService
import com.vibeup.android.data.remote.api.LyricsOvhApiService
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
import javax.inject.Named

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LyricsRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JioSaavnDirectRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL =
        "https://jiosaavn-api.abenanthan-p-2024-cse.workers.dev/"
    private const val LYRICS_BASE_URL = "https://lrclib.net/"
    private const val JIOSAAVN_DIRECT_URL = "https://www.jiosaavn.com/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(LanguageInterceptor())
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

    @Provides
    @Singleton
    @JioSaavnDirectRetrofit
    fun provideJioSaavnDirectRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(JIOSAAVN_DIRECT_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideJioSaavnDirectApiService(
        @JioSaavnDirectRetrofit retrofit: Retrofit
    ): JioSaavnDirectApiService {
        return retrofit.create(JioSaavnDirectApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("lyricsOvh")
    fun provideLyricsOvhRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.lyrics.ovh/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideLyricsOvhApiService(
        @Named("lyricsOvh") retrofit: Retrofit
    ): LyricsOvhApiService {
        return retrofit.create(LyricsOvhApiService::class.java)
    }
}
