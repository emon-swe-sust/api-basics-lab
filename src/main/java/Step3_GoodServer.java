import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * STEP 3 - A WELL WRITTEN HTTP API  (port 8081)
 *
 *   POST /v1/reservations        create a booking (id field is "reservationId")
 *                                the details travel in the BODY: {"ucid":"..","room":".."}
 *   POST /v2/reservations        same, but the id field is renamed to "id"
 *   GET  /v1/reservations/{id}   read a booking back (v2 works too)
 *
 * What makes it "good":
 *   1. The STATUS CODE tells the result:
 *        201 created | 200 ok | 400 your request is wrong
 *        404 not found | 405 wrong HTTP method
 *   2. The BODY (JSON) carries the details: a "code" and a human "message".
 *   3. The VERSION is in the path, so old callers (/v1) keep working.
 *
 * Run:  java -cp out Step3_GoodServer
 */
public class Step3_GoodServer {
    static final RoomService service = new RoomService();
    static final Map<String, Booking> byId = new HashMap<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        // Same shape as Step2_BadServer: each path is bound to a handler METHOD.
        server.createContext("/v1", Step3_GoodServer::handleV1);
        server.createContext("/v2", Step3_GoodServer::handleV2);
        server.start();
        System.out.println("LISTENING 8081  (good server: honest status codes)");
    }

    static void handleV1(HttpExchange ex) throws IOException { 
        handle(ex, false); 
    }

    static void handleV2(HttpExchange ex) throws IOException { handle(ex, true); }

    /** Like Step2_BadServer.handle, but it looks at the path and answers honestly. */
    static synchronized void handle(HttpExchange ex, boolean v2) throws IOException {
        String method = ex.getRequestMethod();
        String[] parts = ex.getRequestURI().getPath().split("/");   // ["", "v1", "reservations", "R-1"]

        boolean collection = parts.length == 3 && parts[2].equals("reservations");   // /v1/reservations
        boolean one = parts.length == 4 && parts[2].equals("reservations");          // /v1/reservations/R-1

        if (collection && method.equals("POST")) {
            create(ex, v2);              // POST /v1/reservations        -> make a booking
        } else if (one && method.equals("GET")) {
            read(ex, parts[3], v2);      // GET  /v1/reservations/{id}   -> read one back
        } else if (collection || one) {
            send(ex, 405, error("method_not_allowed", method + " is not supported here"));
        } else {
            send(ex, 404, error("not_found", "no such endpoint"));
        }
    }

    static void create(HttpExchange ex, boolean v2) throws IOException {
        // The details are in the request BODY (JSON), not in the URL.
        String json = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String ucid = Json.stringField(json, "ucid");
        String room = Json.stringField(json, "room");

        // 400: the request itself is wrong
        if (ucid == null || room == null) {
            send(ex, 400, error("invalid_body", "ucid and room are required"));
            return;
        }
        try {
            Booking b = service.reserve(ucid, room);
            byId.put(b.id, b);
            send(ex, 201, body(b, v2));                       // 201 Created
        } catch (IllegalArgumentException e) {
            send(ex, 400, error("unknown_room", e.getMessage()));
        }
    }

    static void read(HttpExchange ex, String id, boolean v2) throws IOException {
        Booking b = byId.get(id);
        if (b == null) {
            send(ex, 404, error("not_found", "no such reservation"));   // 404: never stored
        } else {
            send(ex, 200, body(b, v2));
        }
    }

    /** v1 calls the id "reservationId", v2 calls it "id". */
    static String body(Booking b, boolean v2) {
        String idName = v2 ? "id" : "reservationId";
        return "{" + Json.quote(idName) + ":" + Json.quote(b.id)
                + ",\"room\":" + Json.quote(b.room)
                + ",\"status\":" + Json.quote(b.status) + "}";
    }

    static String error(String code, String message) {
        return "{\"code\":" + Json.quote(code) + ",\"message\":" + Json.quote(message) + "}";
    }

    static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
