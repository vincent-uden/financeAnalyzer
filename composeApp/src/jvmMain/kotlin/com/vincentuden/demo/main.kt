package com.vincentuden.demo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import getRoomDatabase

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "demo",
    ) {
        val databaseBuilder = getDatabaseBuilder()
        val db = getRoomDatabase(databaseBuilder)
        App(db)
    }
}