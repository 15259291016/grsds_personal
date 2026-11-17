package com.grsds.personal

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.AppPreferencesRepository
import com.grsds.personal.data.DeductionRecord
import com.grsds.personal.data.IncomeRecordRepository
import com.grsds.personal.data.TaxCalculator
import com.grsds.personal.data.formatYuan
import kotlin.math.abs

private val CalcBlue = Color(0xFF168AF4)
private val CalcBackground = Color(0xFFF5F7FB)
private val CalcPrimaryText = Color(0xFF171B22)
private val CalcSecondaryText = Color(0xFF7D8490)
private val CalcDivider = Color(0xFFEFF1F5)
private val CalcGreen = Color(0xFF12A66C)
private val CalcWarn = Color(0xFFE28A2D)

/**
 * 汇算计算明细：把「收入额 → 扣除 → 应纳税所得额 → 税率 → 应纳税额 → 应补退」的每一步摊开。
 *
 * 已申报的年度以申报快照为准（[TaxCalculator.fromDeclaration]），
 * 未申报的年度用当前明细实时试算，两者数字口径一致，不会出现“结果页 5520、明细页另一个数”。
 */
@Composable
fun CalculationDetailScreen(
    year: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val recordRepository = remember { IncomeRecordRepository.get(context) }
    val prefsRepository = remember { AppPreferencesRepository.get(context) }
    val records by recordRepository.records.collectAsState(initial = emptyList())
    val deductions by prefsRepository.deductions.collectAsState(initial = emptyList())
    val declarations by prefsRepository.declarations.collectAsState(initial = emptyList())

    val declaration = declarations.firstOrNull { it.year == year && it.isActive }
    val settlement = remember(records, deductions, declaration) {
        if (declaration != null) {
            TaxCalculator.fromDeclaration(declaration)
        } else {
            TaxCalculator.settle(records, deductions, year)
        }
    }
    val slices = remember(records) { TaxCalculator.incomeSlices(records, year) }
    val sliceTotal = slices.sumOf { it.countedCents }
    val yearDeductions = remember(deductions) { deductions.filter { it.year == year } }
    val deductionTotal = yearDeductions.sumOf { it.amountInCents }
    val snapshotIncome = declaration?.incomeCents
    // 申报快照与当前明细不一致时，构成只能当参考
    val sliceMismatch = snapshotIncome != null && sliceTotal != snapshotIncome

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CalcBackground)
            .statusBarsPadding()
    ) {
        CalcTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            CalcHeader(year = year, settlement = settlement, fromSnapshot = declaration != null)
            Spacer(Modifier.height(12.dp))

            CalcStep(index = 1, title = stringResource(R.string.calc_05)) {
                if (slices.isEmpty()) {
                    Text(
                        stringResource(R.string.calc_31),
                        color = CalcSecondaryText,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    slices.forEach { slice ->
                        CalcLine(
                            label = slice.type,
                            value = "¥${formatYuan(slice.countedCents)}",
                            hint = if (slice.discounted) {
                                stringResource(
                                    R.string.calc_07,
                                    slice.discountPercent,
                                    formatYuan(slice.grossCents)
                                )
                            } else {
                                stringResource(R.string.calc_06)
                            }
                        )
                    }
                }
                if (sliceMismatch && snapshotIncome != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.calc_09, formatYuan(snapshotIncome)),
                        color = CalcWarn,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                CalcDividerLine()
                CalcTotalRow(
                    stringResource(R.string.calc_08),
                    "¥${formatYuan(settlement.incomeCents)}"
                )
            }

            CalcStep(index = 2, title = stringResource(R.string.calc_10)) {
                CalcLine(
                    label = stringResource(R.string.calc_11),
                    value = "¥${formatYuan(settlement.basicDeductionCents)}",
                    hint = stringResource(R.string.calc_33)
                )
                if (declaration != null) {
                    CalcLine(
                        label = stringResource(R.string.calc_12),
                        value = "¥${formatYuan(settlement.specialDeductionCents)}",
                        hint = stringResource(R.string.calc_32)
                    )
                } else {
                    yearDeductions
                        .sortedByDescending { it.amountInCents }
                        .forEach { record: DeductionRecord ->
                            CalcLine(
                                label = record.type,
                                value = "¥${formatYuan(record.amountInCents)}",
                                hint = null
                            )
                        }
                    if (yearDeductions.isEmpty()) {
                        CalcLine(
                            label = stringResource(R.string.calc_12),
                            value = "¥0.00",
                            hint = stringResource(R.string.calc_34)
                        )
                    } else {
                        CalcTotalRow(
                            stringResource(R.string.calc_12),
                            "¥${formatYuan(deductionTotal)}"
                        )
                    }
                }
                CalcDividerLine()
                CalcTotalRow(
                    stringResource(R.string.calc_13),
                    "¥${formatYuan(settlement.totalDeductionCents)}"
                )
            }

            CalcStep(index = 3, title = stringResource(R.string.calc_14)) {
                Text(
                    stringResource(
                        R.string.calc_15,
                        formatYuan(settlement.incomeCents),
                        formatYuan(settlement.totalDeductionCents)
                    ),
                    color = CalcSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "¥${formatYuan(settlement.taxableIncomeCents)}",
                    color = CalcBlue,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
            }

            CalcStep(index = 4, title = stringResource(R.string.calc_16)) {
                CalcLine(
                    label = stringResource(R.string.calc_17),
                    value = "${settlement.taxRatePercent}%",
                    hint = TaxCalculator.bracketRangeLabel(
                        TaxCalculator.bracketOf(settlement.taxableIncomeCents)
                    )
                )
                CalcLine(
                    label = stringResource(R.string.calc_18),
                    value = "¥${formatYuan(settlement.quickDeductionCents)}",
                    hint = null
                )
                CalcDividerLine()
                CalcTotalRow(
                    stringResource(R.string.calc_19),
                    "¥${formatYuan(settlement.taxPayableCents)}"
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    settlement.taxPayableFormula,
                    color = Color(0xFF3C4654),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF2F8FF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.calc_26),
                    color = CalcPrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                TaxBracketTable(taxableCents = settlement.taxableIncomeCents)
            }

            CalcStep(index = 5, title = stringResource(R.string.calc_20)) {
                CalcLine(
                    label = stringResource(R.string.calc_21),
                    value = "¥${formatYuan(settlement.taxPrepaidCents)}",
                    hint = stringResource(R.string.calc_35)
                )
                CalcLine(
                    label = stringResource(R.string.calc_19),
                    value = "¥${formatYuan(settlement.taxPayableCents)}",
                    hint = null
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (settlement.isRefund) Color(0xFFEAF7F1) else Color(0xFFFDF3E7),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (settlement.isRefund) stringResource(R.string.calc_03) else stringResource(R.string.calc_04),
                            color = CalcSecondaryText,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "¥${formatYuan(abs(settlement.balanceCents))}",
                            color = if (settlement.isRefund) CalcGreen else CalcWarn,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.calc_23),
                    color = CalcSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Text(
                stringResource(R.string.calc_24),
                color = CalcSecondaryText,
                fontSize = 12.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CalcTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.calc_30),
            color = CalcBlue,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            stringResource(R.string.calc_01),
            color = CalcPrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = CalcDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun CalcHeader(year: Int, settlement: TaxCalculator.Settlement, fromSnapshot: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF2E93F6), Color(0xFF147EEA))))
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Text(
            stringResource(R.string.calc_02, year),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "¥${formatYuan(abs(settlement.balanceCents))}",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (settlement.isRefund) stringResource(R.string.calc_03) else stringResource(R.string.calc_04),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (fromSnapshot) stringResource(R.string.calc_36) else stringResource(R.string.calc_37),
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

/** 一步计算：左侧序号徽标 + 白色卡片。 */
@Composable
private fun CalcStep(index: Int, title: String, content: @Composable () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(CalcBlue.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$index",
                        color = CalcBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(title, color = CalcPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun CalcLine(label: String, value: String, hint: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color(0xFF3C4654), fontSize = 14.sp)
            if (!hint.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(hint, color = CalcSecondaryText, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        Text(value, color = CalcPrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CalcTotalRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CalcPrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(value, color = CalcBlue, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun CalcDividerLine() {
    Spacer(Modifier.height(4.dp))
    HorizontalDivider(color = CalcDivider)
    Spacer(Modifier.height(4.dp))
}

/** 七级税率表：命中档位高亮，展示门槛与速算扣除数。 */
@Composable
private fun TaxBracketTable(taxableCents: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CalcBackground, RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(stringResource(R.string.calc_27), color = CalcSecondaryText, fontSize = 11.sp, modifier = Modifier.weight(1.6f))
            Text(
                stringResource(R.string.calc_17),
                color = CalcSecondaryText,
                fontSize = 11.sp,
                modifier = Modifier.weight(0.7f),
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.calc_18),
                color = CalcSecondaryText,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
        TaxCalculator.taxBrackets.forEach { bracket ->
            val active = bracket === TaxCalculator.bracketOf(taxableCents)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (active) CalcBlue.copy(alpha = 0.10f) else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    TaxCalculator.bracketRangeLabel(bracket),
                    color = if (active) CalcBlue else Color(0xFF3C4654),
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1.6f)
                )
                Text(
                    "${bracket.ratePercent}%",
                    color = if (active) CalcBlue else Color(0xFF3C4654),
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(0.7f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "¥${formatYuan(bracket.quickDeductionCents)}",
                    color = if (active) CalcBlue else CalcSecondaryText,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
