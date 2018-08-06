name := "StockStream"

scalaVersion := "2.12.6"

lazy val buildSettings = Defaults.coreDefaultSettings ++ Seq(
  organization := "it.alexov",
  version := "0.1",
  scalaVersion := Dependencies.scalaLastVersion,
  target := baseDirectory.value / "build",
  scalacOptions ++= List("-unchecked", "-deprecation", "-encoding", "UTF8"),

)


lazy val root = (project in file("."))
  .settings(buildSettings)
  .settings(
    name := "stockstream",
    libraryDependencies ++= Dependencies.common ++ Dependencies.io ++ Dependencies.tests
  )

