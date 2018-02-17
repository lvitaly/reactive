import ReactiveBuild._


crossScalaVersions in ThisBuild := Seq("2.11.11", "2.12.3")

scalaVersion in ThisBuild := "2.11.11"

organization in ThisBuild := "cc.co.scala-reactive"

scalacOptions in(ThisBuild, Compile, compile) += "-deprecation"

enablePlugins(ScalaUnidocPlugin)

unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(routing, web_demo)

lazy val core = (project in file("reactive-core"))
  .settings(publishingSettings: _*)

lazy val routing = (project in file("reactive-routing"))
  .settings(publishingSettings: _*)

lazy val transport = (project in file("reactive-transport"))
  .settings(publishingSettings: _*)
  .dependsOn(core)

lazy val jsdsl = (project in file("reactive-jsdsl"))
  .settings(publishingSettings: _*)
  .dependsOn(transport)

lazy val web_base = (project in file("reactive-web"))
  .settings(publishingSettings: _*)
  .dependsOn(core, jsdsl)

lazy val web_html = (project in file("reactive-web-html"))
  .settings(publishingSettings: _*)
  .dependsOn(web_base)

lazy val web_widgets = (project in file("reactive-web-widgets"))
  .settings(publishingSettings: _*)
  .dependsOn(web_html)

lazy val web = (project in file("reactive-web-aggregated"))
  .settings(publishingSettings: _*)
  .settings(name := "reactive-web")
  .dependsOn(web_widgets)

lazy val web_lift = (project in file("reactive-web-lift"))
  .settings(publishingSettings: _*)
  .dependsOn(web_widgets, routing)

lazy val web_demo = (project in file("reactive-web-demo"))
  .settings(nonPublishingSettings: _*)
  .dependsOn(web_lift)
  .settings(
    webappPostProcess := { outDir =>
      val docs = outDir / "api"
      println("Copying to " + docs)
      IO.copyDirectory((doc in(core, Compile)).value, docs / "reactive-core")
      IO.copyDirectory((doc in(routing, Compile)).value, docs / "reactive-routing")
      IO.copyDirectory((doc in(transport, Compile)).value, docs / "reactive-transport")
      IO.copyDirectory((doc in(jsdsl, Compile)).value, docs / "reactive-jsdsl")
      IO.copyDirectory((doc in(web_base, Compile)).value, docs / "reactive-web-base")
      IO.copyDirectory((doc in(web_html, Compile)).value, docs / "reactive-web-html")
      IO.copyDirectory((doc in(web_widgets, Compile)).value, docs / "reactive-web-widgets")
      IO.copyDirectory((doc in(web, Compile)).value, docs / "reactive-web")
      IO.copyDirectory((doc in(web_lift, Compile)).value, docs / "reactive-web-lift")
      IO.copyDirectory((doc in(root, ScalaUnidoc)).value, docs / "unidoc")
    }
  )

lazy val root: Project = (project in file("."))
  .settings(nonPublishingSettings: _*)
  .aggregate(
    core,
    transport,
    jsdsl,
    web_base,
    routing,
    web_html,
    web_widgets,
    web,
    web_lift
  )
