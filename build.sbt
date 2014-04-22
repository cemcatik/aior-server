import Dependencies._
import com.typesafe.sbt.SbtGit._
import akka.sbt.AkkaKernelPlugin._

organization := "com.catikkas"

name := "bbremote"

versionWithGit

git.baseVersion := "1.0"

scalaVersion := "2.10.4"

shellPrompt := ShellPrompt.prompt

libraryDependencies ++= Dependencies(
  akkaDeps,
  logback,
  gson,
  specs % "test"
)

scalacOptions := Seq(
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps"
)

distSettings

distJvmOptions in Dist := "-Xms8M -Xmx8M -Xss1M -XX:MaxPermSize=32M -XX:+UseParallelGC"

distBootClass in Dist := "com.catikkas.bbremote.Main"