ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.12.18"

val scalaTest = "org.scalatest" %% "scalatest" % "3.2.18" % Test

val spinalRoot = file("/workspaces/SpinalHDL")
lazy val spinalIdslPlugin = ProjectRef(spinalRoot, "idslplugin")
lazy val spinalSim = ProjectRef(spinalRoot, "sim")
lazy val spinalCore = ProjectRef(spinalRoot, "core")
lazy val spinalLib = ProjectRef(spinalRoot, "lib")

lazy val projectname = (project in file("."))
  .settings(
    Compile / scalaSource := baseDirectory.value / "hw" / "spinal",
    Test / scalaSource := baseDirectory.value / "test" / "spinal",
    libraryDependencies += scalaTest
  ).dependsOn(spinalIdslPlugin, spinalSim, spinalCore, spinalLib)

scalacOptions += (spinalIdslPlugin / Compile / packageBin / artifactPath).map { file =>
  s"-Xplugin:${file.getAbsolutePath}"
}.value

fork := true
