package mapper;

import domain.dto.simulacao.por_produto_dia.response.SimulacaoPorProdutoDiaDTO;
import domain.entity.local.Simulacao;
import domain.entity.remote.Produto;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Mapper para agregações de dados por produto
 */
@ApplicationScoped
public class ProdutoAggregationMapper {

    /**
     * Constrói DTO agregado para um produto específico com base em múltiplas simulações
     */
    public SimulacaoPorProdutoDiaDTO toAggregatedSimulacaoPorProdutoDiaDTO(Produto produto, List<Simulacao> simulacoesProduto) {
        if (simulacoesProduto.isEmpty()) {
            return null;
        }

        var dto = new SimulacaoPorProdutoDiaDTO();
        dto.setCodigoProduto(produto.getCoProduto());
        dto.setDescricaoProduto(produto.getNoProduto());

        // Extrai listas de valores para agregação
        var taxasJuros = extractTaxasJuros(simulacoesProduto);
        var valoresPrestacao = extractValoresPrestacao(simulacoesProduto);
        var valoresDesejados = extractValoresDesejados(simulacoesProduto);
        var valoresCredito = extractValoresCredito(simulacoesProduto);

        // Aplica agregações
        setTaxaMediaJuro(dto, taxasJuros);
        setValorMedioPrestacao(dto, valoresPrestacao);
        setValorTotalDesejado(dto, valoresDesejados);
        setValorTotalCredito(dto, valoresCredito);

        return dto;
    }

    private List<BigDecimal> extractTaxasJuros(List<Simulacao> simulacoes) {
        return simulacoes.stream()
            .map(Simulacao::getTaxaMediaJuros)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<BigDecimal> extractValoresPrestacao(List<Simulacao> simulacoes) {
        return simulacoes.stream()
            .map(Simulacao::getValorMedioPrestacao)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<BigDecimal> extractValoresDesejados(List<Simulacao> simulacoes) {
        return simulacoes.stream()
            .map(Simulacao::getValorDesejado)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<BigDecimal> extractValoresCredito(List<Simulacao> simulacoes) {
        return simulacoes.stream()
            .map(Simulacao::getValorTotalCredito)
            .filter(Objects::nonNull)
            .toList();
    }

    private void setTaxaMediaJuro(SimulacaoPorProdutoDiaDTO dto, List<BigDecimal> taxasJuros) {
        if (!taxasJuros.isEmpty()) {
            var somaJuros = taxasJuros.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTaxaMediaJuro(somaJuros.divide(BigDecimal.valueOf(taxasJuros.size()), 4, RoundingMode.HALF_UP).doubleValue());
        }
    }

    private void setValorMedioPrestacao(SimulacaoPorProdutoDiaDTO dto, List<BigDecimal> valoresPrestacao) {
        if (!valoresPrestacao.isEmpty()) {
            var somaPrestacoes = valoresPrestacao.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setValorMedioPrestacao(somaPrestacoes.divide(BigDecimal.valueOf(valoresPrestacao.size()), 2, RoundingMode.HALF_UP).doubleValue());
        }
    }

    private void setValorTotalDesejado(SimulacaoPorProdutoDiaDTO dto, List<BigDecimal> valoresDesejados) {
        if (!valoresDesejados.isEmpty()) {
            var totalDesejado = valoresDesejados.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setValorTotalDesejado(totalDesejado.doubleValue());
        }
    }

    private void setValorTotalCredito(SimulacaoPorProdutoDiaDTO dto, List<BigDecimal> valoresCredito) {
        if (!valoresCredito.isEmpty()) {
            var totalCredito = valoresCredito.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setValorTotalCredito(totalCredito.doubleValue());
        }
    }
}
