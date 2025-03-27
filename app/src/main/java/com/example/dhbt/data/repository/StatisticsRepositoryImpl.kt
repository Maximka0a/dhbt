package com.example.dhbt.data.repository

import com.example.dhbt.data.local.dao.CategoryDao
import com.example.dhbt.data.local.dao.HabitDao
import com.example.dhbt.data.local.dao.HabitTrackingDao
import com.example.dhbt.data.local.dao.PomodoroSessionDao
import com.example.dhbt.data.local.dao.StatisticSummaryDao
import com.example.dhbt.data.local.dao.TaskDao
import com.example.dhbt.data.mapper.CategoryMapper
import com.example.dhbt.data.mapper.StatisticSummaryMapper
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.StatisticPeriod
import com.example.dhbt.domain.model.StatisticSummary
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

class StatisticsRepositoryImpl @Inject constructor(
    private val statisticSummaryDao: StatisticSummaryDao,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val habitDao: HabitDao,
    private val habitTrackingDao: HabitTrackingDao,
    private val pomodoroSessionDao: PomodoroSessionDao,
    private val statisticSummaryMapper: StatisticSummaryMapper,
    private val categoryMapper: CategoryMapper
) : StatisticsRepository {

    override fun getAllStatisticSummaries(): Flow<List<StatisticSummary>> {
        return statisticSummaryDao.getAllStatisticSummaries().map { entities ->
            entities.map { statisticSummaryMapper.mapFromEntity(it) }
        }
    }

    override suspend fun getCategoryDetails(categoryId: String): Category? {
        val categoryEntity = categoryDao.getCategoryById(categoryId) ?: return null
        return categoryMapper.mapFromEntity(categoryEntity)
    }

    override fun getStatisticSummariesByPeriod(periodType: StatisticPeriod): Flow<List<StatisticSummary>> {
        return statisticSummaryDao.getStatisticSummariesByPeriodType(periodType.value).map { entities ->
            entities.map { statisticSummaryMapper.mapFromEntity(it) }
        }
    }

    override fun getStatisticsForRange(startDate: Long, endDate: Long, periodType: StatisticPeriod): Flow<List<StatisticSummary>> {
        return statisticSummaryDao.getStatisticSummariesInRange(startDate, endDate, periodType.value).map { entities ->
            entities.map { statisticSummaryMapper.mapFromEntity(it) }
        }
    }

    override suspend fun getStatisticSummaryForDate(date: Long, periodType: StatisticPeriod): StatisticSummary? {
        val entity = statisticSummaryDao.getStatisticSummaryByDate(date, periodType.value) ?: return null
        return statisticSummaryMapper.mapFromEntity(entity)
    }

    override suspend fun addOrUpdateStatisticSummary(summary: StatisticSummary) {
        val entity = statisticSummaryMapper.mapToEntity(summary)
        statisticSummaryDao.insertStatisticSummary(entity)
    }

    override suspend fun generateStatisticsForPeriod(startDate: Long, endDate: Long, periodType: StatisticPeriod) {
        // Получаем все задачи и привычки в указанном периоде
        val tasks = taskDao.getTasksByDateRange(startDate, endDate).first()

        // Собираем статистику для каждого дня в периоде
        var currentDate = startDate
        while (currentDate <= endDate) {
            val nextDate = when (periodType) {
                StatisticPeriod.DAY -> {
                    val day = LocalDate.ofEpochDay(currentDate / (24 * 60 * 60 * 1000))
                    day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
                StatisticPeriod.WEEK -> {
                    val day = LocalDate.ofEpochDay(currentDate / (24 * 60 * 60 * 1000))
                    day.plusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
                StatisticPeriod.MONTH -> {
                    val day = LocalDate.ofEpochDay(currentDate / (24 * 60 * 60 * 1000))
                    day.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
                StatisticPeriod.YEAR -> {
                    val day = LocalDate.ofEpochDay(currentDate / (24 * 60 * 60 * 1000))
                    day.plusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }

            // Вычисляем статистику для текущего периода
            val periodTasks = tasks.filter { it.dueDate != null && it.dueDate >= currentDate && it.dueDate < nextDate }
            val totalTasks = periodTasks.size
            val completedTasks = periodTasks.count { it.status == TaskStatus.COMPLETED.value }
            val taskCompletionPercentage = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

            // Статистика Pomodoro сессий
            val pomodoroMinutes = pomodoroSessionDao.getTotalFocusTimeInRange(currentDate, nextDate - 1) ?: 0

            // Собираем распределение задач по приоритету
            val tasksPrioritySummary = mutableMapOf<TaskPriority, Int>()
            periodTasks.forEach { task ->
                val priority = TaskPriority.fromInt(task.priority)
                tasksPrioritySummary[priority] = (tasksPrioritySummary[priority] ?: 0) + 1
            }

            // Собираем распределение задач по категориям
            val tasksCategorySummary = mutableMapOf<String, Int>()
            periodTasks
                .filter { it.categoryId != null }
                .forEach { task ->
                    val categoryId = task.categoryId!!
                    tasksCategorySummary[categoryId] = (tasksCategorySummary[categoryId] ?: 0) + 1
                }

            // Сохраняем статистику в базу данных
            val summary = StatisticSummary(
                id = UUID.randomUUID().toString(),
                date = currentDate,
                periodType = periodType,
                taskCompletionPercentage = taskCompletionPercentage,
                habitCompletionPercentage = null, // Нужна дополнительная логика для вычисления
                totalPomodoroMinutes = pomodoroMinutes,
                productiveStreak = null, // Нужна дополнительная логика для вычисления
                tasksCategorySummary = tasksCategorySummary,
                tasksPrioritySummary = tasksPrioritySummary,
                habitsSuccessRate = null, // Нужна дополнительная логика для вычисления
                pomodoroDistribution = null // Нужна дополнительная логика для вычисления
            )

            addOrUpdateStatisticSummary(summary)

            // Переходим к следующему периоду
            currentDate = nextDate
        }
    }

    override suspend fun deleteOldStatistics(olderThan: Long) {
        statisticSummaryDao.deleteOldStatistics(olderThan)
    }
}