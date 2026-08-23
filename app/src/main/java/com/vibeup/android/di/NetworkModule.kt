package com.vibeup.android.di

import android.content.Context
import com.vibeup.android.BuildConfig
import com.vibeup.android.data.remote.api.JioSaavnDirectApiService
import com.vibeup.android.data.remote.api.LyricsApiService
import com.vibeup.android.data.remote.api.SaavnApiService
import com.vibeup.android.data.remote.api.LyricsOvhApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
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
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        // Level.BODY buffers every response into memory and converts it to a
        // String before writing it to logcat, line by line. Cold start alone
        // fires ~19 requests of song JSON through this client, so it was a large
        // CPU + allocation cost — and it was previously enabled in release too.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(LanguageInterceptor())
            // Disk cache so a warm start can serve the Home feed without hitting
            // the network at all.
            .cache(Cache(File(context.cacheDir, "http_cache"), 20L * 1024 * 1024))
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
                    // lrclib.net asks clients to identify themselves; requests using
                    // the default OkHttp UA get rate-limited/blocked (the reason
                    // lyrics "suddenly" stopped returning results).
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", "VibeUp/1.0 (Android music player; contact: app)")
                                .build()
                        )
                    }
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
