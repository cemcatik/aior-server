package com.catikkas.aiorserver

import java.util._

import akka.actor.typed._
import akka.actor.typed.scaladsl._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._

/**
 * Ideally this is for usage within sbt shell.
 * <p>
 * Under normal operational conditions
 * `Ctrl+C`/`SIGINT`/`SIGTERM` are all handled properly by Akka since it registers
 * a shutdown hook and cleans up all resources by running [[akka.actor.CoordinatedShutdown]].
 * <p>
 * Play's sbt plugin is great at this. Once you're running the application in sbt shell, you
 * can hit `Enter` or `Ctrl+D` to exit back to sbt shell without shutting it down.
 * <p>
 * Neither the `fork` nor `cancelable` configs were handling this use case properly, they
 * were both erroring out or not cleaning up resources completely.
 * <p>
 * This actor sets up a stream listening to StdIn. Once user hits `Ctrl+D` the StdIn stream
 * completes. This actor can be death-watched, and when it's terminated you can initiate
 * shutdown.
 *
 * {{{
 *   // within your supervision hierarchy
 *   class Sup extends Actor with ActorLogging {
 *
 *     val ctrld = context watch context.actorOf(CtrlD.props, "ctrld")
 *
 *     def receive = {
 *       case Terminated(`ctrld`) =>
 *         log.info("Stopping now...")
 *         CoordinatedShutdown(context.system).run()
 *       }
 *     }
 *   }
 * }}}
 *
 * @see akka.actor.CoordinatedShutdown
 */
object CtrlD {
  sealed trait Command
  final case object Wait                     extends Command
  final case class HasNext(hasNext: Boolean) extends Command

  def IO[T](body: => T): Future[T] = Future { blocking { body } }

  def behavior: Behavior[Command] = Behaviors.setup { context =>
    context.log.info("Use Ctrl+D to stop.")
    context.self ! Wait

    val scanner = new Scanner(System.in)

    // This actor uses a [[java.util.Scanner]] to listen to StdIn. Once user hits `Ctrl+D` the
    // stream signals EOF and `scanner.hasNext()` return `false`. Since the operations on
    // [[java.util.Scanner]] are blocking we need to run those operations on a dedicated thread
    Behaviors.logMessages(
      Behaviors.receiveMessage {
        case Wait =>
          context.pipeToSelf(IO(scanner.hasNext())) {
            case Success(n) =>
              HasNext(n)

            case Failure(t) =>
              context.log.error(t, "while scanning next.")
              HasNext(false)
          }
          Behavior.same

        case HasNext(true) =>
          context.pipeToSelf(IO(scanner.next())) {
            case Success(_) =>
              Wait

            case Failure(t) =>
              context.log.error(t, "while reading next.")
              Wait
          }
          Behavior.same

        case HasNext(false) =>
          Behavior.stopped
      }
    )
  }

}
