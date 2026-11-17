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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.AppPreferencesRepository
import com.grsds.personal.data.TaxRecordIssue
import kotlinx.coroutines.launch

private val IssuePageBackground = Color(0xFFF5F7FB)
private val IssuePrimaryText = Color(0xFF171B22)
private val IssueSecondaryText = Color(0xFF7D8490)
private val IssueBlue = Color(0xFF168AF4)

private val issuedYears = 2020..2026

@Composable
fun TaxRecordIssueScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenRecord: (year: Int, issuedAt: Long, verifyCode: String) -> Unit = { _, _, _ -> }
) {
    var selectedYear by remember { mutableIntStateOf(2025) }
    BackHandler(onBack = onBack)
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { AppPreferencesRepository.get(context) }
    val scope = rememberCoroutineScope()
    val issuedRecords by repository.taxRecordIssues.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IssuePageBackground)
            .statusBarsPadding()
    ) {
        IssueTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 开具说明
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color(0xFF15CDB1).copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.issue_01), color = Color(0xFF15CDB1), fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.issue_02), color = IssuePrimaryText, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.issue_03),
                        color = Color(0xFF3C4654),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            // 年度选择
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(stringResource(R.string.issue_04), color = IssuePrimaryText, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    issuedYears.chunked(4).forEach { years ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            years.forEach { year ->
                                val selected = selectedYear == year
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selected) IssueBlue else IssuePageBackground,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedYear = year }
                                        .padding(horizontal = 16.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.issue_05, year),
                                        color = if (selected) Color.White else Color(0xFF3C4654),
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                repository.issueTaxRecord(selectedYear, System.currentTimeMillis())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IssueBlue),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(stringResource(R.string.issue_06), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .size(width = 4.dp, height = 15.dp)
                        .background(IssueBlue, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.issue_07), color = IssuePrimaryText, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.issue_08, issuedRecords.size),
                    color = IssueSecondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 14.dp)
                )
            }
            Spacer(Modifier.height(10.dp))

            if (issuedRecords.isEmpty()) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.issue_16),
                            color = IssuePrimaryText,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.issue_17),
                            color = IssueSecondaryText,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            issuedRecords.forEach { record ->
                IssuedRecordCard(record) {
                    onOpenRecord(record.year, record.issuedAt, record.verifyCode)
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IssueTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.issue_09),
            color = IssueBlue,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            stringResource(R.string.issue_10),
            color = IssuePrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(
            color = Color(0xFFE8EBF1),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun IssuedRecordCard(record: TaxRecordIssue, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.issue_11, record.year),
                        color = IssuePrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        stringResource(R.string.issue_12, record.issuedAtLabel),
                        color = IssueSecondaryText,
                        fontSize = 13.sp
                    )
                }
                Text(
                    stringResource(R.string.issue_13),
                    color = Color(0xFF12A66C),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFF12A66C).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.issue_14, record.verifyCode),
                    color = IssueSecondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.issue_15),
                    color = IssueBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onClick)
                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}
