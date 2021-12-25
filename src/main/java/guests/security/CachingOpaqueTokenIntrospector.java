package guests.security;

import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class CachingOpaqueTokenIntrospector extends SpringOpaqueTokenIntrospector {

    private final Map<String, OAuth2AuthenticatedPrincipal> cache = new ConcurrentHashMap<>();

    public CachingOpaqueTokenIntrospector(String introspectionUri, String clientId, String clientSecret) {
        super(introspectionUri, clientId, clientSecret);
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::cleanUp, 1L, 1L, TimeUnit.HOURS);
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        return cache.computeIfAbsent(token, s -> super.introspect(token));
    }

    void cleanUp() {
        Instant now = Instant.now();
        cache.values().removeIf(principal -> ((Instant) principal.getAttribute("exp")).isBefore(now));
    }
}