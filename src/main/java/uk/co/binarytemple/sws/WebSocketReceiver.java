/*
 *  Copyright (C) 2012 Roderick Baier
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

package uk.co.binarytemple.sws;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class WebSocketReceiver
		extends Thread
{
	private InputStream input = null;
	private WebSocket websocket = null;
	private WebSocketEventHandler eventHandler = null;

	private volatile boolean stop = false;

	
	public WebSocketReceiver(InputStream input, WebSocket websocket)
	{
		this.input = input;
		this.websocket = websocket;
		this.eventHandler = websocket.getEventHandler();
	}


	public void run()
	{
		boolean frameStart = false;
		List<Byte> messageBytes = new ArrayList<Byte>();

		while (!stop) {
			try {
				int b = input.read();
				if (b == 0x00) {
					frameStart = true;
				} else if ((b == 0xff) && frameStart) {
					frameStart = false;
					Byte[] message = messageBytes.toArray(new Byte[messageBytes
							.size()]);
					eventHandler.onMessage(new WebSocketMessage(message));
					messageBytes.clear();
				} else if (frameStart) {
					messageBytes.add((byte) b);
				} else if (b == -1) {
					handleError();
				}
			}
			catch (IOException ioe) {
				handleError();
			}
		}
	}
	
	
	public void stopit()
	{
		stop = true;
	}
	
	
	public boolean isRunning()
	{
		return !stop;
	}
	
	
	private void handleError()
	{
		stopit();
		websocket.handleReceiverError();
	}
}
