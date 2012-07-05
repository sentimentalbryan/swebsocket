
import uk.co.binarytemple.sws.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class LocalhostMain {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			// URI url = new URI("ws://localhost:8080/websocket");
			URI url = new URI("ws://127.0.0.1:8080/websocket");
			final Map<String, String> headers = new HashMap<String, String>();
			// headers.put("Authorization",
			// "xxxxxxxxxxx:xxxxxxxxxxxxxxxxxxxx");
			final WebSocket websocket = new WebSocket(url, headers);
			// Register Event Handlers
			websocket.setEventHandler(new WebSocketEventHandler() {
				public void onOpen() {
					System.out.println("--open sending...");
					try {
						String msg = "{\"action\":\"subscribe\" , \"hash\":\"1b9d781ba676975ad93515d5677e259c\"}";
						System.err.println(msg);
						websocket.send(msg);
					} catch (WebSocketException e) {
						e.printStackTrace();
					}
					System.out.println("--open");
				}

				public void onMessage(WebSocketMessage message) {
					System.out.println("--received message: "
							+ message.getText());
				}

				public void onClose() {
					System.out.println("--close");
				}

				@Override
				public void onError(Throwable t) {
					System.out.println("--onError");
				}

				@Override
				public void onPing() {
					System.out.println("--onPing");
				}

				@Override
				public void onPong() {
					System.out.println("--onPong");
				}
			});
			// Establish WebSocket Connection
			websocket.connect();
			// Send UTF-8 Text
			websocket.send("hello world");
			// Close WebSocket Connection
			websocket.close();
		} catch (WebSocketException wse) {
			wse.printStackTrace();
		} catch (URISyntaxException use) {
			use.printStackTrace();
		}
	}
}
