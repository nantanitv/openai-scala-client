package io.cequence.openaiscala.examples

import scala.util.Random

object CreateThreadMessage extends Example {

  override protected def run =
    for {
      message <- service.createThreadMessage(
        threadId = "thread_c6fFMmUw30l30SzG2KdUViMn",
        content = "Hello, what is AI really?",
        metadata = Map("user_id" -> Random.nextInt().toString)
      )
    } yield {
      println(message)
    }
}
