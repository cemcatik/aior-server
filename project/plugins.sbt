addSbtPlugin("com.typesafe.sbt" % "sbt-git"             % "1.0.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.0")
addSbtPlugin("org.wartremover"  % "sbt-wartremover"     % "2.4.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.20")

libraryDependencies ++= Seq(
  // To get rid of SLF4J warnings when sbt starts up
  "org.slf4j" % "slf4j-simple" % "1.7.26"
)
