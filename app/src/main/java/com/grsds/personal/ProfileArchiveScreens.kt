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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.AppPreferencesRepository
import com.grsds.personal.data.EmploymentRecord
import com.grsds.personal.data.FamilyMember
import kotlinx.coroutines.launch

private val ArchBlue = Color(0xFF168AF4)
private val ArchPageBackground = Color(0xFFF5F7FB)
private val ArchPrimaryText = Color(0xFF171B22)
private val ArchSecondaryText = Color(0xFF7D8490)
private val ArchDivider = Color(0xFFEFF1F5)
private val ArchGreen = Color(0xFF12A66C)
private val ArchWarn = Color(0xFFE5484D)

/** 专项附加扣除里常用到的亲属关系。 */
private val familyRelations = listOf("配偶", "子女", "父母", "祖父母", "外祖父母", "其他")

@Composable
fun FamilyMemberScreen(
    onBack: () -> Unit,
    onOpenLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val repository = remember(context) { AppPreferencesRepository.get(context) }
    val scope = rememberCoroutineScope()
    val session by repository.session.collectAsState(initial = null)
    val members by repository.familyMembers.collectAsState(initial = emptyList())

    // 正在编辑的成员；blank id 表示新增
    var editing by remember { mutableStateOf<FamilyMember?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<FamilyMember?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArchPageBackground)
            .statusBarsPadding()
    ) {
        ArchiveTopBar(title = stringResource(R.string.family_00), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    stringResource(R.string.family_13),
                    color = ArchSecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
            if (session == null) {
                ArchiveLoginPrompt(onOpenLogin = onOpenLogin)
                Spacer(Modifier.height(16.dp))
            } else if (members.isEmpty()) {
                ArchiveEmpty(text = stringResource(R.string.family_12))
            }
            members.forEach { member ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clickable { editing = member }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            member.relation,
                            color = Color(0xFF6C4CF1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFF6C4CF1).copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                member.name,
                                color = ArchPrimaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(member.maskedIdNumber, color = ArchSecondaryText, fontSize = 13.sp)
                        }
                        Text(
                            stringResource(R.string.family_02),
                            color = ArchBlue,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
        if (session == null) {
            Spacer(Modifier.height(16.dp))
        }
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Button(
                onClick = { showNew = true },
                colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(46.dp)
            ) {
                Text(stringResource(R.string.family_01), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
    }

    if (showNew || editing != null) {
        FamilyMemberDialog(
            member = editing,
            existing = members,
            onDismiss = {
                showNew = false
                editing = null
            },
            onSave = { member ->
                showNew = false
                editing = null
                scope.launch { repository.saveFamilyMember(member) }
            },
            onDelete = {
                pendingDelete = editing
                editing = null
            }
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Color.White,
            title = {
                Text(stringResource(R.string.family_11_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    stringResource(R.string.family_11_body, target.name),
                    fontSize = 15.sp,
                    color = Color(0xFF5A616B)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch { repository.removeFamilyMember(target.id) }
                }) {
                    Text(stringResource(R.string.family_03), color = ArchWarn, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.family_10_cancel), color = ArchSecondaryText)
                }
            }
        )
    }
}

@Composable
private fun FamilyMemberDialog(
    member: FamilyMember?,
    existing: List<FamilyMember>,
    onDismiss: () -> Unit,
    onSave: (FamilyMember) -> Unit,
    onDelete: () -> Unit
) {
    var relation by remember { mutableStateOf(member?.relation ?: familyRelations.first()) }
    var name by remember { mutableStateOf(member?.name ?: "") }
    var idNumber by remember { mutableStateOf(member?.idNumber ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                if (member == null) stringResource(R.string.family_01) else stringResource(R.string.family_02),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    familyRelations.take(3).forEach { item ->
                        RelationChip(item, relation == item) { relation = item; error = null }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    familyRelations.drop(3).forEach { item ->
                        RelationChip(item, relation == item) { relation = item; error = null }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(20); error = null },
                    label = { Text(stringResource(R.string.family_05)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = idNumber,
                    onValueChange = { raw ->
                        idNumber = raw.filter { it.isDigit() || it == 'X' || it == 'x' }.take(18).uppercase()
                        error = null
                    },
                    label = { Text(stringResource(R.string.family_06)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(message, color = ArchWarn, fontSize = 13.sp)
                }
                if (member != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.family_03),
                        color = ArchWarn,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDelete)
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val now = System.currentTimeMillis()
                val nameErr = FamilyMember.nameError(name)
                val idErr = FamilyMember.idNumberError(idNumber)
                val duplicate = existing.any { it.id != member?.id && it.idNumber == idNumber }
                val err = nameErr ?: idErr ?: when {
                    duplicate -> "该证件号已添加过成员"
                    else -> null
                }
                if (err != null) {
                    error = err
                } else {
                    onSave(
                        FamilyMember(
                            id = member?.id ?: FamilyMember.generateId(name, now),
                            relation = relation,
                            name = name.trim(),
                            idNumber = idNumber.uppercase(),
                            updatedAt = now
                        )
                    )
                }
            }) {
                Text(stringResource(R.string.family_07), color = ArchBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.family_10_cancel), color = ArchSecondaryText)
            }
        }
    )
}

@Composable
private fun RelationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else Color(0xFF3C4654),
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .background(
                if (selected) ArchBlue else ArchPageBackground,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp)
    )
}

/* ----------------------------- 任职受雇信息 ----------------------------- */

@Composable
fun EmploymentInfoScreen(
    onBack: () -> Unit,
    onOpenLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val repository = remember(context) { AppPreferencesRepository.get(context) }
    val scope = rememberCoroutineScope()
    val session by repository.session.collectAsState(initial = null)
    val records by repository.employmentRecords.collectAsState(initial = emptyList())

    var editing by remember { mutableStateOf<EmploymentRecord?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<EmploymentRecord?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArchPageBackground)
            .statusBarsPadding()
    ) {
        ArchiveTopBar(title = stringResource(R.string.employ_00), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    stringResource(R.string.employ_14),
                    color = ArchSecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
            if (session == null) {
                ArchiveLoginPrompt(onOpenLogin = onOpenLogin)
                Spacer(Modifier.height(16.dp))
            } else if (records.isEmpty()) {
                ArchiveEmpty(text = stringResource(R.string.employ_13))
            }
            records.forEach { record ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clickable { editing = record }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                record.unitName,
                                color = ArchPrimaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (record.isCurrent) stringResource(R.string.employ_11_on) else stringResource(R.string.employ_11_off),
                                color = if (record.isCurrent) ArchGreen else ArchSecondaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        (if (record.isCurrent) ArchGreen else ArchSecondaryText).copy(alpha = 0.12f),
                                        RoundedCornerShape(9.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        if (record.taxNo.isNotBlank()) {
                            Text(
                                stringResource(R.string.employ_03, record.taxNo),
                                color = ArchSecondaryText,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(3.dp))
                        }
                        Text(
                            stringResource(R.string.employ_15, record.periodLabel),
                            color = ArchSecondaryText,
                            fontSize = 13.sp
                        )
                        if (record.position.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                stringResource(R.string.employ_07, record.position),
                                color = ArchSecondaryText,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
        if (session == null) {
            Spacer(Modifier.height(16.dp))
        }
        if (session != null) {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Button(
                    onClick = { showNew = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(46.dp)
                ) {
                    Text(stringResource(R.string.employ_01), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
        }
    }

    if (showNew || editing != null) {
        EmploymentDialog(
            record = editing,
            existing = records,
            onDismiss = {
                showNew = false
                editing = null
            },
            onSave = { saved ->
                showNew = false
                editing = null
                scope.launch { repository.saveEmployment(saved) }
            },
            onDelete = {
                pendingDelete = editing
                editing = null
            }
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Color.White,
            title = {
                Text(stringResource(R.string.employ_16_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    stringResource(R.string.employ_16_body, target.unitName),
                    fontSize = 15.sp,
                    color = Color(0xFF5A616B)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch { repository.removeEmployment(target.id) }
                }) {
                    Text(stringResource(R.string.family_03), color = ArchWarn, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.family_10_cancel), color = ArchSecondaryText)
                }
            }
        )
    }
}

@Composable
private fun EmploymentDialog(
    record: EmploymentRecord?,
    existing: List<EmploymentRecord>,
    onDismiss: () -> Unit,
    onSave: (EmploymentRecord) -> Unit,
    onDelete: () -> Unit
) {
    var unitName by remember { mutableStateOf(record?.unitName ?: "") }
    var taxNo by remember { mutableStateOf(record?.taxNo ?: "") }
    var startMonth by remember { mutableStateOf(record?.startMonth ?: "") }
    var endMonth by remember { mutableStateOf(record?.endMonth ?: "") }
    var position by remember { mutableStateOf(record?.position ?: "") }
    var current by remember { mutableStateOf(record?.isCurrent ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                if (record == null) stringResource(R.string.employ_01) else stringResource(R.string.employ_02),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = unitName,
                    onValueChange = { unitName = it.take(40); error = null },
                    label = { Text(stringResource(R.string.employ_04)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = taxNo,
                    onValueChange = { taxNo = it.uppercase().take(18); error = null },
                    label = { Text(stringResource(R.string.employ_05)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RelationChip(
                        stringResource(R.string.employ_11_on),
                        current
                    ) { current = true; error = null }
                    RelationChip(
                        stringResource(R.string.employ_11_off),
                        !current
                    ) { current = false; error = null }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = startMonth,
                    onValueChange = { raw -> startMonth = formatMonth(raw); error = null },
                    label = { Text(stringResource(R.string.employ_06)) },
                    placeholder = { Text("2023-05") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!current) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = endMonth,
                        onValueChange = { raw -> endMonth = formatMonth(raw); error = null },
                        label = { Text(stringResource(R.string.employ_08)) },
                        placeholder = { Text("2024-08") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it.take(20); error = null },
                    label = { Text(stringResource(R.string.employ_09)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(message, color = ArchWarn, fontSize = 13.sp)
                }
                if (record != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.family_03),
                        color = ArchWarn,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDelete)
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val now = System.currentTimeMillis()
                val err = EmploymentRecord.unitNameError(unitName.trim())
                    ?: EmploymentRecord.monthError(startMonth)
                    ?: (if (!current) EmploymentRecord.monthError(endMonth) else null)
                    ?: EmploymentRecord.taxNoError(taxNo)
                    ?: when {
                        !current && endMonth.isNotBlank() && startMonth.isNotBlank() &&
                            endMonth < startMonth -> "离职年月早于起始年月"
                        existing.any { it.id != record?.id && it.unitName == unitName.trim() } ->
                            "该单位已添加过"
                        else -> null
                    }
                if (err != null) {
                    error = err
                } else {
                    onSave(
                        EmploymentRecord(
                            id = record?.id ?: EmploymentRecord.generateId(unitName, now),
                            unitName = unitName.trim(),
                            taxNo = taxNo.trim(),
                            startMonth = startMonth,
                            endMonth = if (current) "" else endMonth,
                            position = position.trim(),
                            updatedAt = now
                        )
                    )
                }
            }) {
                Text(stringResource(R.string.family_07), color = ArchBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.family_10_cancel), color = ArchSecondaryText)
            }
        }
    )
}

/** 全数字输满 6 位时自动补横杠（202305 → 2023-05）；带横杠的输入原样保留。 */
private fun formatMonth(raw: String): String {
    val stripped = raw.filter { it.isDigit() || it == '-' }.take(7)
    val digits = stripped.filter { it.isDigit() }
    return if (!stripped.contains('-') && digits.length == 6) {
        digits.take(4) + "-" + digits.drop(4)
    } else {
        stripped
    }
}

/* -------------------------------- 公共件 -------------------------------- */

@Composable
private fun ArchiveTopBar(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.refund_17),
            color = ArchBlue,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            title,
            color = ArchPrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = ArchDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ArchiveLoginPrompt(onOpenLogin: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 34.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.info_03), color = ArchPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.info_04),
                color = ArchSecondaryText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.info_05),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(ArchBlue, RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenLogin)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun ArchiveEmpty(text: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text,
            color = ArchSecondaryText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp)
        )
    }
}
