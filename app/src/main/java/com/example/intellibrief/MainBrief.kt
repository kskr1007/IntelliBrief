package com.example.intellibrief

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBriefScreen(onLoadEvents: () -> Unit) {
    val context = LocalContext.current
    // connect to the gdelt manager class
    val gdeltManager = remember { GdeltManager() }
    //connect to groq manager
    val aiManager = remember { AiManager(apiKey = com.example.intellibrief.BuildConfig.GROQ_API_KEY) }
    
    // ai summary that will be populated by the groq request
    var aiSummary by remember { mutableStateOf<String?>(null) }
    // boolean for loading symbol on/off
    var isLoading by remember { mutableStateOf(true) }
    // boolean for loading symbol for ai summary on/off
    var isAiLoading by remember { mutableStateOf(false) }

    // run the request on a different thread
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val result = gdeltManager.getLatestEvents()
            withContext(Dispatchers.Main) {
                isLoading = false
                if (result.isNotEmpty()) {
                    isAiLoading = true
                }
            }
            
            if (result.isNotEmpty()) {
                val summary = aiManager.generateIntelligenceBrief(result)
                withContext(Dispatchers.Main) {
                    aiSummary = summary
                    isAiLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "INTELLIGENCE BRIEF", 
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212) // Dark background for CIA feel
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Main content area with briefing
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
                        AiBriefSection(aiSummary, isAiLoading)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Load Events Button at the bottom
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsListScreen(onBack: () -> Unit) {
    val gdeltManager = remember { GdeltManager() }
    var articles by remember { mutableStateOf<List<GdeltArticle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val result = gdeltManager.getLatestEvents()
            withContext(Dispatchers.Main) {
                articles = result
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SOURCE EVENTS", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // Using a simple text-based back button for now
                        Text("<", color = Color.White, modifier = Modifier.padding(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(articles) { article ->
                    EventCard(article)
                }
            }
        }
    }
}

// Ai briefing using groq ai
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
                    "DAILY INTELLIGENCE BRIEFING",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.width(64.dp).height(2.dp),
                        color = Color.Red,
                        trackColor = Color.Transparent
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    ),
                    color = Color(0xFFE0E0E0)
                )
            } else if (isLoading) {
                Text(
                    "DECRYPTING LATEST INTEL...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text(
                    "UNABLE TO GENERATE BRIEF. CHECK CONNECTIVITY.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// code for each event card
@Composable
fun EventCard(article: GdeltArticle) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, article.url.toUri())
                context.startActivity(intent)
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF252525),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            article.socialImage?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.domain.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = article.seenDate.take(8),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}
