package reactive
package web
package lift

import java.util.{Date, Timer, TimerTask}
import java.util.concurrent.TimeUnit

import net.liftweb.util.ThreadGlobal
import scala.concurrent.duration.Duration
import scala.xml.{ NodeSeq, Null, UnprefixedAttribute }
import net.liftweb.common._
import net.liftweb.util.Helpers._
import net.liftweb.http._
import net.liftweb.http.js.{ JsCmd, JsCmds }

import reactive.logging.{LogLevel, HasLogger}

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

object LiftCometTransportType {
  private val contType = "reactive.web.ReactionsComet"

  private val _overrideCometHack = new scala.util.DynamicVariable(Option.empty[LiftCometTransportType#PageComet])
  private var initted = false

  private def overrideCometHack[A](comet: LiftCometTransportType#PageComet)(f: => A): A =
    if (!initted)
      sys.error("LiftCometTransport was not initialized! You must call LiftCometTransportType.init() in boot in order to use LiftCometTransport.")
    else
      _overrideCometHack.withValue(Some(comet))(f)

  /**
   * Install the reactive comet capability into Lift
   */
  def init(): Unit = {
    LiftRules.cometCreation.append {
      case CometCreationInfo(t, name, defaultXml, attributes, session)
        if _overrideCometHack.value.isDefined && t == contType =>
        val comet = _overrideCometHack.value.get
        comet.initCometActor(session, Full(t), name, defaultXml, attributes)
        comet
    }
    initted = true
  }
}

private object _timer extends Timer("PageComet daemon", true) with Logger {
  case class ExceptionRunningTask(throwable: Throwable)
  private def timerTask(block: =>Unit) = new TimerTask {
    def run =
      try
        block
      catch {
        case e: Throwable =>
          error(ExceptionRunningTask(e))
      }
  }
  def schedule(delay: Long)(p: =>Unit): TimerTask = {
    val tt = timerTask(p)
    super.schedule(tt, delay)
    tt
  }
}

object ProcessingCometTransport extends ThreadGlobal[Boolean]

class LiftCometTransportType(page: Page) extends TransportType with HasLogger {
  // Promise that comet actor will be initialized.
  val initPromise = Promise[Box[String]]()
  val initFuture = initPromise.future

  class PageComet extends CometActor {
    // Make initCometActor accessible

    override protected def around[R](f: => R) = ProcessingCometTransport.doWith(true) { super.around(f) }

    override protected[web] def initCometActor(s: LiftSession, t: Box[String], n: Box[String], x: NodeSeq, a: Map[String, String]): Unit = {
      super.initCometActor(s, t, n, x, a)
    }

    override protected def localSetup(): Unit = {
      initPromise success Full("Actor has started")
    }

    override protected def localShutdown(): Unit = {
      unlinkTransport(cometTransport)
    }

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

  LiftCometTransportType.overrideCometHack(comet) {
    // This is how we install our own comet actors in Lift
    S.withAttrs(new UnprefixedAttribute("type", LiftCometTransportType.contType, new UnprefixedAttribute("name", page.id, Null))) {
      net.liftweb.builtin.snippet.Comet.render(NodeSeq.Empty)
    }
  }

  // Ask comet actor for render after his initialization for avoid NPE
  val askRender = Future {
    initFuture onSuccess {
      case Full(_) =>
        comet !? (comet.cometRenderTimeout, AskRender) foreach {
          case AnswerRender(_, _, when, _) =>
            page.queue(s"var lift_toWatch = lift_toWatch || {}; lift_toWatch['${comet.uniqueId}'] = $when;")
        }
    }
  }
  // Waiting for comet actor answer for render lift_toWatch
  Await.result(askRender, Duration(30, TimeUnit.SECONDS))

  override def render = super.render ++ S.session.map{ session =>
    // TODO ensure that it's not already rendered
    val cometSrc =
      List(S.contextPath, LiftRules.cometPath, urlEncode(session.uniqueId), LiftRules.cometScriptName()) mkString "/"
      <script src={ S.encodeURL(cometSrc) } type="text/javascript"/>
  }.openOr{ logger.warn("Rendering "+this+" outside of session"); NodeSeq.Empty }

  linkTransport(cometTransport)
}
