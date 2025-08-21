package resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class SimulacaoResourceTest {

    @Test
    void testCriarSimulacao_Valid() {
        given()
            .contentType(ContentType.JSON)
            .body("{" +
                    "\"valorDesejado\": 10000, " +
                    "\"prazo\": 12" +
                    "}")
        .when()
            .post("/simulacoes")
        .then()
            .statusCode(200)
            .body("idSimulacao", notNullValue());
    }

    @Test
    void testCriarSimulacao_MissingFields() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/simulacoes")
        .then()
            .statusCode(400)
            .body(containsString("obrigatório"));
    }

    @Test
    void testCriarSimulacao_NegativeValues() {
        given()
            .contentType(ContentType.JSON)
            .body("{" +
                    "\"valorDesejado\": -100, " +
                    "\"prazo\": 0" +
                    "}")
        .when()
            .post("/simulacoes")
        .then()
            .statusCode(400)
            .body(anyOf(containsString("maior que zero"), containsString("obrigatório")));
    }

    @Test
    void testListarSimulacoes_InvalidPagination() {
        given()
            .queryParam("pagina", 0)
            .queryParam("qtdRegistrosPagina", -1)
        .when()
            .get("/simulacoes")
        .then()
            .statusCode(400)
            .body(anyOf(containsString("maior ou igual a 1"), containsString("maior que zero")));
    }

    @Test
    void testPorProdutoDia_InvalidDate() {
        given()
            .queryParam("data", "2025-99-99")
        .when()
            .get("/simulacoes/por-produto-dia")
        .then()
            .statusCode(400)
            .body(containsString("Data inválida"));
    }

    @Test
    void testPorProdutoDia_InvalidProdutoId() {
        given()
            .queryParam("produtoId", -5)
        .when()
            .get("/simulacoes/por-produto-dia")
        .then()
            .statusCode(400)
            .body(containsString("produtoId deve ser um número positivo"));
    }

    // Add more tests for business errors and not found as needed
}

