/** The thing a booking gives back. Just data. */
public class Booking {
    public final String id;      // e.g. "R-1"
    public final String room;    // e.g. "MS-101"
    public final String status;  // e.g. "confirmed"

    public Booking(String id, String room, String status) {
        this.id = id;
        this.room = room;
        this.status = status;
    }

    @Override
    public String toString() {
        return "Booking{id=" + id + ", room=" + room + ", status=" + status + "}";
    }
}
