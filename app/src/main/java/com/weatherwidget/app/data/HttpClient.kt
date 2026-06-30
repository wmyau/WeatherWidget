package com.weatherwidget.app.data

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

// Single instances — OkHttp pools connections; Json is stateless but heavyweight to construct.
internal object HttpClient {
    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val json: Json = Json {
        ignoreUnknownKeys = true   // API may add fields; don't break on them
        coerceInputValues = true   // null JSON values coerce to Kotlin defaults
    }
}
