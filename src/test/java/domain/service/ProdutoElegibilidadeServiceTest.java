package domain.service;

import emprestimos.v1.domain.entity.remote.Produto;
import emprestimos.v1.domain.exception.ProdutoException;
import emprestimos.v1.domain.service.ProdutoElegibilidadeService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("Testes do ProdutoElegibilidadeService - Quarkus")
class ProdutoElegibilidadeServiceTest {

    @Inject
    ProdutoElegibilidadeService produtoElegibilidade;

    private List<Produto> produtosDisponiveis;
    private BigDecimal valorTeste;
    private int prazoTeste;

    @BeforeEach
    void setup() {
        valorTeste = BigDecimal.valueOf(15000.0);
        prazoTeste = 12;

        // Produtos de teste
        Produto produto1 = new Produto();
        produto1.setCoProduto(1);
        produto1.setNoProduto("Crédito Pessoal");
        produto1.setPcTaxaJuros(BigDecimal.valueOf(0.15)); // 15% ao ano
        produto1.setNuMinimoMeses((short) 6);
        produto1.setNuMaximoMeses((short) 24);
        produto1.setVrMinimo(BigDecimal.valueOf(5000));
        produto1.setVrMaximo(BigDecimal.valueOf(50000));

        Produto produto2 = new Produto();
        produto2.setCoProduto(2);
        produto2.setNoProduto("Crédito Consignado");
        produto2.setPcTaxaJuros(BigDecimal.valueOf(0.10)); // 10% ao ano (menor taxa)
        produto2.setNuMinimoMeses((short) 12);
        produto2.setNuMaximoMeses((short) 48);
        produto2.setVrMinimo(BigDecimal.valueOf(10000));
        produto2.setVrMaximo(BigDecimal.valueOf(100000));

        Produto produto3 = new Produto();
        produto3.setCoProduto(3);
        produto3.setNoProduto("Crédito Empresarial");
        produto3.setPcTaxaJuros(BigDecimal.valueOf(0.12)); // 12% ao ano
        produto3.setNuMinimoMeses((short) 24);
        produto3.setNuMaximoMeses((short) 60);
        produto3.setVrMinimo(BigDecimal.valueOf(50000)); // Valor mínimo alto - não elegível
        produto3.setVrMaximo(BigDecimal.valueOf(500000));

        produtosDisponiveis = Arrays.asList(produto1, produto2, produto3);
    }

    @Test
    @DisplayName("Deve filtrar apenas produtos elegíveis")
    void deveFiltrarApenasProdutosElegiveis() {
        // When
        List<Produto> produtosElegiveis = produtoElegibilidade.filtrarProdutosElegiveis(
            produtosDisponiveis, valorTeste, prazoTeste
        );

        // Then
        assertEquals(2, produtosElegiveis.size());

        // Produto 1 e 2 devem estar elegíveis
        assertTrue(produtosElegiveis.stream().anyMatch(p -> p.getCoProduto().equals(1)));
        assertTrue(produtosElegiveis.stream().anyMatch(p -> p.getCoProduto().equals(2)));

        // Produto 3 não deve estar elegível (valor mínimo muito alto)
        assertFalse(produtosElegiveis.stream().anyMatch(p -> p.getCoProduto().equals(3)));
    }

    @Test
    @DisplayName("Deve encontrar o melhor produto (menor taxa de juros)")
    void deveEncontrarMelhorProduto() {
        // When
        Optional<Produto> melhorProdutoOpt = produtoElegibilidade.encontrarMelhorProdutoOptional(
            produtosDisponiveis, valorTeste, prazoTeste
        );

        // Then
        assertTrue(melhorProdutoOpt.isPresent());
        Produto melhorProduto = melhorProdutoOpt.get();
        assertEquals(2, melhorProduto.getCoProduto()); // Produto 2 tem menor taxa (10%)
        assertEquals(BigDecimal.valueOf(0.10), melhorProduto.getPcTaxaJuros());
    }

    @Test
    @DisplayName("Deve lançar exceção quando não há produtos disponíveis")
    void deveLancarExcecaoQuandoNaoHaProdutosDisponiveis() {
        // When & Then
        ProdutoException exception = assertThrows(ProdutoException.class, () ->
            produtoElegibilidade.encontrarMelhorProdutoOptional(Collections.emptyList(), valorTeste, prazoTeste)
        );

        assertEquals("PRODUCTS_NOT_ELIGIBLE", exception.getCodigo());
    }

    @Test
    @DisplayName("Deve lançar exceção quando não há produtos elegíveis")
    void deveLancarExcecaoQuandoNaoHaProdutosElegiveis() {
        // Given - valor muito alto que nenhum produto atende
        BigDecimal valorMuitoAlto = BigDecimal.valueOf(1000000.0);

        // When & Then
        ProdutoException exception = assertThrows(ProdutoException.class, () ->
            produtoElegibilidade.encontrarMelhorProdutoOptional(produtosDisponiveis, valorMuitoAlto, prazoTeste)
        );

        assertEquals("PRODUCTS_NOT_ELIGIBLE", exception.getCodigo());
        assertTrue(exception.getDetalhe().contains("1000000"));
        assertTrue(exception.getDetalhe().contains("12"));
    }

    @Test
    @DisplayName("Deve verificar elegibilidade corretamente por valor")
    void deveVerificarElegibilidadePorValor() {
        Produto produto = produtosDisponiveis.get(0); // Produto 1: 5000-50000

        // Valor dentro do limite
        assertTrue(produtoElegibilidade.isProdutoElegivel(produto, BigDecimal.valueOf(25000), 12));

        // Valor no limite mínimo
        assertTrue(produtoElegibilidade.isProdutoElegivel(produto, BigDecimal.valueOf(5000), 12));

        // Valor no limite máximo
        assertTrue(produtoElegibilidade.isProdutoElegivel(produto, BigDecimal.valueOf(50000), 12));

        // Valor abaixo do mínimo
        assertFalse(produtoElegibilidade.isProdutoElegivel(produto, BigDecimal.valueOf(3000), 12));

        // Valor acima do máximo
        assertFalse(produtoElegibilidade.isProdutoElegivel(produto, BigDecimal.valueOf(60000), 12));
    }

    @Test
    @DisplayName("Deve verificar elegibilidade corretamente por prazo")
    void deveVerificarElegibilidadePorPrazo() {
        Produto produto = produtosDisponiveis.get(0); // Produto 1: 6-24 meses

        // Prazo dentro do limite
        assertTrue(produtoElegibilidade.isProdutoElegivel(produto, valorTeste, 12));

        // Prazo no limite mínimo
        assertTrue(produtoElegibilidade.isProdutoElegivel(produto, valorTeste, 6));

        // Prazo no limite máximo
        assertTrue(produtoElegibilidade.isProdutoElegivel(produto, valorTeste, 24));

        // Prazo abaixo do mínimo
        assertFalse(produtoElegibilidade.isProdutoElegivel(produto, valorTeste, 3));

        // Prazo acima do máximo
        assertFalse(produtoElegibilidade.isProdutoElegivel(produto, valorTeste, 36));
    }

    @Test
    @DisplayName("Deve encontrar produto por simulação existente")
    void deveEncontrarProdutoPorSimulacaoExistente() {
        // When
        Optional<Produto> produto = produtoElegibilidade.encontrarProdutoPorSimulacao(
            produtosDisponiveis, valorTeste, prazoTeste
        );

        // Then
        assertTrue(produto.isPresent());
        assertEquals(2, produto.get().getCoProduto()); // Melhor produto (menor taxa)
    }

    @Test
    @DisplayName("Deve retornar empty quando não encontra produto por simulação")
    void deveRetornarEmptyQuandoNaoEncontraProdutoPorSimulacao() {
        // Given - valor que não atende nenhum produto
        BigDecimal valorInvalido = BigDecimal.valueOf(1000000.0);

        // When
        Optional<Produto> produto = produtoElegibilidade.encontrarProdutoPorSimulacao(
            produtosDisponiveis, valorInvalido, prazoTeste
        );

        // Then
        assertTrue(produto.isEmpty());
    }

    @Test
    @DisplayName("Deve tratar lista nula de produtos")
    void deveTratarListaNulaDeProdutos() {
        // When & Then
        ProdutoException exception = assertThrows(ProdutoException.class, () ->
            produtoElegibilidade.encontrarMelhorProdutoOptional(null, valorTeste, prazoTeste)
        );

        assertEquals("PRODUCTS_NOT_ELIGIBLE", exception.getCodigo());
    }
}
