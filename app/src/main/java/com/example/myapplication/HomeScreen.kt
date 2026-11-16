package com.example.myapplication

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
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HomeBlue = Color(0xFF168AF4)
private val DeepBlue = Color(0xFF147EEA)
private val PageBackground = Color(0xFFF5F7FB)
private val PrimaryText = Color(0xFF171B22)
private val SecondaryText = Color(0xFF7D8490)

private data class ServiceItem(
    val symbol: String,
    val color: Color,
    val title: String,
    val description: String,
    val action: String
)

private data class ArticleItem(
    val title: String,
    val date: String,
    val symbol: String,
    val accent: Color,
    val imageFirst: Boolean = false
)

private val serviceItems = listOf(
    ServiceItem("汇", Color(0xFF1598F4), "综合所得年度汇算", "申报与查询综合所得年度汇算", "去申报"),
    ServiceItem("税", Color(0xFF8B5CF6), "收入纳税明细", "查看个人所得税纳税明细", "去查询"),
    ServiceItem("票", Color(0xFF15CDB1), "纳税记录开具", "生成或查看纳税记录", "去开具"),
    ServiceItem("多", Color(0xFF2A9CF2), "更多服务", "设置并管理常用功能", "去看看")
)

private val bottomTabs = listOf(
    "⌂" to "首页",
    "✓" to "待办",
    "▤" to "办&查",
    "□" to "消息",
    "○" to "我的"
)

private val articleGroups = listOf(
    listOf(
        ArticleItem("办理汇算想便捷 APP密码要记牢", "2025-02-28", "税", Color(0xFF72C8F7)),
        ArticleItem("获取退税想及时 准确卡号必须有", "2025-02-28", "退", Color(0xFFFFD68D)),
        ArticleItem("未依法办理个人所得税综合所得汇算清缴案件", "2024-03-15", "申报", Color(0xFF94E9EB), true),
        ArticleItem("未在法定期限内办理综合所得汇算清缴、虚假填报子女教育专项附加扣除案件", "2024-03-15", "权威", Color(0xFFB9D7FF), true)
    ),
    listOf(
        ArticleItem("关于办理2025年度个人所得税综合所得汇算清缴事项的公告", "2026-02-26", "公告", Color(0xFF81C6FF)),
        ArticleItem("个人所得税APP服务时间调整通知", "2026-01-05", "通知", Color(0xFF9CE4D8))
    ),
    listOf(
        ArticleItem("年度汇算退税进度在哪里查询？", "2025-06-12", "问答", Color(0xFFFFDCA4)),
        ArticleItem("专项附加扣除信息如何修改？", "2025-05-20", "热点", Color(0xFFAED5FF))
    ),
    listOf(
        ArticleItem("综合所得年度汇算政策解读", "2025-03-01", "政策", Color(0xFFA6E6DB)),
        ArticleItem("专项附加扣除政策要点", "2025-02-28", "解读", Color(0xFFC7D7FF))
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
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        PersistentSearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = {
                onAction(if (query.isBlank()) "请输入要搜索的功能或服务" else "搜索功能尚未接入，当前为演示版本")
            },
            onScan = { onAction("扫一扫尚未接入，当前为演示版本") }
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
                    text = "重点服务推荐",
                    color = PrimaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 12.dp, top = 24.dp, bottom = 12.dp)
                )
            }
            item {
                DeductionCard(
                    onPolicyClick = { onAction(unavailableHint("专项附加扣除政策")) },
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
                    onClick = { onAction(unavailableHint("专项附加扣除宣传")) },
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
                    onClick = { onAction(unavailableHint(articleGroups[selectedCategory][index].title)) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                )
            }
            item {
                Text(
                    "查看更多  ›",
                    color = SecondaryText,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction("已展示全部内容") }
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
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
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
                                "请输入想搜索的功能/服务",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
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
                .clickable { onAction(unavailableHint("汇算清缴预约")) }
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
                    "关于2025年度个人所得税综合所得汇算清缴预约办理的通知",
                    color = Color(0xFFAD6700),
                    fontSize = 14.sp,
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
                Text("税务\n服务", color = HomeBlue, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "2025综合所得年度汇算",
                    color = PrimaryText,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    "已于6月30日结束，您可进入本专题页查看记录或补办年度汇算",
                    color = Color(0xFF3C4654),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.fillMaxWidth(0.82f)
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
                    Text("进入专题页", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                    Text("火", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(7.dp))
                Text("专项附加扣除", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "相关政策  ›",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(onClick = onPolicyClick)
                )
            }
            Text(
                "若符合条件，您可点击下方填报",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
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
                    Text("👨‍👩‍👧", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("超1亿人参与", color = Color(0xFF4B5360), fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Text("我要填报", color = PrimaryText, fontWeight = FontWeight.Black, fontSize = 16.sp)
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
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(100.dp)
                    .height(142.dp)
                    .clickable {
                        when (item.title) {
                            "收入纳税明细" -> onOpenIncomeTaxDetails()
                            "纳税记录开具" -> onOpenTaxRecordIssue()
                            "综合所得年度汇算" -> onOpenAnnualReconciliation()
                            else -> onAction(unavailableHint(item.title))
                        }
                    }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(item.color, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.symbol, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(item.title, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(item.description, color = SecondaryText, fontSize = 10.sp, lineHeight = 15.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        item.action,
                        color = item.color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.End)
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
                    if (page == 0) "入职季" else "光影记忆",
                    color = if (page == 0) Color.White else Color(0xFFC86223),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (page == 0) "减负利器“专项附加扣除”" else "税收带来美好生活",
                    color = if (page == 0) Color(0xFF075F9D) else Color(0xFF984418),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                color = Color.White.copy(alpha = 0.84f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(
                    if (page == 0) "了解一下" else "开启回忆",
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
    val categories = listOf("警示案例", "通知公告", "热点问题", "政策解读")
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
                    fontSize = 15.sp,
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
                    article.title,
                    color = PrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Text("◷  ${article.date}", color = Color(0xFFB4B8BF), fontSize = 12.sp)
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
            Text(
                article.symbol,
                color = Color(0xFF2389EA),
                fontSize = if (article.symbol.length > 2) 13.sp else 21.sp,
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
                            fontSize = 18.sp
                        )
                        Text(
                            item.second,
                            color = if (selected) HomeBlue else Color(0xFF343B45),
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
