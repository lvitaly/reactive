package reactive
package web
package demo
package snippet


import net.liftweb.util.Helpers._



class CellDemo extends PageSnippet {
  // Display the number of elapsed seconds, up to 60
  val signal: Signal[String] =
    new Timer(1000, until = _ > 1000 * 60).hold(0L).map(n => (n / 1000).toString)

  def render =
    "#cell" #> Cell {
      signal map {s =>
        "#time" #> s
      }
    }
}
