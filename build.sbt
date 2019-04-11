organization := "com.catikkas"
name := "aior-server"
maintainer := "cem.catikkas@gmail.com"

Global / onChangedBuildSource := ReloadOnSourceChanges

enablePlugins(GitVersioning)
git.baseVersion := "1.0"
git.useGitDescribe := true

shellPrompt := ShellPrompt.prompt

scalaVersion := "2.12.8"
scalacOptions ++= Seq(
  "-feature",
  "-deprecation"
)

def akka(c: String)   = "com.typesafe.akka" %% s"akka-$c"   % "2.5.22"
def specs2(c: String) = "org.specs2"        %% s"specs2-$c" % "4.3.4"

libraryDependencies ++= Seq(
  akka("actor"),
  akka("actor-typed"),
  akka("slf4j"),
  "ch.qos.logback"       % "logback-classic" % "1.2.3",
  "com.google.code.gson" % "gson"            % "2.3.1",
  "com.iheart"           %% "ficus"          % "1.4.3",
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

enablePlugins(JavaAppPackaging)
javaOptions in Universal ++= Seq(
  // -J params will be added as jvm parameters
  "-J-client",
  "-J-XX:+UseG1GC",
  "-J-XX:MaxGCPauseMillis=50",
  "-J-Xmx8m",
  "-J-Xms8m",
  // others will be added as app parameters
  "-Dakka.loglevel=INFO"
)
