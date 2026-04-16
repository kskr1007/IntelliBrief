package com.example.intellibrief

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.intellibrief.ui.theme.IntelliBriefTheme
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EventsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Retrieve articles passed from MainActivity
        // Used AI to debug this as there was a problem recieving the list of articles, due to deprecation
        @Suppress("DEPRECATION")
        val articles = intent.getSerializableExtra("articles") as? List<GdeltArticle> ?: emptyList()

        enableEdgeToEdge()
        setContent {
            IntelliBriefTheme {
                EventsListScreen(
                    initialArticles = articles,
                    onBack = { finish() },
                    onGoToSaved = {
                        // intent to go to saved events activity
                        val intent = Intent(this, SavedEventsActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

// Complexity #3 req: This function writes the saved events to firebase realtime DB
fun addEventToFirebase(fbRef: String, article: GdeltArticle) {
    val database = Firebase.database
    val eventRef = database.getReference(fbRef)
    val newEventRef = eventRef.push()
    newEventRef.setValue(article)
        .addOnSuccessListener {
            Log.d("FirebaseWrite", "Event added successfully")
        }
        .addOnFailureListener { error ->
            Log.e("FirebaseWrite", "Failed to add event", error)
        }
}

@Composable
fun EventsListScreen(
    initialArticles: List<GdeltArticle>,
    onBack: () -> Unit,
    onGoToSaved: () -> Unit
) {
    // for connecting to gdelt api manager class
    val gdeltManager = remember { GdeltManager() }
    // for ui loading
    var isLoading by remember { mutableStateOf(initialArticles.isEmpty()) }
    // article list to display
    var articleList by remember { mutableStateOf(initialArticles) }

    // run on different thread to get articles. IF they weren't already loaded from the intent earlier
    LaunchedEffect(Unit) {
        if (articleList.isEmpty()) {
            withContext(Dispatchers.IO) {
                val result = gdeltManager.getLatestEvents()
                withContext(Dispatchers.Main) {
                    articleList = result
                    isLoading = false
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
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Text("<", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.source_events),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onGoToSaved) {
                        Text(stringResource(R.string.saved), color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
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
                items(articleList) { article ->
                    EventCard(article)
                }
            }
        }
    }
}

// code for each event card
@Composable
fun EventCard(article: GdeltArticle) {
    val context = LocalContext.current
    val eventSavedMessage = stringResource(R.string.event_saved)
    val loginToSaveMessage = stringResource(R.string.please_login_to_save)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Open the article URL in a web browser
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
            // Article Image (if available)
            article.socialImage?.let { imageUrl ->
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentScale = ContentScale.Crop
                    )
                }
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = article.domain.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = article.seenDate.take(8), // Assuming YYYYMMDD format
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray
                        )
                    }

                    // Save feature for firebase db
                    OutlinedButton(
                        onClick = {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid != null) {
                                addEventToFirebase("users/$uid/saved_events", article)
                                Toast.makeText(context, eventSavedMessage, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, loginToSaveMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Text(stringResource(R.string.save_action), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
