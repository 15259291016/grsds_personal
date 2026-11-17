package com.grsds.personal

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.grsds.personal.data.DeclarationRecord
import com.grsds.personal.data.SettlementProcess
import com.grsds.personal.data.formatYuan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val FlowBlue = Color(0xFF168AF4)
private val FlowPageBackground = Color(0xFFF5F7FB)
private val FlowPrimaryText = Color(0xFF171B22)
private val FlowSecondaryText = Color(0xFF7D8490)
private val FlowDivider = Color(0xFFEFF1F5)
private val FlowGreen = Color(0xFF12A66C)
private val FlowWarn = Color(0xFFE28A2D)

/* ------------------------------- 退税进度 ------------------------------- */

@Composable
fun RefundProgressScreen(
    declarationId: String,
    onBack: () -> Unit,
    onOpenBankCards: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val repository = remember(context) { AppPreferencesRepository.get(context) }
    val declarations by repository.declarations.collectAsState(initial = emptyList())
    val bankCards by repository.bankCards.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val declaration = declarations.firstOrNull { it.id == declarationId }

    // 未申请退税时展示选卡申请页；申请后同一页面切换为进度时间轴
    val applied = declaration?.hasAppliedRefund == true
    var selectedCardIndex by remember(declarationId) { mutableIntStateOf(0) }
    var applying by remember { mutableStateOf(false) }

    // 演示推进：进入页面后每 20 秒前进一档，退库完成后停在终点
    LaunchedEffect(declaration?.id, declaration?.processStatus) {
        val current = declaration ?: return@LaunchedEffect
        if (current.hasAppliedRefund && !SettlementProcess.isSettled(current.processStatus)) {
            delay(SettlementProcess.AUTO_ADVANCE_MILLIS)
            repository.advanceRefund(current.id)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlowPageBackground)
            .statusBarsPadding()
    ) {
        FlowTopBar(
            title = if (applied) stringResource(R.string.refund_16) else stringResource(R.string.refund_13),
            backLabel = stringResource(R.string.refund_17),
            onBack = onBack
        )
        when {
            declaration == null -> EmptyState(text = stringResource(R.string.refund_18))
            !applied -> {
                ApplyRefundBody(
                    declaration = declaration,
                    cards = bankCards,
                    selectedCardIndex = selectedCardIndex,
                    onSelectCard = { selectedCardIndex = it },
                    onOpenBankCards = onOpenBankCards,
                    modifier = Modifier.weight(1f)
                )
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Button(
                        onClick = {
                            val card = bankCards.getOrNull(selectedCardIndex) ?: return@Button
                            applying = true
                            scope.launch {
                                repository.applyRefund(
                                    declaration.id,
                                    card.id,
                                    "${card.bankName} ${card.maskedNumber}",
                                    System.currentTimeMillis()
                                )
                                applying = false
                            }
                        },
                        enabled = bankCards.isNotEmpty() && !applying && declaration.isActive,
                        colors = ButtonDefaults.buttonColors(containerColor = FlowBlue),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .height(46.dp)
                    ) {
                        if (applying) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.refund_13), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            }
            else -> {
                val done = declaration.processStatus == SettlementProcess.REFUNDED
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    RefundStatusHeader(declaration = declaration, done = done)
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                stringResource(R.string.refund_19),
                                color = FlowPrimaryText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(16.dp))
                            SettlementProcess.refundStages.forEachIndexed { index, stage ->
                                val reached = index <= SettlementProcess.stageIndex(declaration.processStatus)
                                RefundTimelineRow(
                                    stage = stage,
                                    reached = reached,
                                    current = stage == declaration.processStatus,
                                    isLast = index == SettlementProcess.refundStages.lastIndex,
                                    timeLabel = if (reached) stageTimeLabel(index, declaration.processedAt) else null
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                            InfoLine(stringResource(R.string.refund_20), declaration.bankCardLabel.ifBlank { "—" })
                            HorizontalDivider(color = FlowDivider)
                            InfoLine(stringResource(R.string.refund_21), declaration.declarationNo)
                            HorizontalDivider(color = FlowDivider)
                            InfoLine(stringResource(R.string.refund_22), declaration.processedAtLabel)
                            HorizontalDivider(color = FlowDivider)
                            InfoLine(stringResource(R.string.refund_23), stringResource(R.string.refund_24, declaration.year))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.refund_25),
                        color = FlowSecondaryText,
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                }
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = FlowBlue),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .height(46.dp)
                    ) {
                        Text(stringResource(R.string.refund_26), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            }
        }
    }

}

/** 未申请退税时的申请页：确认金额、选择收款账户。 */
@Composable
private fun ApplyRefundBody(
    declaration: DeclarationRecord,
    cards: List<com.grsds.personal.data.BankCard>,
    selectedCardIndex: Int,
    onSelectCard: (Int) -> Unit,
    onOpenBankCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF2E93F6), Color(0xFF147EEA))))
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Column {
                Text(
                    stringResource(R.string.refund_27),
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "¥${formatYuan(declaration.balanceCents)}",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.refund_24, declaration.year) + " · " + declaration.declarationNo,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    stringResource(R.string.refund_01),
                    color = FlowPrimaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.refund_02, formatYuan(declaration.balanceCents)),
                    color = FlowSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                if (cards.isEmpty()) {
                    Text(
                        stringResource(R.string.refund_03),
                        color = FlowWarn,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        stringResource(R.string.refund_04),
                        color = FlowBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(onClick = onOpenBankCards)
                            .padding(vertical = 6.dp)
                    )
                } else {
                    cards.forEachIndexed { index, card ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .background(
                                    if (selectedCardIndex == index) Color(0xFFEAF4FF) else Color(0xFFF7F9FC),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectCard(index) }
                                .padding(horizontal = 14.dp, vertical = 13.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    card.bankName,
                                    color = FlowPrimaryText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    stringResource(R.string.refund_11, card.maskedNumber),
                                    color = FlowSecondaryText,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(
                                        if (selectedCardIndex == index) FlowBlue else Color.White,
                                        CircleShape
                                    )
                                    .border(1.dp, if (selectedCardIndex == index) FlowBlue else Color(0xFFD7DADF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedCardIndex == index) {
                                    Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.refund_25),
            color = FlowSecondaryText,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun RefundStatusHeader(declaration: DeclarationRecord, done: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF2E93F6), Color(0xFF147EEA))))
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                stringResource(R.string.refund_27),
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "¥${formatYuan(declaration.balanceCents)}",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (done) stringResource(R.string.refund_28) else declaration.processStatus,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    done -> stringResource(R.string.refund_29, declaration.bankCardLabel)
                    declaration.processStatus == SettlementProcess.TREASURY ->
                        stringResource(R.string.refund_31)
                    else -> stringResource(R.string.refund_30)
                },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun RefundTimelineRow(
    stage: String,
    reached: Boolean,
    current: Boolean,
    isLast: Boolean,
    timeLabel: String?
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        if (reached) FlowBlue else Color(0xFFE3E8F0),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (reached && !current) "✓" else "",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(34.dp)
                        .background(if (reached) FlowBlue.copy(alpha = 0.35f) else Color(0xFFEFF1F5))
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 8.dp)) {
            Text(
                stage,
                color = if (reached) FlowPrimaryText else FlowSecondaryText,
                fontSize = 15.sp,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal
            )
            if (timeLabel != null) {
                Spacer(Modifier.height(2.dp))
                Text(timeLabel, color = FlowSecondaryText, fontSize = 12.sp)
            }
        }
    }
}

/** 各阶段的展示时间：以申请时间为基准按天递推，仅用于演示。 */
private fun stageTimeLabel(index: Int, processedAt: Long): String {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
    val offsetDays = when (index) {
        0 -> 0L
        1 -> 1L
        2 -> 3L
        else -> 5L
    }
    return formatter.format(Date(processedAt + offsetDays * 86_400_000L))
}

/* ------------------------------- 补税缴款 ------------------------------- */

@Composable
fun TaxPaymentScreen(
    declarationId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val repository = remember(context) { AppPreferencesRepository.get(context) }
    val declarations by repository.declarations.collectAsState(initial = emptyList())
    val bankCards by repository.bankCards.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val declaration = declarations.firstOrNull { it.id == declarationId }
    var selectedWay by remember { mutableIntStateOf(0) }
    var showConfirm by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf(false) }
    var localPaidNo by remember { mutableStateOf("") }
    val paid = declaration?.isPaid == true || localPaidNo.isNotBlank()

    val defaultCard = bankCards.firstOrNull { it.isDefault } ?: bankCards.firstOrNull()
    val wayLabels = listOf(
        stringResource(R.string.payment_01),
        stringResource(R.string.payment_02),
        stringResource(R.string.payment_03)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlowPageBackground)
            .statusBarsPadding()
    ) {
        FlowTopBar(
            title = stringResource(R.string.payment_04),
            backLabel = stringResource(R.string.refund_17),
            onBack = onBack
        )
        if (declaration == null || declaration.isRefund) {
            EmptyState(text = stringResource(R.string.payment_05))
            return@Column
        }

        if (paid) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(36.dp))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(FlowGreen.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = FlowGreen, fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.payment_06),
                    color = FlowPrimaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "¥${formatYuan(kotlin.math.abs(declaration.balanceCents))}",
                    color = FlowPrimaryText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(18.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        InfoLine(
                            stringResource(R.string.payment_07),
                            declaration.paymentNo.ifBlank { localPaidNo }
                        )
                        HorizontalDivider(color = FlowDivider)
                        InfoLine(stringResource(R.string.payment_08), declaration.processedAtLabel)
                        HorizontalDivider(color = FlowDivider)
                        InfoLine(stringResource(R.string.refund_21), declaration.declarationNo)
                        HorizontalDivider(color = FlowDivider)
                        InfoLine(stringResource(R.string.payment_09), declaration.year.toString())
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.payment_10),
                    color = FlowSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(Modifier.height(24.dp))
            }
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = FlowBlue),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(46.dp)
                ) {
                    Text(stringResource(R.string.payment_11), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFA24C), Color(0xFFF08228))))
                    .padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                Column {
                    Text(
                        stringResource(R.string.payment_12),
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "¥${formatYuan(kotlin.math.abs(declaration.balanceCents))}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.payment_13, declaration.year),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        stringResource(R.string.payment_14),
                        color = FlowPrimaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(12.dp))
                    wayLabels.forEachIndexed { index, label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .background(
                                    if (selectedWay == index) Color(0xFFEAF4FF) else Color(0xFFF7F9FC),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedWay = index }
                                .padding(horizontal = 14.dp, vertical = 13.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, color = FlowPrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                if (index == 0) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        if (defaultCard != null) {
                                            stringResource(R.string.payment_15, defaultCard.bankName, defaultCard.maskedNumber)
                                        } else {
                                            stringResource(R.string.payment_16)
                                        },
                                        color = FlowSecondaryText,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(
                                        if (selectedWay == index) FlowBlue else Color.White,
                                        CircleShape
                                    )
                                    .border(1.dp, if (selectedWay == index) FlowBlue else Color(0xFFD7DADF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedWay == index) {
                                    Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                    if (selectedWay == 0 && defaultCard == null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.payment_17),
                            color = FlowWarn,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = FlowDivider)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.payment_18),
                        color = FlowSecondaryText,
                        fontSize = 12.sp,
                        lineHeight = 19.sp
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.payment_19), color = FlowSecondaryText, fontSize = 12.sp)
                    Text(
                        "¥${formatYuan(kotlin.math.abs(declaration.balanceCents))}",
                        color = FlowWarn,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Button(
                    onClick = { showConfirm = true },
                    enabled = !paying && !(selectedWay == 0 && defaultCard == null),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF08228),
                        disabledContainerColor = Color(0xFFF3E3D3),
                        disabledContentColor = Color(0xFFB99C84)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 26.dp, vertical = 12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    if (paying) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (paying) stringResource(R.string.payment_20) else stringResource(R.string.payment_21),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    if (showConfirm && declaration != null) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.payment_22), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(
                        R.string.payment_23,
                        formatYuan(kotlin.math.abs(declaration.balanceCents)),
                        wayLabels[selectedWay]
                    ),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        paying = true
                        scope.launch {
                            delay(1_200)
                            val no = "PAY" + System.currentTimeMillis().toString().takeLast(10)
                            localPaidNo = no
                            repository.markPaid(declaration.id, no, System.currentTimeMillis())
                            paying = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.payment_24), color = FlowBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.payment_25), color = FlowSecondaryText)
                }
            }
        )
    }
}

/* -------------------------------- 公共件 -------------------------------- */

@Composable
private fun FlowTopBar(title: String, backLabel: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            backLabel,
            color = FlowBlue,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            title,
            color = FlowPrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = FlowDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = FlowSecondaryText, fontSize = 14.sp, modifier = Modifier.width(92.dp))
        Text(
            value,
            color = FlowPrimaryText,
            fontSize = 14.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(100.dp))
        Text(text, color = FlowSecondaryText, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
    }
}
