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

class SWebsocket(val w: WebSocket) {

  var openHandle: Function0[Unit] = () => { println("default-- open") }
  var closeHandle: Function0[Unit] = () => { println("default-- close") }
  var errorHandle: Function1[Throwable, Unit] = (t: Throwable) => {
    println(format("%s:%s", "default-- onError", t.toString()))
    println();
  }
  var msgHandle: Function1[WebSocketMessage, Unit] = (message: WebSocketMessage) => { println("default-- received message: " + message.getText()) }

  private val defaulthandle = new WebSocketEventHandler() {
    def onOpen() = {
      openHandle()
    }
    def onMessage(message: WebSocketMessage) = {
      msgHandle(message)
    }
    def onClose() = {
      closeHandle()
    }
    def onError(t: Throwable) = {
      errorHandle(t)
    }
    def onPing() = {
      System.err.println("default-- onPing")
    }
    def onPong() = {
      System.err.println("default-- onPong")
    }
  }

  def addErrorHandler(handler: Function1[Throwable, Unit]): SWebsocket = { this.errorHandle = handler; this }
  def addOpenHandler(handler: Function0[Unit]): SWebsocket = { this.openHandle = handler; this }
  def addCloseHandler(handler: Function0[Unit]): SWebsocket = { this.closeHandle = handler; this }
  def addMessageHandler(handler: Function1[WebSocketMessage, Unit]): SWebsocket = { this.msgHandle = handler; this }

  def connect() = {
    try {
      w.setEventHandler(defaulthandle)
      w.connect()
    } catch {
      case e => { defaulthandle.onError(e) }
    }
  }
  def close() = {
    w.close()
  }
  def send(s: String) = {
    try {
      w.send(s)
    } catch {
      case e => defaulthandle.onError(e)
    }
  }
}

object SWebsocket {
  def create(headers: Map[String, String], url: URI): SWebsocket = {
    val websocket = new WebSocket(url, headers);
    new SWebsocket(websocket)
  }

}