name := "reactive-routing"

description := "Type safe routing library"

scalacOptions in (Compile, doc) ++= Seq("-implicits", "-implicits-show-all")

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"