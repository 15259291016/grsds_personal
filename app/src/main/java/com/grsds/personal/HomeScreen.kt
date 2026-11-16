package com.grsds.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HomeBlue = Color(0xFF168AF4)
private val DeepBlue = Color(0xFF147EEA)
private val PageBackground = Color(0xFFF5F7FB)
private val PrimaryText = Color(0xFF171B22)
private val SecondaryText = Color(0xFF7D8490)

private data class ServiceItem(
    /** 稳定的跳转标识，与展示文案解耦，避免用文案做分支判断 */
    val id: String,
    @StringRes val symbol: Int,
    val color: Color,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val action: Int
)

private data class ArticleItem(
    @StringRes val title: Int,
    val date: String,
    @StringRes val symbol: Int,
    val accent: Color,
    val imageFirst: Boolean = false
)

private val serviceItems = listOf(
    ServiceItem("annual", R.string.home_35, Color(0xFF1598F4), R.string.home_36, R.string.home_37, R.string.home_38),
    ServiceItem("income", R.string.home_39, Color(0xFF8B5CF6), R.string.home_40, R.string.home_41, R.string.home_42),
    ServiceItem("issue", R.string.home_43, Color(0xFF15CDB1), R.string.home_44, R.string.home_45, R.string.home_46),
    ServiceItem("more", R.string.home_47, Color(0xFF2A9CF2), R.string.home_48, R.string.home_49, R.string.home_50)
)

private val bottomTabs = listOf(
    "⌂" to R.string.home_51,
    "✓" to R.string.home_52,
    "▤" to R.string.home_53,
    "□" to R.string.home_54,
    "○" to R.string.home_55
)

private val articleGroups = listOf(
    listOf(
        ArticleItem(R.string.home_56, "2025-02-28", R.string.home_57, Color(0xFF72C8F7)),
        ArticleItem(R.string.home_58, "2025-02-28", R.string.home_59, Color(0xFFFFD68D)),
        ArticleItem(R.string.home_60, "2024-03-15", R.string.home_61, Color(0xFF94E9EB), true),
        ArticleItem(R.string.home_62, "2024-03-15", R.string.home_63, Color(0xFFB9D7FF), true)
    ),
    listOf(
        ArticleItem(R.string.home_64, "2026-02-26", R.string.home_65, Color(0xFF81C6FF)),
        ArticleItem(R.string.home_66, "2026-01-05", R.string.home_67, Color(0xFF9CE4D8))
    ),
    listOf(
        ArticleItem(R.string.home_68, "2025-06-12", R.string.home_69, Color(0xFFFFDCA4)),
        ArticleItem(R.string.home_70, "2025-05-20", R.string.home_71, Color(0xFFAED5FF))
    ),
    listOf(
        ArticleItem(R.string.home_72, "2025-03-01", R.string.home_73, Color(0xFFA6E6DB)),
        ArticleItem(R.string.home_74, "2025-02-28", R.string.home_75, Color(0xFFC7D7FF))
    )
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
    onOpenLogin: () -> Unit = {}
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
            .background(Color.Black)
    ) {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
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
                    onOpenDeductions = onOpenDeductions
                )
                1 -> TodoTab(
                    onAction = showMessage,
                    onOpenAnnualReconciliation = onOpenAnnualReconciliation,
                    onOpenDeductions = onOpenDeductions,
                    onOpenDeclarationRecords = onOpenDeclarationRecords,
                    modifier = Modifier.padding(innerPadding)
                )
                2 -> ServicesTab(
                    onAction = showMessage,
                    onOpenIncomeTaxDetails = onOpenIncomeTaxDetails,
                    onOpenTaxRecordIssue = onOpenTaxRecordIssue,
                    onOpenAnnualReconciliation = onOpenAnnualReconciliation,
                    onOpenDeductions = onOpenDeductions,
                    onOpenDeclarationRecords = onOpenDeclarationRecords,
                    modifier = Modifier.padding(innerPadding)
                )
                3 -> MessagesTab(
                    onAction = showMessage,
                    modifier = Modifier.padding(innerPadding)
                )
                else -> ProfileTab(
                    onAction = showMessage,
                    onOpenIncomeTaxDetails = onOpenIncomeTaxDetails,
                    onOpenLogin = onOpenLogin,
                    onOpenSettings = onOpenSettings,
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableIntStateOf(0) }
    val searchHintBlank = context.getString(R.string.home_01)
    val searchHintQuery = context.getString(R.string.home_02)
    val scanHint = context.getString(R.string.home_03)
    val policyHint = context.getString(R.string.home_05)
    val promoHint = context.getString(R.string.home_06)
    val moreHint = context.getString(R.string.home_08)

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
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp, top = 24.dp, bottom = 12.dp)
                )
            }
            item {
                DeductionCard(
                    onPolicyClick = { onAction(unavailableHint(context, policyHint)) },
                    onFillClick = onOpenDeductions,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            item {
                ServiceCards(
                    onAction = onAction,
                    onOpenIncomeTaxDetails = onOpenIncomeTaxDetails,
                    onOpenTaxRecordIssue = onOpenTaxRecordIssue,
                    onOpenAnnualReconciliation = onOpenAnnualReconciliation
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
            items(articleGroups[selectedCategory].size) { index ->
                ArticleCard(
                    article = articleGroups[selectedCategory][index],
                    onClick = {
                        val title = context.getString(articleGroups[selectedCategory][index].title)
                        onAction(unavailableHint(context, title))
                    },
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
                        .clickable { onAction(moreHint) }
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
            color = Color(0xFFFFF9EA),
            shape = RoundedCornerShape(13.dp),
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clickable { onAction(unavailableHint(context, context.getString(R.string.home_10))) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFFF7B22), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("!", color = Color.White, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    stringResource(R.string.home_11),
                    color = Color(0xFFAD6700),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text("›", color = Color(0xFFAD6700), fontSize = 24.sp)
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
            .height(190.dp)
            .shadow(10.dp, RoundedCornerShape(16.dp)),
        color = Color(0xFFF1F8FF),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .size(82.dp)
                    .background(Color(0xFFDCEEFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.home_12), color = HomeBlue, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.home_13),
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    stringResource(R.string.home_14),
                    color = Color(0xFF3C4654),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth(0.74f)
                )
                Spacer(Modifier.height(15.dp))
                Button(
                    onClick = onEnter,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2589F4)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .width(170.dp)
                        .height(44.dp)
                ) {
                    Text(stringResource(R.string.home_15), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun DeductionCard(
    onPolicyClick: () -> Unit,
    onFillClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(listOf(Color(0xFF5FB0F6), Color(0xFF238DF1)))
                )
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(Color(0xFFFF4A45), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.home_16), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.home_17), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.home_18),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable(onClick = onPolicyClick)
                )
            }
            Text(
                stringResource(R.string.home_19),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp)
            )
            Surface(
                color = Color.White.copy(alpha = 0.96f),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onFillClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👨‍👩‍👧", fontSize = 17.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_20), color = Color(0xFF4B5360), fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.home_21), color = PrimaryText, fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun ServiceCards(
    onAction: (String) -> Unit,
    onOpenIncomeTaxDetails: () -> Unit,
    onOpenTaxRecordIssue: () -> Unit,
    onOpenAnnualReconciliation: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 12.dp, top = 20.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        serviceItems.forEach { item ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .width(112.dp)
                    .height(156.dp)
                    .clickable {
                        when (item.id) {
                            "income" -> onOpenIncomeTaxDetails()
                            "issue" -> onOpenTaxRecordIssue()
                            "annual" -> onOpenAnnualReconciliation()
                            else -> onAction(unavailableHint(context, context.getString(item.title)))
                        }
                    }
            ) {
                Column(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 8.dp, bottom = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(item.color, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(item.symbol), color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        stringResource(item.title),
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        stringResource(item.description),
                        color = SecondaryText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(item.action),
                        color = item.color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .background(item.color.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(article.title),
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
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
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.linearGradient(
                    listOf(article.accent.copy(alpha = 0.45f), article.accent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.White.copy(alpha = 0.48f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            val symbol = stringResource(article.symbol)
            Text(
                symbol,
                color = Color(0xFF2389EA),
                fontSize = if (symbol.length > 2) 13.sp else 21.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
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
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.96f),
            shape = RoundedCornerShape(26.dp),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bottomTabs.forEachIndexed { index, item ->
                    val selected = selectedTab == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelected(index) }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            item.first,
                            color = if (selected) HomeBlue else Color(0xFF343B45),
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            stringResource(item.second),
                            color = if (selected) HomeBlue else Color(0xFF343B45),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
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
