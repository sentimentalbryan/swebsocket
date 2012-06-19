package de.rbaier.weberknecht

import java.net.URI
import de.roderick.weberknecht.SWebsocket
import de.roderick.weberknecht.WebSocketMessage
import de.roderick.weberknecht.WebSocketEventHandler
import java.util.concurrent.CountDownLatch

object LocalhostSWebsocketMain {

  def main(args: Array[String]): Unit = {

    val uri = new URI("ws://127.0.0.1:8080/websocket");
    val headers = Map("Authorization" -> "xxxxxxxxxxx:xxxxxxxxxxxxxxxxxxxx")
    val openHandle = () => { println("opened") }
    val mReg = """.*msg_([0-9]*).*""" r
    val latch1 = new CountDownLatch(3)
    val latch2 = new CountDownLatch(3)
    
    val msgHandle = (s: String) => {
      println(s)
      s match {
        case mReg(id) => id.toInt match {
          case 1 => latch1.countDown()
          case 2 => latch2.countDown()
        }
        case _ => throw new RuntimeException("bad message")
      }
    }

    val handler = new WebSocketEventHandler() {
      def onOpen() = {
        println("onOpen")
        openHandle
      }
      def onMessage(message: WebSocketMessage) = {
        msgHandle(message.getText())
      }
      def onClose() = {
        System.out.println("--close")
      }

      def onError(t: Throwable) = {
        System.out.println("--onError" + t)
      }

      def onPing() = {
        System.out.println("--onPing")
      }

      def onPong() = {
        System.out.println("--onPong");
      }
    }

    val ws = SWebsocket.create(headers, uri, Some(handler))
    ws.connect();
    Range(1, 3).foreach(_ => ws.send("msg_1"))
    latch1.await()
    ws.close()
    ws.connect()
    Range(1, 3).foreach(_ => ws.send("msg_2"))
    latch2.await()
    ws.close()
  }
}  