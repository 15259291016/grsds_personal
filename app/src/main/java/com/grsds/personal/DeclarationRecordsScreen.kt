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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import kotlin.math.abs

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AppPreferencesRepository.get(context) }
    val declarations by repository.declarations.collectAsState(initial = emptyList())
    BackHandler(onBack = onBack)

    val refundTotal = declarations.filter { it.isRefund }.sumOf { it.balanceCents }
    val payableTotal = declarations.filterNot { it.isRefund }.sumOf { abs(it.balanceCents) }

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
                    count = declarations.size,
                    refundTotal = refundTotal,
                    payableTotal = payableTotal
                )
                Spacer(Modifier.height(10.dp))
                declarations.forEach { record ->
                    DeclarationCard(record = record)
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
        }
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
            fontSize = 17.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            stringResource(R.string.declarations_02),
            color = RecPrimaryText,
            fontSize = 19.sp,
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
            Text(stringResource(R.string.declarations_03), color = RecSecondaryText, fontSize = 13.sp)
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
                    Text(stringResource(R.string.declarations_05), color = RecSecondaryText, fontSize = 12.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "¥${formatYuan(refundTotal)}",
                        color = RecGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.declarations_06), color = RecSecondaryText, fontSize = 12.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "¥${formatYuan(payableTotal)}",
                        color = RecWarn,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DeclarationCard(record: DeclarationRecord) {
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(RecGreen.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(record.status, color = RecGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.declarations_08, record.declarationNo), color = RecSecondaryText, fontSize = 12.sp)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.declarations_09, record.submittedAtLabel), color = RecSecondaryText, fontSize = 12.sp)
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
                    fontSize = 13.sp
                )
                Spacer(Modifier.width(8.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    "¥${formatYuan(abs(record.balanceCents))}",
                    color = if (record.isRefund) RecGreen else RecWarn,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = RecSecondaryText, fontSize = 11.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = RecPrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
            Text(stringResource(R.string.declarations_16), color = RecPrimaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.declarations_17),
                color = RecSecondaryText,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.declarations_18),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(RecBlue, RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenReconciliation)
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            )
        }
    }
}
