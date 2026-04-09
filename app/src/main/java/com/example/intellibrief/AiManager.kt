package com.example.intellibrief

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AiManager(private val apiKey: String) {
    private val client = OkHttpClient()
    // groq request url
    private val groqUrl = "https://api.groq.com/openai/v1/chat/completions"

    suspend fun generateIntelligenceBrief(articles: List<GdeltArticle>): String? = withContext(Dispatchers.IO) {
        // if nothing was found in gdelt
        if (articles.isEmpty()) {
            Log.d("AiManager", "No articles to summarize.")
            return@withContext null
        }

        // format articles for AI
        val articlesText = articles.joinToString("\n") { "- ${it.title} (Source: ${it.domain})" }

        // prompts for AI
        val systemPrompt = "Act like you are a CIA intelligence analyst preparing a Daily Brief for the President of the United States. STYLE: Professional, clinical, urgent but measured."
        val userPrompt = """
            Based on the following news events, create a concise, high-level intelligence brief.
            
            FORMAT:
            1. Start with a "TOP SECRET" header.
            2. Provide a 2-3 sentence executive summary of the global threat landscape.
            3. For each of the key events listed below, provide a 1-sentence assessment and assign a "Threat Score" from 1-10 (10 being critical).
            
            EVENTS:
            $articlesText
        """.trimIndent()
        // I used AI to generate this section
        // this is the json body being sent to groq
        val jsonBody = JSONObject().apply {
            put("model", "llama3-8b-8192")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
            put("temperature", 0.5)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        
        Log.d("AiManager", "Sending request to Groq. Body: ${jsonBody.toString()}")

        // build request
        val request = Request.Builder()
            .url(groqUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val responseCode = response.code
            val responseBody = response.body?.string()
            
            Log.d("AiManager", "Groq Response Code: $responseCode")

            // parsing groq response
            if (response.isSuccessful && responseBody != null) {
                Log.d("AiManager", "Groq Response Body: $responseBody")
                val jsonResponse = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    Log.e("AiManager", "Failed to parse Groq response as JSON: ${e.message}")
                    return@withContext "Error: Received invalid response from AI service."
                }
                
                val choices = jsonResponse.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    Log.e("AiManager", "No choices returned in Groq response")
                    return@withContext "Error: AI service returned no results."
                }
                
                val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content")
                if (content == null) {
                    Log.e("AiManager", "No content found in Groq response message")
                    return@withContext "Error: AI response was empty."
                }

                Log.d("AiManager", "Extracted Content: $content")
                content
            } else {
                Log.e("AiManager", "Groq API Error: $responseCode - $responseBody")
                null
            }
        } catch (e: Exception) {
            Log.e("AiManager", "Exception during Groq request", e)
            null
        }
    }
}
