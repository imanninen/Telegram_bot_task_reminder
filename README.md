<h1>Telegram bot task reminder</h1>
<h2>Product strory</h2>
<p>As a student I have a lot of deadlines, which I have to remember and don't forget about them. But sometimes, when you have a lot of them, you may miss some of them. To prevent this, I created this telegram bot. It stors your task and their deadline and remind about you about it 24 and 3 hours before deadline. It helped me not to forget about important deadlines. And, I believe, it will help anyone else.</p>

<h2>Implementation details</h2>
<p>I store telegram bot token as environment variable.</p>
<p>Also i use mysql as dbms, so to start my bot locally, you have tp install mysql and run it on localhost:3306</p>

<h2>Future plans</h2>
<ul>
  <li>I whant to make bot asynchronous using coroutines. I guess it is mandatory if I want to publish it as a product.</li>
  <li>Also before bublishing a have to optimize storing data</li>
  <li>Put this bot into docker container and run it in the server.</li>
</ul>
