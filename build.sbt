name := "flashcard2"
version := "0.1"
scalaVersion := "2.11.8"

lazy val http4sVersion = "0.15.6"
val circeVersion = "0.5.1"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"
libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.4.2" % Test

libraryDependencies += "ch.qos.logback" %  "logback-classic" % "1.1.7" //exclude("org.slf4j.impl", "StaticLoggerBinder")
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"

libraryDependencies += "com.h2database" % "h2" % "1.3.148"

libraryDependencies ++= Seq(
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % http4sVersion
)

libraryDependencies += "com.typesafe.slick" %% "slick" % "3.1.1"



libraryDependencies ++= Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies += "org.reactormonk" %% "cryptobits" % "1.1"