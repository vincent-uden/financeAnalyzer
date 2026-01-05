package com.vincentuden.demo

import AppDatabase
import TodoEntity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.RoomDatabase
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

@Composable
@Preview
fun App(dbBuilder: RoomDatabase.Builder<AppDatabase>) {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        val todoTitle = rememberTextFieldState(initialText = "")
        val todoContents= rememberTextFieldState(initialText = "")
        val db = remember { getRoomDatabase(dbBuilder) }

        val scope = rememberCoroutineScope()
        var todos by remember { mutableIntStateOf(0)}

        LaunchedEffect(true) {
            todos = db.getDao().count()
        }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
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
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextField(state = todoTitle, label={Text("Title")})
                    TextField(state = todoContents, label={Text("Contents")})
                    Button(onClick = { scope.launch {
                        db.getDao().insert(TodoEntity(title = todoTitle.toString(), content = todoContents.toString()))
                        todos = db.getDao().count()
                    } }) {
                        Text("Click me!")
                    }
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