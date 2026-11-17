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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.AppPreferencesRepository
import com.grsds.personal.data.DeclarationRecord
import com.grsds.personal.data.DeductionRecord
import com.grsds.personal.data.IncomeRecord
import com.grsds.personal.data.IncomeRecordRepository
import com.grsds.personal.data.TaxCalendar
import com.grsds.personal.data.TaxCalculator
import com.grsds.personal.data.formatYuan
import kotlinx.coroutines.launch

private val ReconBlue = Color(0xFF168AF4)
private val ReconPageBackground = Color(0xFFF5F7FB)
private val ReconPrimaryText = Color(0xFF171B22)
private val ReconSecondaryText = Color(0xFF7D8490)
private val ReconDivider = Color(0xFFEFF1F5)
private val ReconGreen = Color(0xFF12A66C)
private val ReconWarn = Color(0xFFE28A2D)

private val stepTitles = listOf(
    R.string.reconcile_39, R.string.reconcile_40, R.string.reconcile_41, R.string.reconcile_42
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnualReconciliationScreen(
    onBack: () -> Unit,
    onOpenDeductions: () -> Unit,
    step: Int,
    onStepChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onOpenBankCards: () -> Unit = {},
    onOpenRefundProgress: (String) -> Unit = {},
    onOpenPayment: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val recordRepository = remember { IncomeRecordRepository.get(context) }
    val prefsRepository = remember { AppPreferencesRepository.get(context) }
    val records by recordRepository.records.collectAsState(initial = emptyList())
    val deductions by prefsRepository.deductions.collectAsState(initial = emptyList())
    val declarations by prefsRepository.declarations.collectAsState(initial = emptyList())
    val bankCards by prefsRepository.bankCards.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // 退税申请弹层
    var refundSheet by remember { mutableStateOf(false) }
    var selectedCardId by remember { mutableStateOf<String?>(null) }

    // 默认选中本地记录里最近的年度，没有记录时回到上一个汇算年度。
    val defaultYear = remember(records) {
        records.maxOfOrNull { it.year } ?: 2025
    }
    var year by remember(defaultYear) { mutableIntStateOf(defaultYear) }
    // 本次刚提交的记录：让结果页在 Flow 回流之前就能展示
    var justSubmitted by remember(year) { mutableStateOf<DeclarationRecord?>(null) }
    BackHandler(onBack = onBack)

    val settlement = remember(records, deductions, year) {
        TaxCalculator.settle(records, deductions, year)
    }
    val yearRecords = remember(records, year) { records.filter { it.year == year } }
    val yearDeductions = remember(deductions, year) { deductions.filter { it.year == year } }

    // 是否已申报以落盘记录为准：杀掉进程再进来仍然停在“申报完成”，不会退回向导；
    // 已作废的申报不算数，作废后可以重新走一遍汇算。
    val declaration = declarations.firstOrNull { it.year == year && it.isActive } ?: justSubmitted
    val submitted = declaration != null
    val displayedDeclaration = declaration ?: DeclarationRecord.from(settlement, 0L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ReconPageBackground)
            .statusBarsPadding()
    ) {
        ReconTopBar(title = stringResource(R.string.reconcile_01, year), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            ReconHeader(year = year, submitted = submitted)
            Spacer(Modifier.height(12.dp))
            StepIndicator(current = if (submitted) 3 else step)
            Spacer(Modifier.height(12.dp))

            when {
                submitted -> SubmitResultCard(
                    declaration = displayedDeclaration,
                    onApplyRefund = {
                        selectedCardId = bankCards.firstOrNull { it.isDefault }?.id
                            ?: bankCards.firstOrNull()?.id
                        refundSheet = true
                    },
                    onOpenRefundProgress = { onOpenRefundProgress(displayedDeclaration.id) },
                    onOpenPayment = { onOpenPayment(displayedDeclaration.id) }
                )
                step == 0 -> IncomeStep(
                    year = year,
                    records = yearRecords,
                    settlement = settlement,
                    onSelectYear = { year = it; onStepChange(0) }
                )
                step == 1 -> DeductionStep(
                    settlement = settlement,
                    yearDeductions = yearDeductions,
                    onOpenDeductions = onOpenDeductions
                )
                else -> TaxStep(settlement)
            }
            Spacer(Modifier.height(20.dp))
        }

        // 底部操作区
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0 && !submitted) {
                    Text(
                        stringResource(R.string.reconcile_02),
                        color = ReconSecondaryText,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { onStepChange(step - 1) }
                            .padding(end = 20.dp, top = 10.dp, bottom = 10.dp)
                    )
                }
                Button(
                    onClick = {
                        when {
                            submitted -> onBack()
                            step < 2 -> onStepChange(step + 1)
                            else -> scope.launch {
                                justSubmitted = prefsRepository.submitDeclaration(
                                    DeclarationRecord.from(settlement, System.currentTimeMillis())
                                )
                            }
                        }
                    },
                    // 没有任何收入时不允许往下走，否则会提交一张 0 元的申报；
                    // 办理期还没开始的年度也不让进向导（真实 APP 会在那时直接提示未开始）
                    enabled = when {
                        submitted -> true
                        step > 0 -> true
                        TaxCalendar.settlementWindow(year) == TaxCalendar.Window.NotOpen -> false
                        else -> settlement.incomeCents > 0
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ReconBlue),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text(
                        when {
                            submitted -> stringResource(R.string.reconcile_03)
                            step == 2 -> stringResource(R.string.reconcile_04)
                            else -> stringResource(R.string.reconcile_05)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }
        }
    }

    // 退税收款账户选择：沿用银行卡管理里的数据，未绑卡时引导去绑卡
    if (refundSheet) {
        ModalBottomSheet(onDismissRequest = { refundSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text(
                    stringResource(R.string.refund_01),
                    color = ReconPrimaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.refund_02, formatYuan(kotlin.math.abs(displayedDeclaration.balanceCents))),
                    color = ReconSecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                Spacer(Modifier.height(16.dp))

                if (bankCards.isEmpty()) {
                    Text(
                        stringResource(R.string.refund_03),
                        color = ReconWarn,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            refundSheet = false
                            onOpenBankCards()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ReconBlue),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(stringResource(R.string.refund_04), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                } else {
                    bankCards.forEach { card ->
                        val selected = card.id == selectedCardId
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(
                                    if (selected) Color(0xFFEAF4FF) else Color(0xFFF7F9FC),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCardId = card.id }
                                .padding(horizontal = 14.dp, vertical = 13.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${card.bankName} ${card.maskedNumber}",
                                    color = ReconPrimaryText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    stringResource(R.string.refund_05, card.holderName),
                                    color = ReconSecondaryText,
                                    fontSize = 12.sp
                                )
                            }
                            if (selected) {
                                Text("✓", color = ReconBlue, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val card = bankCards.firstOrNull { it.id == selectedCardId } ?: return@Button
                            scope.launch {
                                prefsRepository.applyRefund(
                                    id = displayedDeclaration.id,
                                    cardId = card.id,
                                    cardLabel = "${card.bankName} ${card.maskedNumber}",
                                    at = System.currentTimeMillis()
                                )
                                refundSheet = false
                            }
                        },
                        enabled = selectedCardId != null,
                        colors = ButtonDefaults.buttonColors(containerColor = ReconBlue),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(stringResource(R.string.refund_06), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ReconTopBar(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.reconcile_06),
            color = ReconBlue,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            title,
            color = ReconPrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = ReconDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ReconHeader(year: Int, submitted: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF2E93F6), Color(0xFF147EEA))))
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.reconcile_07, year),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    when {
                        submitted -> stringResource(R.string.reconcile_08)
                        TaxCalendar.settlementWindow(year) == TaxCalendar.Window.Open ->
                            stringResource(R.string.status_open)
                        TaxCalendar.settlementWindow(year) == TaxCalendar.Window.NotOpen ->
                            stringResource(R.string.status_not_started)
                        else -> stringResource(R.string.status_supplement)
                    },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = if (submitted) 0.28f else 0.22f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.reconcile_10, year + 1, year + 1),
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.reconcile_11),
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun StepIndicator(current: Int) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            stepTitles.forEachIndexed { index, title ->
                val active = index <= current
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (active) ReconBlue else Color(0xFFE3E8F0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (index < current) "✓" else "${index + 1}",
                            color = if (active) Color.White else ReconSecondaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(title),
                        color = if (active) ReconBlue else ReconSecondaryText,
                        fontSize = 12.sp,
                        fontWeight = if (index == current) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeStep(
    year: Int,
    records: List<IncomeRecord>,
    settlement: TaxCalculator.Settlement,
    onSelectYear: (Int) -> Unit
) {
    Column {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.reconcile_12), color = ReconSecondaryText, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "¥${formatYuan(settlement.incomeCents)}",
                    color = ReconPrimaryText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.reconcile_13, records.size),
                    color = ReconSecondaryText,
                    fontSize = 13.sp
                )
                if (records.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = ReconDivider)
                    records.groupBy { it.incomeType }.forEach { (type, list) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 11.dp)
                        ) {
                            Text(type, color = Color(0xFF3C4654), fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Text(
                                "¥${formatYuan(list.sumOf { it.amountInCents })}",
                                color = ReconPrimaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.reconcile_14),
                        color = ReconWarn,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // 年度切换，方便查看不同年度
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            listOf(year - 1, year, year + 1).forEach { option ->
                val selected = option == year
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) ReconBlue else Color.White,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectYear(option) }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        stringResource(R.string.reconcile_15, option),
                        color = if (selected) Color.White else Color(0xFF3C4654),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun DeductionStep(
    settlement: TaxCalculator.Settlement,
    yearDeductions: List<DeductionRecord>,
    onOpenDeductions: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.reconcile_16), color = ReconPrimaryText, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reconcile_17), color = Color(0xFF3C4654), fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(
                    "¥${formatYuan(settlement.basicDeductionCents)}",
                    color = ReconPrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.reconcile_18), color = ReconSecondaryText, fontSize = 13.sp)

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = ReconDivider)
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reconcile_19), color = Color(0xFF3C4654), fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(
                    "¥${formatYuan(settlement.specialDeductionCents)}",
                    color = ReconPrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            if (yearDeductions.isEmpty()) {
                Text(stringResource(R.string.reconcile_20), color = ReconSecondaryText, fontSize = 13.sp)
            } else {
                yearDeductions.forEach { record ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            if (record.type == "大病医疗") {
                                "· ${record.type}（据实扣除）"
                            } else {
                                "· ${record.type}（${record.ratioPercent}%）"
                            },
                            color = ReconSecondaryText,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "¥${formatYuan(record.amountInCents)}",
                            color = ReconSecondaryText,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.reconcile_21),
                color = ReconBlue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEAF4FF), RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenDeductions)
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TaxStep(settlement: TaxCalculator.Settlement) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.reconcile_22), color = ReconPrimaryText, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            ReconRow(stringResource(R.string.reconcile_23), settlement.incomeCents)
            // 扣除额本身就是正数，写成 -60000 会让人以为收入被减成了负数
            ReconRow(stringResource(R.string.reconcile_24), settlement.basicDeductionCents)
            ReconRow(stringResource(R.string.reconcile_19), settlement.specialDeductionCents)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = ReconDivider)
            Spacer(Modifier.height(8.dp))
            ReconRow(stringResource(R.string.reconcile_25), settlement.taxableIncomeCents, bold = true)
            // 应纳税所得额为 0 时没有适用税率可言，写着 3% 会让人以为还要缴税
            ReconRow(
                stringResource(R.string.reconcile_26),
                null,
                text = if (settlement.taxableIncomeCents > 0) "${settlement.taxRatePercent}%" else "—"
            )
            ReconRow(stringResource(R.string.reconcile_27), settlement.taxPayableCents, bold = true)
            ReconRow(stringResource(R.string.reconcile_28), settlement.taxPrepaidCents)

            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (settlement.isRefund) Color(0xFFEAF9F1) else Color(0xFFFFF3E8),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (settlement.isRefund) stringResource(R.string.reconcile_29) else stringResource(R.string.reconcile_30),
                    color = ReconSecondaryText,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "¥${formatYuan(kotlin.math.abs(settlement.balanceCents))}",
                    color = if (settlement.isRefund) ReconGreen else ReconWarn,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (settlement.isRefund) stringResource(R.string.reconcile_31) else stringResource(R.string.reconcile_32),
                    color = ReconSecondaryText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ReconRow(
    label: String,
    cents: Long?,
    text: String? = null,
    bold: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 5.dp)
    ) {
        Text(label, color = Color(0xFF3C4654), fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(
            text ?: "¥${formatYuan(cents ?: 0L)}",
            color = ReconPrimaryText,
            fontSize = 15.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SubmitResultCard(
    declaration: DeclarationRecord,
    onApplyRefund: () -> Unit,
    onOpenRefundProgress: () -> Unit,
    onOpenPayment: () -> Unit
) {
    val e = declaration
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF12A66C).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color(0xFF12A66C), fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.reconcile_33), color = ReconPrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.reconcile_34, e.declarationNo),
                color = ReconSecondaryText,
                fontSize = 13.sp
            )
            if (e.submittedAt > 0L) {
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.reconcile_35, e.submittedAtLabel),
                    color = ReconSecondaryText,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                if (e.isRefund) stringResource(R.string.reconcile_29) else stringResource(R.string.reconcile_30),
                color = ReconSecondaryText,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "¥${formatYuan(kotlin.math.abs(e.balanceCents))}",
                color = if (e.isRefund) Color(0xFF12A66C) else ReconWarn,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(14.dp))
            Text(
                if (e.isRefund) stringResource(R.string.reconcile_36) else stringResource(R.string.reconcile_37),
                color = ReconSecondaryText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = ReconDivider)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.reconcile_23), color = ReconSecondaryText, fontSize = 12.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("¥${formatYuan(e.incomeCents)}", color = ReconPrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.reconcile_27), color = ReconSecondaryText, fontSize = 12.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("¥${formatYuan(e.taxPayableCents)}", color = ReconPrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.reconcile_38), color = ReconSecondaryText, fontSize = 12.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("¥${formatYuan(e.taxPrepaidCents)}", color = ReconPrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = ReconDivider)
            Spacer(Modifier.height(14.dp))
            SettlementActionBlock(
                declaration = e,
                onApplyRefund = onApplyRefund,
                onOpenRefundProgress = onOpenRefundProgress,
                onOpenPayment = onOpenPayment
            )
        }
    }
}

/** 申报完成后的税款办理入口：退税申请 / 退税进度 / 补税缴款。 */
@Composable
private fun SettlementActionBlock(
    declaration: DeclarationRecord,
    onApplyRefund: () -> Unit,
    onOpenRefundProgress: () -> Unit,
    onOpenPayment: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            declaration.balanceCents == 0L -> Text(
                stringResource(R.string.refund_07),
                color = ReconSecondaryText,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            declaration.isPaid -> {
                Text(
                    stringResource(R.string.refund_08, declaration.paymentNo),
                    color = ReconGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.refund_09, declaration.processedAtLabel),
                    color = ReconSecondaryText,
                    fontSize = 12.sp
                )
            }

            declaration.hasAppliedRefund -> {
                Text(
                    stringResource(R.string.refund_10, declaration.processStatus),
                    color = ReconPrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.refund_11, declaration.bankCardLabel),
                    color = ReconSecondaryText,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                SettlementButton(
                    text = stringResource(R.string.refund_12),
                    onClick = onOpenRefundProgress
                )
            }

            declaration.isRefund -> SettlementButton(
                text = stringResource(R.string.refund_13),
                onClick = onApplyRefund
            )

            else -> {
                Text(
                    stringResource(R.string.refund_14),
                    color = ReconSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(12.dp))
                SettlementButton(
                    text = stringResource(R.string.refund_15),
                    onClick = onOpenPayment
                )
            }
        }
    }
}

@Composable
private fun SettlementButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = ReconBlue),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
