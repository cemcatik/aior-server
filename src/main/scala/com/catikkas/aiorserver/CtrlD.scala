package com.catikkas.aiorserver

import java.util._

import akka.Done
import akka.actor._
import akka.event.LoggingReceive
import akka.pattern._
import akka.stream._
import akka.stream.scaladsl._

import scala.collection.JavaConverters._
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

  implicit val mat: ActorMaterializer = ActorMaterializer()

  override def preStart(): Unit = {
    self ! Initialize
  }

  def receive = LoggingReceive {
    case Initialize =>
      // Ctrl+D sends an EOF and that's when you try to read from StdIn and returns null.
      // Basically we're setting up the Stream to read from StdIn ignoring all input.
      // When user hits Ctrl+D the stream completes and we get notified in `Done`.
      val _ = Source
        .fromIterator(() => new Scanner(System.in).asScala)
        .runWith(Sink.ignore)
        .pipeTo(self)

      log.info("Use Ctrl+D to stop.")

    case Done =>
      context stop self
  }

}

object CtrlD {
  def props = Props(new CtrlD)

  sealed trait Command
  final case object Initialize extends Command
}
