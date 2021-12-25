package guests;

import org.junit.jupiter.api.Test;

class GuestsApplicationTest {

    @Test
    void main() {
        GuestsApplication.main(new String[]{"--server.port=8088"});
    }
}