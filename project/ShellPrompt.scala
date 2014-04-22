import sbt._
import com.typesafe.sbt.SbtGit._

object ShellPrompt {
  def color(c: String, s: String) = c + s + scala.Console.RESET

  val prompt: State => String = { state =>
    val extracted = Project.extract(state)
    val name    = extracted get Keys.name
    val version = extracted get GitKeys.baseVersion
    val branch  = extracted get GitKeys.gitCurrentBranch

    "%s:%s %s> ".format (
      color(scala.Console.YELLOW, name),
      version,
      color(scala.Console.RED, s"[$branch]")
    )
  }
}