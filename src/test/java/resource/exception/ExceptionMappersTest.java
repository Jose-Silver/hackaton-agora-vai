package resource.exception;

import domain.enums.MensagemErro;
import domain.exception.ProdutoException;
import domain.exception.SimulacaoException;
import domain.exception.ParametroInvalidoException;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@DisplayName("Testes de Integração - Sistema de Tratamento de Exceções")
class ExceptionMappersTest {

    @Test
    @DisplayName("Deve retornar erro 400 quando simulação tiver valor inválido")
    void deveRetornarErro400ParaValorInvalido() {
        String simulacaoInvalida = """
            {
                "valorDesejado": -1000,
                "prazo": 12
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(simulacaoInvalida)
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("codigo", notNullValue())
            .body("mensagem", notNullValue())
            .body("timestamp", notNullValue())
            .body("path", equalTo("/v1/simulacoes"));
    }

    @Test
    @DisplayName("Deve retornar erro 400 para parâmetros obrigatórios ausentes")
    void deveRetornarErro400ParaParametrosObrigatoriosAusentes() {
        String simulacaoIncompleta = """
            {
                "prazo": 12
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(simulacaoIncompleta)
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("codigo", notNullValue())
            .body("mensagem", notNullValue())
            .body("erros", notNullValue())
            .body("erros", hasSize(greaterThan(0)));
    }

    @Test
    @DisplayName("Deve retornar erro 404 quando produto não for encontrado")
    void deveRetornarErro404ParaProdutoNaoEncontrado() {
        given()
            .queryParam("produtoId", 99999)
            .queryParam("data", "2025-08-21")
        .when()
            .get("/v1/simulacoes/por-produto-dia")
        .then()
            .statusCode(404)
            .contentType(ContentType.JSON)
            .body("codigo", equalTo("PRODUCT_NOT_FOUND"))
            .body("mensagem", containsString("Produto não encontrado"))
            .body("detalhe", containsString("99999"));
    }

    @Test
    @DisplayName("Deve retornar estrutura de erro padronizada RFC 7807")
    void deveRetornarEstruturaErroPadronizada() {
        String simulacaoInvalida = """
            {
                "valorDesejado": 0,
                "prazo": 0
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(simulacaoInvalida)
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            // Valida estrutura RFC 7807
            .body("codigo", notNullValue())
            .body("mensagem", notNullValue())
            .body("status", equalTo(400))
            .body("timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"))
            .body("path", equalTo("/v1/simulacoes"));
    }

    @Test
    @DisplayName("Deve retornar erro 400 com detalhes de validação Bean Validation")
    void deveRetornarErroValidacaoBeanValidation() {
        String simulacaoComCamposInvalidos = """
            {
                "valorDesejado": null,
                "prazo": null
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(simulacaoComCamposInvalidos)
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("codigo", equalTo("VALIDATION_ERROR"))
            .body("erros", notNullValue())
            .body("erros", hasSize(greaterThan(0)))
            .body("erros[0].campo", notNullValue())
            .body("erros[0].mensagem", notNullValue());
    }


}
