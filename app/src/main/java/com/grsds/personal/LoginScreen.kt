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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.grsds.personal.data.LoginSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LoginBlue = Color(0xFF168AF4)
private val LoginPageBackground = Color(0xFFF5F7FB)
private val LoginPrimaryText = Color(0xFF171B22)
private val LoginSecondaryText = Color(0xFF7D8490)
private val LoginDivider = Color(0xFFEFF1F5)
private val LoginError = Color(0xFFE5484D)

/** 演示版固定验证码，任何手机号都用它登录。 */
private const val DEMO_CODE = "123456"

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AppPreferencesRepository.get(context) }
    val scope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var codeSent by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1_000)
            countdown -= 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LoginPageBackground)
            .statusBarsPadding()
    ) {
        LoginTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFFE7F2FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.login_01), color = LoginBlue, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.login_02), color = LoginPrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.login_03),
                color = LoginSecondaryText,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(26.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { input ->
                    phone = input.filter { it.isDigit() }.take(11)
                    error = null
                },
                label = { Text(stringResource(R.string.login_04)) },
                placeholder = { Text(stringResource(R.string.login_05)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { input ->
                        code = input.filter { it.isDigit() }.take(6)
                        error = null
                    },
                    label = { Text(stringResource(R.string.login_06)) },
                    placeholder = { Text(stringResource(R.string.login_07)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        if (phone.length != 11) {
                            error = context.getString(R.string.login_08)
                        } else {
                            codeSent = true
                            countdown = 60
                            error = null
                        }
                    },
                    enabled = countdown == 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEAF4FF),
                        contentColor = LoginBlue,
                        disabledContainerColor = Color(0xFFF1F3F7),
                        disabledContentColor = LoginSecondaryText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(
                        if (countdown > 0) stringResource(R.string.login_09, countdown) else stringResource(R.string.login_10),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (codeSent) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.login_11, phone.take(3), phone.takeLast(4), DEMO_CODE),
                    color = Color(0xFF12A66C),
                    fontSize = 12.sp
                )
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = LoginError, fontSize = 12.sp)
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = agreed,
                    onCheckedChange = { agreed = it; error = null },
                    colors = CheckboxDefaults.colors(checkedColor = LoginBlue),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.login_12),
                    color = LoginSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    when {
                        phone.length != 11 -> error = context.getString(R.string.login_08)
                        code != DEMO_CODE -> error = context.getString(R.string.login_13)
                        !agreed -> error = context.getString(R.string.login_14)
                        else -> scope.launch {
                            repository.login(
                                LoginSession(
                                    phone = phone,
                                    nickname = context.getString(R.string.login_15) + phone.takeLast(4),
                                    loginAt = System.currentTimeMillis()
                                )
                            )
                            onLoginSuccess()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LoginBlue),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.login_16), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.login_17),
                color = LoginSecondaryText,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LoginBlue,
    unfocusedBorderColor = Color(0xFFD7DCE5),
    focusedLabelColor = LoginBlue,
    cursorColor = LoginBlue
)

@Composable
private fun LoginTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.login_18),
            color = LoginBlue,
            fontSize = 17.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            stringResource(R.string.login_16),
            color = LoginPrimaryText,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(color = LoginDivider, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
