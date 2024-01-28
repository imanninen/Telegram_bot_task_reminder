package utils

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

// format: HH:mm dd.MM.yyyy
fun parseStringToUnixDate(date: String, offset: Long = 0L): Long {
    val l = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy"))
    return l.toInstant(ZoneOffset.ofHours(2)).epochSecond * 1000 + offset
}

// format: HH:mm dd.MM.yyyy
fun parseUnixTimeToStringDate(date: Long, offset: Long = 0L): String {
    val parsedTime = Date(date + offset)
    return SimpleDateFormat("HH:mm dd.MM.yyyy").format(parsedTime)
}

fun fromOperationToString(operation: Operation): String = when (operation) {
    Operation.START -> "START"
    Operation.LIST -> "LIST"
    Operation.HELP -> "HELP"
    Operation.ADD_TASK -> "ADD_TASK"
    Operation.DELETE_TASK -> "DELETE_TASK"
}

fun fromStringToOperation(operation: String): Operation = when (operation) {
    "START" -> Operation.START
    "LIST" -> Operation.LIST
    "HELP" -> Operation.HELP
    "ADD_TASK" -> Operation.ADD_TASK
    "DELETE_TASK" -> Operation.DELETE_TASK
    else -> Operation.HELP // ??
}

fun fromStringToStage(stage: String): Stage = when (stage) {
    "PROCESS_DESC" -> Stage.PROCESS_DESC
    "PROCESS_DATE" -> Stage.PROCESS_DATE
    "PROCESS_INDEX" -> Stage.PROCESS_INDEX
    "PROCESS_OFFSET" -> Stage.PROCESS_OFFSET
    "SUCCESS" -> Stage.SUCCESS
    "NOTHING" -> Stage.NOTHING
    "FAILED" -> Stage.FAILED
    else -> Stage.NOTHING
}

fun isNumber(text: String): Boolean {
    text.forEach { if (!it.isDigit()) return false }
    return true
}