package com.example.dhbt.data.local.dao

import androidx.room.*
import com.example.dhbt.data.local.entity.StatisticSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticSummaryDao {
    @Query("SELECT * FROM statistic_summaries ORDER BY date DESC")
    fun getAllStatisticSummaries(): Flow<List<StatisticSummaryEntity>>

    @Query("SELECT * FROM statistic_summaries WHERE date = :date AND periodType = :periodType")
    suspend fun getStatisticSummaryByDate(date: Long, periodType: Int): StatisticSummaryEntity?

    @Query("SELECT * FROM statistic_summaries WHERE periodType = :periodType ORDER BY date DESC")
    fun getStatisticSummariesByPeriodType(periodType: Int): Flow<List<StatisticSummaryEntity>>

    @Query("SELECT * FROM statistic_summaries WHERE date BETWEEN :startDate AND :endDate AND periodType = :periodType ORDER BY date ASC")
    fun getStatisticSummariesInRange(startDate: Long, endDate: Long, periodType: Int): Flow<List<StatisticSummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatisticSummary(summary: StatisticSummaryEntity)

    @Update
    suspend fun updateStatisticSummary(summary: StatisticSummaryEntity)

    @Delete
    suspend fun deleteStatisticSummary(summary: StatisticSummaryEntity)

    @Query("DELETE FROM statistic_summaries WHERE date < :olderThan")
    suspend fun deleteOldStatistics(olderThan: Long)
}