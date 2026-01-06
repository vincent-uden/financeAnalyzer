package com.vincentuden.financeanalyzer

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import getRoomDatabase

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "financeAnalyzer",
        state = rememberWindowState(width = 1600.dp, height = 900.dp)
    ) {
        val databaseBuilder = getDatabaseBuilder()
        val db = getRoomDatabase(databaseBuilder)
        App(db)
    }
}