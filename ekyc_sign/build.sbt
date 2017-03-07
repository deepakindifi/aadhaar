name := "EKYC"

version := "1.0"

scalaVersion := "2.9.1"

mainClass in (Compile, run) := Some("ReceiveRequest")

libraryDependencies ++= Seq(
  "com.itextpdf" % "itextpdf" % "5.5.4",
  "org.apache.santuario" % "xmlsec" % "1.4.3",
  "com.rabbitmq" % "amqp-client" % "3.6.5",
  "com.googlecode.json-simple" % "json-simple" % "1.1",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.47",
  "org.bouncycastle" % "bcmail-jdk15on" % "1.47", 
  "org.bouncycastle" % "bctsp-jdk15on" % "1.46",
  "commons-codec" % "commons-codec" % "1.4", 
  "commons-io" % "commons-io" % "2.4",
  "javax.persistence" % "persistence-api" % "1.0",
  "log4j" % "log4j" % "1.2.17",
  "commons-logging" % "commons-logging" % "1.1.1",
  "org.apache.santuario" % "xmlsec" % "1.4.3",
  "com.amazonaws" % "aws-java-sdk" % "1.11.98"
)

