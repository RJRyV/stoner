name := "stoner"

version := "v0.3"

//scalaVersion := "2.11.4"
scalaVersion := "2.10.4"

resolvers += "TypeSafe Repository" at "http://repo.typesafe.com/typesafe/releases"

//libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.2.1" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

//libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.2"

//libraryDependencies += "org.apache.commons" % "commons-math3" % "3.4"

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.2.0" % "provided"


//logLevel := Level.Debug

scalacOptions += "-deprecation"
