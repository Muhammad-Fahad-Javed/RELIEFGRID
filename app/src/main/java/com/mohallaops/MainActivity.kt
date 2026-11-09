package com.mohallaops

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mohallaops.ui.AppRoot
import com.mohallaops.ui.MainViewModel
import com.mohallaops.ui.screens.OnboardingScreen
import com.mohallaops.ui.screens.SplashScreen
import com.mohallaops.ui.theme.MohallaTheme

private enum class Stage { SPLASH, ONBOARDING, MAIN }

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("mohalla_prefs", Context.MODE_PRIVATE)
        setContent {
            MohallaTheme {
                var stage by remember { mutableStateOf(Stage.SPLASH) }
                when (stage) {
                    Stage.SPLASH -> SplashScreen {
                        stage = if (prefs.getBoolean("onboarded", false)) Stage.MAIN else Stage.ONBOARDING
                    }
                    Stage.ONBOARDING -> OnboardingScreen {
                        prefs.edit().putBoolean("onboarded", true).apply()
                        stage = Stage.MAIN
                    }
                    Stage.MAIN -> AppRoot(vm)
                }
            }
        }
    }
}
