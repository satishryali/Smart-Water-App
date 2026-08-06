package com.smartdispenser.repository

import com.smartdispenser.database.TimerPresetDao
import com.smartdispenser.model.TimerPreset
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerPresetRepository @Inject constructor(
    private val timerPresetDao: TimerPresetDao
) {

    fun observePresetsByCategory(categoryId: Long): Flow<List<TimerPreset>> =
        timerPresetDao.observePresetsByCategory(categoryId)

    fun observePresetById(presetId: Long): Flow<TimerPreset?> =
        timerPresetDao.observePresetById(presetId)

    suspend fun insertPreset(preset: TimerPreset): Long {
        return timerPresetDao.insertPreset(preset)
    }

    suspend fun updatePreset(preset: TimerPreset) {
        timerPresetDao.updatePreset(preset)
    }

    suspend fun deletePreset(preset: TimerPreset) {
        timerPresetDao.deletePreset(preset)
    }
}