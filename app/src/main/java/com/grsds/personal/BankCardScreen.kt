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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.AppPreferencesRepository
import com.grsds.personal.data.BankCard
import com.grsds.personal.ui.Glyph
import com.grsds.personal.ui.GlyphIcon
import kotlinx.coroutines.launch

private val CardBlue = Color(0xFF168AF4)
private val CardPageBackground = Color(0xFFF5F7FB)
private val CardPrimaryText = Color(0xFF171B22)
private val CardSecondaryText = Color(0xFF7D8490)
private val CardDivider = Color(0xFFEFF1F5)
private val CardGreen = Color(0xFF12A66C)
private val CardWarn = Color(0xFFE5484D)

/**
 * 银行卡管理：展示已绑卡（卡号永远脱敏），支持绑定、解绑（二次确认）、
 * 设为默认退税收款账户。绑卡校验 16–19 位数字卡号，持卡人取自实名档案。
 */
@Composable
fun BankCardScreen(
    onBack: () -> Unit,
    onOpenLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AppPreferencesRepository.get(context) }
    val scope = rememberCoroutineScope()
    val session by repository.session.collectAsState(initial = null)
    val profile by repository.profile.collectAsState(initial = null)
    val cards by repository.bankCards.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingUnbind by remember { mutableStateOf<BankCard?>(null) }
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CardPageBackground)
            .statusBarsPadding()
    ) {
        CardTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (session == null) {
                LoginPromptCard(onOpenLogin = onOpenLogin)
            } else {
                NoticeCard()
                Spacer(Modifier.height(10.dp))
                if (cards.isEmpty()) {
                    EmptyCard()
                } else {
                    cards.forEach { card ->
                        BankCardItem(
                            card = card,
                            onSetDefault = {
                                scope.launch { repository.setDefaultBankCard(card.id) }
                            },
                            onUnbind = { pendingUnbind = card }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                AddButton(onClick = { showAddDialog = true })
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAddDialog && profile != null) {
        AddCardDialog(
            holderName = profile!!.name,
            existingCount = cards.size,
            onDismiss = { showAddDialog = false },
            onConfirm = { bankName, number ->
                scope.launch {
                    repository.addBankCard(
                        BankCard(
                            id = "card-${System.currentTimeMillis()}",
                            bankName = bankName,
                            cardNumber = number,
                            holderName = profile!!.name,
                            boundAt = System.currentTimeMillis()
                        )
                    )
                }
                showAddDialog = false
            }
        )
    }

    pendingUnbind?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingUnbind = null },
            containerColor = Color.White,
            title = {
                Text(
                    stringResource(R.string.bank_20),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    stringResource(R.string.bank_21, target.maskedNumber),
                    fontSize = 15.sp,
                    color = Color(0xFF5A616B)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingUnbind = null
                    scope.launch { repository.removeBankCard(target.id) }
                }) {
                    Text(stringResource(R.string.bank_22), color = CardWarn, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnbind = null }) {
                    Text(stringResource(R.string.bank_23), color = Color(0xFF6B7280))
                }
            }
        )
    }
}

@Composable
private fun CardTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.bank_01),
            color = CardBlue,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            stringResource(R.string.bank_02),
            color = CardPrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = CardDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun LoginPromptCard(onOpenLogin: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 34.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.bank_24), color = CardPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.bank_25), color = CardSecondaryText, fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.info_05),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(CardBlue, RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenLogin)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun NoticeCard() {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(16.dp)
        ) {
            GlyphIcon(Glyph.Card, tint = CardBlue, size = 17.dp, strokeWidth = 1.7.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                stringResource(R.string.bank_03),
                color = CardSecondaryText,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun BankCardItem(
    card: BankCard,
    onSetDefault: () -> Unit,
    onUnbind: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.bankName, color = CardPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (card.isDefault) {
                    Box(
                        modifier = Modifier
                            .background(CardGreen.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(stringResource(R.string.bank_04), color = CardGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.bank_05, card.cardNumber.takeLast(4)),
                color = CardPrimaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.bank_06, card.holderName.first() + "*".repeat((card.holderName.length - 1).coerceAtLeast(1)), card.boundAtLabel),
                color = CardSecondaryText,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = CardDivider)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (card.isDefault) stringResource(R.string.bank_07) else stringResource(R.string.bank_08),
                    color = if (card.isDefault) CardGreen else CardBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !card.isDefault, onClick = onSetDefault)
                )
                Text(
                    stringResource(R.string.bank_09),
                    color = CardWarn,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onUnbind)
                )
            }
        }
    }
}

@Composable
private fun EmptyCard() {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.bank_10), color = CardPrimaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.bank_11), color = CardSecondaryText, fontSize = 14.sp)
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    Text(
        stringResource(R.string.bank_12),
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(CardBlue, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun AddCardDialog(
    holderName: String,
    existingCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (bankName: String, cardNumber: String) -> Unit
) {
    var bankName by remember { mutableStateOf(BankCard.SUPPORTED_BANKS.first()) }
    var bankMenuOpen by remember { mutableStateOf(false) }
    var numberInput by remember { mutableStateOf("") }
    var numberError by remember { mutableStateOf(false) }
    val numberValid = BankCard.isValidCardNumber(numberInput)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                stringResource(R.string.bank_13),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.bank_14, holderName.first() + "*".repeat((holderName.length - 1).coerceAtLeast(1))),
                    color = CardSecondaryText,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                // 银行选择：点开下拉，来自退税支持的银行清单
                Box {
                    Surface(
                        color = Color(0xFFF3F5F9),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { bankMenuOpen = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)
                        ) {
                            Text(bankName, color = CardPrimaryText, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            GlyphIcon(Glyph.ChevronDown, tint = CardSecondaryText, size = 15.dp, strokeWidth = 2.dp)
                        }
                    }
                    DropdownMenu(expanded = bankMenuOpen, onDismissRequest = { bankMenuOpen = false }) {
                        BankCard.SUPPORTED_BANKS.forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank, fontSize = 15.sp) },
                                onClick = {
                                    bankName = bank
                                    bankMenuOpen = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = numberInput,
                    onValueChange = {
                        numberInput = it.filter { ch -> ch.isDigit() }.take(19)
                        numberError = false
                    },
                    label = { Text(stringResource(R.string.bank_15)) },
                    isError = numberError,
                    supportingText = if (numberError) {
                        { Text(stringResource(R.string.bank_16), color = CardWarn) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(if (existingCount == 0) R.string.bank_17 else R.string.bank_18),
                    color = CardSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (numberValid) {
                    onConfirm(bankName, numberInput)
                } else {
                    numberError = true
                }
            }) {
                Text(stringResource(R.string.bank_19), color = CardBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.bank_23), color = Color(0xFF6B7280))
            }
        }
    )
}
