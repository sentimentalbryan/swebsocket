
import java.net.URI

import java.util.concurrent.CountDownLatch
import org.junit.runner.RunWith
import org.specs.runner.JUnitSuiteRunner
import org.specs.runner.JUnit
import org.specs.Specification
import uk.co.binarytemple.sws.SWebsocket
import uk.co.binarytemple.sws.WebSocketMessage

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
        //println(w.getText())
        w.getText() match {
          case mReg(id) => id.toInt match {
            case 1 => latch1.countDown()
            case 2 => latch2.countDown()
          }
          case _ => throw new RuntimeException("bad message")
        }
      }

      val sws = SWebsocket.create(headers, uri).addErrorHandler(failHandle).addOpenHandler(openHandle).addCloseHandler(closeHandle).addMessageHandler(msgHandle)

      for (i <- Range(0, 100)) {
        println("a new session")
        val l = new CountDownLatch(30)
        val msgHandle: Function1[WebSocketMessage, Unit] = (w: WebSocketMessage) => { println("received:%s".format(w.getText)); l.countDown() }
        val closeHandle: Function0[Unit] = () => {
          println("I can handle close")
          for { i <- 0l to 31 } { l.countDown }
        }

        val nws = sws.addMessageHandler(msgHandle).addCloseHandler(closeHandle)
        val failHandle: Function1[Throwable, Unit] = (t: Throwable) => {
          println("I can handle fail")
          println(t.toString)
          for { i <- 0l to 31 } { l.countDown }
          Thread.sleep(1000)
          nws.connect

        }

        nws.addErrorHandler(failHandle)
        nws.connect()
        println("nws.isConnected = %b".format(nws.isConnected))
        Range(1, 30).foreach(_ => sws.send("test"))
        l.await()
        sws.close()
      }
      true must_== true
    }
  }
}

object LocalhostIntegrationTest {
  def main(args: Array[String]) {
    new LocalhostIntegrationTest().main(args)
  }
}
  
  
  
