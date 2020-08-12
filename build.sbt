scalaVersion := "2.13.3"

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