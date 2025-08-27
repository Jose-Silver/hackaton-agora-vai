package emprestimos.v1.resource.profile;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class RateLimitingEnabledProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "emprestimos.rate-limit.enabled", "true"
        );
    }
}

