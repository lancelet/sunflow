import AssemblyKeys._

name := "sunflow"

version := "0.08.0"

organization := "com.github.sunflow"

scalaVersion := "2.9.1-1"

assemblySettings

mainClass := Some("SunflowGUI")

mainClass in assembly := Some("SunflowGUI")

excludedFiles in assembly := { (bases: Seq[File]) =>
  bases flatMap { base =>
    (base / "META-INF" * "*").get collect {
      case f if f.getName.toLowerCase == "dummy.dsa" => f
      case f if f.getName.toLowerCase == "dummy.sf" => f
      case f if f.getName.toLowerCase == "manifest.mf" => f
    }
  }
}

libraryDependencies ++= Seq(
  "janino" % "janino" % "2.5.10"
)