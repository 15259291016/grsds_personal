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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.data.AppPreferencesRepository
import com.grsds.personal.data.IncomeRecordRepository
import kotlinx.coroutines.launch

private val SetBlue = Color(0xFF168AF4)
private val SetPageBackground = Color(0xFFF5F7FB)
private val SetPrimaryText = Color(0xFF171B22)
private val SetSecondaryText = Color(0xFF7D8490)
private val SetDivider = Color(0xFFEFF1F5)
private val SetDanger = Color(0xFFE24B4A)

/**
 * 设置页。真实 APP 的设置里没有“批量录入”，
 * 这里把演示数据工具收进来，避免在首页/明细页放一个真实产品不存在的能力。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBatchCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordRepository = remember { IncomeRecordRepository.get(context) }
    val prefsRepository = remember { AppPreferencesRepository.get(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    // 事件回调里不能调 @Composable，提示文案先在这里取好
    val aboutToast = stringResource(R.string.settings_12)
    val clearedToast = stringResource(R.string.settings_15)
    BackHandler(onBack = onBack)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            containerColor = SetPageBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                SetTopBar(onBack = onBack)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))
                    GroupTitle(stringResource(R.string.settings_01))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.settings_02),
                                color = SetSecondaryText,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                            HorizontalDivider(color = SetDivider, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingRow(
                                title = stringResource(R.string.settings_03),
                                subtitle = stringResource(R.string.settings_04),
                                onClick = onOpenBatchCreate
                            )
                            HorizontalDivider(color = SetDivider, modifier = Modifier.padding(start = 16.dp))
                            SettingRow(
                                title = stringResource(R.string.settings_05),
                                subtitle = stringResource(R.string.settings_06),
                                danger = true,
                                onClick = { confirmClear = true }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    GroupTitle(stringResource(R.string.settings_07))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        Column {
                            SettingRow(title = stringResource(R.string.settings_08), subtitle = stringResource(R.string.settings_09), onClick = null)
                            HorizontalDivider(color = SetDivider, modifier = Modifier.padding(start = 16.dp))
                            SettingRow(
                                title = stringResource(R.string.settings_10),
                                subtitle = stringResource(R.string.settings_11),
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(aboutToast)
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.settings_13), fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.settings_14),
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        scope.launch {
                            recordRepository.clear()
                            prefsRepository.clearDeductions()
                            prefsRepository.clearDeclarations()
                            snackbarHostState.showSnackbar(clearedToast)
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_16), color = SetDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.settings_17)) }
            }
        )
    }
}

@Composable
private fun SetTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.settings_18),
            color = SetBlue,
            fontSize = 17.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            stringResource(R.string.settings_19),
            color = SetPrimaryText,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = SetDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun GroupTitle(title: String) {
    Text(
        title,
        color = SetSecondaryText,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 26.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (danger) SetDanger else SetPrimaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = SetSecondaryText, fontSize = 12.sp)
        }
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Text("›", color = SetSecondaryText, fontSize = 20.sp)
        }
    }
}
