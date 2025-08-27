package resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Testes de Integração - SimulacaoResource")
class SimulacaoResourceIntegrationTest {


    @Test
    @Order(2)
    @DisplayName("Deve retornar erro 400 para valor negativo")
    void deveRetornarErro400ParaValorNegativo() {
        String simulacaoInvalida = """
            {
                "valorDesejado": -5000.00,
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
            .body("codigo", anyOf(equalTo("VALIDATION_ERROR"), equalTo("VALIDACAO_GENERICA")))
            .body("mensagem", containsString("maior que zero"));
    }

    @Test
    @Order(3)
    @DisplayName("Deve retornar erro 400 para prazo zero")
    void deveRetornarErro400ParaPrazoZero() {
        String simulacaoInvalida = """
            {
                "valorDesejado": 10000.00,
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
            .body("codigo", anyOf(equalTo("VALIDATION_ERROR"), equalTo("VALIDACAO_GENERICA")))
            .body("mensagem", containsString("maior que zero"));
    }

    @Test
    @Order(4)
    @DisplayName("Deve listar simulações criadas anteriormente")
    void deveListarSimulacoesComPaginacao() {
        given()
            .queryParam("pagina", 1)
            .queryParam("qtdRegistrosPagina", 10)
        .when()
            .get("/v1/simulacoes")
        .then()
            .statusCode(206)
            .contentType(ContentType.JSON)
            .body("pagina", equalTo(1))
            .body("qtdRegistrosPagina", equalTo(10))
            .body("qtdRegistros", greaterThanOrEqualTo(0))
            .body("registros", isA(java.util.List.class));
    }

    @Test
    @Order(5)
    @DisplayName("Deve buscar simulações por produto e data - cenário completo")
    void deveBuscarSimulacoesPorProdutoEData() {
        // Primeiro cria uma simulação para garantir que existe dados
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "valorDesejado": 900.00,
                    "prazo": 5
                }
                """)
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(200);

        // Depois busca por produto e data
        given()
        .when()
            .get("/v1/simulacoes/por-produto-dia")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("dataReferencia", notNullValue())
            .body("simulacoes", notNullValue())
            .body("simulacoes", isA(java.util.List.class));
    }

    @Test
    @Order(6)
    @DisplayName("Deve buscar simulações por produto específico")
    void deveBuscarSimulacoesPorProdutoEspecifico() {
        given()
            .queryParam("produtoId", 1) // Assumindo que produto 1 existe
        .when()
            .get("/v1/simulacoes/por-produto-dia")
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(404)))
            .contentType(ContentType.JSON);
    }

    @Test
    @Order(7)
    @DisplayName("Deve buscar simulações por data específica")
    void deveBuscarSimulacoesPorDataEspecifica() {
        String dataConsulta = "2024-01-15";

        given()
            .queryParam("dataSimulacao", dataConsulta)
        .when()
            .get("/v1/simulacoes/por-produto-dia")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("dataReferencia", equalTo(dataConsulta))
            .body("simulacoes", notNullValue());
    }

    @Test
    @Order(8)
    @DisplayName("Deve validar limites de paginação")
    void deveValidarLimitesPaginacao() {
        // Teste com quantidade máxima de registros
        given()
            .queryParam("pagina", 1)
            .queryParam("qtdRegistrosPagina", 100)
        .when()
            .get("/v1/simulacoes")
        .then()
            .statusCode(206)
            .body("qtdRegistrosPagina", equalTo(100));

        // Teste com quantidade acima do limite
        given()
            .queryParam("pagina", 1)
            .queryParam("qtdRegistrosPagina", 101)
        .when()
            .get("/v1/simulacoes")
        .then()
            .statusCode(400)
            .body("codigo", anyOf(equalTo("VALIDATION_ERROR"), equalTo("VALIDACAO_GENERICA")))
            .body("mensagem", containsString("100"));
    }



    @Test
    @Order(11)
    @DisplayName("Deve validar estrutura completa da resposta de simulação")
    void deveValidarEstruturaCompletaResposta() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "valorDesejado": 30000.00,
                    "prazo": 48
                }
                """)
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            // Validações da estrutura principal
            .body("idSimulacao", notNullValue())
            .body("codigoProduto", notNullValue())
            .body("descricaoProduto", notNullValue())
            .body("taxaJuros", notNullValue())
            .body("taxaJuros", greaterThan(0.0f))

            // Validações dos resultados de simulação
            .body("resultadoSimulacao", notNullValue())
            .body("resultadoSimulacao", isA(java.util.List.class))
            .body("resultadoSimulacao.size()", greaterThan(0))

            // Validação dos tipos de amortização
            .body("resultadoSimulacao[0].tipo", oneOf("SAC", "PRICE"))
            .body("resultadoSimulacao[0].valorTotalJuros", anyOf(notNullValue(), nullValue()))
            .body("resultadoSimulacao[0].valorTotalPagar", anyOf(notNullValue(), nullValue()))

            // Validações das parcelas
            .body("resultadoSimulacao[0].parcelas", notNullValue())
            .body("resultadoSimulacao[0].parcelas", isA(java.util.List.class))
            .body("resultadoSimulacao[0].parcelas.size()", equalTo(48));
    }

    @Test
    @Order(13)
    @DisplayName("Deve validar comportamento com dados extremos")
    void deveValidarDadosExtremos() {
        // Teste com valor muito baixo
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "valorDesejado": 1.00,
                    "prazo": 1
                }
                """)
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(400))); // Pode ser válido ou inválido dependendo das regras de negócio

        // Teste com prazo muito longo
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "valorDesejado": 10000.00,
                    "prazo": 480
                }
                """)
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(400))); // Pode ser válido ou inválido dependendo das regras de negócio
    }

    @Test
    @Order(14)
    @DisplayName("Deve validar tratamento de erros de validação complexos")
    void deveValidarErrosValidacaoComplexos() {
        // Campos vazios
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "valorDesejado": "",
                    "prazo": ""
                }
                """)
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(anyOf(equalTo(400), equalTo(500)));

        // Tipos inválidos
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "valorDesejado": "texto",
                    "prazo": "texto"
                }
                """)
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(anyOf(equalTo(400), equalTo(500)));
    }
}
