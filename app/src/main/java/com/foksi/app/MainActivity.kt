@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.foksi.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import com.foksi.app.di.AppGraph
import com.foksi.app.notifications.Extras
import com.foksi.app.ui.components.collectSettings
import com.foksi.app.ui.navigation.FoksiNavHost
import com.foksi.app.ui.navigation.Routes
import com.foksi.app.ui.screens.onboarding.OnboardingScreen
import com.foksi.app.ui.theme.FoksiTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.init(this)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val settings by collectSettings()
            var navController by remember { mutableStateOf<NavHostController?>(null) }

            FoksiTheme(settings) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (!settings.onboardingDone) {
                        OnboardingScreen(
                            onFinish = {
                                lifecycleScope.launch {
                                    AppGraph.settingsRepository.setOnboardingDone(true)
                                }
                            }
                        )
                    } else {
                        NotificationPermissionGate()
                        FoksiNavHost(onReady = { navController = it })

                        val controller = navController
                        val route = pendingRoute
                        LaunchedEffect(controller, route) {
                            if (controller != null && route != null) {
                                runCatching { controller.navigate(route) }
                                pendingRoute = null
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        pendingRoute = when (intent?.action) {
            Extras.ACTION_OPEN_EVENT -> {
                val id = intent.getLongExtra(Extras.EXTRA_EVENT_ID, -1L)
                if (id > 0) Routes.detail(id) else null
            }

            Extras.ACTION_QUICK_ADD -> Routes.editor()
            Extras.ACTION_OPEN_CALENDAR -> Routes.CALENDAR
            Extras.ACTION_OPEN_NOTES -> Routes.TASKS
            else -> null
        }
    }
}

/** Asks for the Android 13+ notification permission exactly once, on first launch. */
@androidx.compose.runtime.Composable
private fun NotificationPermissionGate() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (!asked) {
            asked = true
            runCatching { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }
}
