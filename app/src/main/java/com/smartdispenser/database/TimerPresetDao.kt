package com.smartdispenser.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartdispenser.model.TimerPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerPresetDao {

    @Query("SELECT * FROM timer_presets WHERE categoryId = :categoryId ORDER BY createdDate DESC")
    fun observePresetsByCategory(categoryId: Long): Flow<List<TimerPreset>>

    @Query("SELECT * FROM timer_presets WHERE id = :presetId")
    fun observePresetById(presetId: Long): Flow<TimerPreset?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: TimerPreset): Long

    @Update
    suspend fun updatePreset(preset: TimerPreset)

    @Delete
    suspend fun deletePreset(preset: TimerPreset)
}