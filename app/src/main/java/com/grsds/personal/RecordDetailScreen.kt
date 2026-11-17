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
import androidx.compose.ui.res.stringResource
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
            .background(DetailPageBackground)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
            title = { Text(stringResource(R.string.record_detail_01), fontSize = 18.sp) },
            text = {
                Text(
                    stringResource(R.string.record_detail_02, record.month, record.incomeType, record.amountLabel) +
                        stringResource(R.string.record_detail_03),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
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
                            snackbarHostState.showSnackbar(context.getString(R.string.record_detail_04))
                        }
                    }
                ) {
                    Text(stringResource(R.string.record_detail_05), color = DetailBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.record_detail_06), color = DetailSecondaryText)
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
            .background(Color.White)
            .statusBarsPadding()
            .height(50.dp)
    ) {
        Text(
            stringResource(R.string.record_detail_07),
            color = Color(0xFF2787E8),
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            stringResource(R.string.record_detail_08),
            color = DetailPrimaryText,
            fontSize = 17.sp,
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
                Text(record.incomeType, color = DetailPrimaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                StatusTag(record.disputed)
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.record_detail_09), color = DetailSecondaryText, fontSize = 13.sp)
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
                    Text(stringResource(R.string.record_detail_10), color = DetailSecondaryText, fontSize = 13.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(stringResource(R.string.record_detail_11, record.year, record.month), color = DetailPrimaryText, fontSize = 16.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.record_detail_12), color = DetailSecondaryText, fontSize = 13.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (record.disputed) stringResource(R.string.record_detail_13) else stringResource(R.string.record_detail_14),
                        color = if (record.disputed) DetailWarn else Color(0xFF1D9E75),
                        fontSize = 16.sp
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
            InfoRow(stringResource(R.string.record_detail_15), record.withholdingAgent)
            HorizontalDivider(color = DetailDivider)
            InfoRow(stringResource(R.string.record_detail_29), record.displayAgentTaxNo.ifBlank { "—" })
            HorizontalDivider(color = DetailDivider)
            InfoRow(stringResource(R.string.record_detail_16), record.incomeType)
            HorizontalDivider(color = DetailDivider)
            InfoRow(stringResource(R.string.record_detail_30), "¥${formatYuan(record.paidTaxCents)}")
            HorizontalDivider(color = DetailDivider)
            InfoRow(stringResource(R.string.record_detail_17), formatter.format(Date(record.createdAt)))
            HorizontalDivider(color = DetailDivider)
            InfoRow(stringResource(R.string.record_detail_18), stringResource(R.string.record_detail_19))
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
        Text(label, color = DetailSecondaryText, fontSize = 15.sp, modifier = Modifier.width(96.dp))
        Text(
            value,
            color = DetailPrimaryText,
            fontSize = 15.sp,
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
            Text(stringResource(R.string.record_detail_20), color = DetailPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (disputed) {
                    stringResource(R.string.record_detail_21)
                } else {
                    stringResource(R.string.record_detail_22)
                },
                color = DetailSecondaryText,
                fontSize = 13.sp,
                lineHeight = 21.sp
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
                        submitting -> stringResource(R.string.record_detail_23)
                        disputed -> stringResource(R.string.record_detail_24)
                        else -> stringResource(R.string.record_detail_01)
                    },
                    fontSize = 16.sp,
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
            if (disputed) stringResource(R.string.record_detail_25) else stringResource(R.string.record_detail_26),
            color = if (disputed) DetailWarn else Color(0xFF1D9E75),
            fontSize = 12.sp,
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
        Text(stringResource(R.string.record_detail_27), color = DetailPrimaryText, fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.record_detail_28), color = DetailSecondaryText, fontSize = 14.sp)
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun RecordDetailPreview() {
    MyApplicationTheme(dynamicColor = false, darkTheme = false) {
        RecordDetailScreen(recordId = "preview", onBack = {})
    }
}
