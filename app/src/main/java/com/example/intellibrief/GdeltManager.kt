package com.example.intellibrief

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException

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
            .build()
    }

    fun getLatestEvents(): List<GdeltArticle> {
        // building gdelt http request
        val urlBuilder = "https://api.gdeltproject.org/api/v2/doc/doc".toHttpUrlOrNull()?.newBuilder()
            ?: return emptyList()

        // only events related to natsec
        val tightQuery = "(" +
                "theme:MILITARY_POSTURE OR " +
                "theme:MILITARY OR " +
                "theme:ARMED_CONFLICT OR " +
                "theme:TERROR OR " +
                "theme:SANCTIONS OR " +
                "theme:CYBER_ATTACK OR " +
                "theme:SURVEILLANCE" +
                ") sourcelang:eng"

        urlBuilder.addQueryParameter("query", tightQuery)
        urlBuilder.addQueryParameter("mode", "artlist")
        urlBuilder.addQueryParameter("maxrecords", "10") // Limited to top 10
        urlBuilder.addQueryParameter("timespan", "24h")
        urlBuilder.addQueryParameter("sort", "datedesc")
        urlBuilder.addQueryParameter("format", "json")

        // final url
        val url = urlBuilder.build().toString()

        Log.d("GdeltManager", "Executing request with tightened scope to: $url")

        // had to add this to bypass some rate limit errors
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .get()
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            // parse response for articles
            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                val articlesList = mutableListOf<GdeltArticle>()
                val json = JSONObject(responseBody)

                val articles = json.optJSONArray("articles")
                // if no articles found
                if (articles == null) {
                    Log.w("GdeltManager", "No 'articles' array found in JSON response")
                    return emptyList()
                }

                // add each article to total list
                for (i in 0 until articles.length()) {
                    val currentArticle = articles.getJSONObject(i)
                    articlesList.add(
                        GdeltArticle(
                            title = currentArticle.optString("title", "No Title"),
                            url = currentArticle.optString("url", ""),
                            socialImage = if (currentArticle.isNull("socialimage")) null else currentArticle.optString("socialimage"),
                            seenDate = currentArticle.optString("seendate", ""),
                            domain = currentArticle.optString("domain", ""),
                            sourceCountry = if (currentArticle.isNull("sourcecountry")) null else currentArticle.optString("sourcecountry")
                        )
                    )
                }
                // used AI to generate a trail of error logging for gdelt
                Log.d("GdeltManager", "Successfully parsed ${articlesList.size} articles")
                return articlesList
            } else if (response.code == 429) {
                Log.e("GdeltManager", "RATE LIMITED (429): GDELT is blocking the request. Try again later.")
            } else {
                Log.e("GdeltManager", "Request failed with code: ${response.code}")
                Log.d("GdeltManager", "Error body: $responseBody")
            }
        } catch (e: IOException) {
            Log.e("GdeltManager", "Network error fetching GDELT data", e)
        } catch (e: Exception) {
            Log.e("GdeltManager", "Parsing error", e)
        }
        return emptyList()
    }
}
