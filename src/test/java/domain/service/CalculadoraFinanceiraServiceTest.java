package domain.service;

import emprestimos.v1.domain.dto.simulacao.create.request.SimulacaoCreateDTO;
import emprestimos.v1.domain.dto.simulacao.create.response.ParcelaDTO;
import emprestimos.v1.domain.dto.simulacao.create.response.ResultadoSimulacaoDTO;
import emprestimos.v1.domain.entity.remote.Produto;
import emprestimos.v1.domain.enums.TipoAmortizacao;
import emprestimos.v1.domain.exception.SimulacaoException;
import emprestimos.v1.domain.service.CalculadoraFinanceiraService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("Testes do CalculadoraFinanceiraService")
class CalculadoraFinanceiraServiceTest {

    @Inject
    CalculadoraFinanceiraService calculadoraFinanceira;

    private Produto produtoTeste;
    private SimulacaoCreateDTO simulacaoTeste;

    @BeforeEach
    void setup() {
        // Produto de teste
        produtoTeste = new Produto();
        produtoTeste.setCoProduto(1);
        produtoTeste.setNoProduto("Crédito Pessoal Teste");
        produtoTeste.setPcTaxaJuros(BigDecimal.valueOf(0.12)); // 12% ao ano

        // Simulação de teste
        simulacaoTeste = new SimulacaoCreateDTO();
        simulacaoTeste.setValorDesejado(BigDecimal.valueOf(10000.0));
        simulacaoTeste.setPrazo(12);
    }

    @Test
    @DisplayName("Deve calcular corretamente o resultado SAC com injeção CDI")
    void deveCalcularResultadoSAC() {
        // When
        ResultadoSimulacaoDTO resultado = calculadoraFinanceira.calcularResultado(
            simulacaoTeste, produtoTeste, TipoAmortizacao.SAC.getCodigo()
        );

        // Then
        assertNotNull(resultado);
        assertEquals(TipoAmortizacao.SAC.getCodigo(), resultado.getTipo());
        assertEquals(12, resultado.getParcelas().size());

        // SAC = amortização constante + juros decrescentes
        List<ParcelaDTO> parcelas = resultado.getParcelas();
        BigDecimal primeiraParcela = parcelas.get(0).getValorPrestacao();
        BigDecimal ultimaParcela = parcelas.get(11).getValorPrestacao();

        assertTrue(primeiraParcela.compareTo(ultimaParcela) > 0,
            "No SAC, primeira parcela deve ser maior que a última");
    }

    @Test
    @DisplayName("Deve calcular corretamente o resultado PRICE com injeção CDI")
    void deveCalcularResultadoPrice() {
        // When
        ResultadoSimulacaoDTO resultado = calculadoraFinanceira.calcularResultado(
            simulacaoTeste, produtoTeste, TipoAmortizacao.PRICE.getCodigo()
        );

        // Then
        assertNotNull(resultado);
        assertEquals(TipoAmortizacao.PRICE.getCodigo(), resultado.getTipo());
        assertEquals(12, resultado.getParcelas().size());

        // PRICE = prestações fixas (pequena variação por arredondamento)
        List<ParcelaDTO> parcelas = resultado.getParcelas();
        BigDecimal primeiraPrestacao = parcelas.get(0).getValorPrestacao();
        BigDecimal ultimaPrestacao = parcelas.get(11).getValorPrestacao();

        BigDecimal diferenca = primeiraPrestacao.subtract(ultimaPrestacao).abs();
        assertTrue(diferenca.compareTo(BigDecimal.ONE) <= 0,
            "No PRICE, diferença entre parcelas deve ser mínima");
    }

    @Test
    @DisplayName("Deve lançar SimulacaoException para tipo de amortização inválido")
    void deveLancarExcecaoParaTipoAmortizacaoInvalido() {
        // When & Then
        SimulacaoException exception = assertThrows(SimulacaoException.class, () ->
            calculadoraFinanceira.calcularResultado(simulacaoTeste, produtoTeste, "INVALID_TYPE")
        );

        assertEquals("Tipo de amortização 'INVALID_TYPE' não é suportado. Use 'SAC' ou 'PRICE'", exception.getDetalhe());
        assertTrue(exception.getMessage().contains("INVALID_TYPE"));
    }

    @Test
    @DisplayName("Deve calcular valores corretos das parcelas")
    void deveCalcularValoresCorretosDasParcelas() {
        // Given
        ResultadoSimulacaoDTO resultado = calculadoraFinanceira.calcularResultado(
            simulacaoTeste, produtoTeste, TipoAmortizacao.PRICE.getCodigo()
        );

        // When
        BigDecimal valorTotal = calculadoraFinanceira.calcularValorTotalParcelas(resultado.getParcelas());
        BigDecimal valorMedio = calculadoraFinanceira.calcularValorMedioPrestacao(resultado.getParcelas());

        // Then
        assertNotNull(valorTotal);
        assertNotNull(valorMedio);

        // Valor total deve ser maior que o financiado (devido aos juros)
        assertTrue(valorTotal.compareTo(BigDecimal.valueOf(10000)) > 0);

        // Valor médio deve ser positivo e razoável
        assertTrue(valorMedio.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(valorMedio.compareTo(BigDecimal.valueOf(2000)) < 0);
    }

    @Test
    @DisplayName("Deve validar estrutura das parcelas geradas")
    void deveValidarEstruturaDasParcelasGeradas() {
        // When
        ResultadoSimulacaoDTO resultado = calculadoraFinanceira.calcularResultado(
            simulacaoTeste, produtoTeste, TipoAmortizacao.SAC.getCodigo()
        );

        // Then
        List<ParcelaDTO> parcelas = resultado.getParcelas();

        for (int i = 0; i < parcelas.size(); i++) {
            ParcelaDTO parcela = parcelas.get(i);

            // Validações básicas de estrutura
            assertNotNull(parcela.getNumero(), "Número da parcela não deve ser nulo");
            assertNotNull(parcela.getValorPrestacao(), "Valor da prestação não deve ser nulo");
            assertNotNull(parcela.getValorJuros(), "Valor dos juros não deve ser nulo");
            assertNotNull(parcela.getValorAmortizacao(), "Valor da amortização não deve ser nulo");

            // Validações de valores
            assertEquals(i + 1, parcela.getNumero(), "Número da parcela incorreto");
            assertTrue(parcela.getValorPrestacao().compareTo(BigDecimal.ZERO) > 0, "Valor da prestação deve ser positivo");
            assertTrue(parcela.getValorJuros().compareTo(BigDecimal.ZERO) >= 0, "Valor dos juros deve ser não-negativo");
            assertTrue(parcela.getValorAmortizacao().compareTo(BigDecimal.ZERO) > 0, "Valor da amortização deve ser positivo");
        }
    }
}
