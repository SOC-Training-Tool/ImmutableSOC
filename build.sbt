
name := "ImmutableSOC"
organization := "io.github.soc-training-tool"
scalaVersion := "2.13.14"
description := "Library for Immutable Settlers of Catan."
version := "0.0.7-SNAPSHOT"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"
libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.12"

Test / publishArtifact := true

scalacOptions += "-Xlog-implicits"
//scalacOptions += "-Ystatistics"

// Publishing settings
publishTo := sonatypePublishToBundle.value
publishMavenStyle := true

inThisBuild(List(
  // Publishing metadata
  licenses := Seq("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/SOC-Training-Tool/ImmutableSOC")),
  scmInfo := Some(ScmInfo(url("https://github.com/SOC-Training-Tool/ImmutableSOC"), "git@github.com:SOC-Training-Tool/ImmutableSOC.git")),
  developers := List(Developer("grogdotcom", "Gregory Herman", "g.herman27@gmail.com", url("https://github.com/grogdotcom"))),

  // Sonatype credentials
  credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", sys.env.getOrElse("SONATYPE_USERNAME", ""), sys.env.getOrElse("SONATYPE_PASSWORD", "")),

  // PGP signing settings
  usePgpKeyHex(sys.env.getOrElse("PGP_KEY_ID", "0")),
  pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toArray)
))
