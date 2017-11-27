name := "DasaCronJob"

version := "1.0"

scalaVersion := "2.12.4"

sbtVersion := "0.13"


resolvers += "lightshed-maven" at "http://dl.bintray.com/content/lightshed/maven"

libraryDependencies ++= Seq(
  "org.jsoup" % "jsoup" % "1.10.2",
  "org.apache.poi" % "poi" % "3.15",
  "joda-time" % "joda-time" % "2.9.9",
  "javax.mail" % "mail" % "1.4.1",
  "com.github.jurajburian" %% "mailer" % "1.2.1"
)

mainClass in (Compile, run) := Some("Main")
