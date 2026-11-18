import java.util.Set;

/**
 * The "business logic". It knows nothing about HTTP.
 * Every step below calls reserve(), either directly (Step 1)
 * or through a web server (Steps 2 and 3).
 */
public class RoomService {
    private final Set<String> rooms = Set.of("MS-101", "MS-102");
    private int next = 1;

    /** Returns a Booking, or throws IllegalArgumentException if the room doesn't exist. */
    public synchronized Booking reserve(String ucid, String room) {
        if (!rooms.contains(room)) {
            throw new IllegalArgumentException("no bookable room " + room);
        }
        return new Booking("R-" + next++, room, "confirmed");
    }
}
