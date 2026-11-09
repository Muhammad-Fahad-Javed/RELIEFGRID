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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohallaops.R
import com.mohallaops.ui.components.PrimaryButton
import com.mohallaops.ui.theme.MohallaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onDone: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(600), label = "splash")
    LaunchedEffect(Unit) {
        shown = true
        delay(1100)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(MohallaColors.Navy), contentAlignment = Alignment.Center) {
        Column(Modifier.alpha(alpha).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).background(MohallaColors.Teal, CircleShape), contentAlignment = Alignment.Center) {
                Text("M", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.app_name), color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.tagline), color = Color(0xFFB8C7DA), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        R.string.ob1_title to R.string.ob1_body,
        R.string.ob2_title to R.string.ob2_body,
        R.string.ob3_title to R.string.ob3_body
    )
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(MohallaColors.Background).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text(stringResource(R.string.skip), color = MohallaColors.TextSecondary) }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(96.dp).background(MohallaColors.Teal.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Text("${page + 1}", style = MaterialTheme.typography.headlineMedium, color = MohallaColors.Teal)
                }
                Spacer(Modifier.height(24.dp))
                Text(stringResource(pages[page].first), style = MaterialTheme.typography.titleLarge, color = MohallaColors.Navy, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(pages[page].second), style = MaterialTheme.typography.bodyLarge, color = MohallaColors.TextSecondary, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pages.size) { i ->
                Box(
                    Modifier.padding(4.dp).size(if (pager.currentPage == i) 10.dp else 8.dp)
                        .background(if (pager.currentPage == i) MohallaColors.Navy else MohallaColors.Border, CircleShape)
                )
            }
        }
        val last = pager.currentPage == pages.lastIndex
        PrimaryButton(
            text = stringResource(if (last) R.string.get_started else R.string.next),
            onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
