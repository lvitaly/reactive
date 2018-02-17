package reactive
package web
package lift

import java.util.TimerTask

import scala.xml.NodeSeq
import net.liftweb.builtin.snippet.Comet
import net.liftweb.common._
import net.liftweb.util.Helpers._
import net.liftweb.http._
import net.liftweb.http.js.{ JsCmd, JsCmds }

import reactive.logging.HasLogger

class LiftCometTransportType(page: Page) extends TransportType with HasLogger {
  class PageComet extends CometActor {
    // Make initCometActor accessible
    override protected[web] def initCometActor(cci: CometCreationInfo): Unit = super.initCometActor(cci)

    override def lifespan = Full(60.seconds)

    def render = <span/>

    //TODO cluster output, perhaps via timer
    override def lowPriority = {
      case js: JsCmd => partialUpdate(js)
    }
  }

  val comet = new PageComet

  /**
    * - Append commands to StringBuilder
    * - Schedule flush in 10 ms
    * - Don't allow adding more items to queue during flush
    */
  class TransportBuffer {
    private val buffer = new StringBuilder

    var tt: Option[TimerTask] = None

    def add(js: String) = this.synchronized {
      buffer.append(js).append(";\n")
      if (tt.isEmpty) {
        tt = Some(_timer.schedule(20) {
          flush()
        })
      }
    }

    private def flush() = this.synchronized {
      comet ! JsCmds.Run(buffer.toString)
      buffer.clear()
      tt = None
    }

  }

  object cometTransport extends Transport {
    def currentPriority: Int = 0

    private val transportBuffer = new TransportBuffer

    queued foreach { renderable =>
      transportBuffer.add(renderable.render)
    }
  }

  override def render = super.render ++ Comet.containerForCometActor(comet)

  S.session.foreach { session =>
    val cci = CometCreationInfo("reactive.web.ReactionsComet", Full(page.id), NodeSeq.Empty, Map.empty, session)
    comet.initCometActor(cci)
    session.buildAndStoreComet(_ => Full(comet))(cci)
  }
  S.addComet(comet)
  linkTransport(cometTransport)
}