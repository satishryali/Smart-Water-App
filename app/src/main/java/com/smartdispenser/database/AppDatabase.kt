package com.smartdispenser.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartdispenser.model.Category
import com.smartdispenser.model.TimerPreset

@Database(
    entities = [Category::class, TimerPreset::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun timerPresetDao(): TimerPresetDao
}