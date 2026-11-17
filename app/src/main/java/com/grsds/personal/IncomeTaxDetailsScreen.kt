package com.grsds.personal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.grsds.personal.ui.Glyph
import com.grsds.personal.ui.GlyphIcon

private val DetailsBlue = Color(0xFF3D8CF2)
private val DetailsBackground = Color(0xFFF5F7FA)
private val DetailsText = Color(0xFF25282D)
private val DetailsDivider = Color(0xFFE8EAED)

private val incomeTypes = INCOME_TYPE_OPTIONS

@Composable
fun IncomeTaxDetailsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenBatchCreate: () -> Unit = {},
    onQuery: (year: Int, incomeTypes: List<String>) -> Unit = { _, _ -> }
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val repository = remember(context) { IncomeRecordRepository.get(context) }
    val allRecords by repository.records.collectAsState(initial = emptyList())

    var selectedYear by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))
    }
    var pendingYear by remember { mutableIntStateOf(selectedYear) }
    var otherTypesExpanded by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    val checkedTypes = remember {
        mutableStateListOf(true, true, true, true, false, false, false, false, false)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // 删除前的二次确认目标、正在编辑的记录、以及可撤销的最近删除
    var pendingDelete by remember { mutableStateOf<IncomeRecord?>(null) }
    var editingRecord by remember { mutableStateOf<IncomeRecord?>(null) }
    var recentlyDeleted by remember { mutableStateOf<IncomeRecord?>(null) }

    // 撤销条 6 秒后自动收起
    LaunchedEffect(recentlyDeleted) {
        if (recentlyDeleted != null) {
            delay(6_000)
            recentlyDeleted = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DetailsBackground)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DetailsBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                DetailsTopBar(onBack = onBack)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                ) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(DetailsBackground)
                    )
                    SectionTitle(stringResource(R.string.details_22))
                    YearRow(
                        year = selectedYear,
                        onClick = {
                            pendingYear = selectedYear
                            showYearPicker = true
                        }
                    )
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(DetailsBackground)
                    )
                    SectionTitle(stringResource(R.string.details_23))

                    val visibleCount = if (otherTypesExpanded) incomeTypes.size else 4
                    repeat(visibleCount) { index ->
                        IncomeTypeRow(
                            label = incomeTypes[index],
                            checked = checkedTypes[index],
                            onClick = { checkedTypes[index] = !checkedTypes[index] }
                        )
                    }

                    // 「其他类型」也是列表的一行：白底 + 顶部分隔线，比浮在灰底上更像原生列表
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Color.White)
                            .clickable { otherTypesExpanded = !otherTypesExpanded }
                    ) {
                        HorizontalDivider(
                            color = DetailsDivider,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(start = 54.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (otherTypesExpanded) stringResource(R.string.details_24) else stringResource(R.string.details_25),
                                color = Color(0xFF2488E8),
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            GlyphIcon(
                                Glyph.ChevronDown,
                                tint = Color(0xFF2488E8),
                                size = 13.dp,
                                strokeWidth = 2.dp,
                                modifier = Modifier.rotate(if (otherTypesExpanded) 180f else 0f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DetailsBackground)
                            .padding(horizontal = 16.dp, vertical = 28.dp)
                    ) {
                        Button(
                            onClick = {
                                val selectedTypes = incomeTypes.filterIndexed { index, _ ->
                                    checkedTypes.getOrElse(index) { false }
                                }
                                if (selectedTypes.isEmpty()) {
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(context.getString(R.string.details_26))
                                    }
                                } else {
                                    onQuery(selectedYear, selectedTypes)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DetailsBlue),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.details_27), color = Color.White, fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    val yearRecords = allRecords.filter { it.year == selectedYear }
                    SectionTitle(stringResource(R.string.details_28))
                    if (yearRecords.isEmpty()) {
                        EmptyRecordsHint(onOpenBatchCreate = onOpenBatchCreate)
                    } else {
                        yearRecords.forEach { record ->
                            CreatedRecordRow(
                                record = record,
                                onEdit = { editingRecord = record },
                                onDelete = { pendingDelete = record }
                            )
                        }
                        RecordsTotalRow(
                            count = yearRecords.size,
                            totalCents = yearRecords.sumOf { it.amountInCents }
                        )
                    }
                    Spacer(Modifier.height(180.dp))
                }
            }
        }
    }

    // 删除后的撤销条：自己做，不依赖 Snackbar 的 action 回调
    recentlyDeleted?.let { deleted ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .background(Color(0xFF2B2F36), RoundedCornerShape(10.dp))
                .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                context.getString(R.string.details_29, deleted.month, deleted.incomeType, deleted.amountLabel),
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.details_30),
                color = Color(0xFF6FB6FF),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        val target = deleted
                        recentlyDeleted = null
                        scope.launch {
                            repository.addAll(listOf(target))
                            snackbarHostState.showSnackbar(context.getString(R.string.details_31))
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Text(
                "✕",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable { recentlyDeleted = null }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
        }
    }

    if (showYearPicker) {
        YearPickerSheet(
            pendingYear = pendingYear,
            onYearSelected = { pendingYear = it },
            onCancel = { showYearPicker = false },
            onConfirm = {
                selectedYear = pendingYear
                showYearPicker = false
            }
        )
    }

    // 删除：先确认，删完给一次撤销机会
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.details_32), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(
                        R.string.details_33,
                        target.month,
                        target.incomeType,
                        target.amountLabel
                    ),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        scope.launch {
                            repository.delete(target.id)
                            recentlyDeleted = target
                        }
                    }
                ) {
                    Text(stringResource(R.string.details_34), color = Color(0xFFE24B4A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.details_35)) }
            }
        )
    }

    editingRecord?.let { target ->
        EditRecordDialog(
            record = target,
            onDismiss = { editingRecord = null },
            onSave = { updated ->
                editingRecord = null
                scope.launch {
                    repository.update(updated)
                    snackbarHostState.showSnackbar(context.getString(R.string.details_36))
                }
            }
        )
    }
}

@Composable
private fun DetailsTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .height(50.dp)
    ) {
        Text(
            stringResource(R.string.details_01),
            color = Color(0xFF2787E8),
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            stringResource(R.string.details_02),
            color = DetailsText,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(
            color = DetailsDivider,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 20.dp)
                .background(Color(0xFF4C9AF5), RoundedCornerShape(3.dp))
        )
        Text(
            title,
            color = DetailsText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 9.dp)
        )
    }
}

@Composable
private fun YearRow(year: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.details_03), color = Color(0xFF50545B), fontSize = 18.sp)
        Text(
            year.toString(),
            color = DetailsText,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        GlyphIcon(Glyph.ChevronRight, tint = Color(0xFFD7DADF), size = 15.dp, strokeWidth = 2.dp)
    }
}

@Composable
private fun IncomeTypeRow(label: String, checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.White)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(23.dp)
                    .background(if (checked) DetailsBlue else Color(0xFFD7DADE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (checked) 23.dp else 21.dp)
                        .background(if (checked) DetailsBlue else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (checked) {
                        Text("✓", color = Color.White, fontSize = 15.sp)
                    }
                }
            }
            Text(
                label,
                color = DetailsText,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 17.dp)
            )
        }
        HorizontalDivider(
            color = DetailsDivider,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .padding(start = 54.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun YearPickerSheet(
    pendingYear: Int,
    onYearSelected: (Int) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val years = remember(currentYear) { (1900..currentYear).toList() }
    val selectedIndex = years.indexOf(pendingYear).coerceAtLeast(0)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - 3).coerceAtLeast(0)
    )

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.details_37),
                    color = Color(0xFF4E535B),
                    fontSize = 18.sp,
                    modifier = Modifier.clickable(onClick = onCancel)
                )
                Text(
                    stringResource(R.string.details_38),
                    color = Color(0xFF318BE8),
                    fontSize = 18.sp,
                    modifier = Modifier.clickable(onClick = onConfirm)
                )
            }
            HorizontalDivider(color = DetailsDivider)
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState
            ) {
                items(years.size) { index ->
                    val year = years[index]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .clickable { onYearSelected(year) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            year.toString(),
                            color = if (year == pendingYear) DetailsText else Color(0xFFD5D7DA),
                            fontSize = 17.sp
                        )
                    }
                    if (year == pendingYear) {
                        HorizontalDivider(color = DetailsDivider)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatedRecordRow(
    record: IncomeRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.details_04, record.month, record.incomeType),
                    color = DetailsText,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    record.withholdingAgent,
                    color = Color(0xFF8A9099),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "¥${record.amountLabel}",
                color = DetailsText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            // 点击区抬到 40dp 以上，避免误触旁边的删除
            Text(
                stringResource(R.string.details_05),
                color = DetailsBlue,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 6.dp, vertical = 12.dp)
            )
            Text(
                stringResource(R.string.details_06),
                color = Color(0xFFE24B4A),
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 6.dp, vertical = 12.dp)
            )
        }
        HorizontalDivider(
            color = DetailsDivider,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 18.dp)
        )
    }
}

@Composable
private fun EmptyRecordsHint(onOpenBatchCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.details_07), color = Color(0xFF8A9099), fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.details_08), color = Color(0xFFB4B8BF), fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.details_09),
            color = DetailsBlue,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(onClick = onOpenBatchCreate)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RecordsTotalRow(count: Int, totalCents: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.details_10, count), color = Color(0xFF50545B), fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.details_11, formatYuan(totalCents)),
            color = DetailsBlue,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EditRecordDialog(
    record: IncomeRecord,
    onDismiss: () -> Unit,
    onSave: (IncomeRecord) -> Unit
) {
    val context = LocalContext.current
    var amountText by remember(record.id) {
        mutableStateOf(String.format(java.util.Locale.CHINA, "%.2f", record.amountInCents / 100.0))
    }
    var month by remember(record.id) { mutableIntStateOf(record.month) }
    var agent by remember(record.id) { mutableStateOf(record.withholdingAgent) }
    val cents = parseYuanToCents(amountText)
    val invalid = amountText.isNotBlank() && cents == null
    val canSave = cents != null && (record.amountInCents != cents || record.month != month ||
        record.withholdingAgent != agent.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details_12), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.details_13, record.incomeType), color = Color(0xFF7D8490), fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text(stringResource(R.string.details_14)) },
                    isError = invalid,
                    supportingText = if (invalid) {
                        { Text(stringResource(R.string.details_15), fontSize = 12.sp) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.details_16), color = Color(0xFF7D8490), fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..12).forEach { m ->
                        val selected = m == month
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selected) DetailsBlue else Color(0xFFF0F2F6),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { month = m }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                stringResource(R.string.details_17, m),
                                color = if (selected) Color.White else Color(0xFF50545B),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = agent,
                    onValueChange = { agent = it },
                    label = { Text(stringResource(R.string.details_18)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { cents?.let { onSave(record.copy(month = month, amountInCents = it, withholdingAgent = agent.trim().ifEmpty { context.getString(R.string.details_19) })) } }
            ) {
                Text(stringResource(R.string.details_20), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.details_21)) } }
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun IncomeTaxDetailsPreview() {
    MyApplicationTheme(dynamicColor = false, darkTheme = false) {
        IncomeTaxDetailsScreen(onBack = {})
    }
}
