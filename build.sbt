scalaVersion in ThisBuild := "2.11.12"

organization in ThisBuild := "cc.co.scala-reactive"

scalacOptions in (ThisBuild, Compile, compile) += "-deprecation"

import sbtunidoc.Plugin._
import UnidocKeys._
unidocSettings

unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(web_demo)
