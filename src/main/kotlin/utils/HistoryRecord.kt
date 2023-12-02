package utils

enum class Operation {
    ADD_TASK,
    DELETE_TASK,
    START,
    LIST,
    HELP
}

data class HistoryRecord(
    val chatId: Long,
    val operation: Operation,
    val stage: Stage,
    val dateInUnix: Long,
    val data: String?
)

enum class Stage(val string: String){
    PROCESS_DESC("PROCESS_DESC"),
    PROCESS_DATE("PROCESS_DATE"),
    PROCESS_INDEX("PROCESS_INDEX"),
    PROCESS_OFFSET("PROCESS_OFFSET"),
    SUCCESS("SUCCESS"),
    NOTHING("NOTHING"),
    FAILED("FAILED")
}

//const val PROCESS_DESC = "PROCESS_DESC"
//const val PROCESS_DATE = "PROCESS_DATE"
//const val PROCESS_INDEX = "PROCESS_INDEX"
//const val PROCESS_OFFSET = "PROCESS_OFFSET"
//const val SUCCESS = "SUCCESS"
//const val NOTHING = "NOTHING"
//const val FAILED = "FAILED"
