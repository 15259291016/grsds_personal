package com.example.myapplication

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.statusBars())
            isAppearanceLightStatusBars = true
        }
        setContent {
            MyApplicationTheme(dynamicColor = false, darkTheme = false) {
                CoverImage()
            }
        }
    }
}

@Composable
fun CoverImage(modifier: Modifier = Modifier) {
    val view = LocalView.current
    val density = LocalDensity.current
    val isInPreview = LocalInspectionMode.current
    var statusBarHeightPx by remember { mutableIntStateOf(0) }
    var statusBarHidden by remember { mutableStateOf(false) }
    var showHome by remember { mutableStateOf(false) }
    var showIncomeTaxDetails by remember { mutableStateOf(false) }

    LaunchedEffect(view, isInPreview) {
        if (isInPreview) return@LaunchedEffect

        // Wait until the first window insets have arrived, then keep the original
        // status-bar height as a black area after its icons are hidden.
        delay(100)
        statusBarHeightPx = ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())
            ?.top
            ?: 0

        delay(900)
        statusBarHidden = true
        val activity = view.context as? Activity ?: return@LaunchedEffect
        WindowCompat.getInsetsController(activity.window, view)
            .hide(WindowInsetsCompat.Type.statusBars())

        delay(1_000)
        showHome = true
        WindowCompat.getInsetsController(activity.window, view).apply {
            show(WindowInsetsCompat.Type.statusBars())
            isAppearanceLightStatusBars = false
        }
    }

    val hiddenStatusBarPadding = with(density) {
        if (statusBarHidden && !showHome) statusBarHeightPx.toDp() else 0.toDp()
    }

    if (showHome) {
        if (showIncomeTaxDetails) {
            IncomeTaxDetailsScreen(
                onBack = { showIncomeTaxDetails = false },
                modifier = modifier
            )
        } else {
            HomeScreen(
                modifier = modifier,
                onOpenIncomeTaxDetails = { showIncomeTaxDetails = true }
            )
        }
    } else {
        Image(
            painter = painterResource(R.drawable.cover),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(top = hiddenStatusBarPadding)
                .background(Color.White)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CoverImagePreview() {
    MyApplicationTheme {
        CoverImage()
    }
}
