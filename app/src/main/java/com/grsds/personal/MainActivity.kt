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

    // 消息与「更多功能」里用字符串表示目标页，集中在这里映射成路由，
    // 避免每个入口各写一套 if-else。
    fun navigateByTarget(target: String) {
        when (target) {
            MessageCenter.TARGET_ANNUAL -> navigate(AppScreen.AnnualReconciliation)
            MessageCenter.TARGET_DEDUCTIONS -> navigate(AppScreen.Deductions)
            MessageCenter.TARGET_INCOME -> navigate(AppScreen.TaxDetails)
            MessageCenter.TARGET_RECORDS -> navigate(AppScreen.DeclarationRecords)
            MessageCenter.TARGET_SECURITY -> navigate(AppScreen.Settings)
            "issue" -> navigate(AppScreen.TaxRecordIssue)
            "correction" -> navigate(AppScreen.Correction())
            "profile" -> navigate(AppScreen.ProfileInfo)
            "bankcards" -> navigate(AppScreen.BankCards)
            "family" -> navigate(AppScreen.FamilyMembers)
            "employment" -> navigate(AppScreen.EmploymentInfo)
        }
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
                onOpenLogin = { navigate(AppScreen.Login) },
                onOpenCorrection = { navigate(AppScreen.Correction()) },
                onOpenProfileInfo = { navigate(AppScreen.ProfileInfo) },
                onOpenBankCards = { navigate(AppScreen.BankCards) },
                onOpenMoreServices = { navigate(AppScreen.MoreServices) },
                onOpenArticle = { id -> navigate(AppScreen.ArticleDetail(id)) },
                onOpenArticleList = { category -> navigate(AppScreen.ArticleList(category)) },
                onOpenMessage = { id -> navigate(AppScreen.MessageDetail(id)) },
                onOpenRefundProgress = { id -> navigate(AppScreen.RefundProgress(id)) },
                onOpenTaxPayment = { id -> navigate(AppScreen.TaxPayment(id)) },
                onOpenFamilyMembers = { navigate(AppScreen.FamilyMembers) },
                onOpenEmploymentInfo = { navigate(AppScreen.EmploymentInfo) }
            )

            AppScreen.MoreServices -> MoreServicesScreen(
                onBack = { goBack() },
                onLaunch = { target -> goBack(); navigateByTarget(target) },
                modifier = modifier
            )

            is AppScreen.MessageDetail -> MessageDetailScreen(
                messageId = current.messageId,
                onBack = { goBack() },
                onOpenAction = { target ->
                    goBack()
                    navigateByTarget(target)
                },
                modifier = modifier
            )

            is AppScreen.ArticleDetail -> ArticleDetailScreen(
                articleId = current.articleId,
                onBack = { goBack() },
                modifier = modifier
            )

            is AppScreen.ArticleList -> ArticleListScreen(
                initialCategory = current.category,
                onBack = { goBack() },
                onOpenArticle = { id -> navigate(AppScreen.ArticleDetail(id)) },
                modifier = modifier
            )

            AppScreen.DeclarationRecords -> DeclarationRecordsScreen(
                onBack = { goBack() },
                onOpenReconciliation = { navigate(AppScreen.AnnualReconciliation) },
                onOpenCorrection = { id -> navigate(AppScreen.Correction(id)) },
                onOpenRefundProgress = { id -> navigate(AppScreen.RefundProgress(id)) },
                onOpenTaxPayment = { id -> navigate(AppScreen.TaxPayment(id)) },
                onOpenCalculation = { year -> navigate(AppScreen.CalculationDetail(year)) },
                modifier = modifier
            )

            is AppScreen.Correction -> CorrectionScreen(
                preselectedId = current.preselectedId,
                onBack = { goBack() },
                modifier = modifier
            )

            AppScreen.ProfileInfo -> ProfileInfoScreen(
                onBack = { goBack() },
                onOpenLogin = { navigate(AppScreen.Login) },
                modifier = modifier
            )

            AppScreen.BankCards -> BankCardScreen(
                onBack = { goBack() },
                onOpenLogin = { navigate(AppScreen.Login) },
                modifier = modifier
            )

            AppScreen.FamilyMembers -> FamilyMemberScreen(
                onBack = { goBack() },
                onOpenLogin = { navigate(AppScreen.Login) },
                modifier = modifier
            )

            AppScreen.EmploymentInfo -> EmploymentInfoScreen(
                onBack = { goBack() },
                onOpenLogin = { navigate(AppScreen.Login) },
                modifier = modifier
            )

            AppScreen.Settings -> SettingsScreen(
                onBack = { goBack() },
                onOpenBatchCreate = { navigate(AppScreen.BatchCreate) },
                modifier = modifier
            )

            AppScreen.TaxRecordIssue -> TaxRecordIssueScreen(
                onBack = { goBack() },
                modifier = modifier,
                onOpenRecord = { year, issuedAt, code ->
                    navigate(AppScreen.TaxRecordDetail(year, issuedAt, code))
                }
            )

            is AppScreen.TaxRecordDetail -> TaxRecordDetailScreen(
                year = current.year,
                issuedAt = current.issuedAt,
                verifyCode = current.verifyCode,
                onBack = { goBack() },
                modifier = modifier
            )

            AppScreen.AnnualReconciliation -> AnnualReconciliationScreen(
                onBack = { reconcileStep = 0; goBack() },
                onOpenDeductions = { navigate(AppScreen.Deductions) },
                step = reconcileStep,
                onStepChange = { reconcileStep = it },
                modifier = modifier,
                onOpenBankCards = { navigate(AppScreen.BankCards) },
                onOpenRefundProgress = { id -> navigate(AppScreen.RefundProgress(id)) },
                onOpenPayment = { id -> navigate(AppScreen.TaxPayment(id)) },
                onOpenCalculation = { year -> navigate(AppScreen.CalculationDetail(year)) }
            )

            is AppScreen.CalculationDetail -> CalculationDetailScreen(
                year = current.year,
                onBack = { goBack() },
                modifier = modifier
            )

            is AppScreen.RefundProgress -> RefundProgressScreen(
                declarationId = current.declarationId,
                onBack = { goBack() },
                onOpenBankCards = { navigate(AppScreen.BankCards) },
                modifier = modifier
            )

            is AppScreen.TaxPayment -> TaxPaymentScreen(
                declarationId = current.declarationId,
                onBack = { goBack() },
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
    data object ProfileInfo : AppScreen
    data object BankCards : AppScreen
    data object FamilyMembers : AppScreen
    data object EmploymentInfo : AppScreen
    data object MoreServices : AppScreen
    data class MessageDetail(val messageId: String?) : AppScreen
    data class ArticleDetail(val articleId: String?) : AppScreen
    data class ArticleList(val category: Int) : AppScreen
    data class Correction(val preselectedId: String? = null) : AppScreen
    data class TaxRecordDetail(val year: Int, val issuedAt: Long, val verifyCode: String) : AppScreen
    data class RefundProgress(val declarationId: String) : AppScreen
    data class TaxPayment(val declarationId: String) : AppScreen
    data class CalculationDetail(val year: Int) : AppScreen
    data class QueryResult(val year: Int, val incomeTypes: List<String>) : AppScreen
    data class RecordDetail(val recordId: String) : AppScreen
}
