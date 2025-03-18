package com.example.dhbt.domain.repository

import com.example.dhbt.domain.model.StatisticPeriod
import com.example.dhbt.domain.model.StatisticSummary
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    fun getAllStatisticSummaries(): Flow<List<StatisticSummary>>
    fun getStatisticSummariesByPeriod(periodType: StatisticPeriod): Flow<List<StatisticSummary>>
    fun getStatisticsForRange(startDate: Long, endDate: Long, periodType: StatisticPeriod): Flow<List<StatisticSummary>>

    suspend fun getStatisticSummaryForDate(date: Long, periodType: StatisticPeriod): StatisticSummary?
    suspend fun addOrUpdateStatisticSummary(summary: StatisticSummary)
    suspend fun generateStatisticsForPeriod(startDate: Long, endDate: Long, periodType: StatisticPeriod)
    suspend fun deleteOldStatistics(olderThan: Long)
}