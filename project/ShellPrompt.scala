import sbt._
import com.typesafe.sbt.SbtGit._

object ShellPrompt {
  val prompt: State => String = { state =>
    val extracted = Project.extract(state)
    val name    = extracted get Keys.name
    val version = extracted get GitKeys.baseVersion
    val branch  = extracted get GitKeys.gitCurrentBranch

    "%s:%s %s> ".format (
      name.colored(scala.Console.YELLOW),
      version,
      s"[$branch]".colored(scala.Console.RED)
    )
  }

  implicit class ColorOpts(val s: String) extends AnyVal {
    def colored(c: String): String = c + s + scala.Console.RESET
  }
}
