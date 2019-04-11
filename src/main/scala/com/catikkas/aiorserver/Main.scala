package com.catikkas.aiorserver

import akka.actor.CoordinatedShutdown
import akka.{actor => untyped}
import akka.actor.CoordinatedShutdown._
import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.adapter._
import com.catikkas.aiorserver.Main._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

object Main {

  def main(args: Array[String]): Unit = {
    val system = untyped.ActorSystem("aior-server")
    val config = system.settings.config.as[Config]("aiorserver")
    discarding { system.spawn(Supervisor.behavior(config), "sup") }
  }

  final case class Config(
      port: Int,
      mouseSpeed: Double,
      mouseWheelSpeed: Double
  )

}

object Supervisor {
  sealed trait Command
  final case object Initialize extends Command

  def notInitialized(config: Config): Behavior[Command] = Behaviors.setup { context =>
    val robot  = context.spawn(Robot.behavior(config), "robot")
    val server = context.actorOf(Server.props(config, robot.toUntyped), "server")
    val ctrld  = context.spawn(CtrlD.behavior, "ctrld")

    Seq(server) foreach { a =>
      context watch a
    }

    Seq(robot, ctrld) foreach { a =>
      context watch a
    }

    initialized(robot, server, ctrld)
  }

  def initialized(
      robot: ActorRef[Robot.Command],
      server: untyped.ActorRef,
      ctrld: ActorRef[CtrlD.Command]
  ): Behavior[Command] =
    Behaviors.logMessages(Behaviors.receiveSignal {
      case (ctx, Terminated(`robot`)) =>
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

  def behavior(config: Config): Behavior[Command] = notInitialized(config)
}
