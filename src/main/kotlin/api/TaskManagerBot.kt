package api

import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import repository.SQLService
import utils.*
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.Date


class TaskManagerBot(token: String) : TelegramLongPollingBot(token) {
    override fun getBotUsername(): String = System.getenv("BOT_USERNAME") ?: "@task_manager1232_bot"
    private val databaseRepository = SQLService

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val text = update.message.text
            val chatId = update.message.chatId
            when (text) {
                "/start" -> start(chatId, text)
                "/help" -> help(chatId)
                "/add_task" -> addTask(chatId, text)
                "/list" -> listOfTasks(chatId)
                "/delete_task" -> deleteTask(chatId, text)
                else -> operationManager(chatId, text)
            }
        }
    }

    private fun sendTextMessage(chatId: Long, text: String, sendButtons: Boolean = false) {
        val sendMessage = SendMessage()
        sendMessage.chatId = chatId.toString()
        sendMessage.text = text
        if (sendButtons) {
            val keyboardMarkup = ReplyKeyboardMarkup()
            val keyboard = mutableListOf<KeyboardRow>()
            val row1 = KeyboardRow()
            val row2 = KeyboardRow()
            val row3 = KeyboardRow()

            row1.add("/start")
            row1.add("/help")
            row2.add("/list")
            row2.add("/add_task")
            row3.add("/delete_task")
            keyboard.add(row1)
            keyboard.add(row2)
            keyboard.add(row3)
            keyboardMarkup.keyboard = keyboard
            sendMessage.replyMarkup = keyboardMarkup
        }
        execute(sendMessage)
    }

    private fun operationManager(chatId: Long, text: String) {
        val lastOperation = databaseRepository.getNearestHistoryRecord(chatId)
        when {
            lastOperation == null -> sendTextMessage(chatId, text)
            lastOperation.operation == Operation.DELETE_TASK -> deleteTask(chatId, text)
            lastOperation.operation == Operation.ADD_TASK -> addTask(chatId, text)
            lastOperation.operation == Operation.START -> start(chatId, text)
            else -> sendTextMessage(chatId, "Type /help to see list of commands.")
        }
    }

    private fun start(chatId: Long, text: String) {
        val lastOperation = databaseRepository.getNearestHistoryRecord(chatId)
        when {
            lastOperation == null -> startGreetingMessage(chatId)
            lastOperation.operation == Operation.START && lastOperation.stage == Stage.PROCESS_OFFSET -> startProcessOffset(
                chatId,
                text
            )

            else -> sendTextMessage(chatId, "You probably registered already.")
        }
    }

    private fun startProcessOffset(chatId: Long, text: String) {
        val greetingMessage = """
            Welcome to Task Manager bot! I can save upcoming tasks and remind you about it. Click /help to read list of supported commands.
        """.trimIndent()
        val offsetInIntFromUTC = text.toInt()
        val offsetInUnix = 7200000 - offsetInIntFromUTC.toLong() * 120 * 10000 * 3
        databaseRepository.addUser(chatId, offsetInUnix)
        sendTextMessage(chatId, greetingMessage, true)
    }


    private fun startGreetingMessage(chatId: Long) {
        val message = """Please enter a offset from UTC time in hours!"""
        databaseRepository.addHistoryRecord(
            chatId, fromOperationToString(Operation.START),
            Stage.PROCESS_OFFSET.string, Date().time, EMPTY
        )
        sendTextMessage(chatId, message)
    }

    private fun help(chatId: Long) {
        val helpMessage = """
            1) /start - greeting message.
            2) /help - list of my commands and how they works.
            3) /list - list of your upcoming tasks.
            4) /add_task - add new task to the your task list.
            5) /delete task - delete a task from the task list by index.
        """.trimIndent()
        databaseRepository.addHistoryRecord(
            chatId,
            fromOperationToString(Operation.HELP),
            Stage.NOTHING.string,
            Date().time,
            EMPTY
        )
        sendTextMessage(chatId, helpMessage)
    }

    private fun addTask(chatId: Long, text: String) {
        val lastOperation = databaseRepository.getNearestHistoryRecord(chatId)

        when {
            lastOperation == null -> addTaskEnterMessage(chatId)
            lastOperation.operation == Operation.ADD_TASK && lastOperation.stage == Stage.PROCESS_DESC ->
                addTaskProcessDesc(chatId, text)

            lastOperation.operation == Operation.ADD_TASK && lastOperation.stage == Stage.PROCESS_DATE -> addTaskProcessDate(
                chatId,
                text
            )

            else -> addTaskEnterMessage(chatId)
        }
    }

    private fun addTaskEnterMessage(chatId: Long) {
        val reply = "Enter task description:"
        databaseRepository.addHistoryRecord(
            chatId,
            fromOperationToString(Operation.ADD_TASK),
            Stage.PROCESS_DESC.string,
            Date().time,
            EMPTY
        )
        sendTextMessage(chatId, reply)
    }

    private fun addTaskProcessDesc(chatId: Long, taskDescription: String) {
        val reply = """
            Enter task finish date in format <hours>:<minutes> <day>.<month>.<year>
            For instance 23:59 04.05.2021            
            """.trimIndent()

        databaseRepository.addHistoryRecord(
            chatId,
            fromOperationToString(Operation.ADD_TASK),
            Stage.PROCESS_DATE.string,
            Date().time,
            taskDescription
        )
        sendTextMessage(chatId, reply)
    }


    private fun addTaskProcessDate(chatId: Long, textDate: String) {
        val lastOperation = databaseRepository.getNearestHistoryRecord(chatId)!!

        if (lastOperation.data != EMPTY) {
            val parsedData: Long
            try {
                parsedData = parseStringToUnixDate(textDate)
            } catch (e: Exception) {
                sendTextMessage(chatId, "Invalid date format!")
                return
            }
            val userOffset = databaseRepository.getUserOffset(chatId) ?: run {
                databaseRepository.addHistoryRecord(
                    chatId,
                    fromOperationToString(Operation.ADD_TASK),
                    Stage.FAILED.string,
                    Date().time,
                    EMPTY
                )
                sendTextMessage(chatId, "You are not registered. Press /start to register.")
                return
            }
            val timeOffset = parsedData + userOffset
            val currentTaskRecord = TaskRecord(0, chatId, lastOperation.data, parsedData, timeOffset)
            if (lastOperation.stage != Stage.PROCESS_DATE) {
                sendTextMessage(chatId, "Failed to create task. Please try again!")
                databaseRepository.addHistoryRecord(
                    chatId,
                    fromOperationToString(Operation.ADD_TASK),
                    Stage.FAILED.string,
                    0,
                    EMPTY
                )
                return
            }
            if (!databaseRepository.addTask(currentTaskRecord)) {
                databaseRepository.addHistoryRecord(
                    chatId,
                    fromOperationToString(Operation.ADD_TASK),
                    Stage.FAILED.string,
                    Date().time,
                    EMPTY
                )
                sendTextMessage(chatId, "Sql error! Failed to create task. Please try again!")
                return
            } else {
                databaseRepository.addHistoryRecord(
                    chatId,
                    fromOperationToString(Operation.ADD_TASK),
                    Stage.SUCCESS.string,
                    Date().time,
                    EMPTY
                )
                sendTextMessage(chatId, "Your task successfully added to the list.")
                return
            }
        }
        sendTextMessage(chatId, "Failed to create task. Please try again!")
        databaseRepository.addHistoryRecord(
            chatId,
            fromOperationToString(Operation.ADD_TASK),
            Stage.FAILED.string,
            Date().time,
            EMPTY
        )
    }

    private fun listOfTasks(chatId: Long) {
        val response = databaseRepository.userListOfTasks(chatId)
        val builder = StringBuilder()
        if (response.isEmpty())
            builder.append("Your task list is empty! You can add task using /add_task command.")
        else {
            builder.append("Task list:\n")
            response.forEachIndexed { index, task ->
                builder.append(
                    "${index + 1}) '${task.description}' due to " +
                            parseUnixTimeToStringDate(task.date) + "\n"
                )
            }
        }
        val reply = builder.toString()
        databaseRepository.addHistoryRecord(
            chatId,
            fromOperationToString(Operation.LIST),
            Stage.NOTHING.string,
            Date().time,
            EMPTY
        )
        sendTextMessage(chatId, reply)
    }

    private fun deleteTask(chatId: Long, text: String) {
        val lastHistoryRecord = databaseRepository.getNearestHistoryRecord(chatId)

        when {
            lastHistoryRecord == null -> deleteTaskProcessIndex(chatId)
            lastHistoryRecord.operation == Operation.DELETE_TASK && lastHistoryRecord.stage == Stage.PROCESS_INDEX ->
                deleteTaskByIndex(chatId, text.toInt())

            else -> deleteTaskProcessIndex(chatId)
        }
    }

    private fun deleteTaskProcessIndex(chatId: Long) {
        // deleteCommandStage = DeleteCommandState.PROCESS_INDEX
        databaseRepository.addHistoryRecord(
            chatId,
            fromOperationToString(Operation.DELETE_TASK),
            Stage.PROCESS_INDEX.string,
            Date().time,
            EMPTY
        )
        sendTextMessage(chatId, "Send number of task from list to delete.")
    }

    private fun deleteTaskByIndex(chatId: Long, index: Int) {
        val listOfTasks = databaseRepository.userListOfTasks(chatId)

        if (index - 1 >= listOfTasks.size || index - 1 < 0) {
            databaseRepository.addHistoryRecord(
                chatId,
                fromOperationToString(Operation.DELETE_TASK),
                Stage.FAILED.string,
                Date().time,
                EMPTY
            )
            sendTextMessage(chatId, "Invalid index value! Please send valid index from the list.")
            return
        }
        val taskToDelete = listOfTasks[index - 1]
        if (!databaseRepository.deleteTask(taskToDelete)) {
            databaseRepository.addHistoryRecord(
                chatId,
                fromOperationToString(Operation.DELETE_TASK),
                Stage.FAILED.string,
                Date().time,
                EMPTY
            )
            sendTextMessage(chatId, "Can't delete task. Please try again.")
            return
        }
        databaseRepository.addHistoryRecord(
            chatId,
            fromOperationToString(Operation.DELETE_TASK),
            Stage.SUCCESS.string,
            Date().time,
            EMPTY
        )
        sendTextMessage(chatId, "Task successfully deleted from your list.")
    }

    private fun notifyUser(task: TaskRecord) {
        val notification = """There are ${task.hoursBeforeDeadline} hours left before '${task.description}'!
            Don't forget about that!
        """.trimMargin()
        sendTextMessage(task.chatId, notification)
    }

    private fun updateNotificationValueForTask(task: TaskRecord) {
        databaseRepository.updateNotifiedValues(task.id)
    }

    // 3 hours before and 24 hours before +- 5 min
    private fun notifyUsersAboutDeadline(currentTimeInUnix: Long) {
        val tasksToNotify = databaseRepository.findTaskForNotify(currentTimeInUnix)
        if (tasksToNotify.isNotEmpty()) {
            tasksToNotify.forEach {
                if (!(it.hoursBeforeDeadline == 3 && it.notifiedTimes == 1)) {
                    notifyUser(it)
                    updateNotificationValueForTask(it)
                }
            }
        }
    }

    private fun validateTasks(currentTimeInUnix: Long) {
        val tasks = databaseRepository.tasksToDelete(currentTimeInUnix)
        tasks.forEach {
            databaseRepository.deleteTask(it)
        }
    }

    fun run() {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        try {
            botsApi.registerBot(this)
            while (true) {
                sleep(1000)
                val currentDateTime = Date()
                val stringCurrentDate = SimpleDateFormat("HH:mm dd.MM.yyyy").format(currentDateTime)
                println("$stringCurrentDate and in unix: ${currentDateTime.time}")
                notifyUsersAboutDeadline(currentDateTime.time)
                validateTasks(currentDateTime.time)
            }
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }


}