package com.vincentuden.financeanalyzer

import AppDatabase
import TodoEntity
import BankRepository
import bank.AccountBalance
import bank.CategorySpending
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.RoomDatabase
import bank.BankStatementImporter
import bank.CategoriesView
import bank.HandelsbankenStatement
import colors

import com.composeunstyled.Tab
import com.composeunstyled.TabGroup
import com.composeunstyled.TabGroupState
import com.composeunstyled.TabList
import com.composeunstyled.TabPanel
import com.composeunstyled.Text
import com.composeunstyled.TextField
import com.composeunstyled.TextInput
import com.composeunstyled.UnstyledButton
import com.composeunstyled.platformtheme.buildPlatformTheme
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.ThemeProperty
import com.composeunstyled.theme.ThemeToken
import foreground
import background
import backgroundLighter
import black
import blue
import cursor
import cyan

import org.jetbrains.compose.ui.tooling.preview.Preview

import getRoomDatabase
import green
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import magenta
import red
import text
import white
import yellow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale



@Composable
fun TabText(text: String, isSelected: Boolean, thickness: Dp = 4.dp) {
    val cyanColor = Theme[colors][cyan]
    val cornerRadius = thickness / 2
    val paddingBottom = thickness + 4.dp
    Text(modifier = Modifier.padding(bottom = paddingBottom).drawBehind {
        if (isSelected) {
            drawRoundRect(
                color = cyanColor,
                topLeft = Offset(0f, size.height + paddingBottom.toPx() - thickness.toPx()),
                size = Size(size.width, thickness.toPx()),
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }
    }, text = text)
}

@Composable
fun DashboardPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backgroundColor = Theme[colors][backgroundLighter]
    val borderColor = Theme[colors][foreground].copy(alpha = 0.2f)

    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

val AppTheme = buildPlatformTheme {
    properties[colors] = mapOf(
        foreground to Color(0xFFC0CAF5),
        background to Color(0xFF1A1B26),
        backgroundLighter to Color(0xFF35374B),
        text to Color(0xFF1A1B26),
        cursor to Color(0xFFC0CAF5),
        black to Color(0xFF15161E),
        red to Color(0xFFF7768E),
        green to Color(0xFF9ECE6A),
        yellow to Color(0xFFE0AF68),
        blue to Color(0xFF7AA2F7),
        magenta to Color(0xFFBB9AF7),
        cyan to Color(0xFF7DCFFF),
        white to Color(0xFFA9B1D6),
    )
    defaultContentColor = Color(0xFFC0CAF5)
    defaultTextStyle = TextStyle(color = Color(0xFFC0CAF5))
}


@Composable
@Preview
fun App(db: AppDatabase) {
    val state = remember {
        TabGroupState(
            initialTab = "Front page",
            tabs = listOf("Front page", "Import XLSX", "Categories"),
        )
    }

    AppTheme {
        Box(modifier = Modifier.background(Theme[colors][background])) {
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                val cyanColor = Theme[colors][cyan]
                TabGroup(state) {
                    TabList(modifier = Modifier.safeContentPadding()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Tab(key = "Front page") {
                                TabText("Front page", state.selectedTab == "Front page")
                            }
                            Tab(key = "Import XLSX") {
                                TabText("Import XLSX", state.selectedTab == "Import XLSX")
                            }
                            Tab(key = "Categories") {
                                TabText("Categories", state.selectedTab == "Categories")
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().height(16.dp)) {}
                    TabPanel(key = "Front page") {
                        frontPage(db)
                    }
                    TabPanel(key = "Import XLSX") {
                        BankStatementImporter(db)
                    }
                    TabPanel(key = "Categories") {
                        CategoriesView(db)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
fun todaysDate(): String {
    // Monkey-patch LocalDateTime
    fun LocalDateTime.format() = toString().substringBefore('T')

    val now = Clock.System.now()
    val zone = TimeZone.currentSystemDefault()
    return now.toLocalDateTime(zone).format()
}


@Composable
fun frontPage(db: AppDatabase) {
    val scope = rememberCoroutineScope()
    val repo = remember { BankRepository(db) }

    // State for account selection
    var availableAccounts by remember { mutableStateOf<List<AccountBalance>>(emptyList()) }
    var selectedAccountIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // State for current month
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // State for data
    var accountBalances by remember { mutableStateOf<List<AccountBalance>>(emptyList()) }
    var monthlyCosts by remember { mutableStateOf<List<CategorySpending>>(emptyList()) }
    var monthlyIncome by remember { mutableStateOf<List<CategorySpending>>(emptyList()) }

    // Load available accounts on first load
    LaunchedEffect(Unit) {
        availableAccounts = repo.getAccountBalances()
        selectedAccountIds = availableAccounts.map { it.accountId }.toSet()
    }

    // Load data when accounts or month change
    LaunchedEffect(selectedAccountIds, currentMonth) {
        accountBalances = repo.getAccountBalances().filter { it.accountId in selectedAccountIds }
        monthlyCosts = repo.getCategorySpendingForMonth(
            currentMonth.year,
            currentMonth.monthValue,
            selectedAccountIds.toList()
        )
        monthlyIncome = repo.getCategoryIncomeForMonth(
            currentMonth.year,
            currentMonth.monthValue,
            selectedAccountIds.toList()
        )
    }

    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Account Selection
        DashboardPanel(
            title = "Account Selection",
            modifier = Modifier.fillMaxWidth()
        ) {
            if (availableAccounts.isEmpty()) {
                Text("No accounts found. Import some transactions first.")
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableAccounts.forEach { account ->
                        val isSelected = account.accountId in selectedAccountIds
                        UnstyledButton(
                            onClick = {
                                selectedAccountIds = if (isSelected) {
                                    selectedAccountIds - account.accountId
                                } else {
                                    selectedAccountIds + account.accountId
                                }
                            },
                            modifier = Modifier
                                .background(
                                    if (isSelected) Theme[colors][cyan].copy(alpha = 0.2f)
                                    else Theme[colors][background],
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Theme[colors][cyan] else Theme[colors][foreground].copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(account.accountName)
                        }
                    }
                }
            }
        }

        // Account Balances
        DashboardPanel(
            title = "Account Balances",
            modifier = Modifier.fillMaxWidth()
        ) {
            if (accountBalances.isEmpty()) {
                Text("No accounts selected or no data available.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accountBalances.forEach { balance ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(balance.accountName)
                            Text(
                                text = formatAmount(balance.balance),
                                color = if (balance.balance >= 0) Theme[colors][green] else Theme[colors][red]
                            )
                        }
                    }
                }
            }
        }

        // Month Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            UnstyledButton(
                onClick = {
                    currentMonth = currentMonth.minusMonths(1)
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("Previous", color = Theme[colors][cyan])
            }
            Text(
                text = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()).format(currentMonth),
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            UnstyledButton(
                onClick = {
                    currentMonth = currentMonth.plusMonths(1)
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("Next", color = Theme[colors][cyan])
            }
        }

        // Monthly Breakdowns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Monthly Costs
            DashboardPanel(
                title = "Monthly Costs",
                modifier = Modifier.weight(1f)
            ) {
                if (monthlyCosts.isEmpty()) {
                    Text("No expenses for this month.")
                } else {
                    val maxAmount = monthlyCosts.maxOfOrNull { it.total } ?: 1L
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        monthlyCosts.forEach { spending ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(spending.categoryName, modifier = Modifier.weight(1f))
                                Text(formatAmount(spending.total), color = Theme[colors][red])
                            }
                            // Simple bar chart
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(
                                        Theme[colors][background],
                                        RoundedCornerShape(2.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth((spending.total.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f))
                                        .background(Theme[colors][red], RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Income
            DashboardPanel(
                title = "Monthly Income",
                modifier = Modifier.weight(1f)
            ) {
                if (monthlyIncome.isEmpty()) {
                    Text("No income for this month.")
                } else {
                    val maxAmount = monthlyIncome.maxOfOrNull { it.total } ?: 1L
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        monthlyIncome.forEach { income ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(income.categoryName, modifier = Modifier.weight(1f))
                                Text(formatAmount(income.total), color = Theme[colors][green])
                            }
                            // Simple bar chart
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(
                                        Theme[colors][background],
                                        RoundedCornerShape(2.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth((income.total.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f))
                                        .background(Theme[colors][green], RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatAmount(amount: Long): String {
    val sek = amount / 100.0
    return String.format("%.2f SEK", sek)
}