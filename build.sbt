
name := "ImmutableSOC"
organization := "io.github.soc-training-tool"
scalaVersion := "2.13.15"
description := "Library for Immutable Settlers of Catan."
version := "0.1.0"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"
libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.12"

Test / publishArtifact := true

scalacOptions += "-Xlog-implicits"
//scalacOptions += "-Ystatistics"

// Publishing settings
publishTo := Some("GitHub Package Registry" at "https://maven.pkg.github.com/SOC-Training-Tool/ImmutableSOC")
publishMavenStyle := true

inThisBuild(List(
  // Publishing metadata
  licenses := Seq("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/SOC-Training-Tool/ImmutableSOC")),
  scmInfo := Some(ScmInfo(url("https://github.com/SOC-Training-Tool/ImmutableSOC"), "git@github.com:SOC-Training-Tool/ImmutableSOC.git")),
  developers := List(Developer("grogdotcom", "Gregory Herman", "g.herman27@gmail.com", url("https://github.com/grogdotcom"))),

  // GitHub Packages credentials
  credentials += Credentials(
    "GitHub Package Registry",
    "maven.pkg.github.com",
    "SOC-Training-Tool",
    sys.env.getOrElse("GITHUB_TOKEN", "")
  )
))
