
lazy val commonSettings = Seq(
  organization := "io.github.soc-training-tool",
  scalaVersion := "3.5.2",
  version := "0.1.0"
)

// Game subproject - generic game framework with macros
lazy val game = (project in file("game"))
  .settings(
    commonSettings,
    name := "ImmutableSOC-game",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.18" % "test"
    )
  )

// Main project - SOC-specific code
lazy val root = (project in file("."))
  .dependsOn(game)
  .settings(
    commonSettings,
    name := "ImmutableSOC",
    description := "Library for Immutable Settlers of Catan.",
    libraryDependencies ++= Seq(
      "org.scalactic" %% "scalactic" % "3.2.18",
      "org.scalatest" %% "scalatest" % "3.2.18" % "test"
    ),
    Test / publishArtifact := true,
    run / fork := true,
    publishTo := Some("GitHub Package Registry" at "https://maven.pkg.github.com/SOC-Training-Tool/ImmutableSOC"),
    publishMavenStyle := true
  )

inThisBuild(List(
  licenses := Seq("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/SOC-Training-Tool/ImmutableSOC")),
  scmInfo := Some(ScmInfo(url("https://github.com/SOC-Training-Tool/ImmutableSOC"), "git@github.com:SOC-Training-Tool/ImmutableSOC.git")),
  developers := List(Developer("grogdotcom", "Gregory Herman", "g.herman27@gmail.com", url("https://github.com/grogdotcom"))),
  credentials += Credentials(
    "GitHub Package Registry",
    "maven.pkg.github.com",
    "SOC-Training-Tool",
    sys.env.getOrElse("GITHUB_TOKEN", "")
  )
))
