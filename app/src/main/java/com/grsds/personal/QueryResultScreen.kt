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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.IncomeRecord
import com.grsds.personal.data.IncomeRecordRepository
import com.grsds.personal.data.formatYuan
import com.grsds.personal.ui.theme.MyApplicationTheme
import com.grsds.personal.ui.Glyph
import com.grsds.personal.ui.GlyphIcon

private val ResultBlue = Color(0xFF168AF4)
private val ResultPageBackground = Color(0xFFF5F7FB)
private val ResultPrimaryText = Color(0xFF171B22)
private val ResultSecondaryText = Color(0xFF7D8490)
private val ResultDivider = Color(0xFFEFF1F5)
private val ResultWarn = Color(0xFFE28A2D)

@Composable
fun QueryResultScreen(
    year: Int,
    incomeTypes: List<String>,
    onBack: () -> Unit,
    onOpenRecord: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenBatchCreate: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val repository = remember(context) { IncomeRecordRepository.get(context) }
    val allRecords by repository.records.collectAsState(initial = emptyList())

    val records = allRecords.filter { it.year == year && it.incomeType in incomeTypes }
    val totalCents = records.sumOf { it.amountInCents }
    val prepaidCents = records.sumOf { it.paidTaxCents }
    val monthGroups = records.groupBy { it.month }.toList().sortedByDescending { it.first }
    val typeGroups = records.groupBy { it.incomeType }
        .map { (type, list) -> type to list.sumOf { it.amountInCents } }
        .sortedByDescending { it.second }
    // 按扣缴单位汇总：核对“这笔钱是哪家单位报的”
    val agentGroups = records.groupBy { it.withholdingAgent }
        .map { (agent, list) ->
            AgentSummary(
                agent = agent,
                count = list.size,
                incomeCents = list.sumOf { it.amountInCents },
                taxCents = list.sumOf { it.paidTaxCents }
            )
        }
        .sortedByDescending { it.incomeCents }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ResultPageBackground)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ResultPageBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                ResultTopBar(
                    title = stringResource(R.string.query_19, year),
                    onBack = onBack
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        SummaryCard(
                            year = year,
                            totalCents = totalCents,
                            prepaidCents = prepaidCents,
                            recordCount = records.size,
                            typeCount = typeGroups.size,
                            monthCount = monthGroups.size,
                            selectedTypes = incomeTypes
                        )
                    }

                    if (records.isEmpty()) {
                        item {
                            EmptyResultCard(onOpenBatchCreate = onOpenBatchCreate)
                        }
                    } else {
                        if (typeGroups.size > 1) {
                            item {
                                TypeBreakdownCard(
                                    typeGroups = typeGroups,
                                    totalCents = totalCents
                                )
                            }
                        }
                        if (agentGroups.size > 1) {
                            item {
                                AgentBreakdownCard(
                                    agents = agentGroups,
                                    totalCents = totalCents
                                )
                            }
                        }
                        monthGroups.forEach { (month, list) ->
                            item(key = "header-$month") {
                                MonthHeader(
                                    year = year,
                                    month = month,
                                    subtotalCents = list.sumOf { it.amountInCents },
                                    subtotalTaxCents = list.sumOf { it.paidTaxCents },
                                    count = list.size
                                )
                            }
                            items(count = list.size, key = { index -> list[index].id }) { index ->
                                val record = list[index]
                                ResultRecordRow(
                                    record = record,
                                    onClick = { onOpenRecord(record.id) },
                                    showDivider = index != list.lastIndex
                                )
                            }
                        }
                        item {
                            Text(
                                stringResource(R.string.query_20),
                                color = ResultSecondaryText,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultTopBar(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .height(50.dp)
    ) {
        Text(
            stringResource(R.string.query_01),
            color = Color(0xFF2787E8),
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            title,
            color = ResultPrimaryText,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 76.dp)
        )
        HorizontalDivider(
            color = ResultDivider,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SummaryCard(
    year: Int,
    totalCents: Long,
    prepaidCents: Long,
    recordCount: Int,
    typeCount: Int,
    monthCount: Int,
    selectedTypes: List<String>
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.query_02, year),
                color = ResultSecondaryText,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                formatYuan(totalCents),
                color = ResultPrimaryText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(6.dp))
            Row {
                Text(
                    stringResource(R.string.query_22, formatYuan(prepaidCents)),
                    color = ResultSecondaryText,
                    fontSize = 13.sp
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    stringResource(
                        R.string.query_23,
                        formatYuan(if (monthCount > 0) totalCents / monthCount else 0L)
                    ),
                    color = ResultSecondaryText,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.query_03) + selectedTypes.joinToString("、"),
                color = ResultSecondaryText,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = ResultDivider)
            Spacer(Modifier.height(12.dp))
            Row {
                SummaryMetric(stringResource(R.string.query_04), "$recordCount", Modifier.weight(1f))
                SummaryMetric(stringResource(R.string.query_05), "$typeCount", Modifier.weight(1f))
                SummaryMetric(stringResource(R.string.query_06), "$monthCount", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, color = ResultPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = ResultSecondaryText, fontSize = 13.sp)
    }
}

/** 扣缴单位维度的汇总行。 */
private data class AgentSummary(
    val agent: String,
    val count: Int,
    val incomeCents: Long,
    val taxCents: Long
)

@Composable
private fun AgentBreakdownCard(agents: List<AgentSummary>, totalCents: Long) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.query_25),
                    color = ResultPrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.query_26, agents.size),
                    color = ResultSecondaryText,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            agents.forEachIndexed { index, agent ->
                if (index > 0) {
                    HorizontalDivider(color = ResultDivider)
                }
                Column(modifier = Modifier.padding(vertical = 9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            agent.agent,
                            color = ResultPrimaryText,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatYuan(agent.incomeCents),
                            color = ResultPrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(
                                R.string.query_27,
                                agent.count,
                                if (totalCents > 0) (agent.incomeCents * 100 / totalCents).toInt() else 0
                            ),
                            color = ResultSecondaryText,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            stringResource(R.string.query_28, formatYuan(agent.taxCents)),
                            color = ResultSecondaryText,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeBreakdownCard(
    typeGroups: List<Pair<String, Long>>,
    totalCents: Long
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.query_07), color = ResultPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            typeGroups.forEach { (type, cents) ->
                val ratio = if (totalCents > 0) cents.toFloat() / totalCents else 0f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        type,
                        color = ResultSecondaryText,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(104.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .background(Color(0xFFEFF3F9), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio.coerceIn(0.02f, 1f))
                                .height(7.dp)
                                .background(ResultBlue, RoundedCornerShape(4.dp))
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        formatYuan(cents),
                        color = ResultPrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    year: Int,
    month: Int,
    subtotalCents: Long,
    subtotalTaxCents: Long,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(15.dp)
                .background(ResultBlue, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(7.dp))
        Text(
            stringResource(R.string.query_08, year, month),
            color = ResultPrimaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(stringResource(R.string.query_09, count), color = ResultSecondaryText, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                stringResource(R.string.query_10, formatYuan(subtotalCents)),
                color = ResultBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.query_24, formatYuan(subtotalTaxCents)),
                color = ResultSecondaryText,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ResultRecordRow(
    record: IncomeRecord,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(record.incomeType, color = ResultPrimaryText, fontSize = 16.sp)
                    if (record.disputed) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFFDF1E2),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                stringResource(R.string.query_11),
                                color = ResultWarn,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    record.withholdingAgent,
                    color = ResultSecondaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.query_21, formatYuan(record.paidTaxCents)),
                    color = ResultSecondaryText,
                    fontSize = 12.sp
                )
            }
            Text(
                "¥${record.amountLabel}",
                color = ResultPrimaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(6.dp))
            GlyphIcon(Glyph.ChevronRight, tint = Color(0xFFD7DADF), size = 14.dp, strokeWidth = 2.dp)
        }
        if (showDivider) {
            HorizontalDivider(
                color = ResultDivider,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 14.dp)
            )
        }
    }
}

@Composable
private fun EmptyResultCard(onOpenBatchCreate: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 30.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.query_12), color = ResultPrimaryText, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.query_13),
                color = ResultSecondaryText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onOpenBatchCreate,
                colors = ButtonDefaults.buttonColors(containerColor = ResultBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text(stringResource(R.string.query_14), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun QueryResultPreview() {
    MyApplicationTheme(dynamicColor = false, darkTheme = false) {
        QueryResultScreen(
            year = 2026,
            incomeTypes = listOf(stringResource(R.string.query_15), stringResource(R.string.query_16), stringResource(R.string.query_17), stringResource(R.string.query_18)),
            onBack = {},
            onOpenRecord = {}
        )
    }
}
