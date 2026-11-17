package com.grsds.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.ui.Glyph
import com.grsds.personal.ui.GlyphIcon
import com.grsds.personal.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HomeBlue = Color(0xFF168AF4)
private val DeepBlue = Color(0xFF147EEA)
private val PageBackground = Color(0xFFF2F4F8)
private val PrimaryText = Color(0xFF171B22)
private val SecondaryText = Color(0xFF7D8490)

private data class ServiceItem(
    /** 稳定的跳转标识，与展示文案解耦，避免用文案做分支判断 */
    val id: String,
    val glyph: Glyph,
    val color: Color,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val action: Int
)

private val serviceItems = listOf(
    ServiceItem("annual", Glyph.Chart, Color(0xFF2E7BF6), R.string.home_36, R.string.home_37, R.string.home_38),
    ServiceItem("income", Glyph.Receipt, Color(0xFF8B5CF6), R.string.home_40, R.string.home_41, R.string.home_42),
    ServiceItem("issue", Glyph.Printer, Color(0xFF18C7AC), R.string.home_44, R.string.home_45, R.string.home_46),
    ServiceItem("more", Glyph.Grid, Color(0xFF2A9CF2), R.string.home_48, R.string.home_49, R.string.home_50)
)

private val bottomTabs = listOf(
    Glyph.Home to R.string.home_51,
    Glyph.Todo to R.string.home_52,
    Glyph.Services to R.string.home_53,
    Glyph.Message to R.string.home_54,
    Glyph.Smiley to R.string.home_55
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    selectedTab: Int = 0,
    onSelectTab: (Int) -> Unit = {},
    onOpenIncomeTaxDetails: () -> Unit = {},
    onOpenTaxRecordIssue: () -> Unit = {},
    onOpenAnnualReconciliation: () -> Unit = {},
    onOpenDeductions: () -> Unit = {},
    onOpenDeclarationRecords: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenLogin: () -> Unit = {},
    onOpenCorrection: () -> Unit = {},
    onOpenProfileInfo: () -> Unit = {},
    onOpenBankCards: () -> Unit = {},
    onOpenArticle: (String) -> Unit = {},
    onOpenArticleList: (Int) -> Unit = {},
    onOpenMessage: (String) -> Unit = {},
    onOpenMoreServices: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PageBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                HomeBottomNavigation(
                    selectedTab = selectedTab,
                    onSelected = onSelectTab
                )
            }
        ) { innerPadding ->
            when (selectedTab) {
                0 -> HomeFeed(
                    modifier = Modifier.padding(innerPadding),
                    onAction = showMessage,
                    onOpenIncomeTaxDetails = onOpenIncomeTaxDetails,
                    onOpenTaxRecordIssue = onOpenTaxRecordIssue,
                    onOpenAnnualReconciliation = onOpenAnnualReconciliation,
                    onOpenDeductions = onOpenDeductions,
                    onOpenArticle = onOpenArticle,
                    onOpenArticleList = onOpenArticleList,
                    onOpenMoreServices = onOpenMoreServices
                )
                1 -> TodoTab(
                    onAction = showMessage,
                    onOpenAnnualReconciliation = onOpenAnnualReconciliation,
                    onOpenDeductions = onOpenDeductions,
                    onOpenDeclarationRecords = onOpenDeclarationRecords,
                    onOpenBankCards = onOpenBankCards,
                    modifier = Modifier.padding(innerPadding).statusBarsPadding()
                )
                2 -> ServicesTab(
                    onAction = showMessage,
                    onOpenIncomeTaxDetails = onOpenIncomeTaxDetails,
                    onOpenTaxRecordIssue = onOpenTaxRecordIssue,
                    onOpenAnnualReconciliation = onOpenAnnualReconciliation,
                    onOpenDeductions = onOpenDeductions,
                    onOpenDeclarationRecords = onOpenDeclarationRecords,
                    onOpenCorrection = onOpenCorrection,
                    modifier = Modifier.padding(innerPadding).statusBarsPadding()
                )
                3 -> MessagesTab(
                    onOpenMessage = onOpenMessage,
                    modifier = Modifier.padding(innerPadding).statusBarsPadding()
                )
                else -> ProfileTab(
                    onAction = showMessage,
                    onOpenIncomeTaxDetails = onOpenIncomeTaxDetails,
                    onOpenLogin = onOpenLogin,
                    onOpenSettings = onOpenSettings,
                    onOpenProfileInfo = onOpenProfileInfo,
                    onOpenBankCards = onOpenBankCards,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun HomeFeed(
    onAction: (String) -> Unit,
    onOpenIncomeTaxDetails: () -> Unit,
    onOpenTaxRecordIssue: () -> Unit,
    onOpenAnnualReconciliation: () -> Unit,
    onOpenDeductions: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenArticleList: (Int) -> Unit,
    onOpenMoreServices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableIntStateOf(0) }
    val searchHintBlank = context.getString(R.string.home_01)
    val searchHintQuery = context.getString(R.string.home_02)
    val scanHint = context.getString(R.string.home_03)
    val promoHint = context.getString(R.string.home_06)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        PersistentSearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = {
                onAction(if (query.isBlank()) searchHintBlank else searchHintQuery)
            },
            onScan = { onAction(scanHint) }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)
        ) {
            item {
                HomeHeader(
                    onAction = onAction,
                    onOpenAnnualReconciliation = onOpenAnnualReconciliation
                )
            }
            item {
                Text(
                    text = stringResource(R.string.home_04),
                    color = PrimaryText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 12.dp, top = 26.dp, bottom = 12.dp)
                )
            }
            item {
                DeductionCard(
                    // 「相关政策」直接进政策解读列表，不再弹“暂不可用”
                    onPolicyClick = { onOpenArticleList(3) },
                    onFillClick = onOpenDeductions,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            item {
                ServiceCards(
                    onAction = onAction,
                    onOpenIncomeTaxDetails = onOpenIncomeTaxDetails,
                    onOpenTaxRecordIssue = onOpenTaxRecordIssue,
                    onOpenAnnualReconciliation = onOpenAnnualReconciliation,
                    onOpenMoreServices = onOpenMoreServices
                )
            }
            item {
                PromoBanner(
                    onClick = { onAction(unavailableHint(context, promoHint)) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp)
                )
            }
            item {
                ArticleCategories(
                    selected = selectedCategory,
                    onSelected = { selectedCategory = it }
                )
            }
            val articles = ArticleCenter.byCategory(selectedCategory)
            items(articles.size) { index ->
                ArticleCard(
                    article = articles[index],
                    onClick = { onOpenArticle(articles[index].id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                )
            }
            item {
                Text(
                    stringResource(R.string.home_07),
                    color = SecondaryText,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenArticleList(selectedCategory) }
                        .padding(vertical = 22.dp)
                )
            }
        }
    }
}

@Composable
private fun PersistentSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScan: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepBlue)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                        .padding(horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchGlyph()
                    Spacer(Modifier.width(7.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                stringResource(R.string.home_09),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onScan)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            ScanGlyph()
        }
    }
}

@Composable
private fun SearchGlyph() {
    Canvas(Modifier.size(20.dp)) {
        val stroke = 1.8.dp.toPx()
        drawCircle(
            color = Color.White,
            radius = size.minDimension * 0.29f,
            center = Offset(size.width * 0.42f, size.height * 0.42f),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.64f, size.height * 0.64f),
            end = Offset(size.width * 0.88f, size.height * 0.88f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ScanGlyph() {
    Canvas(Modifier.size(28.dp)) {
        val stroke = 2.dp.toPx()
        val edge = 2.dp.toPx()
        val arm = 8.dp.toPx()
        val right = size.width - edge
        val bottom = size.height - edge

        drawLine(Color.White, Offset(edge, edge), Offset(edge + arm, edge), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(edge, edge), Offset(edge, edge + arm), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(right, edge), Offset(right - arm, edge), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(right, edge), Offset(right, edge + arm), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(edge, bottom), Offset(edge + arm, bottom), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(edge, bottom), Offset(edge, bottom - arm), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(right, bottom), Offset(right - arm, bottom), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(right, bottom), Offset(right, bottom - arm), stroke, StrokeCap.Round)
    }
}

@Composable
private fun HomeHeader(
    onAction: (String) -> Unit,
    onOpenAnnualReconciliation: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1689F5), Color(0xFF61A9F7), Color(0xFFF5F7FB))
                )
            )
            .padding(horizontal = 12.dp)
    ) {
        Surface(
            color = Color(0xFFFFF6E5),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(40.dp)
                .clickable { onAction(unavailableHint(context, context.getString(R.string.home_10))) }
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlyphIcon(Glyph.Bell, tint = Color(0xFFFF8A2B), size = 20.dp, strokeWidth = 1.7.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.home_11),
                    color = Color(0xFFA8630B),
                    // 13sp 时这一整句刚好放得下，不会触发跑马灯把首字裁掉
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(iterations = Int.MAX_VALUE)
                )
            }
        }

        AnnualSettlementCard(
            onEnter = onOpenAnnualReconciliation,
            modifier = Modifier.padding(top = 10.dp, bottom = 5.dp)
        )
    }
}

@Composable
private fun AnnualSettlementCard(onEnter: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(204.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    listOf(Color(0xFFFFFFFF), Color(0xFFEDF4FE), Color(0xFFDCEBFC)),
                    start = Offset.Zero,
                    end = Offset(1100f, 520f)
                )
            )
        ) {
            SettlementIllustration(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 6.dp)
                    .size(width = 136.dp, height = 138.dp)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.home_13),
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.home_14),
                    color = Color(0xFF3C4654),
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                    modifier = Modifier.fillMaxWidth(0.72f)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onEnter,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E86F2)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .width(168.dp)
                        .height(46.dp)
                ) {
                    Text(stringResource(R.string.home_15), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
        }
    }
}

/**
 * 年度汇算大卡右侧的插画：淡蓝底、倾斜的申报表卡片、一枚通过标记。
 * 官方这里是人物插画，我们用同色系几何插画占位——比原来写死的「税务服务」圆形徽章自然得多。
 */
@Composable
private fun SettlementIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(Color(0xFFD5E7FB), radius = w * 0.42f, center = Offset(w * 0.52f, h * 0.60f))
        drawCircle(Color(0xFFE9F3FF), radius = w * 0.26f, center = Offset(w * 0.26f, h * 0.30f))

        rotate(degrees = -9f, pivot = Offset(w * 0.50f, h * 0.54f)) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.26f, h * 0.20f),
                size = Size(w * 0.44f, h * 0.58f),
                cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
            )
            drawRoundRect(
                color = Color(0xFF3D8CF6),
                topLeft = Offset(w * 0.26f, h * 0.20f),
                size = Size(w * 0.44f, h * 0.11f),
                cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
            )
            drawRoundRect(
                color = Color(0xFF3D8CF6),
                topLeft = Offset(w * 0.26f, h * 0.20f + h * 0.05f),
                size = Size(w * 0.44f, h * 0.06f)
            )
            val line = Color(0xFFD3E3F8)
            listOf(0.40f, 0.48f, 0.56f).forEachIndexed { index, y ->
                drawRoundRect(
                    color = line,
                    topLeft = Offset(w * 0.33f, h * y),
                    size = Size(w * (if (index == 2) 0.18f else 0.30f), h * 0.028f),
                    cornerRadius = CornerRadius(h * 0.014f, h * 0.014f)
                )
            }
            drawRoundRect(
                color = Color(0xFFBFDCFB),
                topLeft = Offset(w * 0.33f, h * 0.63f),
                size = Size(w * 0.16f, h * 0.06f),
                cornerRadius = CornerRadius(h * 0.03f, h * 0.03f)
            )
        }

        val badge = Offset(w * 0.77f, h * 0.26f)
        drawCircle(Color.White, radius = w * 0.13f, center = badge)
        val stroke = w * 0.022f
        drawLine(
            Color(0xFF2E86F2),
            Offset(badge.x - w * 0.055f, badge.y),
            Offset(badge.x - w * 0.012f, badge.y + w * 0.045f),
            stroke, StrokeCap.Round
        )
        drawLine(
            Color(0xFF2E86F2),
            Offset(badge.x - w * 0.012f, badge.y + w * 0.045f),
            Offset(badge.x + w * 0.058f, badge.y - w * 0.038f),
            stroke, StrokeCap.Round
        )

        drawCircle(Color(0xFFB6D5FA), radius = w * 0.018f, center = Offset(w * 0.16f, h * 0.72f))
        drawCircle(Color(0xFFCBE2FA), radius = w * 0.012f, center = Offset(w * 0.86f, h * 0.62f))
    }
}

@Composable
private fun DeductionCard(
    onPolicyClick: () -> Unit,
    onFillClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF5CA8F8), Color(0xFF2D86F2)),
                    start = Offset.Zero,
                    end = Offset(900f, 420f)
                )
            )
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(26.dp)
                        .background(Color(0xFFFF4A45), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GlyphIcon(Glyph.Flame, tint = Color.White, size = 15.dp, strokeWidth = 1.6.dp)
                }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_17), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Text(
                stringResource(R.string.home_19),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 26.dp)
            )
            Surface(
                color = Color.White.copy(alpha = 0.97f),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(onClick = onFillClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarDot(Color(0xFF4A91F0), Color(0xFF8FC4FF))
                    AvatarDot(Color(0xFFF08AA8), Color(0xFFFFC2D2), overlap = 8.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.home_20), color = Color(0xFF4B5360), fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .rotate(12f)
                            .width(1.4.dp)
                            .height(22.dp)
                            .background(Color(0xFFE1E5EB))
                    )
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.home_21), color = PrimaryText, fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
            }
        }

        // 右上角「相关政策」的斜切标签
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(148.dp)
                .height(34.dp)
                .drawBehind {
                    val path = Path().apply {
                        moveTo(size.width * 0.20f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, Color(0xFF1B74DF).copy(alpha = 0.55f))
                }
                .clickable(onClick = onPolicyClick),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.home_18),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.width(4.dp))
                GlyphIcon(Glyph.ChevronRight, tint = Color.White, size = 13.dp, strokeWidth = 2.dp)
            }
        }
    }
}

/** 白胶囊左侧的两个小头像，官方是两个卡通人物，这里用同尺寸的圆形头像块表示。 */
@Composable
private fun AvatarDot(outer: Color, inner: Color, overlap: Dp = 0.dp) {
    Box(
        modifier = Modifier
            .offset(x = -overlap)
            .size(26.dp)
            .background(outer, CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(inner, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            GlyphIcon(Glyph.Profile, tint = Color.White, size = 15.dp, strokeWidth = 1.6.dp)
        }
    }
}

@Composable
private fun ServiceCards(
    onAction: (String) -> Unit,
    onOpenIncomeTaxDetails: () -> Unit,
    onOpenTaxRecordIssue: () -> Unit,
    onOpenAnnualReconciliation: () -> Unit,
    onOpenMoreServices: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 12.dp, top = 20.dp, end = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        serviceItems.forEach { item ->
            ServiceCard(
                item = item,
                onClick = {
                    when (item.id) {
                        "income" -> onOpenIncomeTaxDetails()
                        "issue" -> onOpenTaxRecordIssue()
                        "annual" -> onOpenAnnualReconciliation()
                        "more" -> onOpenMoreServices()
                        else -> onAction(unavailableHint(context, context.getString(item.title)))
                    }
                }
            )
        }
    }
}

/**
 * 官方服务卡：108×142dp 的白色圆角卡，图标是一个从卡面左上角凸出来 16dp 的彩色圆角方块，
 * 右下角压着一块淡色操作区（约卡片宽的 52%、高 22dp，左上圆角，右下角顺着卡面圆角），
 * 四张之后还会露出下一张的边，提示可以横向滑动。
 */
@Composable
private fun ServiceCard(item: ServiceItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(108.dp)
            .height(142.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    // 底部留出操作块的高度（22dp）+ 2dp 间隙，描述不再被压成一行省略
                    modifier = Modifier.padding(start = 10.dp, top = 31.dp, end = 8.dp, bottom = 24.dp)
                ) {
                    Text(
                        stringResource(item.title),
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(item.description),
                        color = Color(0xFF9AA0A8),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // 操作区贴在右下角，右下圆角由父级 Surface 的裁剪负责
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(56.dp)
                        .height(22.dp)
                        .background(item.color.copy(alpha = 0.22f), RoundedCornerShape(topStart = 12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(item.action),
                        color = item.color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(34.dp)
                .background(
                    Brush.linearGradient(
                        listOf(item.color.copy(alpha = 0.72f), item.color),
                        start = Offset.Zero,
                        end = Offset(120f, 120f)
                    ),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            GlyphIcon(item.glyph, tint = Color.White, size = 19.dp, strokeWidth = 1.7.dp)
        }
    }
}

@Composable
private fun PromoBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var page by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3_500)
            page = (page + 1) % 2
        }
    }

    val colors = if (page == 0) {
        listOf(Color(0xFFBDEEFF), Color(0xFF4DB9E9), Color(0xFFD9F7FF))
    } else {
        listOf(Color(0xFFFFC479), Color(0xFFFFE1A7), Color(0xFFF4B76E))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
            .clickable {
                page = (page + 1) % 2
                onClick()
            },
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(colors))
                .padding(horizontal = 17.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    if (page == 0) stringResource(R.string.home_25) else stringResource(R.string.home_26),
                    color = if (page == 0) Color.White else Color(0xFFC86223),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (page == 0) stringResource(R.string.home_27) else stringResource(R.string.home_28),
                    color = if (page == 0) Color(0xFF075F9D) else Color(0xFF984418),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                color = Color.White.copy(alpha = 0.84f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(
                    if (page == 0) stringResource(R.string.home_29) else stringResource(R.string.home_30),
                    color = if (page == 0) DeepBlue else Color(0xFFE07B2C),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
                )
            }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(2) { index ->
                    Box(
                        Modifier
                            .width(if (index == page) 22.dp else 7.dp)
                            .height(3.dp)
                            .background(Color.White.copy(alpha = if (index == page) 0.95f else 0.45f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleCategories(selected: Int, onSelected: (Int) -> Unit) {
    val categories = listOf(stringResource(R.string.home_31), stringResource(R.string.home_32), stringResource(R.string.home_33), stringResource(R.string.home_34))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEachIndexed { index, title ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelected(index) }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    title,
                    color = if (selected == index) HomeBlue else Color(0xFF515761),
                    fontSize = 16.sp,
                    fontWeight = if (selected == index) FontWeight.Black else FontWeight.Normal
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier
                        .width(24.dp)
                        .height(3.dp)
                        .background(if (selected == index) HomeBlue else Color.Transparent, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ArticleCard(
    article: ArticleItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (article.imageFirst) {
                ArticleThumbnail(article)
                Spacer(Modifier.width(12.dp))
            }
            // 标题只有一行时，原来靠 weight 把日期顶到卡底会留下一大片空白，
            // 改成紧跟标题并在垂直方向居中
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(article.title),
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Text("◷  ${article.date}", color = Color(0xFFB4B8BF), fontSize = 13.sp)
            }
            if (!article.imageFirst) {
                Spacer(Modifier.width(12.dp))
                ArticleThumbnail(article)
            }
        }
    }
}

@Composable
private fun ArticleThumbnail(article: ArticleItem) {
    Box(
        modifier = Modifier
            .width(112.dp)
            .height(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(article.accent.copy(alpha = 0.55f), article.accent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Color.White.copy(alpha = 0.26f),
                radius = size.minDimension * 0.36f,
                center = Offset(size.width * 0.20f, size.height * 0.26f)
            )
            drawCircle(
                Color.White.copy(alpha = 0.18f),
                radius = size.minDimension * 0.28f,
                center = Offset(size.width * 0.84f, size.height * 0.80f)
            )
        }
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color.White, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            GlyphIcon(article.glyph, tint = Color(0xFF2F74D8), size = 26.dp, strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun HomeBottomNavigation(selectedTab: Int, onSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PageBackground)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .height(58.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomTabs.forEachIndexed { index, item ->
                    val selected = selectedTab == index
                    val tint = if (selected) HomeBlue else Color(0xFF3A414B)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelected(index) }
                    ) {
                        GlyphIcon(
                            // 官方选中态用实心图标，未选中是线性
                            if (selected && item.first == Glyph.Home) Glyph.HomeFilled else item.first,
                            tint = tint,
                            size = 24.dp,
                            strokeWidth = if (selected) 2.0.dp else 1.8.dp
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            stringResource(item.second),
                            color = tint,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenPreview() {
    MyApplicationTheme(dynamicColor = false) {
        HomeScreen()
    }
}
