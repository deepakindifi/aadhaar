name := "EKYC"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.bouncycastle" % "bcprov-jdk16" % "1.43",
  "org.bouncycastle" % "bctsp-jdk16" % "1.44",
  "commons-io" % "commons-io" % "2.4",
  "com.itextpdf" % "itextpdf" % "5.5.4",
  "log4j" % "log4j" % "1.2.17",
  "org.apache.santuario" % "xmlsec" % "1.4.3",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.47",
  "commons-codec" % "commons-codec" % "1.10",
  "javax.persistence" % "persistence-api" % "1.0",
  "commons-logging" % "commons-logging" % "1.1.1",
  "com.rabbitmq" % "amqp-client" % "3.6.5",
  "com.googlecode.json-simple" % "json-simple" % "1.1"
)

