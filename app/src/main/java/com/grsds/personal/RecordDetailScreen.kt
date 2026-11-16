package com.grsds.personal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.IncomeRecord
import com.grsds.personal.data.IncomeRecordRepository
import com.grsds.personal.data.formatYuan
import com.grsds.personal.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val DetailBlue = Color(0xFF168AF4)
private val DetailPageBackground = Color(0xFFF5F7FB)
private val DetailPrimaryText = Color(0xFF171B22)
private val DetailSecondaryText = Color(0xFF7D8490)
private val DetailDivider = Color(0xFFEFF1F5)
private val DetailWarn = Color(0xFFE28A2D)

@Composable
fun RecordDetailScreen(
    recordId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val repository = remember(context) { IncomeRecordRepository.get(context) }
    val allRecords by repository.records.collectAsState(initial = emptyList())
    val record = allRecords.firstOrNull { it.id == recordId }

    var showConfirm by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            containerColor = DetailPageBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                DetailTopBar(onBack = onBack)

                if (record == null) {
                    MissingRecordHint()
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        AmountCard(record = record)
                        Spacer(Modifier.height(10.dp))
                        InfoCard(record = record)
                        Spacer(Modifier.height(10.dp))
                        AppealCard(
                            disputed = record.disputed,
                            submitting = submitting,
                            onAppeal = { showConfirm = true }
                        )
                    }
                }
            }
        }
    }

    if (showConfirm && record != null) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("发起异议申诉", fontSize = 17.sp) },
            text = {
                Text(
                    "确认对 ${record.month} 月「${record.incomeType}」这笔 ${record.amountLabel} 元的记录发起申诉？" +
                        "提交后税务机关将在 3 个工作日内核实，期间请留意消息通知。",
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        submitting = true
                        scope.launch {
                            repository.markDisputed(record.id)
                            submitting = false
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("申诉已提交，请留意后续核实结果")
                        }
                    }
                ) {
                    Text("确认提交", color = DetailBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("再想想", color = DetailSecondaryText)
                }
            }
        )
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit) {
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
            "明细详情",
            color = DetailPrimaryText,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = DetailDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun AmountCard(record: IncomeRecord) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(record.incomeType, color = DetailPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                StatusTag(record.disputed)
            }
            Spacer(Modifier.height(10.dp))
            Text("收入额（元）", color = DetailSecondaryText, fontSize = 12.sp)
            Text(
                formatYuan(record.amountInCents),
                color = DetailPrimaryText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DetailDivider)
            Spacer(Modifier.height(12.dp))
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text("所得期间", color = DetailSecondaryText, fontSize = 12.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("${record.year} 年 ${record.month} 月", color = DetailPrimaryText, fontSize = 15.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("申报状态", color = DetailSecondaryText, fontSize = 12.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (record.disputed) "申诉核实中" else "已申报 · 已缴清",
                        color = if (record.disputed) DetailWarn else Color(0xFF1D9E75),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(record: IncomeRecord) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA) }
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            InfoRow("扣缴义务人", record.withholdingAgent)
            HorizontalDivider(color = DetailDivider)
            InfoRow("所得项目", record.incomeType)
            HorizontalDivider(color = DetailDivider)
            InfoRow("录入时间", formatter.format(Date(record.createdAt)))
            HorizontalDivider(color = DetailDivider)
            InfoRow("数据来源", "本机录入（演示数据）")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = DetailSecondaryText, fontSize = 14.sp, modifier = Modifier.width(96.dp))
        Text(
            value,
            color = DetailPrimaryText,
            fontSize = 14.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AppealCard(disputed: Boolean, submitting: Boolean, onAppeal: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("对这笔记录有疑问？", color = DetailPrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (disputed) {
                    "申诉已提交，税务机关正在核实，结果会通过消息通知你。"
                } else {
                    "如果这笔收入并非本人取得，或金额与实际不符，可以向税务机关发起异议申诉。"
                },
                color = DetailSecondaryText,
                fontSize = 12.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onAppeal,
                enabled = !disputed && !submitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DetailBlue,
                    disabledContainerColor = Color(0xFFE6EEF9),
                    disabledContentColor = Color(0xFF9FB2C8)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    when {
                        submitting -> "提交中…"
                        disputed -> "申诉处理中"
                        else -> "发起异议申诉"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatusTag(disputed: Boolean) {
    Surface(
        color = if (disputed) Color(0xFFFDF1E2) else Color(0xFFE6F6F0),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            if (disputed) "申诉中" else "已申报",
            color = if (disputed) DetailWarn else Color(0xFF1D9E75),
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun MissingRecordHint() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(120.dp))
        Text("记录不存在或已被删除", color = DetailPrimaryText, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Text("请返回上一页重新查询", color = DetailSecondaryText, fontSize = 13.sp)
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun RecordDetailPreview() {
    MyApplicationTheme(dynamicColor = false, darkTheme = false) {
        RecordDetailScreen(recordId = "preview", onBack = {})
    }
}
