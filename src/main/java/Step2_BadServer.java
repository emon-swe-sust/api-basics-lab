import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * STEP 2 - A BADLY WRITTEN HTTP API  (port 8080)
 *
 * The mistake: it answers "200 OK" for EVERYTHING, success or failure.
 * The caller has to dig into the body to find out what really happened.
 *
 * Run:  java -cp out Step2_BadServer
 */
public class Step2_BadServer {
    static final RoomService service = new RoomService();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/reserve", Step2_BadServer::handle);
        server.start();
        System.out.println("LISTENING 8080  (bad server: always 200)");
    }

    static void handle(HttpExchange ex) throws IOException {
        Map<String, String> q = query(ex.getRequestURI().getRawQuery());
        try {
            Booking b = service.reserve(q.get("ucid"), q.get("room"));
            send(ex, 200, "{\"ok\":true,\"id\":" + Json.quote(b.id) + ",\"msg\":null}");
        } catch (RuntimeException e) {
            // BAD: a failure, but the status line still says 200 OK
            send(ex, 200, "{\"ok\":false,\"id\":null,\"msg\":\"booking refused\"}");
        }
    }

    static Map<String, String> query(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null) return m;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) m.put(kv[0], kv[1]);
        }
        return m;
    }

    static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
