package com.grsds.personal

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.TaxCalendar
import com.grsds.personal.ui.Glyph
import com.grsds.personal.ui.GlyphIcon
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// 四个底部 Tab 的真实页面：待办 / 办&查 / 消息 / 我的
// ---------------------------------------------------------------------------

private val TabPageBackground = Color(0xFFF5F7FB)
private val TabPrimaryText = Color(0xFF171B22)
private val TabSecondaryText = Color(0xFF7D8490)
private val TabBlue = Color(0xFF168AF4)

private data class TabEntry(
    /** 稳定的跳转标识，与展示文案解耦 */
    val id: String,
    val glyph: Glyph,
    val color: Color,
    @StringRes val title: Int,
    @StringRes val description: Int = 0,
    @StringRes val time: Int = 0,
    val done: Boolean = false,
    /** 待办事项的截止日（年、月、日），用于按当天日期判断是否已结束 */
    val deadline: Triple<Int, Int, Int>? = null
)

// ---------------------------------------------------------------------------
// 待办
// ---------------------------------------------------------------------------

private val todoItems = listOf(
    TabEntry("annual", Glyph.Chart, Color(0xFF1598F4), R.string.tab_30, R.string.tab_31, R.string.tab_32, deadline = Triple(2026, 6, 30)),
    TabEntry("deductions", Glyph.Shield, Color(0xFF8B5CF6), R.string.tab_34, R.string.tab_35, R.string.tab_36, deadline = Triple(2025, 12, 31)),
    TabEntry("bank", Glyph.Card, Color(0xFF15CDB1), R.string.tab_38, R.string.tab_39, R.string.tab_40),
    TabEntry("done", Glyph.Check, Color(0xFF9AB0C4), R.string.tab_42, R.string.tab_43, R.string.tab_44, done = true)
)

/** 未接入功能的统一提示，避免“正在打开”点完却什么都没发生。 */
internal fun unavailableHint(context: Context, title: String) =
    context.getString(R.string.tab_105, title)

@Composable
fun TodoTab(
    onAction: (String) -> Unit,
    onOpenAnnualReconciliation: () -> Unit,
    onOpenDeductions: () -> Unit,
    onOpenDeclarationRecords: () -> Unit,
    onOpenBankCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { com.grsds.personal.data.AppPreferencesRepository.get(context) }
    val declarations by repository.declarations.collectAsState(initial = emptyList())
    // 已经申报过的年度不该再挂着「去办理」：改成已办结并指向申报记录
    val annualDeclared = declarations.any { it.isActive }
    val entries = todoItems.map { item ->
        if (item.id == "annual" && annualDeclared) {
            item.copy(done = true, description = R.string.tab_106, time = 0)
        } else {
            item
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TabPageBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(R.string.tab_01),
            color = TabPrimaryText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 4.dp)
        )
        Text(
            stringResource(R.string.tab_02),
            color = TabSecondaryText,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
        )
        // 已结束的排到后面，和真实 APP「先办该办的」一致
        entries
            .sortedBy { item ->
                val expired = item.deadline?.let { TaxCalendar.isPast(it.first, it.second, it.third) } == true
                when {
                    item.done -> 2
                    expired -> 1
                    else -> 0
                }
            }
            .forEach { item ->
            val expired = item.deadline?.let { TaxCalendar.isPast(it.first, it.second, it.third) } == true
            TodoCard(
                item = item,
                expired = expired,
                onHandle = {
                    when {
                        // 已办结的那条点进去看申报记录，和真实 APP 一致
                        item.done -> onOpenDeclarationRecords()
                        item.id == "annual" -> onOpenAnnualReconciliation()
                        item.id == "deductions" -> onOpenDeductions()
                        item.id == "bank" -> onOpenBankCards()
                        else -> onAction(unavailableHint(context, context.getString(item.title)))
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TodoCard(item: TabEntry, expired: Boolean, onHandle: () -> Unit) {
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
                        .background(
                            if (expired) {
                                Brush.linearGradient(listOf(Color(0xFFC3CBD6), Color(0xFF9AA6B4)))
                            } else {
                                Brush.linearGradient(listOf(item.color.copy(alpha = 0.72f), item.color))
                            },
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    GlyphIcon(item.glyph, tint = Color.White, size = 19.dp, strokeWidth = 1.7.dp)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(item.title),
                    color = TabPrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                if (item.done) {
                    Text(
                        stringResource(R.string.tab_05),
                        color = TabBlue,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable(onClick = onHandle)
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                } else if (expired) {
                    Text(
                        stringResource(R.string.status_closed),
                        color = Color(0xFF8A929C),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFFEEF0F3))
                            .clickable(onClick = onHandle)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                } else {
                    Button(
                        onClick = onHandle,
                        colors = ButtonDefaults.buttonColors(containerColor = TabBlue),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(stringResource(R.string.tab_06), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            if (item.description != 0) {
                Text(
                    stringResource(item.description),
                    color = Color(0xFF3C4654),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            if (item.time != 0) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(item.time), color = TabSecondaryText, fontSize = 13.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 办&查
// ---------------------------------------------------------------------------

private val taxHandlingServices = listOf(
    TabEntry("annual", Glyph.Chart, Color(0xFF1598F4), R.string.tab_46),
    TabEntry("deductions", Glyph.Shield, Color(0xFF8B5CF6), R.string.tab_48),
    TabEntry("income", Glyph.Receipt, Color(0xFF3D8BFF), R.string.tab_50),
    TabEntry("issue", Glyph.Printer, Color(0xFF15CDB1), R.string.tab_52),
    TabEntry("correction", Glyph.Pencil, Color(0xFFFF9F43), R.string.tab_54),
    TabEntry("agent", Glyph.Headset, Color(0xFF6C7CF0), R.string.tab_56)
)

private val queryServices = listOf(
    TabEntry("income", Glyph.Receipt, Color(0xFF3D8BFF), R.string.tab_58),
    TabEntry("issue", Glyph.Printer, Color(0xFF15CDB1), R.string.tab_60),
    TabEntry("declarations", Glyph.DocCheck, Color(0xFFFF9F43), R.string.tab_62),
    TabEntry("proof", Glyph.Stamp, Color(0xFF26B4A8), R.string.tab_64)
)

@Composable
fun ServicesTab(
    onAction: (String) -> Unit,
    onOpenIncomeTaxDetails: () -> Unit,
    onOpenTaxRecordIssue: () -> Unit,
    onOpenAnnualReconciliation: () -> Unit,
    onOpenDeductions: () -> Unit,
    onOpenDeclarationRecords: () -> Unit,
    onOpenCorrection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TabPageBackground)
            .verticalScroll(rememberScrollState())
            .padding(top = 18.dp)
    ) {
        ServiceSection(stringResource(R.string.tab_07), taxHandlingServices) { entry ->
            when (entry.id) {
                "annual" -> onOpenAnnualReconciliation()
                "deductions" -> onOpenDeductions()
                "income" -> onOpenIncomeTaxDetails()
                "issue" -> onOpenTaxRecordIssue()
                "correction" -> onOpenCorrection()
                else -> onAction(unavailableHint(context, context.getString(entry.title)))
            }
        }
        Spacer(Modifier.height(14.dp))
        ServiceSection(stringResource(R.string.tab_12), queryServices) { entry ->
            when (entry.id) {
                "income" -> onOpenIncomeTaxDetails()
                "issue" -> onOpenTaxRecordIssue()
                "declarations" -> onOpenDeclarationRecords()
                else -> onAction(unavailableHint(context, context.getString(entry.title)))
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
                Text(title, color = TabPrimaryText, fontWeight = FontWeight.Black, fontSize = 17.sp)
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
                                    .background(
                                        Brush.linearGradient(listOf(entry.color.copy(alpha = 0.72f), entry.color)),
                                        RoundedCornerShape(13.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                GlyphIcon(entry.glyph, tint = Color.White, size = 22.dp, strokeWidth = 1.7.dp)
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(
                                stringResource(entry.title),
                                color = Color(0xFF3C4654),
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
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

private val messageCategories = listOf(
    R.string.tab_65, R.string.tab_66, R.string.tab_67, R.string.tab_68
)

private val messageItems: List<MessageItem> get() = MessageCenter.items

@Composable
fun MessagesTab(
    onOpenMessage: (String) -> Unit,
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
                stringResource(R.string.tab_16),
                color = TabPrimaryText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.tab_17),
                color = TabBlue,
                fontSize = 14.sp,
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
                        stringResource(name),
                        color = if (selected) Color.White else Color(0xFF3C4654),
                        fontSize = 14.sp,
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
                    onOpenMessage(item.id)
                }
                Spacer(Modifier.height(10.dp))
            }
            if (visible.isEmpty()) {
                Text(
                    stringResource(R.string.tab_19),
                    color = TabSecondaryText,
                    fontSize = 15.sp,
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
                    .background(
                        Brush.linearGradient(listOf(item.color.copy(alpha = 0.72f), item.color)),
                        RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                GlyphIcon(item.glyph, tint = Color.White, size = 21.dp, strokeWidth = 1.7.dp)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(item.title),
                        color = TabPrimaryText,
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp,
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
                    stringResource(item.summary),
                    color = Color(0xFF3C4654),
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(item.time),
                    color = TabSecondaryText,
                    fontSize = 13.sp,
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
            TabEntry("profile", Glyph.IdCard, Color(0xFF1598F4), R.string.tab_90),
            TabEntry("family", Glyph.People, Color(0xFF8B5CF6), R.string.tab_92),
            TabEntry("employment", Glyph.Briefcase, Color(0xFF3D8BFF), R.string.tab_94)
        )
    ),
    ProfileGroup(
        listOf(
            TabEntry("bank", Glyph.Card, Color(0xFF15CDB1), R.string.tab_96),
            TabEntry("security", Glyph.Shield, Color(0xFFFF7043), R.string.tab_98)
        )
    ),
    ProfileGroup(
        listOf(
            TabEntry("help", Glyph.Headset, Color(0xFF6C7CF0), R.string.tab_100),
            TabEntry("about", Glyph.DocCheck, Color(0xFF26B4A8), R.string.tab_102),
            TabEntry("settings", Glyph.Gear, Color(0xFF9AB0C4), R.string.tab_104)
        )
    )
)

@Composable
fun ProfileTab(
    onAction: (String) -> Unit,
    onOpenIncomeTaxDetails: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfileInfo: () -> Unit,
    onOpenBankCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { com.grsds.personal.data.AppPreferencesRepository.get(context) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val session by repository.session.collectAsState(initial = null)
    var showLogoutConfirm by remember { mutableStateOf(false) }

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
                    if (session == null) onOpenLogin() else onOpenProfileInfo()
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
                    Text(stringResource(R.string.tab_21), color = TabBlue, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session?.nickname ?: stringResource(R.string.tab_22),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        session?.maskedPhone ?: stringResource(R.string.tab_23),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
                GlyphIcon(Glyph.ChevronRight, tint = Color.White.copy(alpha = 0.85f), size = 17.dp, strokeWidth = 2.dp)
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
                                    when (entry.id) {
                                        "profile" -> if (session == null) onOpenLogin() else onOpenProfileInfo()
                                        "bank" -> if (session == null) onOpenLogin() else onOpenBankCards()
                                        "settings" -> onOpenSettings()
                                        else -> onAction(unavailableHint(context, context.getString(entry.title)))
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
                                GlyphIcon(entry.glyph, tint = entry.color, size = 16.dp, strokeWidth = 1.6.dp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(entry.title),
                                color = TabPrimaryText,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            GlyphIcon(Glyph.ChevronRight, tint = Color(0xFFC0C6CF), size = 16.dp, strokeWidth = 1.9.dp)
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
                    stringResource(R.string.tab_27),
                    color = Color(0xFFE5484D),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutConfirm = true }
                        .padding(vertical = 15.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            // 真实 APP 退出登录会先确认一次，避免误触直接掉登录态
            if (showLogoutConfirm) {
                AlertDialog(
                    onDismissRequest = { showLogoutConfirm = false },
                    containerColor = Color.White,
                    title = { Text(stringResource(R.string.logout_title), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    text = { Text(stringResource(R.string.logout_message), fontSize = 15.sp, color = Color(0xFF5A616B)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showLogoutConfirm = false
                            scope.launch { repository.logout() }
                        }) {
                            Text(stringResource(R.string.logout_confirm), color = Color(0xFFE5484D), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutConfirm = false }) {
                            Text(stringResource(R.string.logout_cancel), color = Color(0xFF6B7280))
                        }
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(
            stringResource(R.string.tab_28),
            color = TabSecondaryText,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
