package com.vibeup.android.di

import okhttp3.Interceptor
import okhttp3.Response

class LanguageInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val originalUrl = original.url

        // Only the JioSaavn worker understands the `languages` param. Adding it to
        // third-party hosts (lrclib.net lyrics, lyrics.ovh, jiosaavn.com) pollutes
        // their requests and can make them fail — so scope it to the worker only.
        if (!originalUrl.host.contains("workers.dev")) {
            return chain.proceed(original)
        }

        val newUrl = originalUrl.newBuilder()
            .addQueryParameter("languages", "english,hindi,punjabi,tamil,telugu")
            .build()

        val newRequest = original.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}