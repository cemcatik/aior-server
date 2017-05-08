addSbtPlugin("com.typesafe.sbt" % "sbt-git"         % "0.9.2")
addSbtPlugin("com.geirsson"     %% "sbt-scalafmt"   % "0.6.8")
addSbtPlugin("org.wartremover"  % "sbt-wartremover" % "2.0.3")

libraryDependencies ++= Seq(
  // To get rid of SLF4J warnings when sbt starts up
  "org.slf4j" % "slf4j-simple" % "1.7.12"
)
