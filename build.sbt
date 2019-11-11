import Dependencies._

ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "dev.jyuch"
ThisBuild / organizationName := "jyuch"

lazy val root = (project in file("."))
  .settings(
    name := "alpakka-mybatis",
    libraryDependencies += scalaTest % Test
  )
