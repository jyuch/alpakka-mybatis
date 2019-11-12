ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "dev.jyuch"
ThisBuild / organizationName := "jyuch"

lazy val root = (project in file("."))
  .settings(
    name := "alpakka-mybatis",
    libraryDependencies ++= Seq(
      "org.mybatis" % "mybatis" % "3.5.3",
      "com.typesafe.akka" %% "akka-stream" % "2.6.0",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.0" % Test,
      "org.scalatest" %% "scalatest" % "3.0.8" % Test,
      "com.h2database" % "h2" % "1.4.200" % Test,
    ),
    scalacOptions ++= Seq("-feature", "-deprecation", "-language:postfixOps"),
    parallelExecution in Test := false,
  )
