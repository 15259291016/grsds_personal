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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.ui.GlyphIcon

private val MsgPageBackground = Color(0xFFF5F7FB)
private val MsgPrimaryText = Color(0xFF171B22)
private val MsgSecondaryText = Color(0xFF7D8490)
private val MsgBlue = Color(0xFF168AF4)
private val MsgDivider = Color(0xFFEFF1F5)

/**
 * 消息详情：官方点开消息是完整通知正文，末尾带「去办理」入口。
 * 未登录也能看（消息是系统推送），但跳转办理会走登录校验。
 */
@Composable
fun MessageDetailScreen(
    messageId: String?,
    onBack: () -> Unit,
    onOpenAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val item = MessageCenter.find(messageId)

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MsgPageBackground)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.White)
        ) {
            Text(
                stringResource(R.string.msg_01),
                color = MsgBlue,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                stringResource(R.string.msg_02),
                color = MsgPrimaryText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (item == null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(80.dp))
                Text(stringResource(R.string.msg_03), color = MsgSecondaryText, fontSize = 15.sp)
            }
            return@Column
        }

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
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(item.color.copy(alpha = 0.72f), item.color)
                                    ),
                                    RoundedCornerShape(11.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            GlyphIcon(item.glyph, tint = Color.White, size = 20.dp, strokeWidth = 1.7.dp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(item.category),
                            color = item.color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(item.color.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(item.title),
                        color = MsgPrimaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 27.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(item.time),
                        color = MsgSecondaryText,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MsgDivider)
                    Spacer(Modifier.height(16.dp))
                    item.body.forEachIndexed { index, paragraph ->
                        if (index > 0) Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(paragraph),
                            color = Color(0xFF3C4654),
                            fontSize = 15.sp,
                            lineHeight = 25.sp
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        stringResource(R.string.msg_04),
                        color = MsgSecondaryText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item.action?.let { action ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    stringResource(action.label),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MsgBlue, RoundedCornerShape(24.dp))
                        .clickable { onOpenAction(action.target) }
                        .padding(vertical = 13.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
