import sbt._
import language.implicitConversions

object Dependencies {
  val akkaVer = "2.3.2"
  val akkaActor  = "com.typesafe.akka" %% "akka-actor"  % akkaVer
  val akkaSlf4j  = "com.typesafe.akka" %% "akka-slf4j"  % akkaVer
  val akkaKernel = "com.typesafe.akka" %% "akka-kernel" % akkaVer
  val akkaDeps = Seq(akkaActor, akkaSlf4j, akkaKernel)

  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.7"
  val logback  = "ch.qos.logback" % "logback-classic" % "1.1.2"
  val slf4jDeps = Seq(slf4jApi, logback)

  val gson  = "com.google.code.gson" % "gson" % "2.2.4"
  val specs = "org.specs2" %% "specs2" % "2.3.11"

  def apply(dependencies: Either[ModuleID, Seq[ModuleID]]*): Seq[ModuleID] = dependencies flatMap {
    case Left(d)   => Seq(d)
    case Right(ds) => ds
  }

  implicit def moduleId2Left(d: ModuleID) = Left(d)
  implicit def seq2Right(ds: Seq[ModuleID]) = Right(ds)
}