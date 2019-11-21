ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1.0"
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
    scalacOptions ++= Seq("-feature", "-deprecation", "-language:postfixOps", "-Xlint"),
    crossScalaVersions := Seq("2.12.10", "2.13.1"),
    parallelExecution in Test := false,

    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    ),

    pomExtra := <url>https://github.com/jyuch/alpakka-mybatis</url>
      <licenses>
        <license>
          <name>MIT License</name>
          <url>https://opensource.org/licenses/mit-license.php</url>
        </license>
      </licenses>
      <scm>
        <url>https://github.com/jyuch/alpakka-mybatis</url>
        <connection>scm:git:https://github.com/jyuch/alpakka-mybatis.git</connection>
      </scm>
      <developers>
        <developer>
          <id>jyuch</id>
          <url>https://github.com/jyuch</url>
        </developer>
      </developers>
  )
