ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "OrbitalMechanicsSimulator"
  )

libraryDependencies += "org.scalafx" % "scalafx_3" % "20.0.0-R31"