package korolev.execution

import java.util.{Timer, TimerTask}

import korolev.Async
import korolev.Async.AsyncOps

import scala.concurrent.duration.FiniteDuration
import scala.util.Success

final class JavaTimerScheduler[F[+_]: Async] extends Scheduler[F] {

  import Scheduler._

  private val timer = new Timer()
  private val async = Async[F]

  def scheduleOnce[T](delay: FiniteDuration)(job: => T): JobHandler[F, T] = {
    val promise = Async[F].promise[T]
    val task = new TimerTask {
      def run(): Unit = {
        val task = async.fork {
          val result = job // Execute a job
          promise.complete(Success(result))
        }
        task.runIgnoreResult()
      }
    }
    timer.schedule(task, delay.toMillis)
    JobHandler(
      cancel = () => { task.cancel(); () },
      result = promise.future
    )
  }

  def schedule[U](interval: FiniteDuration)(job: => U): Cancel = {
    val task = new TimerTask {
      def run(): Unit = async.fork(job).runIgnoreResult()
    }
    val millis = interval.toMillis
    timer.schedule(task, millis, millis)
    () => { task.cancel(); () }
  }
}
