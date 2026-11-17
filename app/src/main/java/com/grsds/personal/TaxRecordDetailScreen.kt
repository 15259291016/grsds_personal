package com.grsds.personal

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.AppPreferencesRepository
import com.grsds.personal.data.IncomeRecord
import com.grsds.personal.data.IncomeRecordRepository
import com.grsds.personal.data.formatYuan

private val DocBlue = Color(0xFF168AF4)
private val DocPageBackground = Color(0xFFF0F2F6)
private val DocPrimaryText = Color(0xFF171B22)
private val DocSecondaryText = Color(0xFF7D8490)
private val DocDivider = Color(0xFFE8EBF1)
private val DocInk = Color(0xFF2A2F38)

/**
 * 个人所得税纳税记录（版式件）。数据仍取本机录入的纳税明细，
 * 按月份汇总后套用官方记录的排版：抬头、纳税人信息、逐月申报收入与已缴税额、合计、验真码。
 */
@Composable
fun TaxRecordDetailScreen(
    year: Int,
    issuedAt: Long,
    verifyCode: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val recordRepository = remember(context) { IncomeRecordRepository.get(context) }
    val prefsRepository = remember(context) { AppPreferencesRepository.get(context) }
    val allRecords by recordRepository.records.collectAsState(initial = emptyList())
    val profile by prefsRepository.profile.collectAsState(initial = null)
    val records = remember(allRecords, year) {
        allRecords.filter { it.year == year }
            .sortedWith(compareBy({ it.month }, { it.incomeType }))
    }
    val monthGroups = records.groupBy { it.month }.toList().sortedBy { it.first }
    val issuedAtLabel = remember(issuedAt) {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
            .format(java.util.Date(issuedAt))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DocPageBackground)
            .statusBarsPadding()
    ) {
        DocTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp)) {
                    Text(
                        stringResource(R.string.record_doc_01),
                        color = DocInk,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.record_doc_02, year),
                        color = DocInk,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = DocInk, thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))

                    DocInfoRow(stringResource(R.string.record_doc_03), profile?.maskedName ?: "—")
                    DocInfoRow(stringResource(R.string.record_doc_04), profile?.maskedIdNumber ?: "—")
                    DocInfoRow(stringResource(R.string.record_doc_05), issuedAtLabel)
                    DocInfoRow(stringResource(R.string.record_doc_06), stringResource(R.string.record_doc_07))

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = DocDivider)
                    Spacer(Modifier.height(10.dp))

                    // 表头
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DocHeaderCell(stringResource(R.string.record_doc_08), 0.26f)
                        DocHeaderCell(stringResource(R.string.record_doc_09), 0.34f)
                        DocHeaderCell(stringResource(R.string.record_doc_10), 0.40f, TextAlign.End)
                    }
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = DocDivider)

                    if (records.isEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            stringResource(R.string.record_doc_11),
                            color = DocSecondaryText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(20.dp))
                    } else {
                        monthGroups.forEach { (month, list) ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.record_doc_12, year, month),
                                color = DocInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            list.forEach { record ->
                                DocRecordRow(record)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = DocDivider)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DocHeaderCell(stringResource(R.string.record_doc_13), 0.26f)
                        Text(
                            stringResource(R.string.record_doc_14, records.size),
                            color = DocInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.34f)
                        )
                        Text(
                            "¥${formatYuan(records.sumOf { it.amountInCents })} / ¥${formatYuan(records.sumOf { it.paidTaxCents })}",
                            color = DocInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.40f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = DocInk, thickness = 1.dp)

                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.record_doc_15),
                                color = DocSecondaryText,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.record_doc_16, issuedAtLabel),
                                color = DocInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.record_doc_17),
                                color = DocSecondaryText,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(verifyCode, color = DocInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        FakeQrCode(seedText = verifyCode, modifier = Modifier.size(78.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.record_doc_18),
                        color = DocPrimaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.record_doc_19),
                        color = DocSecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.record_doc_20),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF4FF), contentColor = DocBlue),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text(stringResource(R.string.record_doc_21), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.record_doc_22, verifyCode),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DocBlue),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text(stringResource(R.string.record_doc_23), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun DocTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.record_doc_24),
            color = DocBlue,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            stringResource(R.string.record_doc_25),
            color = DocPrimaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = DocDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun DocInfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(label, color = DocSecondaryText, fontSize = 13.sp, modifier = Modifier.width(74.dp))
        Text(value, color = DocInk, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DocHeaderCell(
    text: String,
    weight: Float,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text,
        color = DocSecondaryText,
        fontSize = 12.sp,
        textAlign = align,
        modifier = Modifier.weight(weight)
    )
}

@Composable
private fun DocRecordRow(record: IncomeRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.record_doc_26, record.month),
            color = DocInk,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.26f)
        )
        Text(
            record.incomeType,
            color = DocInk,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.34f)
        )
        Text(
            "¥${formatYuan(record.amountInCents)} / ¥${formatYuan(record.paidTaxCents)}",
            color = DocInk,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.40f)
        )
    }
}

/** 伪二维码：按验真码确定性地铺 21×21 方块，带三个定位角，仅作版式示意。 */
@Composable
private fun FakeQrCode(seedText: String, modifier: Modifier = Modifier) {
    val cells = 21
    val seed = remember(seedText) {
        seedText.fold(17L) { acc, ch -> (acc * 31 + ch.code) and 0x7FFFFFFF }
    }
    Box(
        modifier = modifier
            .background(Color.White)
            .border(1.dp, DocDivider)
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = size.width / cells
            for (row in 0 until cells) {
                for (col in 0 until cells) {
                    val inFinder = (row < 7 && col < 7) || (row < 7 && col >= cells - 7) || (row >= cells - 7 && col < 7)
                    val on = if (inFinder) {
                        val r = if (row >= cells - 7) row - (cells - 7) else row
                        val c = if (col >= cells - 7) col - (cells - 7) else col
                        r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4)
                    } else {
                        val mixed = (seed xor (row * 73856093L) xor (col * 19349663L))
                        mixed % 5L < 2L
                    }
                    if (on) {
                        drawRect(
                            color = DocInk,
                            topLeft = Offset(col * cell, row * cell),
                            size = Size(cell * 0.92f, cell * 0.92f)
                        )
                    }
                }
            }
        }
    }
}
