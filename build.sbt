organization    := "com.catikkas"
name            := "aior-server"
versionWithGit
git.baseVersion := "1.0"

shellPrompt := ShellPrompt.prompt

scalaVersion  := "2.11.11"
scalacOptions := Seq(
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps"
)

def akka(c: String) = "com.typesafe.akka" %% s"akka-$c" % "2.5.1"
def specs2(c: String) = "org.specs2" %% s"specs2-$c" % "2.4.11"

libraryDependencies ++= Seq(
  akka("actor"),
  akka("slf4j"),
  "org.slf4j"             % "slf4j-api"       % "1.7.7",
  "ch.qos.logback"        % "logback-classic" % "1.1.2",
  "com.google.code.gson"  % "gson"            % "2.3.1",

  specs2("core") % Test,
  specs2("junit") % Test
)
