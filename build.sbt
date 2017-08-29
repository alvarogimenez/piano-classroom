name in ThisBuild := "piano-classroom"

version in ThisBuild:= "1.0"

scalaVersion in ThisBuild := "2.11.8"


lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "org.json4s" %% "json4s-native" % "3.5.3"
  )
)


lazy val `piano-classroom-app` =
  (project in file("piano-classroom-app"))
    .settings(
      commonSettings,
      mainClass in Compile := Some("ui.App")
    )

