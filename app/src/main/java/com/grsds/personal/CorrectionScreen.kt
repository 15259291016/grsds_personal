package com.grsds.personal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.AppPreferencesRepository
import com.grsds.personal.data.DeclarationRecord
import com.grsds.personal.data.TaxCalculator
import com.grsds.personal.data.formatYuan
import com.grsds.personal.data.parseYuanToCents
import kotlin.math.abs
import kotlinx.coroutines.launch

private val CorBlue = Color(0xFF168AF4)
private val CorPageBackground = Color(0xFFF5F7FB)
private val CorPrimaryText = Color(0xFF171B22)
private val CorSecondaryText = Color(0xFF7D8490)
private val CorDivider = Color(0xFFEFF1F5)
private val CorGreen = Color(0xFF12A66C)
private val CorWarn = Color(0xFFE28A2D)

/**
 * 更正申报：选择一条生效中的申报，修改收入合计与已缴税额后重算，
 * 提交后原申报置为“已更正”，生成一条带更正链（correctionOf / correctionSeq）的新记录。
 */
@Composable
fun CorrectionScreen(
    preselectedId: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AppPreferencesRepository.get(context) }
    val scope = rememberCoroutineScope()
    val declarations by repository.declarations.collectAsState(initial = emptyList())
    val deductions by repository.deductions.collectAsState(initial = emptyList())

    val correctable = declarations.filter { it.isActive }
    var targetId by remember { mutableStateOf(preselectedId) }
    // 提交更正后原申报会变成“已更正”、不再命中 correctable，
    // 所以把新记录单独存住，成功页不依赖列表重算。
    var submittedNew by remember { mutableStateOf<DeclarationRecord?>(null) }

    // 未指定预选、或预选那条已被更正/作废时，自动落到第一条可更正的记录；
    // 数据还没落盘（列表为空）时保持 null，等 Flow 到了再选。
    LaunchedEffect(declarations) {
        if (submittedNew == null && correctable.none { it.id == targetId }) {
            targetId = correctable.firstOrNull()?.id
        }
    }

    val target = correctable.firstOrNull { it.id == targetId }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CorPageBackground)
            .statusBarsPadding()
    ) {
        CorTopBar(
            year = (submittedNew ?: target)?.year,
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                submittedNew != null -> SubmittedCard(
                    record = submittedNew!!,
                    onDone = onBack
                )
                target == null -> EmptyState()
                else -> CorrectionForm(
                    record = target,
                    specialDeductionCents = deductions
                        .filter { it.year == target.year }
                        .sumOf { it.amountInCents },
                    onSubmit = { newRecord ->
                        scope.launch {
                            repository.correctDeclaration(target.id, newRecord)
                        }
                        submittedNew = newRecord
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CorTopBar(year: Int?, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.correction_01),
            color = CorBlue,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            if (year != null) stringResource(R.string.correction_02, year) else stringResource(R.string.correction_03),
            color = CorPrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = CorDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun EmptyState() {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.correction_04), color = CorPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.correction_05),
                color = CorSecondaryText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CorrectionForm(
    record: DeclarationRecord,
    specialDeductionCents: Long,
    onSubmit: (DeclarationRecord) -> Unit
) {
    var incomeText by remember(record.id) {
        mutableStateOf(String.format(java.util.Locale.CHINA, "%.2f", record.incomeCents / 100.0))
    }
    var prepaidText by remember(record.id) {
        mutableStateOf(String.format(java.util.Locale.CHINA, "%.2f", record.taxPrepaidCents / 100.0))
    }
    var showConfirm by remember { mutableStateOf(false) }
    var incomeError by remember { mutableStateOf(false) }
    var prepaidError by remember { mutableStateOf(false) }

    val incomeCents = parseYuanToCents(incomeText)
    val prepaidCents = parseYuanToCents(prepaidText)
    val settlement = if (incomeCents != null && prepaidCents != null) {
        TaxCalculator.settleWith(record.year, incomeCents, specialDeductionCents, prepaidCents)
    } else {
        null
    }

    NoticeCard(record = record, specialDeductionCents = specialDeductionCents)
    Spacer(Modifier.height(10.dp))

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.correction_10), color = CorPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = incomeText,
                onValueChange = {
                    incomeText = it
                    incomeError = false
                },
                label = { Text(stringResource(R.string.correction_11)) },
                isError = incomeError,
                supportingText = if (incomeError) {
                    { Text(stringResource(R.string.correction_12), color = CorWarn) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = prepaidText,
                onValueChange = {
                    prepaidText = it
                    prepaidError = false
                },
                label = { Text(stringResource(R.string.correction_13)) },
                isError = prepaidError,
                supportingText = if (prepaidError) {
                    { Text(stringResource(R.string.correction_12), color = CorWarn) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.correction_14), color = CorSecondaryText, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
    Spacer(Modifier.height(10.dp))

    settlement?.let { s ->
        ResultCard(settlement = s, original = record)
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.correction_20),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(CorBlue, RoundedCornerShape(14.dp))
                .clickable { showConfirm = true }
                .padding(vertical = 14.dp),
            textAlign = TextAlign.Center
        )
    }

    if (showConfirm && settlement != null) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = Color.White,
            title = {
                Text(
                    stringResource(R.string.correction_21),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.correction_22,
                        record.declarationNo,
                        if (settlement.isRefund) stringResource(R.string.correction_23, formatYuan(settlement.balanceCents))
                        else stringResource(R.string.correction_24, formatYuan(abs(settlement.balanceCents)))
                    ),
                    fontSize = 15.sp,
                    color = Color(0xFF5A616B),
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onSubmit(
                        DeclarationRecord(
                            id = "decl-${record.year}-cor-${System.currentTimeMillis()}",
                            year = record.year,
                            declarationNo = DeclarationRecord.generateNo(record.year, System.currentTimeMillis()),
                            submittedAt = System.currentTimeMillis(),
                            incomeCents = settlement.incomeCents,
                            taxableIncomeCents = settlement.taxableIncomeCents,
                            taxPayableCents = settlement.taxPayableCents,
                            taxPrepaidCents = settlement.taxPrepaidCents,
                            balanceCents = settlement.balanceCents,
                            status = "更正申报",
                            correctionOf = record.declarationNo,
                            correctionSeq = record.correctionSeq + 1
                        )
                    )
                }) {
                    Text(stringResource(R.string.correction_25), color = CorBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.correction_26), color = Color(0xFF6B7280))
                }
            }
        )
    }
}

@Composable
private fun NoticeCard(record: DeclarationRecord, specialDeductionCents: Long) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.correction_06, record.year),
                    color = CorPrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(CorBlue.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(record.status, color = CorBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.correction_07, record.declarationNo), color = CorSecondaryText, fontSize = 13.sp)
            Text(stringResource(R.string.correction_08, record.submittedAtLabel), color = CorSecondaryText, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = CorDivider)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Metric(stringResource(R.string.correction_09), "¥${formatYuan(record.incomeCents)}", Modifier.weight(1f))
                Metric(stringResource(R.string.correction_15), "¥${formatYuan(record.taxPrepaidCents)}", Modifier.weight(1f))
                Metric(
                    if (record.isRefund) stringResource(R.string.declarations_13) else stringResource(R.string.declarations_14),
                    // 标签已经说明是补还是退，数值不再带负号
                    "¥${formatYuan(abs(record.balanceCents))}",
                    Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.correction_27, formatYuan(specialDeductionCents)),
                color = CorSecondaryText,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ResultCard(settlement: TaxCalculator.Settlement, original: DeclarationRecord) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.correction_16), color = CorPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            CorRow(stringResource(R.string.reconcile_23), settlement.incomeCents)
            CorRow(stringResource(R.string.reconcile_24), settlement.basicDeductionCents)
            CorRow(stringResource(R.string.reconcile_19), settlement.specialDeductionCents)
            CorRow(stringResource(R.string.reconcile_25), settlement.taxableIncomeCents, bold = true)
            CorRowText(stringResource(R.string.reconcile_26), "${settlement.taxRatePercent}%")
            CorRow(stringResource(R.string.reconcile_27), settlement.taxPayableCents, bold = true)
            CorRow(stringResource(R.string.reconcile_28), settlement.taxPrepaidCents)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (settlement.isRefund) Color(0xFFEAF7F1) else Color(0xFFFDF3E7),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (settlement.isRefund) stringResource(R.string.declarations_13) else stringResource(R.string.declarations_14),
                    color = if (settlement.isRefund) CorGreen else CorWarn,
                    fontSize = 14.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "¥${formatYuan(abs(settlement.balanceCents))}",
                    color = if (settlement.isRefund) CorGreen else CorWarn,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun CorRowText(label: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(label, color = CorSecondaryText, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(text, color = CorPrimaryText, fontSize = 14.sp)
    }
}

@Composable
private fun CorRow(label: String, cents: Long, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(label, color = CorSecondaryText, fontSize = 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.weight(1f))
        Text(
            if (cents < 0) "-¥${formatYuan(-cents)}" else "¥${formatYuan(cents)}",
            color = CorPrimaryText,
            fontSize = 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = CorSecondaryText, fontSize = 12.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = CorPrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SubmittedCard(record: DeclarationRecord, onDone: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(CorGreen.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(stringResource(R.string.correction_28), color = CorGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.correction_29, record.year, record.correctionSeq), color = CorPrimaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.correction_30, record.declarationNo), color = CorSecondaryText, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = CorDivider)
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (record.isRefund) stringResource(R.string.declarations_13) else stringResource(R.string.declarations_14),
                    color = CorSecondaryText,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "¥${formatYuan(abs(record.balanceCents))}",
                    color = if (record.isRefund) CorGreen else CorWarn,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.correction_34), color = CorSecondaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(record.submittedAtLabel, color = CorPrimaryText, fontSize = 13.sp)
            }
            if (record.correctionOf != null) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(stringResource(R.string.correction_35), color = CorSecondaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(record.correctionOf, color = CorPrimaryText, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.correction_31),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(CorBlue, RoundedCornerShape(20.dp))
                    .clickable(onClick = onDone)
                    .padding(horizontal = 26.dp, vertical = 10.dp)
            )
        }
    }
}
