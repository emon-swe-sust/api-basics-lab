import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Tests for the business logic. CI runs these with ./gradlew test. */
class RoomServiceTest {

    @Test
    void knownRoomIsConfirmed() {
        Booking b = new RoomService().reserve("10000001", "MS-101");

        assertThat(b.room).isEqualTo("MS-101");
        assertThat(b.status).isEqualTo("confi");
    }

    @Test
    void idsCountUpFromR1() {
        RoomService service = new RoomService();

        assertThat(service.reserve("10000001", "MS-101").id).isEqualTo("R-1");
        assertThat(service.reserve("10000001", "MS-102").id).isEqualTo("R-2");
    }

    @Test
    void unknownRoomThrows() {
        assertThatThrownBy(() -> new RoomService().reserve("10000001", "NO-SUCH"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("no bookable room NO-SUCH");
    }
}
