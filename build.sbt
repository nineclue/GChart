scalaVersion := "2.13.3"

/*
version in ThisBuild := "0.0.1"
enablePlugins(WindowsPlugin)

maintainer := "Suhku Huh <nineclue@gmail.com>"
packageSummary := "GChart Test"
packageDescription := "Testing package to check GChart"

wixProductId := "941699a7-d7b8-406b-8d0e-ac0c958522f5"
wixProductUpgradeId := "71fe9ea5-9eca-41f0-af6c-736c811b9233"

// enablePlugins(JavaAppPackaging)
enablePlugins(JlinkPlugin)
jlinkIgnoreMissingDependency := JlinkIgnore.everything
*/

fork := true

val folderName =
  if (System.getProperty("os.name").startsWith("Windows")) "windows" else "linux"

// for opencv 
// val libPath = Seq("/usr/lib/jni").mkString(java.io.File.pathSeparator)

// javaOptions in run += s"-Djava.library.path=$libPath"

libraryDependencies ++= Seq(
    "org.openjfx" % "javafx-base" % "11",
    "org.openjfx" % "javafx-controls" % "11",
    "org.openjfx" % "javafx-graphics" % "11" classifier "win",
    // "org.openjfx" % "javafx-graphics" % "11" classifier "linux",
    "org.xerial" % "sqlite-jdbc" % "3.32.3.2",
    "org.tpolecat" %% "doobie-core" % "0.9.0",
    "org.tpolecat" %% "doobie-quill" % "0.9.0"
)