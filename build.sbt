organization    := "com.catikkas"
name            := "aior-server"
versionWithGit
git.baseVersion := "1.0"

shellPrompt := ShellPrompt.prompt

scalaVersion  := "2.11.4"
scalacOptions := Seq(
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps"
)

val akkaVer   = "2.3.7"
val specs2Ver = "2.4.11"
libraryDependencies ++= Seq(
  "com.typesafe.akka"    %% "akka-actor"      % akkaVer,
  "com.typesafe.akka"    %% "akka-slf4j"      % akkaVer,
  "com.typesafe.akka"    %% "akka-kernel"     % akkaVer,
  "org.slf4j"             % "slf4j-api"       % "1.7.7",
  "ch.qos.logback"        % "logback-classic" % "1.1.2",
  "com.google.code.gson"  % "gson"            % "2.3.1",

  "org.specs2" %% "specs2-core"  % specs2Ver % "test",
  "org.specs2" %% "specs2-junit" % specs2Ver % "test"
)

enablePlugins(AkkaAppPackaging)
mainClass in Compile := Some("com.catikkas.aiorserver.Main")
