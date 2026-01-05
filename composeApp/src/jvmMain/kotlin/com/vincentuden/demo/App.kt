package com.vincentuden.demo

import AppDatabase
import TodoEntity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.RoomDatabase
import bank.BankStatementImporter
import bank.HandelsbankenStatement
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
import com.composeunstyled.theme.buildTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import demo.composeapp.generated.resources.Res
import demo.composeapp.generated.resources.compose_multiplatform
import getRoomDatabase
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

val AppTheme = buildPlatformTheme {

}

@Composable
@Preview
fun App(dbBuilder: RoomDatabase.Builder<AppDatabase>) {
    val state = remember {
        TabGroupState(
            initialTab = "Front page",
            tabs = listOf("Front page", "Import XLSX"),
        )
    }

    AppTheme {
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
                    frontPage(dbBuilder)
                }
                TabPanel(key = "Import XLSX") {
                    BankStatementImporter()
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
fun todaysDate(): String {
    // Monkeypatch LocalDateTime
    fun LocalDateTime.format() = toString().substringBefore('T')

    val now = Clock.System.now()
    val zone = TimeZone.currentSystemDefault()
    return now.toLocalDateTime(zone).format()
}


@Composable
fun frontPage(dbBuilder: RoomDatabase.Builder<AppDatabase>) {
    var showContent by remember { mutableStateOf(false) }
    val todoTitle = remember { TextFieldState() }
    val todoContents = remember { TextFieldState() }
    val db = remember { getRoomDatabase(dbBuilder) }

    val scope = rememberCoroutineScope()
    var todos by remember { mutableIntStateOf(0) }

    LaunchedEffect(true) {
        todos = db.getDao().count()
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
                        db.getDao()
                            .insert(TodoEntity(title = todoTitle.toString(), content = todoContents.toString()))
                        todos = db.getDao().count()
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