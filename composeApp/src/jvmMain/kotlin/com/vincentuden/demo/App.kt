package com.vincentuden.demo

import AppDatabase
import TodoEntity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.RoomDatabase
import bank.BankStatementImporter
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
            tabs = listOf("Front page", "Import XLSX"),
        )
    }

    AppTheme {
        Box(modifier = Modifier.background(Theme[colors][background])) {
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                TabGroup(state) {
                    TabList(modifier = Modifier.safeContentPadding()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Tab(key = "Front page") {
                                Text("Front page")
                            }
                            Tab(key = "Import XLSX") {
                                Text("Import XLSX")
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
    var showContent by remember { mutableStateOf(false) }
    val todoTitle = remember { TextFieldState() }
    val todoContents = remember { TextFieldState() }

    val scope = rememberCoroutineScope()
    var todos by remember { mutableIntStateOf(0) }

    LaunchedEffect(true) {
        todos = db.todoDao().count()
    }
    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Today's date is ${todaysDate()}",
            modifier = Modifier.padding(20.dp),
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "There are $todos TODOs saved",
            modifier = Modifier.padding(20.dp),
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        UnstyledButton(onClick = { showContent = !showContent }) {
            Text("Click me!")
        }
        AnimatedVisibility(showContent) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextField(state = todoTitle) {
                    TextInput()
                }
                TextField(state = todoContents) {
                    TextInput()
                }
                UnstyledButton(onClick = {
                    scope.launch {
                        db.todoDao()
                            .insert(TodoEntity(title = todoTitle.toString(), content = todoContents.toString()))
                        todos = db.todoDao().count()
                    }
                }) {
                    Text("Click me!")
                }
            }
        }
        UnstyledButton(onClick = { print(HandelsbankenStatement.fromXlsx("/home/vincent/Downloads/Handelsbanken_Account_Transactions_2026-01-05.xlsx")) }) {
            Text("Import XLSX")
        }
    }
}