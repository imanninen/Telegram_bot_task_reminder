package repository

import utils.*
import java.sql.Connection
import java.sql.DriverManager

object SQLService {
    private var connection: Connection? = null

    init {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306", "root", "soplaotves")
            println("Successfully connected to SQL server")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to connect to SQL server!")
        }
        createDatabase()
        createTable()
    }

    private fun createDatabase() {
        try {
            val sql1 = """
              CREATE DATABASE IF NOT EXISTS telegram_bot_db;
              """.trimIndent()
            val query1 = connection!!.prepareStatement(sql1)
            query1.execute()
            val sql2 = """
                USE telegram_bot_db;
                """.trimIndent()
            val query2 = connection!!.prepareStatement(sql2)
            query2.execute()
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to create database!")
        }
    }

    private fun createTable() {
        try {
            val sql1 = """
              CREATE TABLE IF NOT EXISTS users (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  chat_id LONG,
                  time_offset LONG 
                  );
              """.trimIndent()
            val query1 = connection!!.prepareStatement(sql1)
            query1.execute()
            val sql2 = """
              CREATE TABLE IF NOT EXISTS tasks (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  chat_id LONG,
                  description TEXT,
                  date_in_unix LONG,
                  time_offset LONG,
                  notified_times INT
                  );
              """.trimIndent()
            val query2 = connection!!.prepareStatement(sql2)
            query2.execute()
            val sql3 = """CREATE TABLE IF NOT EXISTS history (
                id INT PRIMARY KEY AUTO_INCREMENT,
                chat_id LONG,
                operation TEXT,
                stage TEXT,
                date_in_unix LONG,
                data TEXT);
            """.trimMargin()
            val query3 = connection!!.prepareStatement(sql3)
            query3.execute()
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to create table!")
        }
    }

    fun addUser(chatId: Long, offset: Long) {
        try {
            val sql = """
                INSERT INTO users (chat_id, time_offset) VALUES (?, ?);
            """.trimIndent()
            val query = connection!!.prepareStatement(sql)
            query.setLong(1, chatId)
            query.setLong(2, offset)
            query.execute()
        } catch (e: Exception) {
            println("Error in add user: $e")
        }
    }

    fun getUserOffset(chatId: Long): Long? {
        try {
            val sql = """SELECT * FROM users WHERE chat_id = $chatId;"""
            val query = connection!!.prepareStatement(sql)
            val result = query.executeQuery()
            while (result.next()) {
                return result.getLong(3)
            }
        } catch (e: Exception) {
            println("Error getUser: $e")
        }
        return null
    }

    fun addHistoryRecord(chatId: Long, operation: String, stage: String, dateInUnix: Long, data: String?) {
        try {
            val sql = """
                INSERT INTO history (chat_id, operation, stage, date_in_unix, data) VALUES (?, ?, ?, ?, ?);
            """.trimIndent()
            val query = connection!!.prepareStatement(sql)
            query.setLong(1, chatId)
            query.setString(2, operation)
            query.setString(3, stage)
            query.setLong(4, dateInUnix)
            query.setString(5, data)
            query.execute()
        } catch (e: Exception) {
            println("Error in add history: $e")
        }
    }

    fun getNearestHistoryRecord(chatId: Long): HistoryRecord? {
        try {
            val sql =
                """SELECT * FROM history INNER JOIN
                 (SELECT  chat_id, MAX(id) as top_date
                  FROM history
                  WHERE chat_id = $chatId
                  GROUP BY chat_id) 
                  AS EachItem ON EachItem.top_date = history.id AND EachItem.chat_id = history.chat_id"""
            val query = connection!!.prepareStatement(sql)
            val result = query.executeQuery()
            while (result.next()) {
                val operation = fromStringToOperation(result.getString(3))
                val stage = fromStringToStage(result.getString(4))
                val dateInUnix = result.getLong(5)
                val data = result.getString(6)
                return HistoryRecord(chatId, operation, stage, dateInUnix, data)
            }
        } catch (e: Exception) {
            println("getNearestHistoryRecord: $e")
        }
        return null
    }

    fun getLastOperationHistoryRecord(chatId: Long, operation: String): HistoryRecord? {
        try {
            val sql =
                """SELECT * FROM history INNER JOIN
                    (SELECT  chat_id, MAX(id) as top_date
                    FROM history
                    WHERE chat_id = 824863395 and operation = '$operation'
                    GROUP BY chat_id) 
                    AS EachItem ON EachItem.top_date = history.id AND EachItem.chat_id = history.chat_id""".trimMargin()
            val query = connection!!.prepareStatement(sql)
            val result = query.executeQuery()
            while (result.next()) {
                val operationNorm = fromStringToOperation(operation)
                val stage = fromStringToStage(result.getString(4))
                val dateInUnix = result.getLong(5)
                val date = result.getString(6)
                return HistoryRecord(chatId, operationNorm, stage, dateInUnix, date)
            }
        } catch (e: Exception) {
            println("Error getLastOperationHistoryRecord: $e")
        }
        return null
    }

    fun userListOfTasks(chatId: Long): List<TaskRecord> {
        try {
            val sql = """SELECT * FROM tasks WHERE chat_id = '$chatId';
        """.trimMargin()
            val query = connection!!.prepareStatement(sql)
            val result = query.executeQuery()
            val taskList = mutableListOf<TaskRecord>()
            while (result.next()) {
                val id = result.getInt(1)
                val chat = result.getLong(2)
                val desc = result.getString(3)
                val dueToDateUnix = result.getLong(4)
                val offset = result.getLong(5)
                val task = TaskRecord(id, chat, desc, dueToDateUnix, offset)
                taskList.add(task)
            }
            return taskList
        } catch (e: Exception) {
            println("Error: $e")
            return emptyList()
        }
    }

    fun tasksToDelete(currentTime: Long): List<TaskRecord> {
        try {
            val sql = """SELECT * FROM tasks WHERE (time_offset - $currentTime) <= 0; 
        """.trimMargin()
            val query = connection!!.prepareStatement(sql)
            val result = query.executeQuery()
            val taskList = mutableListOf<TaskRecord>()
            while (result.next()) {
                val id = result.getInt(1)
                val chat = result.getLong(2)
                val desc = result.getString(3)
                val dueToDateUnix = result.getLong(4)
                val offset = result.getLong(5)
                val task = TaskRecord(id, chat, desc, dueToDateUnix, offset)
                taskList.add(task)
            }
            return taskList
        } catch (e: Exception) {
            println("Error: $e")
            return emptyList()
        }
    }

    fun deleteTask(task: TaskRecord): Boolean {
        return try {
            val sql = """
                    DELETE FROM tasks WHERE chat_id = '${task.chatId}' and description = '${task.description}'
                     and date_in_unix = '${task.date}';  
                """.trimIndent()
            val query = connection!!.prepareStatement(sql)
            query.execute()
            true
        } catch (e: Exception) {
            println("Error in delete: $e")
            false
        }
    }

    fun findTaskForNotify(currentTimeInUnix: Long): List<TaskRecord> { // 86700 - 86100 -> 600 * 1000 == 5 min => 1 min == 120 * 1000
        try {
            val sql1 = """SELECT * FROM tasks WHERE (notified_times < 4)
                and ((time_offset - $currentTimeInUnix) < 86700000) and
                 ((time_offset - $currentTimeInUnix) > 86100000);
        """.trimMargin()
            val query1 = connection!!.prepareStatement(sql1)
            var result = query1.executeQuery()
            val taskToNotifyList = mutableListOf<TaskRecord>()

            while (result.next()) {
                val id = result.getInt(1)
                val chat = result.getLong(2)
                val desc = result.getString(3)
                val dueToDateUnix = result.getLong(4)
                val offset = result.getLong(5)
                val notifiedTimes = result.getInt(6)
                val task = TaskRecord(id, chat, desc, dueToDateUnix, offset, 24, notifiedTimes)
                taskToNotifyList.add(task)
            }
            val sql2 = """SELECT * FROM tasks WHERE (notified_times < 2) and
                ((time_offset - $currentTimeInUnix) < 11100000) and ((time_offset - $currentTimeInUnix) > 10500000);
        """.trimMargin()
            val query2 = connection!!.prepareStatement(sql2)
            result = query2.executeQuery()

            while (result.next()) {
                val id = result.getInt(1)
                val chat = result.getLong(2)
                val desc = result.getString(3)
                val dueToDateUnix = result.getLong(4)
                val offset = result.getLong(5)
                val notifiedTimes = result.getInt(6)
                val task = TaskRecord(id, chat, desc, dueToDateUnix, offset, 3, notifiedTimes)
                taskToNotifyList.add(task)
            }
            return taskToNotifyList
        } catch (e: Exception) {
            println("Error in findTaskForNotify: $e")
            return emptyList() // todo something
        }
    }

    fun updateNotifiedValues(taskId: Int) {
        try {
            val sql = """UPDATE tasks SET notified_times = notified_times + 1 WHERE id=$taskId;"""
            val query = connection!!.prepareStatement(sql)
            query.execute()
        } catch (e: Exception) {
            println("Error in updateNotificationValues: $e")
        }
    }

    fun addTask(task: TaskRecord): Boolean {
        return try {
            val sql = """
                INSERT INTO tasks (chat_id, description, date_in_unix, time_offset, notified_times)
                 VALUES (?, ?, ?, ?, ?);""".trimIndent()
            val query = connection!!.prepareStatement(sql)
            query.setLong(1, task.chatId)
            query.setString(2, task.description)
            query.setLong(3, task.date)
            query.setLong(4, task.offset)
            query.setInt(5, 0)
            query.execute()
            true
        } catch (e: Exception) {
            println("Error: $e")
            false
        }
    }
}