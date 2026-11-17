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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grsds.personal.ui.GlyphIcon

private val ArtPageBackground = Color(0xFFF5F7FB)
private val ArtPrimaryText = Color(0xFF171B22)
private val ArtSecondaryText = Color(0xFF7D8490)
private val ArtBlue = Color(0xFF168AF4)
private val ArtDivider = Color(0xFFEFF1F5)

@Composable
private fun ArticleTopBar(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            stringResource(R.string.article_01),
            color = ArtBlue,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Text(
            title,
            color = ArtPrimaryText,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/**
 * 政策资讯详情：标题 + 日期 + 正文段落 + 末尾的「本文仅供参考」声明。
 * 官方还有分享与收藏，这两个依赖系统分享面板和服务端，这里不做。
 */
@Composable
fun ArticleDetailScreen(
    articleId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val article = ArticleCenter.find(articleId)

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArtPageBackground)
            .statusBarsPadding()
    ) {
        ArticleTopBar(stringResource(R.string.article_02), onBack)

        if (article == null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(80.dp))
                Text(stringResource(R.string.article_03), color = ArtSecondaryText, fontSize = 15.sp)
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
                    Text(
                        stringResource(ArticleCenter.categories[article.categoryIndex]),
                        color = ArtBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(ArtBlue.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(article.title),
                        color = ArtPrimaryText,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 30.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.article_04),
                            color = ArtSecondaryText,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(article.date, color = ArtSecondaryText, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = ArtDivider)
                    Spacer(Modifier.height(16.dp))
                    article.body.forEachIndexed { index, paragraph ->
                        if (index > 0) Spacer(Modifier.height(13.dp))
                        Text(
                            stringResource(paragraph),
                            color = Color(0xFF3C4654),
                            fontSize = 15.sp,
                            lineHeight = 26.sp
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = ArtDivider)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.article_05),
                        color = ArtSecondaryText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/**
 * 文章列表：从首页「查看更多」或「相关政策」进来，默认展开对应分类，
 * 顶部可以切分类，点条目进详情。
 */
@Composable
fun ArticleListScreen(
    initialCategory: Int,
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember {
        mutableIntStateOf(initialCategory.coerceIn(0, ArticleCenter.categories.lastIndex))
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArtPageBackground)
            .statusBarsPadding()
    ) {
        ArticleTopBar(stringResource(R.string.article_06), onBack)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            ArticleCenter.categories.forEachIndexed { index, name ->
                val active = selected == index
                Text(
                    stringResource(name),
                    color = if (active) Color.White else Color(0xFF3C4654),
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .background(
                            if (active) ArtBlue else Color.White,
                            RoundedCornerShape(15.dp)
                        )
                        .clickable { selected = index }
                        .padding(horizontal = 13.dp, vertical = 6.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            ArticleCenter.byCategory(selected).forEach { item ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .clickable { onOpenArticle(item.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(item.accent, item.accent.copy(alpha = 0.55f))
                                    ),
                                    RoundedCornerShape(11.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            GlyphIcon(item.glyph, tint = Color.White, size = 22.dp, strokeWidth = 1.7.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(item.title),
                                color = ArtPrimaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 21.sp
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(item.date, color = ArtSecondaryText, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
