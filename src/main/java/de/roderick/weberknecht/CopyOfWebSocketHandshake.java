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
package de.roderick.weberknecht;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;

public class CopyOfWebSocketHandshake {
	private URI url = null;
	private String origin = null;
	private int protocol = -1;
	private String nonce = null;
	private Map<String, String> headers;
	public CopyOfWebSocketHandshake(URI url, int protocol, String origin,
			Map<String, String> headers) {
		this.url = url;
		this.protocol = protocol;
		this.origin = origin;
		this.nonce = this.createNonce();
		this.headers = headers;
	}
	private String genHeaders() {
		if (headers != null && headers.keySet().size() > 0) {
			StringBuilder s = new StringBuilder();
			for (String key : headers.keySet()) {
				s.append(key);
				s.append(':');
				s.append(headers.get(key));
				s.append("\r\n");
				return s.toString();
			}
		}
		return "";
	}
	public byte[] getHandshake() {
		String path = url.getPath();
		String host = url.getHost();
		int port = url.getPort();
		
		StringBuilder sb  = new StringBuilder();
		
		origin = "http://" + host + ":" + port;
		
		sb.append("GET ").append(path).append(" HTTP/1.1\r\n");
		sb.append("Host: ").append(host).append("\r\n");
		sb.append("Upgrade: websocket\r\n");
		sb.append("Connection: Upgrade\r\n");
		sb.append("Sec-WebSocket-Key: ").append(this.nonce).append("\r\n"); 
		sb.append("Sec-WebSocket-Protocol: chat\r\n");
		sb.append(genHeaders());
		
		if (this.protocol != -1) {
			sb.append("Sec-WebSocket-Protocol: " + this.protocol + "\r\n");
		}
		if (this.origin != null) {
			sb.append("Origin: " + this.origin + "\r\n");
		}
		sb.append("\r\n");
		
		String handshake = sb.toString();
		
		byte[] handshakeBytes = new byte[handshake .getBytes().length];
		System.arraycopy(handshake.getBytes(), 0, handshakeBytes, 0,
				handshake.getBytes().length);
		return handshakeBytes;
	}
	private String createNonce() {
		byte[] nonce = new byte[16];
		for (int i = 0; i < 16; i++) {
			nonce[i] = (byte) rand(0, 255);
		}
		return Base64.encodeBase64String(nonce);
	}
	public void verifyServerStatusLine(String statusLine)
			throws WebSocketException {
		int statusCode = Integer.valueOf(statusLine.substring(9, 12));
		if (statusCode == 407) {
			throw new WebSocketException(
					"connection failed: proxy authentication not supported");
		} else if (statusCode == 404) {
			throw new WebSocketException("connection failed: 404 not found");
		} else if (statusCode != 101) {
			throw new WebSocketException(
					"connection failed: unknown status code " + statusCode);
		}
	}
	public void verifyServerHandshakeHeaders(HashMap<String, String> headers)
			throws WebSocketException {
		if (!headers.get("Upgrade").equalsIgnoreCase("websocket")) {
			throw new WebSocketException(
					"connection failed: missing header field in server handshake: Upgrade");
		} else if (!headers.get("Connection").equals("Upgrade")) {
			throw new WebSocketException(
					"connection failed: missing header field in server handshake: Connection");
		}
	}
	private int rand(int min, int max) {
		int rand = (int) (Math.random() * max + min);
		return rand;
	}
}
