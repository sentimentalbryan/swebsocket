/*
 *  
 *  Copyright (C) 2012 Roderick Baier
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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocket {
	// TODO: Use in auth...
	// private static final String GUID =
	// "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	public static final int VERSION = 13;

	Logger logger = LoggerFactory.getLogger(WebSocket.class);

	static final byte OPCODE_TEXT = 0x1;
	static final byte OPCODE_BINARY = 0x2;
	static final byte OPCODE_CLOSE = 0x8;
	static final byte OPCODE_PING = 0x9;
	static final byte OPCODE_PONG = 0xA;

	private URI url = null;
	private WebSocketEventHandler eventHandler = null;

	private volatile boolean connected = false;

	private Socket socket = null;
	private DataInputStream input = null;
	private PrintStream output = null;

	private WebSocketReceiver receiver = null;
	private WebSocketHandshake handshake = null;

	private String header = "";

	public WebSocket(URI url) throws WebSocketException {
		this(url, VERSION, new HashMap<String, String>());
	}

	public WebSocket(URI url, int protocol) throws WebSocketException {
		this.url = url;
		handshake = new WebSocketHandshake(url, protocol, null,
				new HashMap<String, String>());
	}

	public WebSocket(URI url, Map<String, String> headers)
			throws WebSocketException {
		this.url = url;
		handshake = new WebSocketHandshake(url, VERSION, null, headers);
	}

	public WebSocket(URI url, int protocol, Map<String, String> headers)
			throws WebSocketException {
		this.url = url;
		handshake = new WebSocketHandshake(url, protocol, null, headers);
	}

	public boolean isConnected() {
		return connected;
	}

	public void setEventHandler(WebSocketEventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

	public WebSocketEventHandler getEventHandler() {
		return this.eventHandler;
	}

	public void connect() throws WebSocketException {

		synchronized (this) {
			try {
				if (connected) {
					logger.warn("attempting to connect - while already connected");
					return;
				}

				socket = createSocket();
				input = new DataInputStream(socket.getInputStream());
				output = new PrintStream(socket.getOutputStream());

				output.write(handshake.getHandshake());

				boolean handshakeComplete = false;
				int len = 1000;
				byte[] buffer = new byte[len];
				int pos = 0;
				ArrayList<String> handshakeLines = new ArrayList<String>();

				while (!handshakeComplete) {
					int b = input.read();
					buffer[pos] = (byte) b;
					pos += 1;

					if (buffer[pos - 1] == 0x0A && buffer[pos - 2] == 0x0D) {
						String line = new String(buffer, "UTF-8");
						if (line.trim().equals("")) {
							handshakeComplete = true;
						} else {
							handshakeLines.add(line.trim());
						}

						buffer = new byte[len];
						pos = 0;
					}
				}

				for (String line : handshakeLines) {
					logger.debug(line);
					
				}
				handshake.verifyServerStatusLine(handshakeLines.get(0));
				handshakeLines.remove(0);

				HashMap<String, String> headers = new HashMap<String, String>();
				for (String line : handshakeLines) {
					String[] keyValue = line.split(": ", 2);
					headers.put(keyValue[0], keyValue[1]);
				}
				handshake.verifyServerHandshakeHeaders(headers);

				receiver = new WebSocketReceiver(input, this);
				receiver.start();
				connected = true;
				eventHandler.onOpen();
			} catch (IOException ioe) {
				throw new WebSocketException("error while connecting: "
						+ ioe.getMessage(), ioe);
			}
		}
	}

	public synchronized void send(String data) throws WebSocketException {
		if (!connected) {
			throw new NotConnectedException(
					"error while sending text data: not connected");
		}
		try {
			// This code didn't work, took code from Datasift instead.
			// this.send_frame((byte) 0x00, false, data.getBytes(("UTF-8")));
			output.write(0x00);
			output.write(data.getBytes(("UTF-8")));
			output.write(0xff);
			output.flush();
		} catch (IOException e) {
			throw new WebSocketIOException(e);
		}
	}

	private synchronized void send_frame(byte opcode, boolean masking,
			byte[] data) throws WebSocketException, IOException {
		ByteBuffer frame = FrameUtil.generateFrame(opcode, masking, data);
		output.write(frame.array());
		output.flush();
	}

	public void handleReceiverError() {
		try {
			if (connected) {
				close();
			}
		} catch (WebSocketException wse) {
			logger.error("receiver error", wse);
		}
	}

	public void close() throws WebSocketException {
		synchronized (this) {
			if (!connected) {
				return;
			}

			sendCloseHandshake();

			if (receiver.isRunning()) {
				receiver.stopit();
			}

			closeStreams();

			eventHandler.onClose();
		}
	}

	private synchronized void sendCloseHandshake() throws WebSocketException {
		logger.debug("Sending close");
		if (!connected) {
			throw new WebSocketException(
					"error while sending close handshake: not connected");
		}

		if (!connected) {
			throw new WebSocketException("error while sending close");
		}

		try {
			this.send_frame(OPCODE_CLOSE, false, new byte[0]);
		} catch (IOException e) {
			logger.error("error closing", e);
		}

		connected = false;
	}

	private Socket createSocket() throws WebSocketException {
		String scheme = url.getScheme();
		String host = url.getHost();
		int port = url.getPort();

		Socket socket = null;

		if (scheme != null && scheme.equals("ws")) {
			if (port == -1) {
				port = 80;
			}
			try {
				socket = new Socket(host, port);
			} catch (UnknownHostException uhe) {
				throw new WebSocketException("unknown host: " + host, uhe);
			} catch (IOException ioe) {
				throw new WebSocketException("error while creating socket to "
						+ url, ioe);
			}
		} else if (scheme != null && scheme.equals("wss")) {
			if (port == -1) {
				port = 443;
			}
			try {
				SocketFactory factory = SSLSocketFactory.getDefault();
				socket = factory.createSocket(host, port);
			} catch (UnknownHostException uhe) {
				throw new WebSocketException("unknown host: " + host, uhe);
			} catch (IOException ioe) {
				throw new WebSocketException(
						"error while creating secure socket to " + url, ioe);
			}
		} else {
			throw new WebSocketException("unsupported protocol: " + scheme);
		}

		return socket;
	}

	private void closeStreams() throws WebSocketException {
		try {
			input.close();
			output.close();
			socket.close();
		} catch (IOException ioe) {
			throw new WebSocketException(
					"error while closing websocket connection: ", ioe);
		}
	}
}
