package com.example.intellibrief

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainBriefScreen(
    articles: List<GdeltArticle>,
    aiSummary: String?,
    onDataFetched: (List<GdeltArticle>, String?) -> Unit,
    onLoadEvents: () -> Unit
) {
    // for connecting to the GDELT API and AI service
    val gdeltManager = remember { GdeltManager() }
    val aiManager = remember { AiManager(apiKey = BuildConfig.GROQ_API_KEY) }
    
    // UI states to track loading progress
    var isLoading by remember { mutableStateOf(articles.isEmpty()) }
    var isAiLoading by remember { mutableStateOf(articles.isNotEmpty() && aiSummary == null) }

    // Date formatting for the top bar
    val currentDate = remember {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
    }

    // trigger data fetching and AI generation
    LaunchedEffect(Unit) {
        if (articles.isEmpty()) {
            withContext(Dispatchers.IO) {
                // Fetch news articles from GDELT
                val fetchedArticles = gdeltManager.getLatestEvents()
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (fetchedArticles.isNotEmpty()) {
                        isAiLoading = true
                    }
                }
                
                if (fetchedArticles.isNotEmpty()) {
                    // Generate intelligence summary using AI
                    val summary = aiManager.generateIntelligenceBrief(fetchedArticles)
                    withContext(Dispatchers.Main) {
                        onDataFetched(fetchedArticles, summary)
                        isAiLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onDataFetched(emptyList(), null)
                    }
                }
            }
        } else if (aiSummary == null) {
            // Case where articles exist but summary is missing
            isAiLoading = true
            withContext(Dispatchers.IO) {
                val summary = aiManager.generateIntelligenceBrief(articles)
                withContext(Dispatchers.Main) {
                    onDataFetched(articles, summary)
                    isAiLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.Black,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        "INTELLIGENCE BRIEF - $currentDate",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // the Ai briefing
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // calls the function to get AI brief and display it in thr UI
                        AiBriefSection(aiSummary, isAiLoading)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Load Events Button triggers navigation to EventsActivity
            Button(
                onClick = onLoadEvents,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(4.dp),
                enabled = !isLoading
            ) {
                Text(
                    "LOAD SOURCE EVENTS",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}


@Composable
fun AiBriefSection(summary: String?, isLoading: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "TOP SECRET // NOFORN",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.width(64.dp).height(2.dp),
                        color = Color.Red,
                        trackColor = Color.Transparent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (summary != null) {
                // Parse AI response based on expected format markers
                val parts = summary.split("[RECS]")
                val summaryText = parts.getOrNull(0)?.replace("[SUMMARY]", "")?.trim() ?: ""
                val recsText = parts.getOrNull(1)?.trim() ?: ""

                // AI Summary Display
                Text(
                    "EXECUTIVE SUMMARY",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 22.sp
                    ),
                    color = Color(0xFFE0E0E0)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Recommendations
                if (recsText.isNotEmpty()) {
                    Text(
                        "FIELD RECOMMENDATIONS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    recsText.split("\n").filter { it.isNotBlank() }.forEach { rec ->
                        Card(
                            modifier = Modifier.padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = rec,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            } else if (isLoading) {
                Text(
                    "ESTABLISHING SECURE CHANNEL...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text(
                    "SIGNAL LOST. RE-AUTHORIZE CONNECTION.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
            Text(
                "INTERNAL USE ONLY",
                modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray
            )
        }
    }
}
