package com.grsds.personal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.INCOME_TYPE_OPTIONS
import com.grsds.personal.data.IncomeRecord
import com.grsds.personal.data.IncomeRecordRepository
import com.grsds.personal.data.formatYuan
import com.grsds.personal.data.parseYuanToCents
import com.grsds.personal.ui.theme.MyApplicationTheme
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.launch

private val BatchBlue = Color(0xFF168AF4)
private val BatchPageBackground = Color(0xFFF5F7FB)
private val BatchPrimaryText = Color(0xFF171B22)
private val BatchSecondaryText = Color(0xFF7D8490)
private val BatchFieldBackground = Color(0xFFF3F5F9)
private val BatchDanger = Color(0xFFE24B4A)

/** 一行待提交的录入草稿，金额保存用户原始输入文本，提交时统一校验解析。 */
private data class DraftRow(
    val id: Int,
    val month: Int = 1,
    val incomeType: String = INCOME_TYPE_OPTIONS.first(),
    val amount: String = "",
    val withholdingAgent: String = ""
)

private fun validateDraft(row: DraftRow): String? {
    if (row.amount.isBlank()) return "请填写收入金额"
    if (parseYuanToCents(row.amount) == null) return "金额需为大于 0 的数字"
    return null
}

@Composable
fun BatchCreateScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val repository = remember(context) { IncomeRecordRepository.get(context) }
    val existingRecords by repository.records.collectAsState(initial = emptyList())
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    var year by remember { mutableIntStateOf(currentYear) }
    val drafts = remember { mutableStateListOf(DraftRow(id = 1)) }
    var nextDraftId by remember { mutableIntStateOf(2) }
    var showErrors by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val submit: () -> Unit = {
        val firstError = drafts.withIndex().firstNotNullOfOrNull { (index, row) ->
            validateDraft(row)?.let { "第 ${index + 1} 行：$it" }
        }
        if (firstError != null) {
            showErrors = true
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(firstError)
            }
        } else {
            saving = true
            scope.launch {
                val now = System.currentTimeMillis()
                val newRecords = drafts.mapIndexed { index, row ->
                    IncomeRecord(
                        id = UUID.randomUUID().toString(),
                        year = year,
                        month = row.month,
                        incomeType = row.incomeType,
                        amountInCents = parseYuanToCents(row.amount) ?: 0L,
                        withholdingAgent = row.withholdingAgent.trim().ifBlank { "未填写扣缴义务人" },
                        createdAt = now + index
                    )
                }
                repository.addAll(newRecords)
                saving = false
                showErrors = false
                drafts.clear()
                drafts.add(DraftRow(id = nextDraftId++))
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("已保存 ${newRecords.size} 条 ${year} 年度记录")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            containerColor = BatchPageBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                BatchActionBar(
                    rowCount = drafts.size,
                    saving = saving,
                    onAddRow = {
                        drafts.add(DraftRow(id = nextDraftId++))
                    },
                    onSubmit = submit
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                BatchTopBar(
                    onBack = onBack,
                    onClear = {
                        drafts.clear()
                        drafts.add(DraftRow(id = nextDraftId++))
                        showErrors = false
                    }
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        BatchSummaryCard(
                            year = year,
                            onYearSelected = { year = it },
                            savedCount = existingRecords.size,
                            savedTotalCents = existingRecords.sumOf { it.amountInCents }
                        )
                    }
                    item {
                        Text(
                            "一次可录入多行，提交后统一写入本地",
                            color = BatchSecondaryText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    itemsIndexed(drafts, key = { _, row -> row.id }) { index, row ->
                        DraftRowEditor(
                            index = index,
                            row = row,
                            errorText = if (showErrors) validateDraft(row) else null,
                            canDelete = drafts.size > 1,
                            onMonthChange = { month -> drafts[index] = drafts[index].copy(month = month) },
                            onTypeChange = { type -> drafts[index] = drafts[index].copy(incomeType = type) },
                            onAmountChange = { amount -> drafts[index] = drafts[index].copy(amount = amount) },
                            onAgentChange = { agent -> drafts[index] = drafts[index].copy(withholdingAgent = agent) },
                            onDuplicate = {
                                drafts.add(
                                    index + 1,
                                    drafts[index].copy(id = nextDraftId++)
                                )
                            },
                            onDelete = { drafts.removeAt(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchTopBar(onBack: () -> Unit, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            "‹ 返回",
            color = Color(0xFF2787E8),
            fontSize = 17.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            "批量添加纳税明细",
            color = BatchPrimaryText,
            fontSize = 19.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        Text(
            "清空",
            color = Color(0xFF2787E8),
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(onClick = onClear)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        HorizontalDivider(
            color = Color(0xFFE8EAED),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun BatchSummaryCard(
    year: Int,
    onYearSelected: (Int) -> Unit,
    savedCount: Int,
    savedTotalCents: Long
) {
    var yearMenuExpanded by remember { mutableStateOf(false) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val yearOptions = remember(currentYear) { (currentYear downTo currentYear - 6).toList() }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("纳税年度", color = BatchSecondaryText, fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("$year 年度", color = BatchPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Box {
                    Surface(
                        color = Color(0xFFE9F3FE),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { yearMenuExpanded = true }
                    ) {
                        Text(
                            "切换年度",
                            color = BatchBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = yearMenuExpanded,
                        onDismissRequest = { yearMenuExpanded = false }
                    ) {
                        yearOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("$option 年度") },
                                onClick = {
                                    yearMenuExpanded = false
                                    onYearSelected(option)
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEFF1F5))
            Spacer(Modifier.height(12.dp))
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text("已保存记录", color = BatchSecondaryText, fontSize = 12.sp)
                    Text("$savedCount 条", color = BatchPrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("累计金额（元）", color = BatchSecondaryText, fontSize = 12.sp)
                    Text(
                        formatYuan(savedTotalCents),
                        color = BatchBlue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftRowEditor(
    index: Int,
    row: DraftRow,
    errorText: String?,
    canDelete: Boolean,
    onMonthChange: (Int) -> Unit,
    onTypeChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onAgentChange: (String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(BatchBlue.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", color = BatchBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text("第 ${index + 1} 条记录", color = BatchSecondaryText, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "复制",
                    color = BatchBlue,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable(onClick = onDuplicate)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    if (canDelete) "删除" else "清空",
                    color = if (canDelete) BatchDanger else BatchSecondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable(onClick = onDelete)
                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthField(
                    month = row.month,
                    onMonthChange = onMonthChange,
                    modifier = Modifier.width(96.dp)
                )
                IncomeTypeField(
                    incomeType = row.incomeType,
                    onTypeChange = onTypeChange,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInputField(
                    label = "收入金额（元）",
                    value = row.amount,
                    placeholder = "0.00",
                    keyboardType = KeyboardType.Decimal,
                    prefix = "¥",
                    onValueChange = onAmountChange,
                    modifier = Modifier.weight(0.9f)
                )
                TextInputField(
                    label = "扣缴义务人",
                    value = row.withholdingAgent,
                    placeholder = "选填",
                    keyboardType = KeyboardType.Text,
                    onValueChange = onAgentChange,
                    modifier = Modifier.weight(1.1f)
                )
            }

            if (errorText != null) {
                Spacer(Modifier.height(8.dp))
                Text(errorText, color = BatchDanger, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MonthField(
    month: Int,
    onMonthChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        FieldLabel("纳税月份")
        Spacer(Modifier.height(4.dp))
        Box {
            FieldSurface(onClick = { expanded = true }) {
                Text("$month 月", color = BatchPrimaryText, fontSize = 15.sp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                (1..12).forEach { option ->
                    DropdownMenuItem(
                        text = { Text("$option 月") },
                        onClick = {
                            expanded = false
                            onMonthChange(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeTypeField(
    incomeType: String,
    onTypeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        FieldLabel("所得类型")
        Spacer(Modifier.height(4.dp))
        Box {
            FieldSurface(onClick = { expanded = true }) {
                Text(
                    incomeType,
                    color = BatchPrimaryText,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                INCOME_TYPE_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            onTypeChange(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TextInputField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null
) {
    Column(modifier = modifier) {
        FieldLabel(label)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BatchFieldBackground)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (prefix != null) {
                Text(prefix, color = BatchSecondaryText, fontSize = 15.sp)
                Spacer(Modifier.width(3.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, color = Color(0xFFB4B8BF), fontSize = 15.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = BatchPrimaryText, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = BatchSecondaryText, fontSize = 12.sp)
}

@Composable
private fun FieldSurface(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BatchFieldBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}

@Composable
private fun BatchActionBar(
    rowCount: Int,
    saving: Boolean,
    onAddRow: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onAddRow,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(0.42f)
                    .height(46.dp)
            ) {
                Text("+ 添加一行", fontSize = 15.sp, color = BatchBlue)
            }
            Button(
                onClick = onSubmit,
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(containerColor = BatchBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(0.58f)
                    .height(46.dp)
            ) {
                Text(
                    if (saving) "保存中…" else "提交 $rowCount 条记录",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun BatchCreatePreview() {
    MyApplicationTheme(dynamicColor = false, darkTheme = false) {
        BatchCreateScreen(onBack = {})
    }
}
