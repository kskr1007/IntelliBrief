package com.example.intellibrief

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.intellibrief.ui.theme.IntelliBriefTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntelliBriefTheme {
                Navigate()
            }
        }
    }
}

@Composable
fun Navigate() {
    val navController = rememberNavController()
    
    // saved articles and summary to preserve data between screens
    var savedArticles by remember { mutableStateOf<List<GdeltArticle>>(emptyList()) }
    var savedAiSummary by remember { mutableStateOf<String?>(null) }

    NavHost(navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main_brief") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate("signup")
                }
            )
        }
        composable("signup") {
            SignUpScreen(
                onSignupSuccess = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("main_brief") {
            val context = androidx.compose.ui.platform.LocalContext.current
            MainBriefScreen(
                articles = savedArticles,
                aiSummary = savedAiSummary,
                onDataFetched = { articles, summary ->
                    // save the articles
                    savedArticles = articles
                    // save to AI summary
                    savedAiSummary = summary
                },
                // using intents to pass already loaded articles to the EventsActivity
                onLoadEvents = {
                    val intent = android.content.Intent(context, EventsActivity::class.java).apply {
                        putExtra("articles", ArrayList(savedArticles))
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}


