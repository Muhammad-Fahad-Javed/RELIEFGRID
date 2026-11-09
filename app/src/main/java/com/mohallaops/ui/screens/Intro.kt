package com.mohallaops.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohallaops.R
import com.mohallaops.ui.components.PrimaryButton
import com.mohallaops.ui.theme.MohallaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash screen with RELIEFGRID branding.
 * Shield icon represents the disaster-relief mission.
 * Shows for ~1.1 seconds then transitions to onboarding or main.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(600), label = "splash")
    LaunchedEffect(Unit) {
        shown = true
        delay(1500)
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MohallaColors.Navy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .alpha(alpha)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Shield icon for RELIEFGRID
            Box(
                Modifier
                    .size(88.dp)
                    .background(MohallaColors.Teal, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.HealthAndSafety,
                    contentDescription = "RELIEFGRID shield",
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.app_name),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.tagline),
                color = Color(0xFFB8C7DA),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            // Practice mode indicator
            Box(
                Modifier
                    .background(MohallaColors.Teal.copy(alpha = 0.25f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    "PRACTICE DRILL MODE",
                    color = MohallaColors.Teal,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

/**
 * Onboarding screen (3 pages) explaining RELIEFGRID's purpose.
 * Swipe or tap Continue to proceed; Skip jumps straight to the main app.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        R.string.ob1_title to R.string.ob1_body,
        R.string.ob2_title to R.string.ob2_body,
        R.string.ob3_title to R.string.ob3_body
    )
    // Icons for each onboarding page
    val pageIcons = listOf(
        Icons.Filled.HealthAndSafety,
        Icons.Filled.HealthAndSafety,
        Icons.Filled.HealthAndSafety
    )
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(MohallaColors.Background)
            .padding(24.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // RELIEFGRID wordmark
            Text(
                "RELIEFGRID",
                style = MaterialTheme.typography.labelLarge,
                color = MohallaColors.Navy,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            TextButton(onClick = onFinish) {
                Text(stringResource(R.string.skip), color = MohallaColors.TextSecondary)
            }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large page icon
                Box(
                    Modifier
                        .size(112.dp)
                        .background(MohallaColors.Teal.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(80.dp)
                            .background(MohallaColors.Navy.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = pageIcons[page],
                            contentDescription = null,
                            tint = MohallaColors.Teal,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    stringResource(pages[page].first),
                    style = MaterialTheme.typography.titleLarge,
                    color = MohallaColors.Navy,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(pages[page].second),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MohallaColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        // Page indicator dots
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 5.dp)
                        .size(if (pager.currentPage == i) 10.dp else 7.dp)
                        .background(
                            if (pager.currentPage == i) MohallaColors.Navy else MohallaColors.Border,
                            CircleShape
                        )
                )
            }
        }
        val last = pager.currentPage == pages.lastIndex
        PrimaryButton(
            text = stringResource(if (last) R.string.get_started else R.string.next),
            onClick = {
                if (last) onFinish()
                else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth()
        )
        // Disclaimer
        Spacer(Modifier.height(12.dp))
        Text(
            "Practice drill — not a live emergency system",
            style = MaterialTheme.typography.labelMedium,
            color = MohallaColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
