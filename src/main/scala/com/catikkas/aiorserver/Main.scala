package com.catikkas.aiorserver

import akka.actor.CoordinatedShutdown
import akka.{actor => untyped}
import akka.actor.CoordinatedShutdown._
import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.adapter._

object Main {

  def main(args: Array[String]): Unit = {
    val system = untyped.ActorSystem("aior-server")
    discarding { system.spawn(Supervisor.behavior, "sup") }
  }

}

object Supervisor {
  sealed trait Command
  final case object Initialize extends Command

  val notInitialized: Behavior[Command] = Behaviors.setup { context =>
    val robot  = context.actorOf(Robot.props, "robot")
    val server = context.actorOf(Server.props(robot), "server")
    val ctrld  = context.spawn(CtrlD.behavior, "ctrld")

    Seq(robot, server) foreach { a =>
      context watch a
    }

    Seq(ctrld) foreach { a =>
      context watch a
    }

    initialized(robot, server, ctrld)
  }

  def initialized(
      robot: untyped.ActorRef,
      server: untyped.ActorRef,
      ctrld: ActorRef[CtrlD.Command]
  ): Behavior[Command] =
    Behaviors.logMessages(Behaviors.receiveSignal {
      case (ctx, Terminated(r)) if r == robot.toTyped =>
        ctx.log.error("java.awt.Robot terminated. Please make sure environment is not headless.")
        terminate(ctx)
        Behavior.same

      case (ctx, Terminated(s)) if s == server.toTyped =>
        ctx.log.error("Server failed to bind. Please check config.")
        terminate(ctx)
        Behavior.same

      case (ctx, Terminated(`ctrld`)) =>
        terminate(ctx)
        Behavior.same
    })

  def terminate(context: ActorContext[Command]): Unit = {
    context.log.info("Shutting down now.")
    discarding { CoordinatedShutdown(context.system.toUntyped).run(JvmExitReason) }
  }

  val behavior: Behavior[Command] = notInitialized
}
