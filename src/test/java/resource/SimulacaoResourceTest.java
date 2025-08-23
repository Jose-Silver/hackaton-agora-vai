package resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@DisplayName("Testes Unitários - SimulacaoResource")
public class SimulacaoResourceTest {

    // TESTES PARA CRIAÇÃO DE SIMULAÇÃO

    @Test
    @DisplayName("Deve criar simulação com dados válidos")
    void testCriarSimulacao_Valid() {
        given()
            .contentType(ContentType.JSON)
            .body("{" +
                    "\"valorDesejado\": 10000.00, " +
                    "\"prazo\": 12" +
                    "}")
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("idSimulacao", notNullValue())
            .body("codigoProduto", notNullValue())
            .body("descricaoProduto", notNullValue())
            .body("taxaJuros", notNullValue())
            .body("resultadoSimulacao", notNullValue())
            .body("resultadoSimulacao", isA(java.util.List.class))
            .body("resultadoSimulacao.size()", greaterThan(0));
    }

    @Test
    @DisplayName("Deve retornar erro 400 para campos obrigatórios ausentes")
    void testCriarSimulacao_MissingFields() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("codigo", anyOf(equalTo("VALIDATION_ERROR"), equalTo("VALIDACAO_GENERICA")))
            .body("mensagem", anyOf(
                containsString("obrigatório"),
                containsString("Valor desejado"),
                containsString("Prazo")
            ));
    }

    @Test
    @DisplayName("Deve retornar erro 400 para valores negativos ou zero")
    void testCriarSimulacao_NegativeValues() {
        given()
            .contentType(ContentType.JSON)
            .body("{" +
                    "\"valorDesejado\": -100, " +
                    "\"prazo\": 0" +
                    "}")
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("codigo", anyOf(equalTo("VALIDATION_ERROR"), equalTo("VALIDACAO_GENERICA")))
            .body("mensagem", anyOf(
                containsString("maior que zero"),
                containsString("positivo")
            ));
    }

    @Test
    @DisplayName("Deve retornar erro 400 para valores nulos")
    void testCriarSimulacao_NullValues() {
        given()
            .contentType(ContentType.JSON)
            .body("{" +
                    "\"valorDesejado\": null, " +
                    "\"prazo\": null" +
                    "}")
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("Deve retornar erro para JSON malformado")
    void testCriarSimulacao_InvalidJson() {
        given()
            .contentType(ContentType.JSON)
            .body("{ invalid json }")
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(anyOf(equalTo(400), equalTo(500))); // Pode ser 400 ou 500 dependendo do parser
    }

    // TESTES PARA LISTAGEM DE SIMULAÇÕES

    @Test
    @DisplayName("Deve listar simulações com paginação padrão")
    void testListarSimulacoes_Default() {
        given()
        .when()
            .get("/v1/simulacoes")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("pagina", equalTo(1))
            .body("qtdRegistrosPagina", equalTo(10))
            .body("qtdRegistros", greaterThanOrEqualTo(0)) // Fixed: removed L suffix for Integer
            .body("registros", notNullValue())
            .body("registros", isA(java.util.List.class));
    }

    @Test
    @DisplayName("Deve listar simulações com paginação customizada")
    void testListarSimulacoes_CustomPagination() {
        given()
            .queryParam("pagina", 1)
            .queryParam("qtdRegistrosPagina", 5)
        .when()
            .get("/v1/simulacoes")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("pagina", equalTo(1))
            .body("qtdRegistrosPagina", equalTo(5));
    }

    @Test
    @DisplayName("Deve retornar erro 400 para página inválida (zero)")
    void testListarSimulacoes_InvalidPage() {
        given()
            .queryParam("pagina", 0)
            .queryParam("qtdRegistrosPagina", 10)
        .when()
            .get("/v1/simulacoes")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("codigo", anyOf(equalTo("VALIDATION_ERROR"), equalTo("VALIDACAO_GENERICA")))
            .body("mensagem", containsString("maior ou igual a 1"));
    }

    @Test
    @DisplayName("Deve retornar erro 400 para quantidade de registros inválida")
    void testListarSimulacoes_InvalidPageSize() {
        given()
            .queryParam("pagina", 1)
            .queryParam("qtdRegistrosPagina", -1)
        .when()
            .get("/v1/simulacoes")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("codigo", anyOf(equalTo("VALIDATION_ERROR"), equalTo("VALIDACAO_GENERICA")))
            .body("mensagem", containsString("maior que zero"));
    }

    @Test
    @DisplayName("Deve retornar erro 400 para quantidade de registros acima do limite")
    void testListarSimulacoes_ExceedMaxPageSize() {
        given()
            .queryParam("pagina", 1)
            .queryParam("qtdRegistrosPagina", 101)
        .when()
            .get("/v1/simulacoes")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("codigo", anyOf(equalTo("VALIDATION_ERROR"), equalTo("VALIDACAO_GENERICA")))
            .body("mensagem", containsString("100"));
    }

    // TESTES PARA BUSCA POR PRODUTO E DATA

    @Test
    @DisplayName("Deve buscar simulações por produto e data - sem parâmetros")
    void testBuscarSimulacoesPorProdutoDia_SemParametros() {
        given()
        .when()
            .get("/v1/simulacoes/por-produto-dia")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("dataReferencia", notNullValue())
            .body("produtos", notNullValue())
            .body("produtos", isA(java.util.List.class));
    }

    @Test
    @DisplayName("Deve buscar simulações por produto e data - apenas com data")
    void testBuscarSimulacoesPorProdutoDia_ApenasData() {
        given()
            .queryParam("data", "2024-01-15")
        .when()
            .get("/v1/simulacoes/por-produto-dia")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("dataReferencia", equalTo("2024-01-15"))
            .body("produtos", notNullValue());
    }

    @Test
    @DisplayName("Deve buscar simulações por produto e data - apenas com produtoId")
    void testBuscarSimulacoesPorProdutoDia_ApenasProduto() {
        given()
            .queryParam("produtoId", 1)
        .when()
            .get("/v1/simulacoes/por-produto-dia")
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(404))) // 404 se produto não existir
            .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("Deve buscar simulações por produto e data - com ambos os parâmetros")
    void testBuscarSimulacoesPorProdutoDia_DataEProduto() {
        given()
            .queryParam("data", "2024-01-15")
            .queryParam("produtoId", 1)
        .when()
            .get("/v1/simulacoes/por-produto-dia")
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(404)))
            .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("Deve retornar erro 400 para data inválida")
    void testBuscarSimulacoesPorProdutoDia_DataInvalida() {
        given()
            .queryParam("data", "data-invalida")
        .when()
            .get("/v1/simulacoes/por-produto-dia")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("codigo", anyOf(equalTo("VALIDATION_ERROR"), equalTo("VALIDACAO_GENERICA")))
            .body("mensagem", anyOf(
                containsString("inválida"),
                containsString("formato"),
                containsString("YYYY-MM-DD")
            ));
    }

    @Test
    @DisplayName("Deve retornar erro 400 para produtoId inválido (negativo)")
    void testBuscarSimulacoesPorProdutoDia_ProdutoIdInvalido() {
        given()
            .queryParam("produtoId", -1)
        .when()
            .get("/v1/simulacoes/por-produto-dia")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("codigo", anyOf(equalTo("VALIDATION_ERROR"), equalTo("VALIDACAO_GENERICA")))
            .body("mensagem", containsString("positivo"));
    }

    @Test
    @DisplayName("Deve retornar erro 400 para produtoId inválido (zero)")
    void testBuscarSimulacoesPorProdutoDia_ProdutoIdZero() {
        given()
            .queryParam("produtoId", 0)
        .when()
            .get("/v1/simulacoes/por-produto-dia")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("codigo", anyOf(equalTo("VALIDATION_ERROR"), equalTo("VALIDACAO_GENERICA")))
            .body("mensagem", containsString("positivo"));
    }

    @Test
    @DisplayName("Deve retornar 404 para produto não encontrado")
    void testBuscarSimulacoesPorProdutoDia_ProdutoNaoEncontrado() {
        given()
            .queryParam("produtoId", 99999) // ID que provavelmente não existe
        .when()
            .get("/v1/simulacoes/por-produto-dia")
        .then()
            .statusCode(404)
            .contentType(ContentType.JSON)
            .body("codigo", anyOf(equalTo("PRODUCT_NOT_FOUND"), equalTo("PRODUTO_NAO_ENCONTRADO")))
            .body("mensagem", containsString("não encontrado"));
    }

    // TESTES DE INTEGRAÇÃO E CENÁRIOS COMPLEXOS

    @Test
    @DisplayName("Deve criar simulação e depois listá-la")
    void testCriarSimulacao_EntaoListar() {
        // Primeiro cria uma simulação
        given()
            .contentType(ContentType.JSON)
            .body("{" +
                    "\"valorDesejado\": 900.00, " +
                    "\"prazo\": 5" +
                    "}")
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(200)
            .body("idSimulacao", notNullValue());

        // Depois verifica se pode listar
        given()
        .when()
            .get("/v1/simulacoes")
        .then()
            .statusCode(200)
            .body("registros", notNullValue());
    }

    @Test
    @DisplayName("Deve validar comportamento sem Content-Type")
    void testCriarSimulacao_SemContentType() {
        given()
            .body("{" +
                    "\"valorDesejado\": 10000.00, " +
                    "\"prazo\": 12" +
                    "}")
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(anyOf(equalTo(415), equalTo(500))); // Pode ser 415 ou 500 dependendo da implementação
    }

    @Test
    @DisplayName("Deve retornar erro 405 para método não permitido")
    void testMetodoNaoPermitido() {
        given()
        .when()
            .delete("/v1/simulacoes")
        .then()
            .statusCode(405)
            .body("codigo", anyOf(equalTo("METHOD_NOT_ALLOWED"), equalTo("INTERNAL_ERROR")));
    }

    // TESTES DE VALIDAÇÃO DE ESTRUTURA DE RESPOSTA

    @Test
    @DisplayName("Deve validar estrutura da resposta de simulação")
    void testValidarEstruturaResposta() {
        given()
            .contentType(ContentType.JSON)
            .body("{" +
                    "\"valorDesejado\": 20000.00, " +
                    "\"prazo\": 36" +
                    "}")
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("idSimulacao", notNullValue())
            .body("codigoProduto", notNullValue())
            .body("descricaoProduto", notNullValue())
            .body("taxaJuros", notNullValue())
            .body("taxaJuros", greaterThan(0.0f))
            .body("resultadoSimulacao", notNullValue())
            .body("resultadoSimulacao", isA(java.util.List.class))
            .body("resultadoSimulacao.size()", greaterThan(0))
            .body("resultadoSimulacao[0].tipo", oneOf("SAC", "PRICE"))
            .body("resultadoSimulacao[0].parcelas", notNullValue())
            .body("resultadoSimulacao[0].parcelas", isA(java.util.List.class));
    }

    @Test
    @DisplayName("Deve validar diferentes valores de simulação")
    void testDiferentesValoresSimulacao() {
        // Testa com valor baixo
        given()
            .contentType(ContentType.JSON)
            .body("{\"valorDesejado\": 1000000.00, \"prazo\": 95}")
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(200)
            .body("idSimulacao", notNullValue());

    }

    @Test
    @DisplayName("Deve retornar status 200 com mensagem informativa quando não encontrar produto ideal")
    void testCriarSimulacao_SemProdutoIdeal() {
        // Testando com valores que provavelmente não têm produtos elegíveis
        given()
            .contentType(ContentType.JSON)
            .body("{" +
                    "\"valorDesejado\": 999999999.00, " +
                    "\"prazo\": 1000" +
                    "}")
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("sucesso", equalTo(false))
            .body("mensagem", containsString("Não foi possível encontrar um produto ideal"))
            .body("mensagem", containsString("999999999"))
            .body("mensagem", containsString("1000 meses"))
            .body("resultadoSimulacao", hasSize(0))
            .body("idSimulacao", nullValue())
            .body("codigoProduto", nullValue());
    }

    @Test
    @DisplayName("Deve retornar status 200 com mensagem de sucesso quando encontrar produto ideal")
    void testCriarSimulacao_ComProdutoIdeal() {
        given()
            .contentType(ContentType.JSON)
            .body("{" +
                    "\"valorDesejado\": 10000.00, " +
                    "\"prazo\": 12" +
                    "}")
        .when()
            .post("/v1/simulacoes")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("sucesso", equalTo(true))
            .body("mensagem", equalTo("Simulação realizada com sucesso. Produto ideal encontrado."))
            .body("idSimulacao", notNullValue())
            .body("codigoProduto", notNullValue())
            .body("descricaoProduto", notNullValue())
            .body("taxaJuros", notNullValue())
            .body("resultadoSimulacao", notNullValue())
            .body("resultadoSimulacao", isA(java.util.List.class))
            .body("resultadoSimulacao.size()", greaterThan(0));
    }
}
