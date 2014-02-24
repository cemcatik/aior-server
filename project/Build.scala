import sbt._
import Keys._
import scala.language.postfixOps

object Settings {
  val name = "bbremote"
  val version = "0.0.1-SNAPSHOT"
  val scalaVersion = "2.10.3"

  val resolvers = Seq(
    "typesafe" at "http://repo.typesafe.com/typesafe/repo"
  )

  val akkaVer = "2.2.3"
  val akkaActor  = "com.typesafe.akka" %% "akka-actor"  % akkaVer
  val akkaSlf4j  = "com.typesafe.akka" %% "akka-slf4j"  % akkaVer
  val akkaKernel = "com.typesafe.akka" %% "akka-kernel" % akkaVer

  val logback  = "ch.qos.logback"       % "logback-classic" % "1.0.13"
  val gson     = "com.google.code.gson" % "gson"            % "2.2.4"
  val specs    = "org.specs2"          %% "specs2"          % "2.3.8"

  val dependencies = List(
    akkaActor,
    akkaSlf4j,
    akkaKernel,
    logback,
    gson,
    specs % "test"
  )

  val scalacOptions = Seq(
    "-feature",
    "-language:implicitConversions",
    "-language:postfixOps"
  )

  val distJvmOptions = "-Xms8M -Xmx8M -Xss1M -XX:MaxPermSize=32M -XX:+UseParallelGC"
  val distBootClass  = "com.catikkas.bbremote.Main"
}

object BBRemoteBuild extends Build {
  import ShellPrompt._
  import com.typesafe.sbteclipse.plugin.EclipsePlugin._
  import akka.sbt.AkkaKernelPlugin
  import akka.sbt.AkkaKernelPlugin._

  lazy val project = Project(
    id = "bbremote",
    base = file("."),
    settings = Defaults.defaultSettings
  )
  .settings(
    name           := Settings.name,
    version        := Settings.version,
    scalaVersion   := Settings.scalaVersion,
    resolvers      := Settings.resolvers,
    libraryDependencies ++= Settings.dependencies,
    shellPrompt    := buildShellPrompt,
    scalacOptions ++= Settings.scalacOptions
  )
  .settings(
    EclipseKeys.createSrc  := EclipseCreateSrc.Default + EclipseCreateSrc.Resource + EclipseCreateSrc.Managed,
    EclipseKeys.withSource := true
  )
  .settings(distSettings: _*)
  .settings(
    distJvmOptions in Dist := Settings.distJvmOptions,
    distBootClass  in Dist := Settings.distBootClass
  )
}

// Shell prompt which show the current project,
// git branch and build version
object ShellPrompt {
  object devnull extends ProcessLogger {
    def info(s: => String) {}
    def error(s: => String) {}
    def buffer[T](f: => T): T = f
  }
  def currBranch = (
    ("git status -sb" lines_! devnull headOption)
     getOrElse "-" stripPrefix "## "
  )

  def color(c: String, s: String) = c + s + scala.Console.RESET

  val buildShellPrompt = (state: State) => {
    val currProject = Project.extract(state).currentProject.id
    "%s:%s %s> ".format(
      color(scala.Console.YELLOW, currProject),
      Settings.version,
      color(scala.Console.RED, s"[$currBranch]")
    )
  }
}