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
    // OkHttpClient is used for making network requests to the Groq API
    private val client = OkHttpClient()
    
    // The endpoint for Groq
    private val groqUrl = "https://api.groq.com/openai/v1/chat/completions"


     // Generates a structured intelligence brief from a list of GDELT articles.
     // This function runs on a background IO thread to avoid blocking the UI.
    suspend fun generateIntelligenceBrief(articles: List<GdeltArticle>): String? = withContext(Dispatchers.IO) {
        //  Ensure we have data to send to the AI
        if (articles.isEmpty()) {
            Log.d("AiManager", "No articles to summarize.")
            return@withContext null
        }

         // Convert article list into a clean text block for the prompt
         // Used AI for this step
        val articlesText = articles.joinToString("\n") { "- ${it.title} (Source: ${it.domain})" }

        //  Define the context for the AI model
        val systemPrompt = "Act like you are a CIA intelligence analyst. STYLE: Clinical, objective, and urgent. No conversational filler."
        
        // Give AI the task
        val userPrompt = """
            Analyze these events and provide:
            1. A 3-sentence executive summary of the primary global threat.
            2. Three specific, actionable security recommendations for field agents.
            
            FORMAT YOUR RESPONSE EXACTLY LIKE THIS:
            [SUMMARY]
            (Your 3-sentence summary here)
            [RECS]
            1. (Rec 1)
            2. (Rec 2)
            3. (Rec 3)
            
            EVENTS:
            $articlesText
        """.trimIndent()

        // Build the JSON body required by Groq
        val jsonBody = JSONObject().apply {
            put("model", "openai/gpt-oss-120b")
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

         // Convert the JSON object to a raw string
         // Used AI for this step
        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        
        Log.d("AiManager", "Sending request to Groq. Body: ${jsonBody.toString()}")

        // Adds the necessary auth headers
        val request = Request.Builder()
            .url(groqUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

         // Execution
         // return@withContext is used to stop the code block when the error arises and reports the error to the logcat
         // I used AI for the error handling below
         return@withContext try {
            val response = client.newCall(request).execute()
            val responseCode = response.code
            val responseBody = response.body?.string()
            
            Log.d("AiManager", "Groq Response Code: $responseCode")

            // Parse Successful Response
            if (response.isSuccessful && responseBody != null) {
                Log.d("AiManager", "Groq Response Body: $responseBody")
                val jsonResponse = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    Log.e("AiManager", "Failed to parse Groq response as JSON: ${e.message}")
                    return@withContext "Error: Received invalid response from AI service."
                }
                
                // extract the generate text from choices[0].message.content
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
            // catch general errors with the groq request
            Log.e("AiManager", "Exception during Groq request", e)
            null
        }
    }
}
