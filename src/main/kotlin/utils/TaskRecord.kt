package utils

data class TaskRecord(
    val id: Int,
    val chatId: Long,
    val description: String,
    val date: Long,
    val offset: Long = 0,
    val hoursBeforeDeadline: Int = 0,
    val notifiedTimes: Int = 0
)

val EMPTY = null
