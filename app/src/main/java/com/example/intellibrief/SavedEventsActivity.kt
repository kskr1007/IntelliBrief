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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class SavedEventsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntelliBriefTheme {
                SavedEventsScreen(onBack = { finish() })
            }
        }
    }
}

// Complexity req #3: This function reads from Firebase realtime db
@Composable
fun SavedEventsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val failedToLoadMessage = stringResource(R.string.failed_to_load)
    val savedEvents = remember { mutableStateListOf<GdeltArticle>() }
    var isLoading by remember { mutableStateOf(true) }
    
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val fbRef = if (uid != null) "users/$uid/saved_events" else null

    // read from firebase realtime db, on different thread
    LaunchedEffect(fbRef) {
        if (fbRef == null) {
            isLoading = false
            return@LaunchedEffect
        }
        
        val eventRef = Firebase.database.getReference(fbRef)

        eventRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isLoading = false
                if (!snapshot.exists()) {
                    savedEvents.clear()
                    return
                }

                savedEvents.clear()
                snapshot.children
                    .mapNotNull { it.getValue(GdeltArticle::class.java) }
                    .forEach { savedEvents.add(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                Toast.makeText(context, failedToLoadMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    // UI for saved events activity
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
                        stringResource(R.string.saved_events),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
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
        } else if (savedEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_saved_events), color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(savedEvents) { article ->
                    SavedEventCard(article, onDelete = {
                        // delete logic for firebase db
                        if (uid != null) {
                            val eventRef = Firebase.database.getReference("users/$uid/saved_events")
                            // finding event using key
                            eventRef.orderByChild("title").equalTo(article.title)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        for (child in snapshot.children) {
                                            child.ref.removeValue()
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                    })
                }
            }
        }
    }
}

// UI for saved event cards
@Composable
fun SavedEventCard(article: GdeltArticle, onDelete: () -> Unit) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to remove this intelligence report from your database?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("DELETE", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL")
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }

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
                            text = article.seenDate.take(8),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray
                        )
                    }

                    // Delete Button
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Text("🗑️", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
