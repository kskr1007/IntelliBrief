package com.example.intellibrief

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GdeltManager {
    private val okHttpClient: OkHttpClient

    init {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("GDELT_API", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // get latest events from gdelt
    fun getLatestEvents(): List<GdeltArticle> {
        return fetchWithRetry(3)
    }

    // I used AI for this function because of the GDELT rate limiting.
    // This function will retry the request up to 3 times if it fails.
    private fun fetchWithRetry(maxRetries: Int): List<GdeltArticle> {
        var currentAttempt = 0
        while (currentAttempt < maxRetries) {
            // call gdelt api
            val result = executeRequest()
            if (result.isNotEmpty()) return result
            currentAttempt++
            if (currentAttempt < maxRetries) {
                Log.d("GdeltManager", "Retrying GDELT request (Attempt ${currentAttempt + 1})...")
                Thread.sleep(2000)
            }
        }
        return emptyList()
    }

    private fun executeRequest(): List<GdeltArticle> {
        // building gdelt https request
        val urlBuilder = "https://api.gdeltproject.org/api/v2/doc/doc".toHttpUrlOrNull()?.newBuilder()
            ?: return emptyList()

        // topics concerning nat sec
        val query = "(theme:MILITARY OR theme:TERRORISM OR theme:CYBER_ATTACK OR theme:INTELLIGENCE) sourcelang:eng"

        // add in query parameters
        urlBuilder.addQueryParameter("query", query)
        urlBuilder.addQueryParameter("mode", "artlist")
        urlBuilder.addQueryParameter("maxrecords", "10")
        urlBuilder.addQueryParameter("timespan", "24h")
        urlBuilder.addQueryParameter("sort", "datedesc")
        urlBuilder.addQueryParameter("format", "json")

        // make the final url a string
        val url = urlBuilder.build().toString()
        Log.d("GdeltManager", "Executing request: $url")

        // send request
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            // get response
            val response = okHttpClient.newCall(request).execute()
            // get response body
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                // create articles list
                val articlesList = mutableListOf<GdeltArticle>()
                val json = JSONObject(responseBody)
                // look for articles field
                val articles = json.optJSONArray("articles") ?: return emptyList()

                // for all 10 articles...
                for (i in 0 until articles.length()) {
                    val currentArticle = articles.getJSONObject(i)
                    articlesList.add(
                        // add the article as a GDELT Article object with all the fields
                        GdeltArticle(
                            title = currentArticle.optString("title", "No Title"),
                            url = currentArticle.optString("url", ""),
                            socialImage = if (currentArticle.isNull("socialimage")) null else currentArticle.optString("socialimage"),
                            seenDate = currentArticle.optString("seendate", ""),
                            domain = currentArticle.optString("domain", "")
                        )
                    )
                }
                // return the articles list
                return articlesList
            }
            // GDELT has strict rate limiting, so this catch block reports the error
        } catch (e: Exception) {
            Log.e("GdeltManager", "Request failed: ${e.message}")
        }
        // if failed, return empty list
        return emptyList()
    }
}
