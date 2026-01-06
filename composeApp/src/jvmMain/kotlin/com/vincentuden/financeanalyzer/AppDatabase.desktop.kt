package com.vincentuden.financeanalyzer

import AppDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import java.nio.file.Paths

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbPath = System.getenv("FINANCE_ANALYZER_DB_PATH")
    val dbFile = if (dbPath != null) {
        File(dbPath).also { it.parentFile?.mkdirs() }
    } else {
        val userHome = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()
        val configDir = when {
            os.contains("linux") -> Paths.get(userHome, ".config", "financeAnalyzer")
            os.contains("windows") -> Paths.get(userHome, "AppData", "Roaming", "financeAnalyzer")
            os.contains("mac") -> Paths.get(userHome, "Library", "Application Support", "financeAnalyzer")
            else -> Paths.get(userHome, ".config", "financeAnalyzer") // fallback to linux-like
        }.toString()
        File(configDir, "data.db").also { it.parentFile.mkdirs() }
    }
    println("Using db file at ${dbFile.absolutePath}" + if (dbPath != null) " (from env var)" else " (default path)")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
}