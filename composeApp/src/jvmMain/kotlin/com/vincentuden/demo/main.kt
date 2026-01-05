package com.vincentuden.demo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "demo",
    ) {
        val databaseBuilder = getDatabaseBuilder()
        App(databaseBuilder)
    }
}