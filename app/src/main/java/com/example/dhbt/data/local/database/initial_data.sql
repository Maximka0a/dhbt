-- Очистка существующих данных (опционально)
DELETE FROM statistic_summaries;
DELETE FROM notifications;
DELETE FROM task_tag_cross_refs;
DELETE FROM pomodoro_sessions;
DELETE FROM task_recurrences;
DELETE FROM subtasks;
DELETE FROM tasks;
DELETE FROM habit_trackings;
DELETE FROM habit_frequencies;
DELETE FROM habits;
DELETE FROM tags;
DELETE FROM categories;

-- Заполнение таблицы categories
INSERT INTO categories (categoryId, name, color, iconEmoji, type, "order")
VALUES 
('cat_work', 'Работа', '#FF4040', '💼', 0, 0),
('cat_personal', 'Личное', '#4169E1', '🙂', 0, 1),
('cat_health', 'Здоровье', '#32CD32', '❤️', 1, 2),
('cat_study', 'Учёба', '#9370DB', '📚', 0, 3),
('cat_fitness', 'Фитнес', '#FF8C00', '🏋️', 1, 4),
('cat_finance', 'Финансы', '#20B2AA', '💰', 2, 5),
('cat_home', 'Дом', '#A0522D', '🏠', 2, 6);

-- Заполнение таблицы tags
INSERT INTO tags (tagId, name, color)
VALUES 
('tag_urgent', 'Срочно', '#FF0000'),
('tag_important', 'Важно', '#FFA500'),
('tag_easy', 'Легко', '#32CD32'),
('tag_hard', 'Сложно', '#8B0000'),
('tag_meeting', 'Встреча', '#4682B4'),
('tag_idea', 'Идея', '#9932CC'),
('tag_review', 'Обзор', '#008080'),
('tag_goal', 'Цель', '#FFD700');

-- Заполнение таблицы tasks
INSERT INTO tasks (taskId, title, description, categoryId, color, creationDate, dueDate, dueTime, duration, priority, status, completionDate, eisenhowerQuadrant, estimatedPomodoroSessions)
VALUES 
('task_1', 'Подготовить отчет', 'Ежемесячный отчет о проделанной работе', 'cat_work', '#FF4040', strftime('%s', 'now') * 1000, strftime('%s', 'now', '+3 day') * 1000, '15:00', 120, 2, 0, NULL, 1, 4),
('task_2', 'Созвониться с клиентом', 'Обсудить детали проекта', 'cat_work', '#FF4040', strftime('%s', 'now') * 1000, strftime('%s', 'now', '+1 day') * 1000, '14:30', 45, 2, 0, NULL, 1, 1),
('task_3', 'Купить продукты', 'Молоко, хлеб, яйца, овощи', 'cat_personal', '#4169E1', strftime('%s', 'now') * 1000, strftime('%s', 'now', '+1 day') * 1000, '18:00', 60, 1, 0, NULL, 3, 1),
('task_4', 'Подготовиться к экзамену', 'Повторить главы 1-5', 'cat_study', '#9370DB', strftime('%s', 'now', '-1 day') * 1000, strftime('%s', 'now', '+7 day') * 1000, '12:00', 180, 2, 0, NULL, 2, 6),
('task_5', 'Оплатить счета', 'Электричество, интернет, телефон', 'cat_finance', '#20B2AA', strftime('%s', 'now') * 1000, strftime('%s', 'now', '+2 day') * 1000, '10:00', 30, 1, 0, NULL, 1, 1),
('task_6', 'Прочитать книгу', 'Закончить главы 7-10', 'cat_personal', '#4169E1', strftime('%s', 'now', '-3 day') * 1000, strftime('%s', 'now', '+5 day') * 1000, NULL, 120, 0, 0, NULL, 4, 3),
('task_7', 'Починить кран', 'Купить новые прокладки', 'cat_home', '#A0522D', strftime('%s', 'now', '-2 day') * 1000, strftime('%s', 'now', '+1 day') * 1000, NULL, 45, 1, 1, strftime('%s', 'now') * 1000, 2, 1),
('task_8', 'Подготовить презентацию', 'Для встречи с инвесторами', 'cat_work', '#FF4040', strftime('%s', 'now') * 1000, strftime('%s', 'now', '+4 day') * 1000, '16:00', 150, 2, 0, NULL, 1, 4);

-- Заполнение таблицы subtasks
INSERT INTO subtasks (subtaskId, taskId, title, isCompleted, completionDate, "order")
VALUES 
('subtask_1', 'task_1', 'Собрать данные', 1, strftime('%s', 'now', '-1 day') * 1000, 0),
('subtask_2', 'task_1', 'Создать графики', 0, NULL, 1),
('subtask_3', 'task_1', 'Написать выводы', 0, NULL, 2),
('subtask_4', 'task_3', 'Составить список продуктов', 1, strftime('%s', 'now') * 1000, 0),
('subtask_5', 'task_3', 'Проверить наличие скидок', 0, NULL, 1),
('subtask_6', 'task_4', 'Прочитать главу 1', 1, strftime('%s', 'now', '-1 day') * 1000, 0),
('subtask_7', 'task_4', 'Прочитать главу 2', 1, strftime('%s', 'now', '-1 day') * 1000, 1),
('subtask_8', 'task_4', 'Прочитать главу 3', 0, NULL, 2),
('subtask_9', 'task_4', 'Прочитать главу 4', 0, NULL, 3),
('subtask_10', 'task_4', 'Прочитать главу 5', 0, NULL, 4);

-- Заполнение таблицы task_tag_cross_refs
INSERT INTO task_tag_cross_refs (taskId, tagId)
VALUES 
('task_1', 'tag_important'),
('task_1', 'tag_review'),
('task_2', 'tag_urgent'),
('task_2', 'tag_meeting'),
('task_4', 'tag_important'),
('task_4', 'tag_hard'),
('task_5', 'tag_urgent'),
('task_8', 'tag_important'),
('task_8', 'tag_meeting');

-- Заполнение таблицы task_recurrences
INSERT INTO task_recurrences (recurrenceId, taskId, recurrenceType, daysOfWeek, monthDay, customInterval, startDate, endDate)
VALUES 
('rec_1', 'task_3', 1, '1,4,7', NULL, NULL, strftime('%s', 'now') * 1000, strftime('%s', 'now', '+30 day') * 1000),
('rec_2', 'task_5', 2, NULL, 15, NULL, strftime('%s', 'now') * 1000, NULL),
('rec_3', 'task_2', 3, NULL, NULL, 14, strftime('%s', 'now') * 1000, strftime('%s', 'now', '+90 day') * 1000);

-- Заполнение таблицы habits
INSERT INTO habits (habitId, title, description, iconEmoji, color, creationDate, habitType, targetValue, unitOfMeasurement, targetStreak, currentStreak, bestStreak, status, pausedDate, categoryId)
VALUES 
('habit_1', 'Пить воду', 'Выпивать 2 литра воды в день', '💧', '#1E90FF', strftime('%s', 'now', '-30 day') * 1000, 0, 2.0, 'литра', 30, 5, 15, 0, NULL, 'cat_health'),
('habit_2', 'Бегать по утрам', 'Бег трусцой 30 минут', '🏃', '#FF8C00', strftime('%s', 'now', '-60 day') * 1000, 0, 30.0, 'минут', 90, 3, 25, 0, NULL, 'cat_fitness'),
('habit_3', 'Медитация', '10 минут медитации', '🧘', '#9370DB', strftime('%s', 'now', '-15 day') * 1000, 0, 10.0, 'минут', 30, 12, 12, 0, NULL, 'cat_health'),
('habit_4', 'Чтение', 'Читать минимум 20 страниц', '📚', '#20B2AA', strftime('%s', 'now', '-45 day') * 1000, 0, 20.0, 'страниц', 60, 0, 30, 1, strftime('%s', 'now', '-5 day') * 1000, 'cat_personal'),
('habit_5', 'Откладывать деньги', 'Отложить 10% от дохода', '💰', '#FFD700', strftime('%s', 'now', '-90 day') * 1000, 1, 10.0, '%', 12, 3, 3, 0, NULL, 'cat_finance');

-- Заполнение таблицы habit_frequencies
INSERT INTO habit_frequencies (frequencyId, habitId, frequencyType, daysOfWeek, timesPerPeriod, periodType)
VALUES 
('freq_1', 'habit_1', 0, '1,2,3,4,5,6,7', NULL, NULL),
('freq_2', 'habit_2', 0, '1,3,5', NULL, NULL),
('freq_3', 'habit_3', 0, '1,2,3,4,5,6,7', NULL, NULL),
('freq_4', 'habit_4', 0, '1,2,3,4,5', NULL, NULL),
('freq_5', 'habit_5', 1, NULL, 1, 2);

-- Заполнение таблицы habit_trackings
INSERT INTO habit_trackings (trackingId, habitId, date, isCompleted, value, duration, notes)
VALUES 
('track_1', 'habit_1', strftime('%s', 'now', '-1 day') * 1000, 1, 2.2, NULL, 'Легко выполнил цель'),
('track_2', 'habit_1', strftime('%s', 'now', '-2 day') * 1000, 1, 2.0, NULL, NULL),
('track_3', 'habit_1', strftime('%s', 'now', '-3 day') * 1000, 1, 1.8, NULL, 'Не хватило немного'),
('track_4', 'habit_1', strftime('%s', 'now', '-4 day') * 1000, 1, 2.5, NULL, 'Жарко было, пил больше'),
('track_5', 'habit_1', strftime('%s', 'now', '-5 day') * 1000, 1, 2.0, NULL, NULL),
('track_6', 'habit_2', strftime('%s', 'now', '-1 day') * 1000, 1, NULL, 35, 'Хорошая пробежка'),
('track_7', 'habit_2', strftime('%s', 'now', '-3 day') * 1000, 1, NULL, 30, NULL),
('track_8', 'habit_2', strftime('%s', 'now', '-5 day') * 1000, 1, NULL, 25, 'Немного устал'),
('track_9', 'habit_3', strftime('%s', 'now', '-1 day') * 1000, 1, NULL, 12, 'Очень спокойно'),
('track_10', 'habit_3', strftime('%s', 'now', '-2 day') * 1000, 1, NULL, 10, NULL),
('track_11', 'habit_3', strftime('%s', 'now', '-3 day') * 1000, 0, NULL, 5, 'Не удалось сосредоточиться'),
('track_12', 'habit_5', strftime('%s', 'now', '-30 day') * 1000, 1, 15.0, NULL, 'Получил премию'),
('track_13', 'habit_5', strftime('%s', 'now', '-60 day') * 1000, 1, 10.0, NULL, NULL),
('track_14', 'habit_5', strftime('%s', 'now', '-90 day') * 1000, 1, 8.0, NULL, 'Были большие расходы');

-- Заполнение таблицы pomodoro_sessions
INSERT INTO pomodoro_sessions (sessionId, taskId, startTime, endTime, duration, type, isCompleted, notes)
VALUES 
('pomodoro_1', 'task_1', strftime('%s', 'now', '-1 day') * 1000, strftime('%s', 'now', '-1 day', '+25 minute') * 1000, 25, 0, 1, 'Хорошая сессия'),
('pomodoro_2', 'task_1', strftime('%s', 'now', '-1 day', '+30 minute') * 1000, strftime('%s', 'now', '-1 day', '+55 minute') * 1000, 25, 0, 1, NULL),
('pomodoro_3', 'task_1', strftime('%s', 'now', '-1 day', '+60 minute') * 1000, strftime('%s', 'now', '-1 day', '+85 minute') * 1000, 25, 0, 1, 'Почти закончил'),
('pomodoro_4', 'task_4', strftime('%s', 'now', '-2 day') * 1000, strftime('%s', 'now', '-2 day', '+25 minute') * 1000, 25, 0, 1, 'Начал изучать'),
('pomodoro_5', 'task_4', strftime('%s', 'now', '-2 day', '+30 minute') * 1000, strftime('%s', 'now', '-2 day', '+55 minute') * 1000, 25, 0, 1, NULL),
('pomodoro_6', 'task_8', strftime('%s', 'now') * 1000, NULL, 25, 0, 0, 'В процессе');

-- Заполнение таблицы notifications
INSERT INTO notifications (notificationId, targetId, targetType, title, message, time, scheduledDate, daysOfWeek, repeatInterval, workId, isEnabled, priority, createdAt, updatedAt)
VALUES 
('notif_1', 'task_1', 0, 'Напоминание о задаче', 'Подготовить отчет', '15:00', strftime('%s', 'now', '+3 day') * 1000, NULL, NULL, 'work_123456', 1, 1, strftime('%s', 'now') * 1000, strftime('%s', 'now') * 1000),
('notif_2', 'task_3', 0, 'Напоминание о покупке', 'Купить продукты', '17:30', strftime('%s', 'now', '+1 day') * 1000, NULL, NULL, 'work_234567', 1, 0, strftime('%s', 'now') * 1000, strftime('%s', 'now') * 1000),
('notif_3', 'habit_1', 1, 'Привычка', 'Пить воду', '09:00', NULL, '1,2,3,4,5,6,7', 1440, 'work_345678', 1, 1, strftime('%s', 'now') * 1000, strftime('%s', 'now') * 1000),
('notif_4', 'habit_2', 1, 'Привычка', 'Бегать по утрам', '07:00', NULL, '1,3,5', 1440, 'work_456789', 1, 1, strftime('%s', 'now') * 1000, strftime('%s', 'now') * 1000),
('notif_5', 'task_5', 0, 'Срочное напоминание', 'Оплатить счета', '09:45', strftime('%s', 'now', '+2 day') * 1000, NULL, NULL, 'work_567890', 1, 2, strftime('%s', 'now') * 1000, strftime('%s', 'now') * 1000);

-- Заполнение таблицы statistic_summaries
INSERT INTO statistic_summaries (summaryId, date, periodType, taskCompletionPercentage, habitCompletionPercentage, totalPomodoroMinutes, productiveStreak, tasksCategorySummary, tasksPrioritySummary, habitsSuccessRate, pomodoroDistribution)
VALUES 
('stats_day_1', strftime('%s', 'now', '-1 day') * 1000, 0, 75.0, 80.0, 75, 5, '{"cat_work": 3, "cat_study": 2, "cat_personal": 1}', '{"low": 1, "medium": 3, "high": 2}', '{"habit_1": 1, "habit_2": 1, "habit_3": 1, "habit_4": 0}', '{"morning": 2, "afternoon": 4, "evening": 1}'),
('stats_day_2', strftime('%s', 'now', '-2 day') * 1000, 0, 60.0, 66.6, 50, 4, '{"cat_work": 2, "cat_study": 3, "cat_personal": 1}', '{"low": 2, "medium": 2, "high": 2}', '{"habit_1": 1, "habit_2": 0, "habit_3": 1, "habit_4": 1}', '{"morning": 1, "afternoon": 3, "evening": 0}'),
('stats_week_1', strftime('%s', 'now', '-7 day') * 1000, 1, 68.5, 75.0, 225, 5, '{"cat_work": 8, "cat_study": 6, "cat_personal": 4, "cat_finance": 2, "cat_home": 1}', '{"low": 5, "medium": 10, "high": 6}', '{"habit_1": 7, "habit_2": 3, "habit_3": 5, "habit_4": 2, "habit_5": 0}', '{"morning": 7, "afternoon": 12, "evening": 6}'),
('stats_month_1', strftime('%s', 'now', '-30 day') * 1000, 2, 72.0, 80.0, 950, 12, '{"cat_work": 35, "cat_study": 20, "cat_personal": 15, "cat_finance": 10, "cat_home": 5}', '{"low": 20, "medium": 45, "high": 20}', '{"habit_1": 25, "habit_2": 10, "habit_3": 20, "habit_4": 15, "habit_5": 1}', '{"morning": 30, "afternoon": 45, "evening": 20}');