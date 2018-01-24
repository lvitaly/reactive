name := "reactive-web-lift"

description := "Lift bindings for reactive-web"

libraryDependencies += "net.liftweb" %% "lift-webkit" % "3.0.2" exclude("ch.qos.logback","logback-classic")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % "test"
