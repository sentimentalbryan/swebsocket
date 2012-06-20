package uk.co.binarytemple.swebsocket

import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.Date
import java.io.File
import scala.Option.option2Iterable
import scala.xml.Elem
import scala.xml.NodeBuffer
import scala.xml.XML
import org.junit.runner.RunWith
import org.specs.runner.JUnit
import org.specs.Specification
import org.specs.SpecificationWithJUnit
import org.specs.runner.JUnitSuiteRunner
import uk.co.binarytemple.sws.WebSocketMessage
import uk.co.binarytemple.sws.SWebsocket

/**
 * Specs test of JSON parsing functionality
 */

@RunWith(classOf[JUnitSuiteRunner])
class LocalhostIntegrationTest extends Specification with JUnit {

  "should be able to " should {
    "parse a query add instruction correctly" in {

      val uri = new URI("ws://127.0.0.1:8080/websocket");
      val headers = Map("Authorization" -> "xxxxxxxxxxx:xxxxxxxxxxxxxxxxxxxx")

      val openHandle: Function0[Unit] = () => { println("opened") }
      val closeHandle: Function0[Unit] = () => { println("closed") }
      val failHandle: Function1[Throwable, Nothing] = (t: Throwable) => {
        fail(t.toString())
      }

      val mReg = """.*msg_([0-9]*).*""" r
      val latch1 = new CountDownLatch(3)
      val latch2 = new CountDownLatch(3)

      val msgHandle: Function1[WebSocketMessage, Unit] = (w: WebSocketMessage) => {
        println(w.getText())
        w.getText() match {
          case mReg(id) => id.toInt match {
            case 1 => latch1.countDown()
            case 2 => latch2.countDown()
          }
          case _ => throw new RuntimeException("bad message")
        }
      }

      val ws = SWebsocket.create(headers, uri).addErrorHandler(failHandle).addOpenHandler(openHandle).addCloseHandler(closeHandle).addMessageHandler(msgHandle)
      

      for (i <- Range(0, 100)) {
        val l = new CountDownLatch(3)
        val msgHandle: Function1[WebSocketMessage, Unit] = (w: WebSocketMessage) => {l.countDown()}
        val nws = ws.addMessageHandler(msgHandle) 
        nws.connect()
        Range(1, 30).foreach(_ => ws.send("test"))
        l.await()
        ws.close()
      }

//      Range(1, 3).foreach(_ => ws.send("msg_1"))
//      latch1.await()
//      ws.close()
//      ws.connect()
//      Range(1, 3).foreach(_ => ws.send("msg_2"))
//      latch2.await()
//      ws.close()
      true must_== true
    }
  }
}

object LocalhostIntegrationTest {
  def main(args: Array[String]) {
    new LocalhostIntegrationTest().main(args)
  }
}
  
  
  
