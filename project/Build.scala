import sbt._
import Keys._
import com.typesafe.sbteclipse.plugin.EclipsePlugin._

object BuildSettings {
  import Resolvers._
  import ShellPrompt._

  val buildOrganization = "bbremote"
  val buildVersion = "0.0.1-SNAPSHOT"
  val buildScalaVersion = "2.10.3"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := buildOrganization,
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    shellPrompt := buildShellPrompt,
    resolvers := twirperResolvers,
    EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource)
}

object BBRemoteBuild extends Build {
  import BuildSettings._
  
  val akkaVer = "2.2.3"
  val akkaActor = "com.typesafe.akka"  %% "akka-actor" % akkaVer
  val akkaSlf4j = "com.typesafe.akka"  %% "akka-slf4j" % akkaVer
  val akkaDeps = Seq(akkaActor, akkaSlf4j)

  val logback  = "ch.qos.logback" % "logback-classic" % "1.0.13"
  val gson = "com.google.code.gson" % "gson" % "2.2.4"
  val deps = List(
      logback,
      gson
      ) ++ akkaDeps

  lazy val project = Project(
    id = "bbremote",
    base = file("."),
    settings = buildSettings ++ Seq(libraryDependencies ++= deps)
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
    getOrElse "-" stripPrefix "## ")

  def color(c: String, s: String) = c + s + scala.Console.RESET

  val buildShellPrompt = {
    (state: State) =>
      {
        val currProject = Project.extract(state).currentProject.id
        "%s:%s %s> ".format(
          color(scala.Console.YELLOW, currProject),
          BuildSettings.buildVersion,
          color(scala.Console.RED, s"[$currBranch]"))
      }
  }
}

object Resolvers {
  val typesafe = "typesafe" at "http://repo.typesafe.com/typesafe/repo"

  val twirperResolvers = Seq(typesafe)
}