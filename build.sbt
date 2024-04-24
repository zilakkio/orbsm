
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "OrbitalMechanicsSimulator"
  )

libraryDependencies += "org.scalafx" % "scalafx_3" % "20.0.0-R31"
libraryDependencies += "com.lihaoyi" %% "upickle" % "3.1.4"

assembly / assemblyMergeStrategy := {
        case PathList("META-INF", xs @ _*) => MergeStrategy.discard
        case x => MergeStrategy.first
}

Compile / resourceDirectory := baseDirectory.value / "src/main/resources"
