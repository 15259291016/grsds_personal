package com.example.myapplication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.util.Calendar
import kotlinx.coroutines.launch

private val DetailsBlue = Color(0xFF3D8CF2)
private val DetailsBackground = Color(0xFFF5F7FA)
private val DetailsText = Color(0xFF25282D)
private val DetailsDivider = Color(0xFFE8EAED)

private val incomeTypes = listOf(
    "工资薪金",
    "劳务报酬",
    "稿酬",
    "特许权使用费",
    "经营所得",
    "利息、股息、红利",
    "财产租赁",
    "财产转让",
    "偶然所得"
)

@Composable
fun IncomeTaxDetailsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var selectedYear by remember {
        mutableStateOf(Calendar.getInstance().get(Calendar.YEAR))
    }
    var pendingYear by remember { mutableStateOf(selectedYear) }
    var otherTypesExpanded by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    val checkedTypes = remember {
        mutableStateListOf(true, true, true, true, false, false, false, false, false)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            containerColor = DetailsBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                DetailsTopBar(onBack = onBack)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                ) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(DetailsBackground)
                    )
                    SectionTitle("请选择纳税年度")
                    YearRow(
                        year = selectedYear,
                        onClick = {
                            pendingYear = selectedYear
                            showYearPicker = true
                        }
                    )
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(DetailsBackground)
                    )
                    SectionTitle("请选择所得类型")

                    val visibleCount = if (otherTypesExpanded) incomeTypes.size else 4
                    repeat(visibleCount) { index ->
                        IncomeTypeRow(
                            label = incomeTypes[index],
                            checked = checkedTypes[index],
                            onClick = { checkedTypes[index] = !checkedTypes[index] }
                        )
                    }

                    Text(
                        text = if (otherTypesExpanded) "收起 ︿" else "其他类型 ﹀",
                        color = Color(0xFF2488E8),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { otherTypesExpanded = !otherTypesExpanded }
                            .padding(vertical = 17.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DetailsBackground)
                            .padding(horizontal = 16.dp, vertical = 28.dp)
                    ) {
                        Button(
                            onClick = {
                                val count = checkedTypes.count { it }
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "正在查询${selectedYear}年度，共选择${count}种所得类型"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DetailsBlue),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("查询", color = Color.White, fontSize = 17.sp)
                        }
                    }
                    Spacer(Modifier.height(180.dp))
                }
            }
        }
    }

    if (showYearPicker) {
        YearPickerSheet(
            pendingYear = pendingYear,
            onYearSelected = { pendingYear = it },
            onCancel = { showYearPicker = false },
            onConfirm = {
                selectedYear = pendingYear
                showYearPicker = false
            }
        )
    }
}

@Composable
private fun DetailsTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        Text(
            "‹ 返回",
            color = Color(0xFF2787E8),
            fontSize = 17.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Text(
            "收入纳税明细",
            color = DetailsText,
            fontSize = 19.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        HorizontalDivider(
            color = DetailsDivider,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 20.dp)
                .background(Color(0xFF4C9AF5), RoundedCornerShape(3.dp))
        )
        Text(
            title,
            color = DetailsText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 9.dp)
        )
    }
}

@Composable
private fun YearRow(year: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("年度", color = Color(0xFF50545B), fontSize = 17.sp)
        Text(
            year.toString(),
            color = DetailsText,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text("›", color = Color(0xFFD7DADF), fontSize = 27.sp)
    }
}

@Composable
private fun IncomeTypeRow(label: String, checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.White)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(23.dp)
                    .background(if (checked) DetailsBlue else Color(0xFFD7DADE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (checked) 23.dp else 21.dp)
                        .background(if (checked) DetailsBlue else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (checked) {
                        Text("✓", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            Text(
                label,
                color = DetailsText,
                fontSize = 17.sp,
                modifier = Modifier.padding(start = 17.dp)
            )
        }
        HorizontalDivider(
            color = DetailsDivider,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .padding(start = 54.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun YearPickerSheet(
    pendingYear: Int,
    onYearSelected: (Int) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val years = remember(currentYear) { (1900..currentYear).toList() }
    val selectedIndex = years.indexOf(pendingYear).coerceAtLeast(0)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - 3).coerceAtLeast(0)
    )

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "取消",
                    color = Color(0xFF4E535B),
                    fontSize = 17.sp,
                    modifier = Modifier.clickable(onClick = onCancel)
                )
                Text(
                    "确定",
                    color = Color(0xFF318BE8),
                    fontSize = 17.sp,
                    modifier = Modifier.clickable(onClick = onConfirm)
                )
            }
            HorizontalDivider(color = DetailsDivider)
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState
            ) {
                items(years.size) { index ->
                    val year = years[index]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .clickable { onYearSelected(year) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            year.toString(),
                            color = if (year == pendingYear) DetailsText else Color(0xFFD5D7DA),
                            fontSize = 18.sp
                        )
                    }
                    if (year == pendingYear) {
                        HorizontalDivider(color = DetailsDivider)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun IncomeTaxDetailsPreview() {
    MyApplicationTheme(dynamicColor = false, darkTheme = false) {
        IncomeTaxDetailsScreen(onBack = {})
    }
}
