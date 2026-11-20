package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.auth.configureAppCheck
import com.example.ui.DoveAppRoot
import com.example.ui.theme.DoveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase App Check before setContent and Firebase AI/Firestore usage
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            DoveTheme {
                DoveAppRoot()
            }
        }
    }
}
