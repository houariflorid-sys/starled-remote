package com.starled.remote.hk320df

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.starled.remote.hk320df.ui.RemoteScreen
import com.starled.remote.hk320df.ui.RemoteSetupScreen
import com.starled.remote.hk320df.ui.theme.StarLEDHK320DFTheme

class MainActivity : ComponentActivity() {

    private lateinit var controller: StarLEDInfraredController
    private lateinit var repository: RemoteCommandRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controller = StarLEDInfraredController(this)
        repository = RemoteCommandRepository(this)

        setContent {
            StarLEDHK320DFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("remote") }

                    when (currentScreen) {
                        "remote" -> {
                            RemoteScreen(
                                controller = controller,
                                repository = repository,
                                onNavigateToDiscovery = { currentScreen = "discovery" }
                            )
                        }
                        "discovery" -> {
                            RemoteSetupScreen(
                                controller = controller,
                                repository = repository,
                                onNavigateBack = { currentScreen = "remote" }
                            )
                        }
                    }
                }
            }
        }
    }
}
