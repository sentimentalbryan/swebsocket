/*
 *  Copyright (C) 2012 Sentiment Metrics Ltd
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package de.roderick.weberknecht


import java.net.URI
import java.net.URISyntaxException
import scala.collection.JavaConversions._
import scala.actors.Actor
import scala.actors.Actor._

class SWebsocket(val w: WebSocket) extends Actor {
  
  def handler() = {
    w.getEventHandler()
  }
  
  def act() = {
  }

  def connect() = {
    try {
      w.connect()
    } catch {
      case e => { handler().onError(e) }
    }
  }
  def close() = {
    w.close()
  }
  def send(s: String) = {
    try {
      w.send(s)
    } catch {
      case e => handler().onError(e)
    }
  }
}

object SWebsocket {
  def create(headers: Map[String, String], url: URI, handler: Option[WebSocketEventHandler]): SWebsocket = {
    val websocket = new WebSocket(url, headers);
    handler match {
      case Some(handler) => websocket.setEventHandler(handler);
      case None => websocket.setEventHandler(defaulthandle)
    }
    new SWebsocket(websocket)
  }
  private val defaulthandle = new WebSocketEventHandler() {
    def onOpen() = {
      System.out.println("--open");
    }
    def onMessage(message: WebSocketMessage) = {
      System.out.println("--received message: " + message.getText())
    }
    def onClose() = {
      System.out.println("--close")
    }
    def onError(t: Throwable) = {
      System.out.println("--onError")
    }
    def onPing() = {
      System.out.println("--onPing")
    }
    def onPong() = {
      System.out.println("--onPong")
    }
  }

}