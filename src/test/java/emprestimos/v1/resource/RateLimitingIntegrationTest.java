package emprestimos.v1.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class RateLimitingIntegrationTest {

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
        RestAssured.basePath = "/emprestimos";
    }

    @Test
    void testRateLimitingOnListarSimulacoes() {
        String endpoint = "/v1/simulacoes";
        
        // Faz 50 requisições (limite configurado para listar simulações)
        for (int i = 1; i <= 50; i++) {
            given()
                .header("X-Forwarded-For", "192.168.1.100") // Simula mesmo IP
                .when()
                .get(endpoint)
                .then()
                .statusCode(anyOf(is(206), is(400))) // 206 se há dados, 400 se não há simulações ainda
                .header("X-RateLimit-Limit", "50")
                .header("X-RateLimit-Remaining", String.valueOf(50 - i))
                .header("X-RateLimit-Reset", notNullValue());
        }

        // 51ª requisição deve ser bloqueada com 429
        given()
            .header("X-Forwarded-For", "192.168.1.100")
            .when()
            .get(endpoint)
            .then()
            .statusCode(429)
            .header("X-RateLimit-Limit", "50")
            .header("X-RateLimit-Remaining", "0")
            .header("Retry-After", notNullValue())
            .body("error", equalTo("Rate limit exceeded"))
            .body("message", equalTo("Too many requests. Please try again later."))
            .body("retryAfter", notNullValue());
    }

    @Test
    void testRateLimitingWithDifferentIPs() {
        String endpoint = "/v1/simulacoes";
        
        // IP 1 faz 50 requisições
        for (int i = 1; i <= 50; i++) {
            given()
                .header("X-Forwarded-For", "192.168.1.101")
                .when()
                .get(endpoint)
                .then()
                .statusCode(anyOf(is(206), is(400)));
        }
        
        // IP 1 - 51ª requisição bloqueada
        given()
            .header("X-Forwarded-For", "192.168.1.101")
            .when()
            .get(endpoint)
            .then()
            .statusCode(429);
        
        // IP 2 - primeira requisição ainda permitida
        given()
            .header("X-Forwarded-For", "192.168.1.102")
            .when()
            .get(endpoint)
            .then()
            .statusCode(anyOf(is(206), is(400)))
            .header("X-RateLimit-Remaining", "49");
    }

    @Test
    void testRateLimitingOnBuscarPorProdutoEData() {
        String endpoint = "/v1/simulacoes/por-produto-dia";
        
        // Faz 30 requisições (limite configurado)
        for (int i = 1; i <= 30; i++) {
            given()
                .header("X-Forwarded-For", "192.168.1.103")
                .when()
                .get(endpoint)
                .then()
                .statusCode(anyOf(is(200), is(400))) // 200 ou 400 dependendo dos dados
                .header("X-RateLimit-Limit", "30");
        }

        // 31ª requisição deve ser bloqueada
        given()
            .header("X-Forwarded-For", "192.168.1.103")
            .when()
            .get(endpoint)
            .then()
            .statusCode(429)
            .header("X-RateLimit-Limit", "30")
            .header("X-RateLimit-Remaining", "0");
    }

    @Test
    void testRateLimitingHeaders() {
        String endpoint = "/v1/simulacoes";
        
        // Primeira requisição deve incluir headers de rate limit
        given()
            .header("X-Forwarded-For", "192.168.1.104")
            .when()
            .get(endpoint)
            .then()
            .statusCode(anyOf(is(206), is(400)))
            .header("X-RateLimit-Limit", "50")
            .header("X-RateLimit-Remaining", "49")
            .header("X-RateLimit-Reset", notNullValue());
    }

    @Test
    void testRateLimitingWithRealIP() {
        String endpoint = "/v1/simulacoes";
        
        // Teste com header X-Real-IP
        given()
            .header("X-Real-IP", "10.0.0.1")
            .when()
            .get(endpoint)
            .then()
            .statusCode(anyOf(is(206), is(400)))
            .header("X-RateLimit-Limit", "50")
            .header("X-RateLimit-Remaining", "49");
    }
}
