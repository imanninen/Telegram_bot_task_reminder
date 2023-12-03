# Telegram bot task reminder
## Product story
As a student I have a lot of deadlines, which I have to remember and don't forget about them. But sometimes, when you have a lot of work, you may miss some of them. To prevent this, I created this telegram bot. It stors your task and their deadline and remind about you about it 24 and 3 hours before deadline. It helped me not to forget about important deadlines. And, I believe, it will help anyone else.

## Implementation details
I store telegram bot token as environment variable.
Also I use mysql as dbms, so to start my bot locally, you have tp install mysql and run it on `localhost:3306`

## Future plans
* I whant to make bot asynchronous using coroutines. I guess it is mandatory if I want to publish it as a product.
* Also before bublishing a have to optimize storing data
* Put this bot into docker container and run it in the server.
