organization := "com.catikkas"
name := "aior-server"

enablePlugins(GitVersioning)
git.baseVersion := "1.0"
git.useGitDescribe := true

shellPrompt := ShellPrompt.prompt

scalaVersion := "2.12.2"
scalacOptions ++= Seq(
  "-feature"
)

def akka(c: String)   = "com.typesafe.akka" %% s"akka-$c"   % "2.5.1"
def specs2(c: String) = "org.specs2"        %% s"specs2-$c" % "3.8.9"

libraryDependencies ++= Seq(
  akka("actor"),
  akka("slf4j"),
  "ch.qos.logback"       % "logback-classic" % "1.2.3",
  "com.google.code.gson" % "gson"            % "2.3.1",
  //
  // Test libraries
  specs2("core")  % Test,
  specs2("junit") % Test
)

wartremoverErrors in (Compile, compile) ++= Warts.allBut(
  Wart.DefaultArguments,
  Wart.ImplicitParameter,
  Wart.Any,
  Wart.Nothing,
  Wart.Overloading,
  Wart.PublicInference,
  Wart.Equals
)
wartremoverErrors in (Test, compileIncremental) := (wartremoverErrors in (Compile, compile)).value diff Seq(
  Wart.NonUnitStatements,
  Wart.Product
)
