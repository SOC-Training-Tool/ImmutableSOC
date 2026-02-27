
val shapelessDep = "com.chuusai" %% "shapeless" % "2.3.12"

lazy val game = project.in(file("game"))
  .settings(
    name         := "game",
    scalaVersion := "2.13.15",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      shapelessDep
    ),
    scalacOptions += "-J-Xss4m"
  )

lazy val root = project.in(file("."))
  .dependsOn(game)
  .settings(
    name         := "ImmutableSOC",
    organization := "io.github.soc-training-tool",
    scalaVersion := "2.13.15",
    description  := "Library for Immutable Settlers of Catan.",
    version      := "0.1.0",

    libraryDependencies ++= Seq(
      "org.scalactic" %% "scalactic" % "3.0.8",
      "org.scalatest" %% "scalatest" % "3.0.8" % "test",
      shapelessDep
    ),

    Test / publishArtifact := true,

    fork in run       := true,
    javaOptions in run += "-Xmx2G",
    scalacOptions     += "-Xlog-implicits",
    scalacOptions     += "-J-Xss4m",

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
