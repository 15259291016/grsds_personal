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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.AppPreferencesRepository
import com.grsds.personal.data.DeclarationRecord
import com.grsds.personal.data.formatYuan
import androidx.annotation.StringRes
import kotlin.math.abs
import kotlinx.coroutines.launch

private val RecBlue = Color(0xFF168AF4)
private val RecPageBackground = Color(0xFFF5F7FB)
private val RecPrimaryText = Color(0xFF171B22)
private val RecSecondaryText = Color(0xFF7D8490)
private val RecDivider = Color(0xFFEFF1F5)
private val RecGreen = Color(0xFF12A66C)
private val RecWarn = Color(0xFFE28A2D)

/**
 * 申报记录查询：展示每年汇算的申报快照。
 * 数据来自本地落盘的 [DeclarationRecord]，冷启动后仍可查。
 */
@Composable
fun DeclarationRecordsScreen(
    onBack: () -> Unit,
    onOpenReconciliation: () -> Unit,
    onOpenCorrection: (String) -> Unit = {},
    onOpenRefundProgress: (String) -> Unit = {},
    onOpenTaxPayment: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AppPreferencesRepository.get(context) }
    val scope = rememberCoroutineScope()
    val declarations by repository.declarations.collectAsState(initial = emptyList())
    var pendingVoid by remember { mutableStateOf<DeclarationRecord?>(null) }
    BackHandler(onBack = onBack)

    val active = declarations.filter { it.isActive }
    val refundTotal = active.filter { it.isRefund }.sumOf { it.balanceCents }
    val payableTotal = active.filterNot { it.isRefund }.sumOf { abs(it.balanceCents) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RecPageBackground)
            .statusBarsPadding()
    ) {
        RecTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (declarations.isEmpty()) {
                EmptyState(onOpenReconciliation = onOpenReconciliation)
            } else {
                SummaryCard(
                    count = active.size,
                    refundTotal = refundTotal,
                    payableTotal = payableTotal
                )
                Spacer(Modifier.height(10.dp))
                declarations.forEach { record ->
                    DeclarationCard(
                        record = record,
                        onCorrect = { onOpenCorrection(record.id) },
                        onVoid = { pendingVoid = record },
                        onProcess = {
                            if (record.isRefund) onOpenRefundProgress(record.id) else onOpenTaxPayment(record.id)
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    pendingVoid?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingVoid = null },
            containerColor = Color.White,
            title = {
                Text(
                    stringResource(R.string.declarations_20),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    stringResource(R.string.declarations_21, target.declarationNo),
                    fontSize = 15.sp,
                    color = Color(0xFF5A616B)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingVoid = null
                    scope.launch { repository.voidDeclaration(target.id) }
                }) {
                    Text(stringResource(R.string.declarations_22), color = Color(0xFFE5484D), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingVoid = null }) {
                    Text(stringResource(R.string.declarations_23), color = Color(0xFF6B7280))
                }
            }
        )
    }
}

@Composable
private fun RecTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.declarations_01),
            color = RecBlue,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            stringResource(R.string.declarations_02),
            color = RecPrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = RecDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SummaryCard(count: Int, refundTotal: Long, payableTotal: Long) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.declarations_03), color = RecSecondaryText, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.declarations_04, count),
                color = RecPrimaryText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = RecDivider)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.declarations_05), color = RecSecondaryText, fontSize = 13.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "¥${formatYuan(refundTotal)}",
                        color = RecGreen,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.declarations_06), color = RecSecondaryText, fontSize = 13.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "¥${formatYuan(payableTotal)}",
                        color = RecWarn,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DeclarationCard(
    record: DeclarationRecord,
    onCorrect: () -> Unit,
    onVoid: () -> Unit,
    onProcess: () -> Unit
) {
    val statusColor = when {
        record.status == "已作废" -> Color(0xFF9AA0A8)
        record.status == "已更正" -> Color(0xFF9AA0A8)
        record.status == "更正申报" -> RecWarn
        else -> RecGreen
    }
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.declarations_07, record.year),
                    color = RecPrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(record.status, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    processTag(record)?.let { (label, color) ->
                        Box(
                            modifier = Modifier
                                .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.declarations_08, record.declarationNo), color = RecSecondaryText, fontSize = 13.sp)
            if (record.correctionOf != null) {
                Text(
                    stringResource(R.string.declarations_24, record.correctionOf, record.correctionSeq),
                    color = RecSecondaryText,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.declarations_09, record.submittedAtLabel), color = RecSecondaryText, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = RecDivider)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Metric(stringResource(R.string.declarations_10), "¥${formatYuan(record.incomeCents)}", Modifier.weight(1f))
                Metric(stringResource(R.string.declarations_11), "¥${formatYuan(record.taxPayableCents)}", Modifier.weight(1f))
                Metric(stringResource(R.string.declarations_12), "¥${formatYuan(record.taxPrepaidCents)}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (record.isRefund) Color(0xFFEAF7F1) else Color(0xFFFDF3E7), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (record.isRefund) stringResource(R.string.declarations_13) else stringResource(R.string.declarations_14),
                    color = if (record.isRefund) RecGreen else RecWarn,
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(8.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    "¥${formatYuan(abs(record.balanceCents))}",
                    color = if (record.isRefund) RecGreen else RecWarn,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
            if (record.isActive) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = RecDivider)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (processAction(record) != null) {
                        Text(
                            stringResource(processAction(record)!!),
                            color = RecGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onProcess)
                        )
                    }
                    Text(
                        stringResource(R.string.declarations_19),
                        color = RecBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = if (processAction(record) != null) Modifier.clickable(onClick = onCorrect) else Modifier
                            .weight(1f)
                            .clickable(onClick = onCorrect)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        stringResource(R.string.declarations_25),
                        color = Color(0xFFE5484D),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onVoid)
                    )
                }
            }
        }
    }
}

/** 办理状态标签：为空表示申报已终结或无需单独提示。 */
@Composable
private fun processTag(record: DeclarationRecord): Pair<String, Color>? = when {
    record.isActive && record.isRefund && !record.hasAppliedRefund ->
        stringResource(R.string.declarations_26) to RecWarn
    record.isActive && !record.isRefund && !record.isPaid ->
        stringResource(R.string.declarations_27) to RecWarn
    record.hasAppliedRefund -> record.processStatus to
        (if (com.grsds.personal.data.SettlementProcess.isSettled(record.processStatus)) RecGreen else RecBlue)
    else -> null
}

/** 卡片主操作：退税走进度页（未申请时页内申请），补税走缴款页。 */
private fun processAction(record: DeclarationRecord): Int? = when {
    record.isActive && record.isRefund && !record.hasAppliedRefund -> R.string.declarations_28
    record.isActive && record.isRefund &&
        record.hasAppliedRefund &&
        !com.grsds.personal.data.SettlementProcess.isSettled(record.processStatus) -> R.string.declarations_29
    record.isActive && !record.isRefund && !record.isPaid -> R.string.declarations_30
    else -> null
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = RecSecondaryText, fontSize = 12.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = RecPrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyState(onOpenReconciliation: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 30.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFE6F1FB), RoundedCornerShape(28.dp))
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Text(stringResource(R.string.declarations_15), color = RecBlue, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.declarations_16), color = RecPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.declarations_17),
                color = RecSecondaryText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.declarations_18),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(RecBlue, RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenReconciliation)
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            )
        }
    }
}
