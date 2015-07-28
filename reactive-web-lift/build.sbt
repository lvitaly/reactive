name := "reactive-web-lift"

description := "Lift bindings for reactive-web"

{
  val liftVersion = "2.6.2"
  libraryDependencies ++= Seq(
    "net.liftweb" %% "lift-webkit" % liftVersion exclude("ch.qos.logback","logback-classic")
  )
}
