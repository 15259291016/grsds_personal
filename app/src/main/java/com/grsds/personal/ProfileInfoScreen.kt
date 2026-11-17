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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.grsds.personal.data.ProfileInfo
import kotlinx.coroutines.launch

private val InfoBlue = Color(0xFF168AF4)
private val InfoPageBackground = Color(0xFFF5F7FB)
private val InfoPrimaryText = Color(0xFF171B22)
private val InfoSecondaryText = Color(0xFF7D8490)
private val InfoDivider = Color(0xFFEFF1F5)
private val InfoGreen = Color(0xFF12A66C)

/**
 * 个人信息：实名要素（姓名、证件号、出生日期等）由登录手机号生成的档案带出、只读，
 * 证件号默认脱敏，可点「显示」核对；电子邮箱与联系地址对应官方可编辑的联系信息。
 */
@Composable
fun ProfileInfoScreen(
    onBack: () -> Unit,
    onOpenLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AppPreferencesRepository.get(context) }
    val scope = rememberCoroutineScope()
    val session by repository.session.collectAsState(initial = null)
    val profile by repository.profile.collectAsState(initial = null)
    var showId by rememberSaveable { mutableStateOf(false) }
    var editingContact by rememberSaveable { mutableStateOf(false) }
    var emailInput by rememberSaveable { mutableStateOf("") }
    var addressInput by rememberSaveable { mutableStateOf("") }
    var savedContact by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    LaunchedEffect(profile) {
        profile?.let {
            if (!editingContact) {
                emailInput = it.email
                addressInput = it.address
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(InfoPageBackground)
            .statusBarsPadding()
    ) {
        InfoTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (session == null || profile == null) {
                LoginPrompt(onOpenLogin = onOpenLogin)
            } else {
                val info = profile!!
                IdentityCard(
                    info = info,
                    showId = showId,
                    onToggleId = { showId = !showId }
                )
                Spacer(Modifier.height(10.dp))
                ResidenceCard(info = info)
                Spacer(Modifier.height(10.dp))
                ContactCard(
                    info = info,
                    editing = editingContact,
                    emailInput = emailInput,
                    addressInput = addressInput,
                    onEmailChange = { emailInput = it },
                    onAddressChange = { addressInput = it },
                    onEdit = { editingContact = true },
                    onSave = {
                        scope.launch {
                            repository.saveContactInfo(info.phone, emailInput, addressInput)
                        }
                        editingContact = false
                        savedContact = true
                    },
                    saved = savedContact
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.info_01),
            color = InfoBlue,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            stringResource(R.string.info_02),
            color = InfoPrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = InfoDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun LoginPrompt(onOpenLogin: () -> Unit) {
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
            Text(stringResource(R.string.info_03), color = InfoPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.info_04),
                color = InfoSecondaryText,
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
                    .background(InfoBlue, RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenLogin)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun IdentityCard(info: ProfileInfo, showId: Boolean, onToggleId: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.info_06), color = InfoPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(InfoGreen.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(stringResource(R.string.info_07), color = InfoGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            InfoRow(stringResource(R.string.info_08), if (showId) info.name else info.maskedName)
            InfoRow(stringResource(R.string.info_09), info.idType)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 13.dp)
            ) {
                Text(stringResource(R.string.info_10), color = InfoSecondaryText, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    if (showId) info.idNumber else info.maskedIdNumber,
                    color = InfoPrimaryText,
                    fontSize = 15.sp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(if (showId) R.string.info_11 else R.string.info_12),
                    color = InfoBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onToggleId)
                )
            }
            InfoRow(stringResource(R.string.info_13), info.birthDate)
            InfoRow(stringResource(R.string.info_14), info.maskedPhone)
        }
    }
}

@Composable
private fun ResidenceCard(info: ProfileInfo) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.info_15), color = InfoPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            InfoRow(stringResource(R.string.info_16), info.residence)
            InfoRow(stringResource(R.string.info_17), info.employer)
        }
    }
}

@Composable
private fun ContactCard(
    info: ProfileInfo,
    editing: Boolean,
    emailInput: String,
    addressInput: String,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    saved: Boolean
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
                Text(stringResource(R.string.info_18), color = InfoPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (!editing) {
                    Text(
                        stringResource(R.string.info_19),
                        color = InfoBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onEdit)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (editing) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = onEmailChange,
                    label = { Text(stringResource(R.string.info_20)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = addressInput,
                    onValueChange = onAddressChange,
                    label = { Text(stringResource(R.string.info_21)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.info_22),
                        color = InfoSecondaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onEmailChange(info.email); onAddressChange(info.address) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.info_23),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(InfoBlue, RoundedCornerShape(18.dp))
                            .clickable(onClick = onSave)
                            .padding(horizontal = 22.dp, vertical = 8.dp)
                    )
                }
            } else {
                InfoRow(stringResource(R.string.info_20), info.email.ifEmpty { stringResource(R.string.info_24) })
                InfoRow(stringResource(R.string.info_21), info.address.ifEmpty { stringResource(R.string.info_24) })
                if (saved) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.info_25),
                        color = InfoGreen,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp)
    ) {
        Text(label, color = InfoSecondaryText, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = InfoPrimaryText, fontSize = 15.sp)
    }
}
