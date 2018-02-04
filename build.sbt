name in ThisBuild := "piano-classroom"

scalaVersion in ThisBuild := "2.11.8"

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "joda-time" % "joda-time" % "2.9.9"
  )
)

lazy val `piano-classroom-app` =
  (project in file("piano-classroom-app"))
    .settings(
      commonSettings,
      libraryDependencies ++= Seq(
        "org.json4s" %% "json4s-native" % "3.5.3",
        "com.github.sarxos" % "webcam-capture" % "0.3.11",
        "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.0",
        "com.sksamuel.scrimage" %% "scrimage-io-extra" % "2.1.0",
        "com.sksamuel.scrimage" %% "scrimage-filters" % "2.1.0"
      ),
      mainClass in Compile := Some("ui.App")
    )


