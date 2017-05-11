package com.catikkas.aiorserver

import java.util._

import akka.actor._
import akka.event.LoggingReceive
import akka.pattern._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

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
class CtrlD extends Actor with ActorLogging {
  import CtrlD._

  override def preStart(): Unit = {
    self ! Initialize
  }

  def receive = notInitialized

  def notInitialized = LoggingReceive.withLabel("notInitialized") {
    case Initialize =>
      log.info("Use Ctrl+D to stop.")
      val scanner = new Scanner(System.in)
      context become waiting(scanner)
      self ! Wait
  }

  // This actor uses a [[java.util.Scanner]] to listen to StdIn. Once user hits `Ctrl+D` the
  // stream signals EOF and `scanner.hasNext()` return `false`. Since the operations on
  // [[java.util.Scanner]] are blocking we need to run those operations on a dedicated thread
  def waiting(scanner: Scanner) = LoggingReceive.withLabel("waiting") {
    case Wait =>
      val _ = IO(scanner.hasNext())
        .map(HasNext)
        .pipeTo(self)

    case HasNext(true) =>
      val _ = IO(scanner.next())
        .map(_ => Wait)
        .pipeTo(self)

    case HasNext(false) => context stop self
  }

  def IO[T](body: => T): Future[T] = Future { blocking { body } }
}

object CtrlD {
  def props = Props(new CtrlD)

  sealed trait Command
  final case object Initialize extends Command
  final case object Wait       extends Command

  sealed trait Event
  final case class HasNext(hasNext: Boolean) extends Event
}
