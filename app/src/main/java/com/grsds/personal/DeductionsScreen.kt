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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.AppPreferencesRepository
import com.grsds.personal.data.DEDUCTION_FIELDS
import com.grsds.personal.data.DEDUCTION_OPTIONS
import com.grsds.personal.data.DeductionOption
import com.grsds.personal.data.DeductionFieldKind
import com.grsds.personal.data.DeductionRecord
import com.grsds.personal.data.DeductionRules
import com.grsds.personal.data.formatYuan
import kotlinx.coroutines.launch

private val DeducBlue = Color(0xFF168AF4)
private val DeducPageBackground = Color(0xFFF5F7FB)
private val DeducPrimaryText = Color(0xFF171B22)
private val DeducSecondaryText = Color(0xFF7D8490)
private val DeducDivider = Color(0xFFEFF1F5)
private val DeducGreen = Color(0xFF12A66C)
private val DeducWarn = Color(0xFFE5484D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeductionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AppPreferencesRepository.get(context) }
    val scope = rememberCoroutineScope()
    val deductions by repository.deductions.collectAsState(initial = emptyList())

    var year by remember { mutableIntStateOf(2026) }
    var sheetOption by remember { mutableStateOf<DeductionOption?>(null) }
    // 删除前二次确认的目标
    var pendingDelete by remember { mutableStateOf<DeductionOption?>(null) }
    BackHandler(onBack = onBack)

    val yearRecords = deductions.filter { it.year == year }
    val totalCents = yearRecords.sumOf { it.amountInCents }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeducPageBackground)
            .statusBarsPadding()
    ) {
        DeducTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // 年度切换 + 合计
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.deductions_01), color = DeducSecondaryText, fontSize = 14.sp)
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2025, 2026).forEach { option ->
                            val selected = year == option
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (selected) DeducBlue else DeducPageBackground,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { year = option }
                                    .padding(horizontal = 18.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    stringResource(R.string.deductions_02, option),
                                    color = if (selected) Color.White else Color(0xFF3C4654),
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = DeducDivider)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.deductions_03), color = DeducSecondaryText, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "¥${formatYuan(totalCents)}",
                                color = DeducPrimaryText,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            stringResource(R.string.deductions_04, yearRecords.size, DEDUCTION_OPTIONS.size),
                            color = DeducBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 项目列表
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column {
                    DEDUCTION_OPTIONS.forEachIndexed { index, option ->
                        val record = yearRecords.firstOrNull { it.type == option.type }
                        DeductionRow(option, record) { sheetOption = option }
                        if (index < DEDUCTION_OPTIONS.lastIndex) {
                            HorizontalDivider(
                                color = DeducDivider,
                                modifier = Modifier.padding(start = 58.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.deductions_05),
                color = DeducSecondaryText,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    val currentOption = sheetOption
    if (currentOption != null) {
        val existing = yearRecords.firstOrNull { it.type == currentOption.type }
        DeductionSheet(
            option = currentOption,
            year = year,
            existing = existing,
            onDismiss = { sheetOption = null },
            onSave = { ratio, details ->
                scope.launch {
                    repository.saveDeduction(
                        DeductionRecord(
                            type = currentOption.type,
                            year = year,
                            ratioPercent = ratio,
                            amountInCents = DeductionRules.effectiveAmountOf(currentOption, ratio, details),
                            updatedAt = System.currentTimeMillis(),
                            details = details
                        )
                    )
                    sheetOption = null
                }
            },
            onDelete = {
                // 先关弹层、弹确认框，避免误删已填的扣除信息
                pendingDelete = currentOption
                sheetOption = null
            }
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.deductions_06), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.deductions_07, target.type, year),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        scope.launch { repository.removeDeduction(target.type, year) }
                    }
                ) {
                    Text(stringResource(R.string.deductions_08), color = Color(0xFFE24B4A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.deductions_09)) }
            }
        )
    }
}

@Composable
private fun DeducTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.deductions_10),
            color = DeducBlue,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            stringResource(R.string.deductions_11),
            color = DeducPrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = DeducDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun DeductionRow(
    option: DeductionOption,
    record: DeductionRecord?,
    onClick: () -> Unit
) {
    val color = Color(option.colorHex)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.13f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(option.symbol, color = color, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(option.type, color = DeducPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (record != null) {
                    if (option.actualBased) {
                        stringResource(R.string.deductions_25, formatYuan(record.amountInCents))
                    } else {
                        stringResource(R.string.deductions_12, record.ratioPercent, formatYuan(record.amountInCents))
                    }
                } else {
                    stringResource(R.string.deductions_13, formatYuan(option.yearlyStandardInCents))
                },
                color = if (record != null) DeducGreen else DeducSecondaryText,
                fontSize = 13.sp
            )
            if (record != null && record.filledDetailCount > 0) {
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.deductions_24, record.filledDetailCount),
                    color = DeducSecondaryText,
                    fontSize = 12.sp
                )
            }
        }
        Text(
            if (record != null) stringResource(R.string.deductions_14) else stringResource(R.string.deductions_15),
            color = DeducBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(Color(0xFFEAF4FF), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeductionSheet(
    option: DeductionOption,
    year: Int,
    existing: DeductionRecord?,
    onDismiss: () -> Unit,
    onSave: (Int, Map<String, String>) -> Unit,
    onDelete: () -> Unit
) {
    var ratio by remember { mutableIntStateOf(existing?.ratioPercent ?: 100) }
    // 已填过的信息回填；选择型字段默认落到第一项，减少一次点击
    val details = remember(option.type, existing) {
        mutableStateMapOf<String, String>().apply {
            existing?.details?.forEach { (key, value) -> if (value.isNotBlank()) put(key, value) }
            DEDUCTION_FIELDS[option.type].orEmpty()
                .filter { it.kind == DeductionFieldKind.Choice }
                .forEach { field ->
                    if (this[field.key].isNullOrBlank()) put(field.key, field.options.first())
                }
        }
    }
    var error by remember { mutableStateOf<String?>(null) }
    var expandedChoice by remember { mutableStateOf<String?>(null) }
    val fields = DEDUCTION_FIELDS[option.type].orEmpty()
    val previewCents = DeductionRules.effectiveAmountOf(option, ratio, details.toMap())

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .heightIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Text(option.type, color = DeducPrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(5.dp))
            Text(
                option.ruleHint.ifBlank {
                    stringResource(R.string.deductions_16, year, formatYuan(option.yearlyStandardInCents))
                },
                color = DeducSecondaryText,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))

            if (!option.actualBased && option.supportsHalf) {
                Text(stringResource(R.string.deductions_17), color = DeducPrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(100, 50).forEach { value ->
                        val selected = ratio == value
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selected) DeducBlue else DeducPageBackground,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { ratio = value }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                        ) {
                            Text(
                                if (value == 100) stringResource(R.string.deductions_18) else stringResource(R.string.deductions_19),
                                color = if (selected) Color.White else Color(0xFF3C4654),
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text(
                stringResource(R.string.deductions_26),
                color = DeducPrimaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.deductions_27),
                color = DeducSecondaryText,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))

            fields.forEach { field ->
                when (field.kind) {
                    DeductionFieldKind.Choice -> {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Text(field.label, color = DeducSecondaryText, fontSize = 13.sp)
                            Spacer(Modifier.height(5.dp))
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DeducPageBackground, RoundedCornerShape(10.dp))
                                        .clickable { expandedChoice = field.key }
                                        .padding(horizontal = 14.dp, vertical = 13.dp)
                                ) {
                                    Text(
                                        details[field.key].orEmpty(),
                                        color = DeducPrimaryText,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("▾", color = DeducSecondaryText, fontSize = 14.sp)
                                }
                                DropdownMenu(
                                    expanded = expandedChoice == field.key,
                                    onDismissRequest = { expandedChoice = null }
                                ) {
                                    field.options.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item, fontSize = 15.sp) },
                                            onClick = {
                                                details[field.key] = item
                                                expandedChoice = null
                                                error = null
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = details[field.key].orEmpty(),
                            onValueChange = { input ->
                                details[field.key] = if (field.kind == DeductionFieldKind.Date) {
                                    input.filter { it.isDigit() || it == '-' }.take(10)
                                } else {
                                    input.take(40)
                                }
                                error = null
                            },
                            label = { Text(field.label) },
                            placeholder = { Text(field.placeholder, fontSize = 14.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (field.kind == DeductionFieldKind.Amount) {
                                    KeyboardType.Number
                                } else {
                                    KeyboardType.Text
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F8FF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    stringResource(R.string.deductions_20),
                    color = DeducSecondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "¥${formatYuan(previewCents)}",
                    color = DeducBlue,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }
            if (option.type == "大病医疗") {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(
                        R.string.deductions_28,
                        formatYuan(DeductionRules.MEDICAL_THRESHOLD_CENTS),
                        formatYuan(DeductionRules.MEDICAL_CAP_CENTS)
                    ),
                    color = DeducSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
            error?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(message, color = DeducWarn, fontSize = 13.sp)
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val invalid = DeductionRules.validate(option, details.toMap())
                    if (invalid != null) {
                        error = invalid
                    } else {
                        onSave(ratio, details.toMap())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeducBlue),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    if (existing != null) stringResource(R.string.deductions_21) else stringResource(R.string.deductions_22),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
            if (existing != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.deductions_23),
                    color = Color(0xFFE5484D),
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDelete)
                        .padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
