package guests.security;

import guests.AbstractTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class CachingOpaqueTokenIntrospectorTest extends AbstractTest {

    private final CachingOpaqueTokenIntrospector subject =
            new CachingOpaqueTokenIntrospector("http://localhost:8081/introspect", "rp", "secret");

    @Test
    void introspect() throws IOException {
        String token = super.opaqueAccessToken("admin@utrecht.nl", "introspect.json");
        subject.introspect(token);
        stubFor(post(urlPathMatching("/introspect")).willReturn(aResponse()
                .withStatus(403)));
        //Ensure the cache get hits
        subject.introspect(token);
        subject.cleanUp();
        Assertions.assertThrows(OAuth2IntrospectionException.class, () -> subject.introspect(token));
    }

}