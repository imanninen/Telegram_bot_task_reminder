import api.TaskManagerBot

fun main(args: Array<String>) {
    val token = System.getenv("TG_BOT_TOKEN") ?: run {
        println("You didn't set up telegram bot token.")
        return
    }
    val bot = TaskManagerBot(token)

    bot.run()
}