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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.grsds.personal.ui.Glyph
import com.grsds.personal.ui.GlyphIcon

private data class MoreEntry(
    @StringRes val label: Int,
    val glyph: Glyph,
    val color: Color,
    /** 对应 MainActivity 的路由键；为空表示这项还没实现。 */
    val target: String? = null
)

private val moreEntries = listOf(
    MoreEntry(R.string.more_01, Glyph.Chart, Color(0xFF2E7BF6), MessageCenter.TARGET_ANNUAL),
    MoreEntry(R.string.more_02, Glyph.Shield, Color(0xFF8B5CF6), MessageCenter.TARGET_DEDUCTIONS),
    MoreEntry(R.string.more_03, Glyph.Receipt, Color(0xFF18C7AC), MessageCenter.TARGET_INCOME),
    MoreEntry(R.string.more_04, Glyph.Printer, Color(0xFF2A9CF2), "issue"),
    MoreEntry(R.string.more_05, Glyph.Pencil, Color(0xFFFF9F43), "correction"),
    MoreEntry(R.string.more_06, Glyph.DocCheck, Color(0xFF6C7CF0), MessageCenter.TARGET_RECORDS),
    MoreEntry(R.string.more_07, Glyph.IdCard, Color(0xFF26B4A8), "profile"),
    MoreEntry(R.string.more_08, Glyph.Card, Color(0xFF1598F4), "bankcards"),
    MoreEntry(R.string.more_09, Glyph.People, Color(0xFF9B8AFB)),
    MoreEntry(R.string.more_10, Glyph.Briefcase, Color(0xFFF78CA0)),
    MoreEntry(R.string.more_11, Glyph.Stamp, Color(0xFF4FB0C6)),
    MoreEntry(R.string.more_12, Glyph.Headset, Color(0xFF9AB0C4))
)

/**
 * 「更多功能」：首页服务卡最后一张对应的全部功能入口。
 * 官方这里是可长按排序的宫格，排序依赖服务端保存的偏好，本地版先只做平铺。
 */
@Composable
fun MoreServicesScreen(
    onBack: () -> Unit,
    onLaunch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var unavailable by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.White)
        ) {
            Text(
                stringResource(R.string.article_01),
                color = Color(0xFF168AF4),
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                stringResource(R.string.more_00),
                color = Color(0xFF171B22),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 6.dp)) {
                    moreEntries.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { entry ->
                                MoreCell(
                                    entry = entry,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val target = entry.target
                                        if (target == null) {
                                            unavailable = context.getString(entry.label)
                                        } else {
                                            onLaunch(target)
                                        }
                                    }
                                )
                            }
                            // 最后一行不足 4 个时占位，保持列宽一致
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    unavailable?.let { name ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { unavailable = null },
            containerColor = Color.White,
            title = {
                Text(
                    stringResource(R.string.more_not_ready),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    stringResource(R.string.more_not_ready_body, name),
                    fontSize = 14.sp,
                    color = Color(0xFF5A616B),
                    lineHeight = 21.sp
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { unavailable = null }) {
                    Text(stringResource(R.string.more_ok), color = Color(0xFF168AF4), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun MoreCell(entry: MoreEntry, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(
                    Brush.linearGradient(listOf(entry.color.copy(alpha = 0.72f), entry.color)),
                    RoundedCornerShape(13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            GlyphIcon(entry.glyph, tint = Color.White, size = 24.dp, strokeWidth = 1.7.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(entry.label),
            color = Color(0xFF3C4654),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
