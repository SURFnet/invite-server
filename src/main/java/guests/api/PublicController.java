package guests.api;

import com.nimbusds.oauth2.sdk.pkce.CodeChallenge;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/guests/api/public", produces = MediaType.APPLICATION_JSON_VALUE)
public class PublicController {

    private final String authorizationUrl;
    private final String tokenUrl;
    private final String relyingParty;
    private final String redirectUri;

    public PublicController(@Value("${oidc.authorization_uri}") String authorizationUrl,
                            @Value("${oidc.token_uri}") String tokenUrl,
                            @Value("${oidc.relying_party_id}") String relyingParty,
                            @Value("${oidc.client_redirect_uri}") String redirectUri) {
        this.authorizationUrl = authorizationUrl;
        this.tokenUrl = tokenUrl;
        this.relyingParty = relyingParty;
        this.redirectUri = redirectUri;
    }

    @PostMapping(value = "/authorize")
    public ResponseEntity<Map<String, String>> authorize(@RequestBody(required = false) Map<String, String> additionalParameters) {
        CodeVerifier codeVerifier = new CodeVerifier();
        CodeChallenge codeChallenge = CodeChallenge.compute(CodeChallengeMethod.S256, codeVerifier);
        Map<String, String> parameters = new HashMap<>();

        parameters.put("response_type", "code");
        parameters.put("scope", "openid");
        parameters.put("client_id", relyingParty);

        parameters.put("redirect_uri", redirectUri);

        parameters.put("nonce", UUID.randomUUID().toString());
        if (additionalParameters != null && additionalParameters.containsKey("state")) {
            parameters.put("state", additionalParameters.get("state"));
        }
        parameters.put("code_challenge", codeChallenge.getValue());
        parameters.put("code_challenge_method", CodeChallengeMethod.S256.getValue());

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(authorizationUrl);
        parameters.forEach((key, value) -> {
            if (StringUtils.hasText(value)) {
                builder.queryParam(key, encode(value));
            }
        });

        Map<String, String> results = new HashMap<>();
        results.put("grantType", "authorization_code");
        results.put("codeChallengeMethod", CodeChallengeMethod.S256.getValue());
        results.put("redirectUri", redirectUri);
        results.put("authorizationUrl", builder.build().toUriString());
        results.put("tokenUrl", tokenUrl);
        results.put("clientId", relyingParty);
        results.put("codeVerifier", codeVerifier.getValue());

        return ResponseEntity.ok(results);
    }

    @SneakyThrows
    private String encode(String s) {
        return URLEncoder.encode(s, Charset.defaultCharset().toString());
    }

}
