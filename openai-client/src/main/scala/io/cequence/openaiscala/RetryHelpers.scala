package io.cequence.openaiscala

import akka.actor.{ActorSystem, Scheduler}
import akka.pattern.after
import akka.stream.Materializer
import io.cequence.openaiscala.RetryHelpers.RetrySettings

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object RetryHelpers {
  final case class RetrySettings(
      maxRetries: Int = 5,
      delayOffset: FiniteDuration = 2.seconds,
      delayBase: Double = 2
  )
}

trait RetryHelpers {

  def actorSystem: ActorSystem
  implicit val materializer: Materializer = Materializer(actorSystem)

  implicit class FutureWithRetry[T](f: Future[T]) {

    def retryOnFailure(implicit
        retrySettings: RetrySettings,
        ec: ExecutionContext,
        scheduler: Scheduler
    ): Future[T] = {
      retry(() => f, retrySettings.maxRetries)
    }

    private[openaiscala] def delay(
        n: Integer
    )(implicit retrySettings: RetrySettings): FiniteDuration =
      FiniteDuration(
        scala.math.round(
          retrySettings.delayOffset.length + scala.math.pow(
            retrySettings.delayBase,
            n.doubleValue()
          )
        ),
        retrySettings.delayOffset.unit
      )

    private[openaiscala] def retry(
        attempt: () => Future[T],
        attempts: Int
    )(implicit
        ec: ExecutionContext,
        scheduler: Scheduler,
        retrySettings: RetrySettings
    ): Future[T] = {
      try {
        if (attempts > 0) {
          attempt().recoverWith { case Retryable(_) =>
            after(delay(attempts), scheduler) {
              retry(attempt, attempts - 1)
            }
          }
        } else {
          attempt()
        }
      } catch {
        case NonFatal(error) => Future.failed(error)
      }
    }

  }
}
