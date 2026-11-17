package com.grsds.personal

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.grsds.personal.ui.theme.MyApplicationTheme
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
    val isInPreview = LocalInspectionMode.current
    var showHome by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    // 汇算向导的步骤放在这里：从「专项附加扣除」返回时页面会离开组合，
    // 状态留在页面内部会退回第 1 步，用户得重新点一遍。
    var reconcileStep by rememberSaveable { mutableIntStateOf(0) }
    val backStack = remember { mutableStateListOf<AppScreen>(AppScreen.Home) }

    fun navigate(screen: AppScreen) {
        backStack.add(screen)
    }

    fun goBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    LaunchedEffect(view, isInPreview) {
        if (isInPreview) return@LaunchedEffect

        // The splash is a full-bleed white page, so the status bar is simply hidden
        // while it is on screen and brought back together with the home page.
        delay(1_000)
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

    if (showHome) {
        val currentScreen = backStack.last()

        // 首页和「我的」顶部是蓝色渐变，状态栏用白色图标；其余页面都是白底，
        // 必须换成深色图标，否则时间、电量白字白底，几乎看不见。
        val lightStatusIcons = currentScreen == AppScreen.Home && (selectedTab == 0 || selectedTab == 4)
        LaunchedEffect(currentScreen, lightStatusIcons) {
            if (isInPreview) return@LaunchedEffect
            val activity = view.context as? Activity ?: return@LaunchedEffect
            WindowCompat.getInsetsController(activity.window, view)
                .isAppearanceLightStatusBars = !lightStatusIcons
        }

        when (val current = currentScreen) {
            AppScreen.Home -> HomeScreen(
                modifier = modifier,
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                onOpenIncomeTaxDetails = { navigate(AppScreen.TaxDetails) },
                onOpenTaxRecordIssue = { navigate(AppScreen.TaxRecordIssue) },
                onOpenAnnualReconciliation = { navigate(AppScreen.AnnualReconciliation) },
                onOpenDeductions = { navigate(AppScreen.Deductions) },
                onOpenDeclarationRecords = { navigate(AppScreen.DeclarationRecords) },
                onOpenSettings = { navigate(AppScreen.Settings) },
                onOpenLogin = { navigate(AppScreen.Login) }
            )

            AppScreen.DeclarationRecords -> DeclarationRecordsScreen(
                onBack = { goBack() },
                onOpenReconciliation = { navigate(AppScreen.AnnualReconciliation) },
                modifier = modifier
            )

            AppScreen.Settings -> SettingsScreen(
                onBack = { goBack() },
                onOpenBatchCreate = { navigate(AppScreen.BatchCreate) },
                modifier = modifier
            )

            AppScreen.TaxRecordIssue -> TaxRecordIssueScreen(
                onBack = { goBack() },
                modifier = modifier
            )

            AppScreen.AnnualReconciliation -> AnnualReconciliationScreen(
                onBack = { reconcileStep = 0; goBack() },
                onOpenDeductions = { navigate(AppScreen.Deductions) },
                step = reconcileStep,
                onStepChange = { reconcileStep = it },
                modifier = modifier
            )

            AppScreen.Deductions -> DeductionsScreen(
                onBack = { goBack() },
                modifier = modifier
            )

            AppScreen.Login -> LoginScreen(
                onBack = { goBack() },
                onLoginSuccess = { goBack() },
                modifier = modifier
            )

            AppScreen.TaxDetails -> IncomeTaxDetailsScreen(
                onBack = { goBack() },
                onOpenBatchCreate = { navigate(AppScreen.BatchCreate) },
                onQuery = { year, types -> navigate(AppScreen.QueryResult(year, types)) },
                modifier = modifier
            )

            AppScreen.BatchCreate -> BatchCreateScreen(
                onBack = { goBack() },
                modifier = modifier
            )

            is AppScreen.QueryResult -> QueryResultScreen(
                year = current.year,
                incomeTypes = current.incomeTypes,
                onBack = { goBack() },
                onOpenRecord = { recordId -> navigate(AppScreen.RecordDetail(recordId)) },
                onOpenBatchCreate = { navigate(AppScreen.BatchCreate) },
                modifier = modifier
            )

            is AppScreen.RecordDetail -> RecordDetailScreen(
                recordId = current.recordId,
                onBack = { goBack() },
                modifier = modifier
            )
        }
    } else {
        Image(
            painter = painterResource(R.drawable.cover),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxSize()
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

/** 应用内页面，配合 [backStack] 形成简单的返回栈，替代多个布尔量拼出来的导航。 */
private sealed interface AppScreen {
    data object Home : AppScreen
    data object TaxDetails : AppScreen
    data object BatchCreate : AppScreen
    data object TaxRecordIssue : AppScreen
    data object AnnualReconciliation : AppScreen
    data object Deductions : AppScreen
    data object DeclarationRecords : AppScreen
    data object Settings : AppScreen
    data object Login : AppScreen
    data class QueryResult(val year: Int, val incomeTypes: List<String>) : AppScreen
    data class RecordDetail(val recordId: String) : AppScreen
}
