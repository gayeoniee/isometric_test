package com.daengs.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.daengs.app.ui.home.HomeScreen
import com.daengs.app.ui.theme.DaengsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaengsTheme {
                HomeScreen()
            }
        }
    }
}
