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

public class WebSocketHandshake {
	private URI url = null;
	private String origin = null;
	private int protocol = -1;
	private String nonce = null;
	private Map<String, String> headers;
	public WebSocketHandshake(URI url, int protocol, String origin,
			Map<String, String> headers) {
		this.url = url;
		this.protocol = protocol;
		this.origin = origin;
		this.nonce = this.createNone();
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
		origin = "http://" + host;
		String handshake = "GET " + path + " HTTP/1.1\r\n" + "Host: " + host
				+ "\r\n" + genHeaders() + "Upgrade: websocket\r\n"
				+ "Connection: Upgrade\r\n" + "Sec-WebSocket-Key: "
				+ this.nonce + "\r\n";
		
		System.err.println(handshake);
		
		if (this.protocol != -1) {
			handshake += "Sec-WebSocket-Protocol: " + this.protocol + "\r\n";
		}
		if (this.origin != null) {
			handshake += "Origin: " + this.origin + "\r\n";
		}
		handshake += "\r\n";
		byte[] handshakeBytes = new byte[handshake.getBytes().length];
		System.arraycopy(handshake.getBytes(), 0, handshakeBytes, 0,
				handshake.getBytes().length);
		return handshakeBytes;
	}
	private String createNone() {
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
