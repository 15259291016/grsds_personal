package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// 四个底部 Tab 的真实页面：待办 / 办&查 / 消息 / 我的
// ---------------------------------------------------------------------------

private val TabPageBackground = Color(0xFFF5F7FB)
private val TabPrimaryText = Color(0xFF171B22)
private val TabSecondaryText = Color(0xFF7D8490)
private val TabBlue = Color(0xFF168AF4)

private data class TabEntry(
    val symbol: String,
    val color: Color,
    val title: String,
    val description: String = "",
    val time: String = "",
    val done: Boolean = false
)

// ---------------------------------------------------------------------------
// 待办
// ---------------------------------------------------------------------------

private val todoItems = listOf(
    TabEntry("汇", Color(0xFF1598F4), "2025年度综合所得年度汇算", "办理时间为2026年3月1日至6月30日，可办理申报或补办", "2026-06-30 截止"),
    TabEntry("扣", Color(0xFF8B5CF6), "2026年度专项附加扣除确认", "一键带入上年度扣除信息，确认后2026年度可继续享受", "2026-01-31 截止"),
    TabEntry("卡", Color(0xFF15CDB1), "银行卡信息核实", "退税款项将退至您绑定的银行卡，请核实卡片状态", "尽快处理"),
    TabEntry("汇", Color(0xFF9AB0C4), "2024年度综合所得年度汇算", "已于2025年6月30日办理完成，退税已退至绑定银行卡", "已完成", done = true)
)

/** 未接入功能的统一提示，避免“正在打开”点完却什么都没发生。 */
internal fun unavailableHint(title: String) = "「$title」尚未接入，当前为演示版本"

@Composable
fun TodoTab(
    onAction: (String) -> Unit,
    onOpenAnnualReconciliation: () -> Unit,
    onOpenDeductions: () -> Unit,
    onOpenDeclarationRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TabPageBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "待办",
            color = TabPrimaryText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 4.dp)
        )
        Text(
            "需要您办理或确认的事项",
            color = TabSecondaryText,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
        )
        todoItems.forEach { item ->
            TodoCard(
                item = item,
                onHandle = {
                    when {
                        // 已办结的那条点进去看申报记录，和真实 APP 一致
                        item.done -> onOpenDeclarationRecords()
                        item.title.contains("年度汇算") -> onOpenAnnualReconciliation()
                        item.title.contains("专项附加扣除") -> onOpenDeductions()
                        else -> onAction(unavailableHint(item.title))
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TodoCard(item: TabEntry, onHandle: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(item.color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.symbol, color = item.color, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    item.title,
                    color = TabPrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                if (item.done) {
                    Text(
                        "查看记录 ›",
                        color = TabBlue,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable(onClick = onHandle)
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                } else {
                    Button(
                        onClick = onHandle,
                        colors = ButtonDefaults.buttonColors(containerColor = TabBlue),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("去办理", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                item.description,
                color = Color(0xFF3C4654),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(item.time, color = TabSecondaryText, fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------------------------
// 办&查
// ---------------------------------------------------------------------------

private val taxHandlingServices = listOf(
    TabEntry("汇", Color(0xFF1598F4), "综合所得年度汇算"),
    TabEntry("扣", Color(0xFF8B5CF6), "专项附加扣除填报"),
    TabEntry("税", Color(0xFF3D8BFF), "收入纳税明细"),
    TabEntry("票", Color(0xFF15CDB1), "纳税记录开具"),
    TabEntry("改", Color(0xFFFF9F43), "更正申报"),
    TabEntry("委", Color(0xFF6C7CF0), "委托代理")
)

private val queryServices = listOf(
    TabEntry("税", Color(0xFF3D8BFF), "收入纳税明细查询"),
    TabEntry("票", Color(0xFF15CDB1), "纳税记录查询"),
    TabEntry("报", Color(0xFFFF9F43), "申报信息查询"),
    TabEntry("证", Color(0xFF26B4A8), "税收完税证明")
)

@Composable
fun ServicesTab(
    onAction: (String) -> Unit,
    onOpenIncomeTaxDetails: () -> Unit,
    onOpenTaxRecordIssue: () -> Unit,
    onOpenAnnualReconciliation: () -> Unit,
    onOpenDeductions: () -> Unit,
    onOpenDeclarationRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TabPageBackground)
            .verticalScroll(rememberScrollState())
            .padding(top = 18.dp)
    ) {
        ServiceSection("我要办税", taxHandlingServices) { entry ->
            when (entry.title) {
                "综合所得年度汇算" -> onOpenAnnualReconciliation()
                "专项附加扣除填报" -> onOpenDeductions()
                "收入纳税明细" -> onOpenIncomeTaxDetails()
                "纳税记录开具" -> onOpenTaxRecordIssue()
                else -> onAction(unavailableHint(entry.title))
            }
        }
        Spacer(Modifier.height(14.dp))
        ServiceSection("我要查询", queryServices) { entry ->
            when (entry.title) {
                "收入纳税明细查询" -> onOpenIncomeTaxDetails()
                "纳税记录查询" -> onOpenTaxRecordIssue()
                "申报信息查询" -> onOpenDeclarationRecords()
                else -> onAction(unavailableHint(entry.title))
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ServiceSection(
    title: String,
    entries: List<TabEntry>,
    onClick: (TabEntry) -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .size(width = 4.dp, height = 15.dp)
                        .background(TabBlue, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(7.dp))
                Text(title, color = TabPrimaryText, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
            entries.chunked(4).forEach { rowEntries ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowEntries.forEach { entry ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(78.dp)
                                .clickable { onClick(entry) }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(entry.color.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(entry.symbol, color = entry.color, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(
                                entry.title,
                                color = Color(0xFF3C4654),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    repeat(4 - rowEntries.size) {
                        Spacer(Modifier.width(78.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 消息
// ---------------------------------------------------------------------------

private val messageCategories = listOf("全部", "办税提醒", "互动消息", "系统通知")

private data class MessageItem(
    val category: String,
    val symbol: String,
    val color: Color,
    val title: String,
    val summary: String,
    val time: String
)

private val messageItems = listOf(
    MessageItem("办税提醒", "汇", Color(0xFF1598F4), "年度汇算申报提醒", "您的2025年度综合所得汇算已可办理，请及时申报", "昨天 14:20"),
    MessageItem("办税提醒", "扣", Color(0xFF8B5CF6), "专项附加扣除信息确认", "请及时确认2026年度子女教育、赡养老人等扣除信息", "11-10 09:00"),
    MessageItem("互动消息", "诉", Color(0xFFFF7043), "申诉进度通知", "您发起的收入纳税明细异议申诉已受理，预计5个工作日内反馈", "11-08 16:35"),
    MessageItem("系统通知", "安", Color(0xFF26B4A8), "账户安全提示", "检测到您的账号在新设备登录，如非本人操作请及时修改密码", "11-01 10:12"),
    MessageItem("系统通知", "通", Color(0xFF6C7CF0), "服务时间调整通知", "个人所得税APP将于每周日23:00至次日2:00进行例行维护", "10-28 18:00")
)

@Composable
fun MessagesTab(
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableIntStateOf(0) }
    val readIndexes = remember { mutableStateListOf<Int>() }
    val visible = if (selectedCategory == 0) {
        messageItems
    } else {
        messageItems.filter { it.category == messageCategories[selectedCategory] }
    }
    val globalIndexes = if (selectedCategory == 0) {
        messageItems.indices.toList()
    } else {
        messageItems.indices.filter { messageItems[it].category == messageCategories[selectedCategory] }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TabPageBackground)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(TabPageBackground)
                .padding(start = 16.dp, end = 12.dp, top = 18.dp, bottom = 8.dp)
        ) {
            Text(
                "消息",
                color = TabPrimaryText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            Text(
                "全部已读",
                color = TabBlue,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    readIndexes.addAll(messageItems.indices.filterNot { it in readIndexes })
                }
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            messageCategories.forEachIndexed { index, name ->
                val selected = selectedCategory == index
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) TabBlue else Color.White,
                            RoundedCornerShape(15.dp)
                        )
                        .clickable { selectedCategory = index }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        name,
                        color = if (selected) Color.White else Color(0xFF3C4654),
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp)
        ) {
            visible.forEachIndexed { position, item ->
                val globalIndex = globalIndexes[position]
                val unread = globalIndex !in readIndexes
                MessageCard(item, unread) {
                    if (globalIndex !in readIndexes) readIndexes.add(globalIndex)
                    onAction("正在查看：${item.title}")
                }
                Spacer(Modifier.height(10.dp))
            }
            if (visible.isEmpty()) {
                Text(
                    "暂无消息",
                    color = TabSecondaryText,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp)
                        .align(Alignment.CenterHorizontally),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MessageCard(item: MessageItem, unread: Boolean, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(item.color.copy(alpha = 0.14f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.symbol, color = item.color, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        color = TabPrimaryText,
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (unread) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFF4D4F), CircleShape)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    item.summary,
                    color = Color(0xFF3C4654),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    item.time,
                    color = TabSecondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 我的
// ---------------------------------------------------------------------------

private data class ProfileGroup(val entries: List<TabEntry>)

private val profileGroups = listOf(
    ProfileGroup(
        listOf(
            TabEntry("证", Color(0xFF1598F4), "个人信息"),
            TabEntry("家", Color(0xFF8B5CF6), "家庭成员"),
            TabEntry("职", Color(0xFF3D8BFF), "任职受雇信息")
        )
    ),
    ProfileGroup(
        listOf(
            TabEntry("卡", Color(0xFF15CDB1), "银行卡管理"),
            TabEntry("安", Color(0xFFFF7043), "安全中心")
        )
    ),
    ProfileGroup(
        listOf(
            TabEntry("帮", Color(0xFF6C7CF0), "帮助与反馈"),
            TabEntry("关", Color(0xFF26B4A8), "关于"),
            TabEntry("设", Color(0xFF9AB0C4), "设置")
        )
    )
)

@Composable
fun ProfileTab(
    onAction: (String) -> Unit,
    onOpenIncomeTaxDetails: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { com.example.myapplication.data.AppPreferencesRepository.get(context) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val session by repository.session.collectAsState(initial = null)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TabPageBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // 头部：未登录时引导登录，已登录时展示账号信息
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF2589F4), Color(0xFF147EEA)))
                )
                .statusBarsPadding()
                .clickable {
                    if (session == null) onOpenLogin() else onAction("正在查看个人资料")
                }
                .padding(horizontal = 18.dp, vertical = 26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("税", color = TabBlue, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session?.nickname ?: "立即登录 / 注册",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        session?.maskedPhone ?: "登录后可查询收入纳税明细、办理年度汇算",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
                Text("›", color = Color.White.copy(alpha = 0.85f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        profileGroups.forEach { group ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column {
                    group.entries.forEachIndexed { index, entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (entry.title) {
                                        "个人信息" -> onAction(
                                            if (session == null) "登录后可查看个人信息"
                                            else unavailableHint("个人信息")
                                        )
                                        "设置" -> onOpenSettings()
                                        else -> onAction(unavailableHint(entry.title))
                                    }
                                }
                                .padding(horizontal = 15.dp, vertical = 15.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(entry.color.copy(alpha = 0.13f), RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(entry.symbol, color = entry.color, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                entry.title,
                                color = TabPrimaryText,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text("›", color = Color(0xFFC0C6CF), fontSize = 18.sp)
                        }
                        if (index < group.entries.lastIndex) {
                            HorizontalDivider(
                                color = Color(0xFFF0F2F6),
                                modifier = Modifier.padding(start = 57.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (session != null) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    "退出登录",
                    color = Color(0xFFE5484D),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { repository.logout() } }
                        .padding(vertical = 15.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(
            "个人所得税 APP · 演示版",
            color = TabSecondaryText,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
