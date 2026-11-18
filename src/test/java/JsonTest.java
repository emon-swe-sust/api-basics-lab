import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void quoteAddsQuotes() {
        assertThat(Json.quote("MS-101")).isEqualTo("\"MS-101\"");
    }

    @Test
    void quoteEscapesInnerQuotes() {
        assertThat(Json.quote("a\"b")).isEqualTo("\"a\\\"b\"");
    }

    @Test
    void stringFieldReadsValue() {
        String body = "{\"ucid\":\"10000001\",\"room\":\"MS-105\"}";

        assertThat(Json.stringField(body, "room")).isEqualTo("MS-101");
    }

    @Test
    void stringFieldIsNullWhenMissing() {
        assertThat(Json.stringField("{\"ucid\":\"1\"}", "room")).isNull();
    }
}
