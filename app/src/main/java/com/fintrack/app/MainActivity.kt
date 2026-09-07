package com.fintrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fintrack.app.navigation.FinTrackApp
import com.fintrack.app.ui.theme.FinTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinTrackRoot()
        }
    }
}

@Composable
private fun FinTrackRoot() {
    FinTrackTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            FinTrackApp()
        }
    }
}
