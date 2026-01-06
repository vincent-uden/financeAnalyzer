package com.vincentuden.financeanalyzer

import AppDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "my_room.db")
    println("Using db file at ${dbFile.absolutePath}")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
}