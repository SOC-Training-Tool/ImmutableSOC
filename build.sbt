
lazy val root = project.in(file("."))
  .settings(
    name         := "ImmutableSOC",
    organization := "io.github.soc-training-tool",
    scalaVersion := "3.5.2",
    description  := "Library for Immutable Settlers of Catan.",
    version      := "0.1.0",

    libraryDependencies ++= Seq(
      "org.scalactic" %% "scalactic" % "3.2.19",
      "org.scalatest" %% "scalatest" % "3.2.19" % "test",
    ),

    Test / publishArtifact := true,

    run / fork       := true,
    run / javaOptions += "-Xmx2G",

    publishTo      := Some("GitHub Package Registry" at "https://maven.pkg.github.com/SOC-Training-Tool/ImmutableSOC"),
    publishMavenStyle := true,

    inThisBuild(List(
      licenses  := Seq("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
      homepage  := Some(url("https://github.com/SOC-Training-Tool/ImmutableSOC")),
      scmInfo   := Some(ScmInfo(
        url("https://github.com/SOC-Training-Tool/ImmutableSOC"),
        "git@github.com:SOC-Training-Tool/ImmutableSOC.git"
      )),
      developers := List(Developer(
        "grogdotcom", "Gregory Herman",
        "g.herman27@gmail.com",
        url("https://github.com/grogdotcom")
      )),
      credentials += Credentials(
        "GitHub Package Registry",
        "maven.pkg.github.com",
        "SOC-Training-Tool",
        sys.env.getOrElse("GITHUB_TOKEN", "")
      )
    ))
  )
